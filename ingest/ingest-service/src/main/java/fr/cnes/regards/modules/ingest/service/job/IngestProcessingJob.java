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
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.step.IProcessingStep;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.aip.AIP;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.entity.request.RequestState;
import fr.cnes.regards.modules.ingest.service.chain.step.GenerationStep;
import fr.cnes.regards.modules.ingest.service.chain.step.InternalFinalStep;
import fr.cnes.regards.modules.ingest.service.chain.step.InternalInitialStep;
import fr.cnes.regards.modules.ingest.service.chain.step.PostprocessingStep;
import fr.cnes.regards.modules.ingest.service.chain.step.PreprocessingStep;
import fr.cnes.regards.modules.ingest.service.chain.step.TaggingStep;
import fr.cnes.regards.modules.ingest.service.chain.step.ValidationStep;
import fr.cnes.regards.modules.ingest.service.request.IngestRequestPublisher;
import fr.cnes.regards.modules.ingest.service.request.IngestRequestService;

/**
 * This job manages processing chain for AIP generation from a SIP
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class IngestProcessingJob extends AbstractJob<Void> {

    public static final String CHAIN_NAME_PARAMETER = "chain";

    public static final String IDS_PARAMETER = "ids";

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IngestRequestService ingestRequestService;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private IngestRequestPublisher publisher;

    private IngestProcessingChain ingestChain;

    private List<IngestRequest> requests;

    /**
     * The request we are currently processing
     */
    private IngestRequest currentRequest;

    /**
     * The SIP entity we are currently working on
     */
    private SIPEntity currentEntity;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {

        // Retrieve processing chain from parameters
        String processingChainName = getValue(parameters, CHAIN_NAME_PARAMETER);

        // Load processing chain
        Optional<IngestProcessingChain> chain = processingChainRepository.findOneByName(processingChainName);
        if (!chain.isPresent()) {
            String message = String.format("No related chain has been found for value \"%s\"", processingChainName);
            handleInvalidParameter(CHAIN_NAME_PARAMETER, message);
        } else {
            ingestChain = chain.get();
        }

        // Load ingest requests
        Type type = new TypeToken<Set<Long>>() {

        }.getType();
        Set<Long> ids = getValue(parameters, IDS_PARAMETER, type);
        requests = ingestRequestService.getIngestRequests(ids);
    }

    @Override
    public void run() {
        // Lets prepare a fex things in case there is errors
        StringJoiner notifMsg = new StringJoiner("\n");
        notifMsg.add("Errors occurred during SIPs processing using " + ingestChain.getName() + ":");
        boolean errorOccured = false;

        // Internal initial step
        IProcessingStep<IngestRequest, SIPEntity> initStep = new InternalInitialStep(this, ingestChain);
        beanFactory.autowireBean(initStep);

        // Initializing steps
        // Step 1 : optional preprocessing
        IProcessingStep<SIP, SIP> preStep = new PreprocessingStep(this, ingestChain);
        beanFactory.autowireBean(preStep);
        // Step 2 : required validation
        IProcessingStep<SIP, Void> validationStep = new ValidationStep(this, ingestChain);
        beanFactory.autowireBean(validationStep);
        // Step 3 : required AIP generation
        IProcessingStep<SIP, List<AIP>> generationStep = new GenerationStep(this, ingestChain);
        beanFactory.autowireBean(generationStep);
        // Step 4 : optional AIP tagging
        IProcessingStep<List<AIP>, Void> taggingStep = new TaggingStep(this, ingestChain);
        beanFactory.autowireBean(taggingStep);
        // Step 5 : optional postprocessing
        IProcessingStep<SIP, Void> postprocessingStep = new PostprocessingStep(this, ingestChain);
        beanFactory.autowireBean(postprocessingStep);

        // Internal final step
        IProcessingStep<List<AIP>, Void> finalStep = new InternalFinalStep(this, ingestChain);
        beanFactory.autowireBean(finalStep);

        for (IngestRequest request : requests) {
            currentRequest = request;
            try {

                // Internal preparation step (no plugin involved)
                currentEntity = initStep.execute(request);

                // Step 1 : optional preprocessing
                SIP sip = preStep.execute(request.getSip());
                // Propagate to entity
                currentEntity.setSip(sip);

                // Step 2 : required validation
                validationStep.execute(sip);
                // Step 3 : required AIP generation
                List<AIP> aips = generationStep.execute(sip);
                // Step 4 : optional AIP tagging
                taggingStep.execute(aips);
                // Step 5 : optional postprocessing
                postprocessingStep.execute(sip);

                // Internal finalization step (no plugin involved)
                finalStep.execute(aips);

                // Clean and publish
                handleRequestSuccess();

            } catch (ProcessingStepException e) {
                errorOccured = true;
                String msg = String.format("Error while ingesting SIP \"%s\" in request \"%s\"",
                                           currentRequest.getSip().getId(), currentRequest.getRequestId());
                notifMsg.add(msg);
                super.logger.error(msg);
                super.logger.error("Ingestion step error", e);
                // Continue with following SIPs
            }
        }
        // notify if errors occured
        if (errorOccured) {
            notificationClient.notify(notifMsg.toString(), "Error occurred during SIPs Ingestion.",
                                      NotificationLevel.INFO, DefaultRole.ADMIN);
        }
    }

    /**
     * Method always called when error occurs during ingest processing
     */
    public void handleRequestError(Set<String> errors) {
        currentRequest.setState(RequestState.ERROR);
        currentRequest.setErrors(errors);
        ingestRequestService.updateIngestRequest(currentRequest);
        publisher.publishIngestRequest(currentRequest, currentEntity);
    }

    /**
     * Method always called after successful processing
     */
    private void handleRequestSuccess() {
        currentRequest.setState(RequestState.DONE);
        ingestRequestService.deleteIngestRequest(currentRequest);
        publisher.publishIngestRequest(currentRequest, currentEntity);
    }

    @Override
    public int getCompletionCount() {
        return 7;
    }

    public SIPEntity getCurrentEntity() {
        return currentEntity;
    }
}
