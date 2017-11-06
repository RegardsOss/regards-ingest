/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.ingest.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.SIPEntitySpecifications;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Service to handle access to {@link SIPEntity} entities.
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class SIPService implements ISIPService {

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Override
    public Page<SIPEntity> getSIPEntities(String sessionId, String owner, OffsetDateTime from, SIPState state,
            Pageable page) {
        return sipRepository.findAll(SIPEntitySpecifications.search(sessionId, owner, from, state), page);
    }

    @Override
    public SIPEntity getSIPEntity(String ipId) throws ModuleException {
        Optional<SIPEntity> sipEntity = sipRepository.findOneByIpId(ipId);
        if (sipEntity.isPresent()) {
            return sipEntity.get();
        } else {
            throw new EntityNotFoundException(ipId, SIPEntity.class);
        }
    }

    @Override
    public void deleteSIPEntity(String ipId) throws ModuleException {
        Optional<SIPEntity> oSip = sipRepository.findOneByIpId(ipId);
        if (!oSip.isPresent()) {
            throw new EntityNotFoundException(ipId, SIPEntity.class);
        } else {
            if (isDeletable(ipId)) {
                SIPEntity sip = oSip.get();
                // Do delete associated aips
                Set<AIPEntity> aips = aipRepository.findBySip(sip);
                aipRepository.delete(aips);
                // Change SIP state to DELETING
                sip.setState(SIPState.DELETED);
                sip.setLastUpdateDate(OffsetDateTime.now());
                sipRepository.save(sip);
            } else {
                throw new EntityOperationForbiddenException(ipId, SIPEntity.class,
                        String.format("SIPEntity with state %s are not deletable", oSip.get().getState()));
            }
        }
    }

    @Override
    public Collection<SIPEntity> getAllVersions(String sipId) {
        return sipRepository.getAllVersions(sipId);
    }

    @Override
    public Boolean isDeletable(String ipId) throws EntityNotFoundException {
        Optional<SIPEntity> os = sipRepository.findOneByIpId(ipId);
        if (os.isPresent()) {
            switch (os.get().getState()) {
                case CREATED:
                case AIP_CREATED:
                case INVALID:
                case AIP_GEN_ERROR:
                case REJECTED:
                case STORED:
                case STORE_ERROR:
                    return true;
                default:
                    return false;
            }
        } else {
            throw new EntityNotFoundException(ipId, SIPEntity.class);
        }
    }

    @Override
    public Boolean isRetryable(String ipId) throws EntityNotFoundException {
        Optional<SIPEntity> os = sipRepository.findOneByIpId(ipId);
        if (os.isPresent()) {
            switch (os.get().getState()) {
                case INVALID:
                case AIP_GEN_ERROR:
                case REJECTED:
                case DELETED:
                    return true;
                default:
                    return false;
            }
        } else {
            throw new EntityNotFoundException(ipId, SIPEntity.class);
        }
    }

}
