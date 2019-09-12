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
package fr.cnes.regards.modules.ingest.dao;

import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPIdNProcessing;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@link SIPEntity} repository
 *
 * @author Marc Sordi
 *
 */
public interface ISIPRepository extends JpaRepository<SIPEntity, Long>, JpaSpecificationExecutor<SIPEntity> {

    @Override
    Optional<SIPEntity> findById(Long id);

    /**
     * Retrieve all {@link SIPEntity} for the given ids
     * @param ids
     * @return {@link SIPEntity}s
     */
    Set<SIPEntity> findByIdIn(Collection<Long> ids);

    /**
     * Find last ingest SIP with specified SIP ID according to ingest date
     * @param providerId external SIP identifier
     * @return the latest registered SIP
     */
    SIPEntity findTopByProviderIdOrderByCreationDateDesc(String providerId);

    /**
     * Find all SIP version of a provider id
     * @param providerId provider id
     * @return all SIP versions of this provider id
     */
    Collection<SIPEntity> findAllByProviderIdOrderByVersionAsc(String providerId);

    /**
     * Count SIPEntity with given providerId that have one the given states
     */
    long countByProviderIdAndStateIn(String providerId, Collection<SIPState> states);

    default long countByProviderIdAndStateIn(String providerId, SIPState... states) {
        return countByProviderIdAndStateIn(providerId, Arrays.asList(states));
    }

    /**
     * Find all {@link SIPEntity}s by given {@link SIPState}
     * @param state {@link SIPState}
     * @return {@link SIPEntity}s
     */
    Collection<SIPEntity> findAllByState(SIPState state);

    /**
     * Find one {@link SIPEntity} by its unique ipId
     */
    Optional<SIPEntity> findOneBySipId(String sipId);

    /**
     * Retrieve all {@link SIPEntity} for the given ipIds
     * @param sipIds
     * @return {@link SIPEntity}s
     */
    Collection<SIPEntity> findBySipIdIn(Collection<String> sipIds);

    /**
     * Retrieve all {@link SIPEntity} associated to the given session source/name.
     * @param sessionOwner {@link String}
     * @param session {@link String}
     * @return {@link SIPEntity}s
     */
    Collection<SIPEntity> findByIngestMetadataSessionOwnerAndIngestMetadataSession(String sessionOwner, String session);

    /**
     * Find all {@link SIPEntity}s by given {@link SIPState}.
     * Unlike findAllByState, the possibly huge parameter rawSip is not loaded.
     * @param state {@link SIPState}
     * @return {@link SIPEntity}s
     */
    List<SIPIdNProcessing> findIdAndIngestMetadataByState(SIPState state);

    /**
     * Update state of a {@link SIPEntity}
     * @param state new {@link SIPState}
     * @param id id of the {@link SIPEntity} to update
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE SIPEntity s set s.state = :state where s.id = :id")
    void updateSIPEntityState(@Param("state") SIPState state, @Param("id") Long id);

    /**
     * Update state for a set of {@link SIPEntity}
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE SIPEntity s set s.state = :state where s.id in (:ids)")
    void updateSIPEntitiesState(@Param("state") SIPState state, @Param("ids") Collection<Long> ids);

    /**
     * Switch state for a given session
     */
    //TODO add session
    @Modifying
    @Query("UPDATE SIPEntity s set s.state = :newState where s.state = :state AND s.ingestMetadata.sessionOwner = :session")
    void updateSIPEntityStateByStateAndSession(@Param("newState") SIPState state, @Param("state") SIPState filterState,
            @Param("session") String session);

    // FIXME
    //    /**
    //     * Count number of {@link SIPEntity} associated to a given session
    //     * @param Cl
    //     * @return number of {@link SIPEntity}
    //     */
    //    //TODO
    //    long countByIngestMetadataSessionSource(String sessionOwner);
    //
    //    /**
    //     * Count number of {@link SIPEntity} associated to a given session and in a specific given {@link SIPState}
    //     * @param sessionId
    //     * @return number of {@link SIPEntity}
    //     */
    //    //TODO add session name
    //    long countByIngestMetadataSessionSourceAndStateIn(String sessionId, Collection<SIPState> states);

    /**
     * Check if SIP already ingested
     * @param checksum checksum
     * @return 0 or 1
     */
    Long countByChecksum(String checksum);

    long countByState(SIPState sipState);

    /**
     * Get next version of the SIP identified by provider id
     * @param providerId provider id
     * @return next version
     */
    default Integer getNextVersion(String providerId) {
        SIPEntity latest = findTopByProviderIdOrderByCreationDateDesc(providerId);
        return latest == null ? 1 : latest.getVersion() + 1;
    }

    /**
     * Find all SIP version of a provider id
     * @param providerId prodiver id
     * @return all SIP versions of a provider id
     */
    default Collection<SIPEntity> getAllVersions(String providerId) {
        return findAllByProviderIdOrderByVersionAsc(providerId);
    }

    /**
     * Check if SIP is already ingested based on its checksum
     */
    default boolean isAlreadyIngested(String checksum) {
        return countByChecksum(checksum) != 0;
    }

    Page<SIPEntity> findPageByState(SIPState state, Pageable pageable);

    default Page<SIPEntity> loadAll(Specification<SIPEntity> search, Pageable pageable) {
        // as a Specification is used to constrain the page, we cannot simply ask for ids with a query
        // to mimic that, we are querying without any entity graph to extract ids
        Page<SIPEntity> sips = findAll(search, pageable);
        List<Long> sipIds = sips.stream().map(p -> p.getId()).collect(Collectors.toList());
        // now that we have the ids, lets load the products and keep the same sort
        List<SIPEntity> loaded = findAllByIdIn(sipIds, pageable.getSort());
        return new PageImpl<>(loaded, PageRequest.of(sips.getNumber(), sips.getSize(), sips.getSort()),
                sips.getTotalElements());
    }

    List<SIPEntity> findAllByIdIn(List<Long> ingestProcChainIds, Sort sort);

}