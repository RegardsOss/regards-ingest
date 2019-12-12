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
package fr.cnes.regards.modules.ingest.service.job;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.deletion.DeletionRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.request.OAISDeletionService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;

/**
 * Job to run deletion of a given {@link AIPEntity}.<br/>
 * <ul>
 * <li> Ask for remote file storage deletion if asked for</li>
 * <li> Delete AIP or mark it as DELETED </li>
 * <li> Delete SIP or mark it as DELETED </li>
 * </ul>
 *
 * @author Sébastien Binda
 */
public class OAISDeletionJob extends AbstractJob<Void> {

    public static final String OAIS_DELETION_REQUEST_IDS = "OAIS_DELETION_REQUEST_IDS";

    private List<OAISDeletionRequest> requests = Lists.newArrayList();

    @Autowired
    private OAISDeletionService oaisDeletionRequestService;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private ISIPService sipService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve param
        Type type = new TypeToken<List<Long>>() {
        }.getType();
        List<Long> deleteRequestIds = getValue(parameters, OAIS_DELETION_REQUEST_IDS, type);
        // Retrieve list of AIP save metadata requests to handle
        requests = oaisDeletionRequestService.searchRequests(deleteRequestIds);
    }

    @Override
    public void run() {
        for (OAISDeletionRequest request : requests) {
            AIPEntity aipToDelete = request.getAip();
            SIPEntity sipToDelete = aipToDelete.getSip();
            if (request.isDeleteFiles()) {
                String deleteRequestId = aipService.scheduleAIPEntityDeletion(sipToDelete.getSipId());
                request.setRemoteStepGroupIds(Lists.newArrayList(deleteRequestId));
                request.setStep(DeletionRequestStep.STORAGE_DELETION_REQUESTED);
                oaisDeletionRequestService.update(request);
            } else {
                aipService.processDeletion(sipToDelete.getSipId(),
                                           request.getDeletionMode() == SessionDeletionMode.IRREVOCABLY);
                sipService.processDeletion(sipToDelete.getSipId(),
                                           request.getDeletionMode() == SessionDeletionMode.IRREVOCABLY);
                oaisDeletionRequestService.deleteRequest(request);
            }
            advanceCompletion();
        }
    }

    @Override
    public int getCompletionCount() {
        return requests.size();
    }

}