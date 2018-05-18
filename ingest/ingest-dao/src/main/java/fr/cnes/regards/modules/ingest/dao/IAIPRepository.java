/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dao;

import java.util.Optional;
import java.util.Set;

import javax.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SipAIPState;
import fr.cnes.regards.modules.storage.domain.IAipState;

/**
 * JPA Repository to access {@link AIPEntity}
 * @author Sébastien Binda
 *
 */
public interface IAIPRepository extends JpaRepository<AIPEntity, Long> {

    /**
     * Retrieve all {@link AIPEntity}s associated to the given {@link SIPEntity}
     * @param sip {@link SIPEntity}
     * @return {@link AIPEntity}s
     */
    Set<AIPEntity> findBySip(SIPEntity sip);

    /**
     * Retrieve all {@link AIPEntity}s associated to the given {@link SIPEntity}
     * @param sip {@link SIPEntity}
     * @return {@link AIPEntity}s
     */
    Set<AIPEntity> findBySipIpId(String sipIpId);

    /**
     * Retrieve an {@link AIPEntity} by is {@link AIPEntity#getIpId()}
     * @param ipId {@link String}
     * @return optional {@link AIPEntity}
     */
    Optional<AIPEntity> findByIpId(String ipId);

    /**
     * Retrieve an {@link AIPEntity} by is {@link AIPEntity#getState()}
     * @param state {@link SipAIPState}
     * @return optional {@link AIPEntity}
     */
    @Query("select id from AIPEntity a where a.state= ?1")
    Set<Long> findIdByState(IAipState state);

    default boolean isAlreadyWorking(String processingChain) {
        Page<AIPEntity> page = findWithLockBySipProcessingAndState(processingChain, SipAIPState.SUBMISSION_SCHEDULED,
                                                                   new PageRequest(0, 1));
        return page.hasContent();
    }

    @Lock(LockModeType.PESSIMISTIC_READ)
    Page<AIPEntity> findWithLockBySipProcessingAndState(String processingChain, IAipState state, Pageable pageable);

    Set<AIPEntity> findBySipProcessingAndState(String processingChain, IAipState state);

    /**
     * Update state of the given {@link AIPEntity}
     * @param state New state
     * @param id {@link AIPEntity} to update
     */
    @Modifying
    @Query("UPDATE AIPEntity a set a.state = ?1, a.errorMessage = ?3 where a.ipId = ?2")
    void updateAIPEntityStateAndErrorMessage(IAipState state, String ipId, String errorMessage);

}
