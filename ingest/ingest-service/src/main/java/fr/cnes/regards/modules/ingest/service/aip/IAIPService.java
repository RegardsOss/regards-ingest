/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.aip;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.dto.RejectedAipDto;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;

/**
 * AIP Service interface. Service to handle business around {@link AIPEntity}s
 * @author Sébastien Binda
 */
public interface IAIPService {

    /**
     * Create and save {@link AIPEntity} from list of {@link AIP}
     * @param sip linked {@link SIPEntity}
     * @param aips list of {@link AIP}
     * @return list of related {@link AIPEntity}
     */
    List<AIPEntity> createAndSave(SIPEntity sip, List<AIP> aips);

    /**
     * Delete the {@link AIPEntity} by his ipId
     */
    Collection<RejectedAipDto> deleteAip(String sipId);

    /**
     * Set {@link AIPEntity} state to give none
     */
    void setAipToStored(UniformResourceName aipId, AIPState state);

    /**
     * Search for a {@link AIPEntity} by its ipId
     */
    Optional<AIPEntity> searchAip(UniformResourceName aipId);

    /**
     * Save AIP
     */
    AIPEntity save(AIPEntity entity);

    /**
     * Look for sips in state {@link fr.cnes.regards.modules.ingest.domain.sip.SIPState#TO_BE_DELETED} and
     * ask to rs-storage to delete them per page of 100.
     */
    void askForAipsDeletion();
}
