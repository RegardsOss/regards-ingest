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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.storagelight.domain.dto.request.RequestResultInfoDTO;
import java.util.Collection;
import java.util.List;

/**
 * Manage AIP storage
 * @author Léo Mieulet
 */
public interface IAIPStorageService {

    /**
     * Store AIPs Files
     * @param aips
     * @return file storage event group_id list
     * @throws ModuleException
     */
    List<String> storeAIPFiles(List<AIPEntity> aips) throws ModuleException;

    /**
     * Store AIPs
     * @param aips
     * @return group id
     * @throws ModuleException
     */
    String storeAIPs(List<AIPEntity> aips) throws ModuleException;

    /**
     * Update provided {@link AIPEntity} aips with updated metadata from storage
     * @param aips to update
     * @param storeRequestInfos storage result
     */
    void updateAIPsUsingStorageResult(List<AIPEntity> aips, Collection<RequestResultInfoDTO> storeRequestInfos);

}
