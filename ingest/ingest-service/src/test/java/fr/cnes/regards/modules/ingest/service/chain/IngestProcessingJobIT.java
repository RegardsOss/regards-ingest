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
package fr.cnes.regards.modules.ingest.service.chain;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobWorkspaceException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.entity.SipAIPState;
import fr.cnes.regards.modules.ingest.service.IIngestService;
import fr.cnes.regards.modules.ingest.service.TestConfiguration;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.plugin.AIPGenerationTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.AIPTaggingTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSingleAIPGeneration;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSipValidation;
import fr.cnes.regards.modules.ingest.service.plugin.PreprocessingTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.ValidationTestPlugin;

/**
 * Test class to verify {@link IngestProcessingJob}.
 * @author Sébastien Binda
 */
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = { TestConfiguration.class })
@RegardsTransactional
public class IngestProcessingJobIT extends AbstractRegardsServiceTransactionalIT {

    public static final String SIP_ID_TEST = "SIP_001";

    public static final String SIP_DEFAULT_CHAIN_ID_TEST = "SIP_002";

    public static final String SIP_REF_ID_TEST = "SIP_003";

    public static final String DEFAULT_PROCESSING_CHAIN_TEST = "defaultProcessingChain";

    public static final String PROCESSING_CHAIN_TEST = "fullProcessingChain";

    public static final String SESSION_ID = "sessionId";

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(IngestProcessingJobIT.class);

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private ProcessingChainTestErrorSimulator stepErrorSimulator;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    private Long entityIdTest;

    private Long sipRefIdTest;

    private Long entityDefaultChainTest;

    @Before
    public void init() throws ModuleException {

        pluginConfRepo.deleteAll();
        aipRepository.deleteAll();
        sipRepository.deleteAll();

        initFullPRocessingChain();
        initDefaultProcessingChain();

        // Init a SIP in database with state CREATED and managed with default chain
        SIPCollectionBuilder colBuilder = new SIPCollectionBuilder(DEFAULT_PROCESSING_CHAIN_TEST, SESSION_ID);
        SIPCollection collection = colBuilder.build();

        SIPBuilder builder = new SIPBuilder(SIP_DEFAULT_CHAIN_ID_TEST);
        builder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("data1.fits"), "sdsdfm1211vd");
        builder.setSyntax("FITS(FlexibleImageTransport)",
                          "http://www.iana.org/assignments/media-types/application/fits",
                          MediaType.valueOf("application/fits"));
        builder.addContentInformation();
        collection.add(builder.build());

        Collection<SIPDto> results = ingestService.ingest(collection);
        String sipId = results.stream().findFirst().get().getSipId();
        Optional<SIPEntity> resultSip = sipRepository.findOneBySipId(sipId);
        entityDefaultChainTest = resultSip.get().getId();

        // Init a SIP in database with state CREATED
        colBuilder = new SIPCollectionBuilder(PROCESSING_CHAIN_TEST, SESSION_ID);
        collection = colBuilder.build();

        builder = new SIPBuilder(SIP_ID_TEST);
        builder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("data2.fits"), "sdsdfm1211vd");
        builder.setSyntax("FITS(FlexibleImageTransport)",
                          "http://www.iana.org/assignments/media-types/application/fits",
                          MediaType.valueOf("application/fits"));
        builder.addContentInformation();
        collection.add(builder.build());

        results = ingestService.ingest(collection);
        sipId = results.stream().findFirst().get().getSipId();
        resultSip = sipRepository.findOneBySipId(sipId);
        entityIdTest = resultSip.get().getId();

        // Init a SIP with reference in database with state CREATED
        colBuilder = new SIPCollectionBuilder(PROCESSING_CHAIN_TEST, SESSION_ID);
        collection = colBuilder.build();

        builder = new SIPBuilder(SIP_REF_ID_TEST);
        collection.add(builder.buildReference(Paths.get("src/test/resources/file_ref.xml"),
                                              "1e2d4ab665784e43243b9b07724cd483"));
        builder.setSyntax("XML", "https://en.wikipedia.org/wiki/XML", MediaType.valueOf("application/xml"));
        results = ingestService.ingest(collection);
        sipId = results.stream().findFirst().get().getSipId();
        resultSip = sipRepository.findOneBySipId(sipId);
        sipRefIdTest = resultSip.get().getId();
    }

    @After
    public void cleanJobs() {
        jobInfoRepo.deleteAll();
    }

    private void initFullPRocessingChain() throws ModuleException {
        PluginMetaData preProcessingPluginMeta = PluginUtils.createPluginMetaData(PreprocessingTestPlugin.class);
        PluginConfiguration preProcessingPlugin = new PluginConfiguration(preProcessingPluginMeta,
                                                                          "preProcessingPlugin");
        pluginService.savePluginConfiguration(preProcessingPlugin);

        PluginMetaData validationPluginMeta = PluginUtils.createPluginMetaData(ValidationTestPlugin.class);
        PluginConfiguration validationPlugin = new PluginConfiguration(validationPluginMeta, "validationPlugin");
        pluginService.savePluginConfiguration(validationPlugin);

        PluginMetaData generationPluginMeta = PluginUtils.createPluginMetaData(AIPGenerationTestPlugin.class);
        PluginConfiguration generationPlugin = new PluginConfiguration(generationPluginMeta, "generationPlugin");
        pluginService.savePluginConfiguration(generationPlugin);

        PluginMetaData taggingPluginMeta = PluginUtils.createPluginMetaData(AIPTaggingTestPlugin.class);
        PluginConfiguration taggingPlugin = new PluginConfiguration(taggingPluginMeta, "taggingPlugin");
        pluginService.savePluginConfiguration(taggingPlugin);

        IngestProcessingChain fullChain = new IngestProcessingChain(PROCESSING_CHAIN_TEST,
                                                                    "Full test Ingestion processing chain",
                                                                    validationPlugin,
                                                                    generationPlugin);
        fullChain.setPreProcessingPlugin(preProcessingPlugin);
        fullChain.setGenerationPlugin(generationPlugin);
        fullChain.setTagPlugin(taggingPlugin);
        processingChainRepository.save(fullChain);
    }

    private void initDefaultProcessingChain() throws ModuleException {
        PluginMetaData defaultValidationPluginMeta = PluginUtils.createPluginMetaData(DefaultSipValidation.class);
        PluginConfiguration defaultValidationPlugin = new PluginConfiguration(defaultValidationPluginMeta,
                                                                              "DefaultValidationPlugin");
        pluginService.savePluginConfiguration(defaultValidationPlugin);

        PluginMetaData defaultGenerationPluginMeta = PluginUtils.createPluginMetaData(DefaultSingleAIPGeneration.class);
        PluginConfiguration defaultGenerationPlugin = new PluginConfiguration(defaultGenerationPluginMeta,
                                                                              "DefaultGenerationPlugin");
        pluginService.savePluginConfiguration(defaultGenerationPlugin);

        IngestProcessingChain defaultChain = new IngestProcessingChain(DEFAULT_PROCESSING_CHAIN_TEST,
                                                                       "Default Ingestion processing chain",
                                                                       defaultValidationPlugin,
                                                                       defaultGenerationPlugin);
        processingChainRepository.save(defaultChain);
    }

    @Requirement("REGARDS_DSL_ING_PRO_160")
    @Purpose("Test default process chain to ingest a new SIP provided by value")
    @Test
    public void testDefaultProcessingChain() {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(IngestProcessingJob.CHAIN_NAME_PARAMETER, DEFAULT_PROCESSING_CHAIN_TEST));
        parameters.add(new JobParameter(IngestProcessingJob.IDS_PARAMETER, Sets.newHashSet(entityDefaultChainTest)));

        // Simulate a full process without error
        JobInfo toTest = new JobInfo(false, 0, parameters, "owner", IngestProcessingJob.class.getName());
        runJob(toTest);
        // Assert that SIP is in AIP_CREATED state
        SIPEntity resultSip = sipRepository.findById(entityDefaultChainTest).get();
        Assert.assertTrue("SIP should be the one generated in the test initialization.",
                          SIP_DEFAULT_CHAIN_ID_TEST.equals(resultSip.getSip().getId()));
        Assert.assertEquals("Wrong SIP state after a successful process",
                            SIPState.AIP_SUBMITTED,
                            resultSip.getState());
    }

    @Requirement("REGARDS_DSL_ING_PRO_160")
    @Requirement("REGARDS_DSL_ING_PRO_170")
    @Requirement("REGARDS_DSL_ING_PRO_180")
    @Requirement("REGARDS_DSL_ING_PRO_300")
    @Requirement("REGARDS_DSL_ING_PRO_400")
    @Purpose("Test fully configured process chain to ingest a new SIP provided by value")
    @Test
    public void testProcessingChain() throws JobParameterMissingException, JobParameterInvalidException {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(IngestProcessingJob.CHAIN_NAME_PARAMETER, PROCESSING_CHAIN_TEST));
        parameters.add(new JobParameter(IngestProcessingJob.IDS_PARAMETER, Sets.newHashSet(entityIdTest)));

        // Simulate an error during PreprocessingStep
        stepErrorSimulator.setSimulateErrorForStep(PreprocessingTestPlugin.class);
        JobInfo toTest = new JobInfo(false, 1, parameters, "owner", IngestProcessingJob.class.getName());
        runJob(toTest);

        // Assert that SIP is in INVALID state
        SIPEntity resultSip = sipRepository.findById(entityIdTest).get();
        Assert.assertTrue("State of SIP should be INVALID after a error during PreprocessingTestPlugin",
                          SIPState.INVALID.equals(resultSip.getState()));
        // Assert that no AIP is generated
        Set<AIPEntity> aips = aipRepository.findBySip(resultSip);
        Assert.assertTrue("No AIP should be generated after error", aips.isEmpty());

        // Simulate an error during ValidationStep
        stepErrorSimulator.setSimulateErrorForStep(ValidationTestPlugin.class);
        toTest = new JobInfo(false, 1, parameters, "owner", IngestProcessingJob.class.getName());
        runJob(toTest);

        // Assert that SIP is in INVALID state
        resultSip = sipRepository.findById(entityIdTest).get();
        Assert.assertTrue("State of SIP should be INVALID after a error during ValidationStep",
                          SIPState.INVALID.equals(resultSip.getState()));
        // Assert that no AIP is generated
        aips = aipRepository.findBySip(resultSip);
        Assert.assertTrue("No AIP should be generatedafter error", aips.isEmpty());

        // Simulate an error during GenerationStep
        stepErrorSimulator.setSimulateErrorForStep(AIPGenerationTestPlugin.class);
        toTest = new JobInfo(false, 1, parameters, "owner", IngestProcessingJob.class.getName());
        runJob(toTest);

        // Assert that SIP is in AIP_GEN_ERROR state
        resultSip = sipRepository.findById(entityIdTest).get();
        Assert.assertTrue("State of SIP should be AIP_GEN_ERROR after a error during GenerationStep",
                          SIPState.AIP_GEN_ERROR.equals(resultSip.getState()));
        // Assert that no AIP is generated
        aips = aipRepository.findBySip(resultSip);
        Assert.assertTrue("No AIP should be generatedafter error", aips.isEmpty());

        // Simulate an error during TaggingStep
        stepErrorSimulator.setSimulateErrorForStep(AIPTaggingTestPlugin.class);
        toTest = new JobInfo(false, 1, parameters, "owner", IngestProcessingJob.class.getName());
        runJob(toTest);

        // Assert that SIP is in AIP_GEN_ERROR state
        resultSip = sipRepository.findById(entityIdTest).get();
        Assert.assertTrue("State of SIP should be AIP_GEN_ERROR after a error during GenerationStep",
                          SIPState.AIP_GEN_ERROR.equals(resultSip.getState()));
        // Assert that no AIP is generated
        aips = aipRepository.findBySip(resultSip);
        Assert.assertTrue("No AIP should be generatedafter error", aips.isEmpty());

        // Simulate a full process without error
        stepErrorSimulator.setSimulateErrorForStep(null);
        toTest = new JobInfo(false, 1, parameters, "owner", IngestProcessingJob.class.getName());
        runJob(toTest);
        // Assert that SIP is in AIP_CREATED state
        resultSip = sipRepository.findById(entityIdTest).get();
        Assert.assertTrue("SIP should be the one generated in the test initialization.",
                          SIP_ID_TEST.equals(resultSip.getSip().getId()));
        Assert.assertEquals("Wrong SIP state after a successful process",
                            SIPState.AIP_SUBMITTED,
                            resultSip.getState());
        // Assert that te AIP generated is in db and in state CREATED
        aips = aipRepository.findBySip(resultSip);
        Assert.assertTrue("There should be one AIP generated associated to the entry sip", aips.size() == 1);
        Assert.assertTrue("The AIP generated should be in CREATED state",
                          SipAIPState.CREATED.equals(aips.stream().findFirst().get().getState()));
        Assert.assertEquals("AIP should contain the session ID",
                            aips.stream().findFirst().get().getAip().getProperties().getPdi().getProvenanceInformation()
                                    .getSession(),
                            SESSION_ID);

    }

    @Purpose("Test fully configured process chain to ingest a new SIP provided by reference")
    @Test
    public void testProcessingChainByRef() throws JobParameterMissingException, JobParameterInvalidException {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(IngestProcessingJob.CHAIN_NAME_PARAMETER, PROCESSING_CHAIN_TEST));
        parameters.add(new JobParameter(IngestProcessingJob.IDS_PARAMETER, Sets.newHashSet(sipRefIdTest)));

        // Simulate a full process without error
        JobInfo toTest = new JobInfo(false, 0, parameters, "owner", IngestProcessingJob.class.getName());
        runJob(toTest);
        // Assert that SIP is in AIP_CREATED state
        SIPEntity resultSip = sipRepository.findById(sipRefIdTest).get();
        Assert.assertTrue("SIP should be the one generated in the test initialization.",
                          SIP_REF_ID_TEST.equals(resultSip.getSip().getId()));
        Assert.assertEquals("Wrong SIP state after a successful process",
                            SIPState.AIP_SUBMITTED,
                            resultSip.getState());
        // Assert that te AIP generated is in db and in state CREATED
        Set<AIPEntity> aips = aipRepository.findBySip(resultSip);
        Assert.assertTrue("There should be one AIP generated associated to the entry sip", aips.size() == 1);
        Assert.assertTrue("The AIP generated should be in CREATED state",
                          SipAIPState.CREATED.equals(aips.stream().findFirst().get().getState()));
        Assert.assertEquals("AIP should contain the session ID",
                            aips.stream().findFirst().get().getAip().getProperties().getPdi().getProvenanceInformation()
                                    .getSession(),
                            SESSION_ID);

    }

    protected IJob<?> runJob(JobInfo jobInfo) {
        try {
            IJob<?> job = (IJob<?>) Class.forName(jobInfo.getClassName()).newInstance();
            beanFactory.autowireBean(job);
            job.setParameters(jobInfo.getParametersAsMap());
            if (job.needWorkspace()) {
                job.setWorkspace(() -> Files.createTempDirectory(jobInfo.getId().toString()));
            }
            jobInfo.setJob(job);
            jobInfo.getStatus().setStartDate(OffsetDateTime.now());
            job.run();
            return job;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOG.error("Unable to instantiate job", e);
            Assert.fail("Unable to instantiate job");
        } catch (JobWorkspaceException e) {
            LOG.error("Cannot set workspace", e);
            Assert.fail("Cannot set workspace");
        } catch (JobParameterMissingException e) {
            LOG.error("Missing parameter", e);
            Assert.fail("Missing parameter");
        } catch (JobParameterInvalidException e) {
            LOG.error("Invalid parameter", e);
            Assert.fail("Invalid parameter");
        }
        return null;
    }

}