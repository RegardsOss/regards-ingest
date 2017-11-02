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
package fr.cnes.regards.modules.ingest.service.chain;

import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.AIPState;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * Ingest processing service interface
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public interface IIngestProcessingService {

    /**
     * Schedule {@link IngestProcessingJob}s for all {@link SIPEntity} with {@link SIPState#CREATED} Status.
     */
    void ingest();

    /**
     * Update state of given SIPEntity
     * @param id of {@link SIPEntity} to update
     * @param newState new {@link SIPState}
     * @return updated {@link SIPEntity}
     */
    SIPEntity updateSIPEntityState(Long id, SIPState newState);

    /**
     * Return {@link SIPEntity} for the given id
     * @param id
     * @return
     */
    SIPEntity getSIPEntity(Long id);

    /**
     *
     * @param sipEntityId
     * @param aipState
     * @param aip
     * @return
     */
    AIPEntity createAIP(Long sipEntityId, AIPState aipState, AIP aip);
}