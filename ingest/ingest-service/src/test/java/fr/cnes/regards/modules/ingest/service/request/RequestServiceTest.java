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
package fr.cnes.regards.modules.ingest.service.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionPayload;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.StorageDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=request_service_test",
        "regards.aips.save-metadata.bulk.delay=20000000", "regards.amqp.enabled=true", "eureka.client.enabled=false",
        "regards.scheduler.pool.size=0", "regards.ingest.maxBulkSize=100", "spring.jpa.show-sql=true" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock", "noscheduler" })
public class RequestServiceTest extends AbstractIngestRequestTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private RequestService requestService;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    private static final List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY");

    private static final List<String> CATEGORIES_1 = Lists.newArrayList("CATEGORY1");

    private static final List<String> CATEGORIES_2 = Lists.newArrayList("CATEGORY", "CATEGORY2");

    private static final List<String> TAG_0 = Lists.newArrayList("toto", "tata");

    private static final List<String> TAG_1 = Lists.newArrayList("toto", "tutu");

    private static final List<String> TAG_2 = Lists.newArrayList("antonio", "farra's");

    private static final String STORAGE_0 = "fake";

    private static final String STORAGE_1 = "AWS";

    private static final String STORAGE_2 = "Azure";

    private static final String SESSION_OWNER_0 = "NASA";

    private static final String SESSION_OWNER_1 = "CNES";

    public static final String SESSION_0 = OffsetDateTime.now().toString();

    public static final String SESSION_1 = OffsetDateTime.now().minusDays(4).toString();

    private List<AIPEntity> aips;

    private IngestMetadataDto mtd;

    public void prepareOAISEntities() {
        SIPEntity sip4 = new SIPEntity();

        sip4.setSip(SIP.build(EntityType.DATA, "SIP_001").withDescriptiveInformation("version", "2"));
        sip4.setSipId(UniformResourceName
                .fromString("URN:SIP:COLLECTION:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        sip4.setProviderId("SIP_003");
        sip4.setCreationDate(OffsetDateTime.now().minusHours(6));
        sip4.setLastUpdate(OffsetDateTime.now().minusHours(6));
        sip4.setSessionOwner("SESSION_OWNER");
        sip4.setSession("SESSION");
        sip4.setCategories(org.assertj.core.util.Sets.newLinkedHashSet("CATEGORIES"));
        sip4.setState(SIPState.INGESTED);
        sip4.setVersion(2);
        sip4.setChecksum("123456789032");

        sip4 = sipRepository.save(sip4);

        AIP aip = AIP.build(sip4.getSip(),
                UniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP, EntityType.DATA, "tenant", 1),
                Optional.empty(), "SIP_001"
        );
        AIPEntity aipEntity = AIPEntity.build(sip4, AIPState.GENERATED, aip);

        aipEntity = aipRepository.save(aipEntity);



        AIP aip2 = AIP.build(sip4.getSip(),
                UniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP, EntityType.DATA, "tenant", 1),
                Optional.empty(), "SIP_002"
        );
        AIPEntity aipEntity2 = AIPEntity.build(sip4, AIPState.GENERATED, aip2);

        aipEntity2 = aipRepository.save(aipEntity2);



        mtd = IngestMetadataDto.build(SESSION_OWNER_0, SESSION_0,
                IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                Sets.newHashSet(CATEGORIES_0),
                StorageMetadata.build(STORAGE_0));

        aips = aipRepository.findAll();

        LOGGER.info("=========================> END INIT DATA FOR TESTS <=====================");
    }

    private IngestRequest createIngestRequest(AIPEntity aipEntity) {
        IngestRequest ingestRequest = IngestRequest.build(IngestMetadata.build("SESSION_OWNER", "SESSION", "ingestChain",
                new HashSet<>(), StorageMetadata.build("RAS"))
                , InternalRequestState.CREATED, IngestRequestStep.LOCAL_SCHEDULED, aipEntity.getSip().getSip());
        ingestRequest.setAips(Lists.newArrayList(aipEntity));
        return ingestRequestRepository.save(ingestRequest);
    }

    private StorageDeletionRequest createStorageDeletionRequest(List<AIPEntity> aips) {
        StorageDeletionRequest storageDeletionRequest = StorageDeletionRequest
                .build("some request id", aips.get(0).getSip(), SessionDeletionMode.BY_STATE);
        return (StorageDeletionRequest) requestService.scheduleRequest(storageDeletionRequest);
    }

    private OAISDeletionRequest createOAISDeletionRequest() {
        OAISDeletionRequest deletionRequest = OAISDeletionRequest.build(new OAISDeletionPayload());
        return (OAISDeletionRequest) requestService.scheduleRequest(deletionRequest);
    }

    private List<AIPUpdateRequest> createUpdateRequest(List<AIPEntity> aips) {
        List<AIPUpdateRequest> updateRequests = AIPUpdateRequest.build(aips.get(0), AIPUpdateParametersDto
                        .build(SearchAIPsParameters.build().withSession(SESSION_0)).withAddTags(Lists.newArrayList("SOME TAG")),
                false);
        List<AbstractRequest> list = new ArrayList<>();
        for (AIPUpdateRequest ur : updateRequests) {
            list.add(ur);
        }
        requestService.scheduleRequests(list);
        return updateRequests;
    }

    private AIPUpdatesCreatorRequest createAIPUpdatesCreatorRequest() {
        AIPUpdatesCreatorRequest updateCreatorRequest = AIPUpdatesCreatorRequest
                .build(AIPUpdateParametersDto.build(SearchAIPsParameters.build().withSession(SESSION_0)));
        return (AIPUpdatesCreatorRequest) requestService.scheduleRequest(updateCreatorRequest);
    }

    private AIPStoreMetaDataRequest createStoreMetaDataRequest(List<AIPEntity> aips) {
        AIPStoreMetaDataRequest storeMetaDataRequest = AIPStoreMetaDataRequest.build(aips.get(0), null, true, true);
        return (AIPStoreMetaDataRequest) requestService.scheduleRequest(storeMetaDataRequest);
    }

    public void clearRequest() {
        List<AbstractRequest> entities = abstractRequestRepository.findAll();
        LOGGER.info("Let's remove {} entities", entities.size());
        abstractRequestRepository.deleteAll(entities);
        LOGGER.info("Entities still existing count : {} ", abstractRequestRepository.count());

        LOGGER.info("Jobs stil existing : {}", jobInfoRepository.count());
        LOGGER.info("Let's remove {} jobs info", jobInfoRepository.count());
        jobInfoRepository.deleteAll();

    }

    @Test
    public void testScheduleRequest() {
        clearRequest();
        prepareOAISEntities();

        // BEGIN ------ Empty repo tests
        IngestRequest ingestRequest = createIngestRequest(aips.get(0));
        Assert.assertEquals("The request should not be blocked", InternalRequestState.CREATED, ingestRequest.getState());
        clearRequest();

        AIPStoreMetaDataRequest storeMetaDataRequest = createStoreMetaDataRequest(aips);
        Assert.assertEquals("The request should not be blocked", InternalRequestState.CREATED, storeMetaDataRequest.getState());
        clearRequest();

        AIPUpdatesCreatorRequest aipUpdatesCreatorRequest = createAIPUpdatesCreatorRequest();
        Assert.assertEquals("The request should not be blocked", InternalRequestState.CREATED, aipUpdatesCreatorRequest.getState());
        clearRequest();

        List<AIPUpdateRequest> updateRequest = createUpdateRequest(aips);
        for (AIPUpdateRequest request : updateRequest) {
            Assert.assertEquals("The request should not be blocked", InternalRequestState.CREATED, request.getState());
        }
        clearRequest();


        OAISDeletionRequest oaisDeletionRequest = createOAISDeletionRequest();
        Assert.assertEquals("The request should not be blocked", InternalRequestState.CREATED, oaisDeletionRequest.getState());
        clearRequest();

        StorageDeletionRequest storageDeletionRequest = createStorageDeletionRequest(aips);
        Assert.assertEquals("The request should not be blocked", InternalRequestState.RUNNING, storageDeletionRequest.getState());
        clearRequest();
        // END  ------ Empty repo tests


        // BEGIN ------- Test AIPUpdatesCreatorRequest
        createIngestRequest(aips.get(0));
        createAIPUpdatesCreatorRequest();
        createUpdateRequest(aips);
        createStorageDeletionRequest(aips);

        aipUpdatesCreatorRequest = createAIPUpdatesCreatorRequest();
        Assert.assertEquals("The request should not be blocked", InternalRequestState.CREATED, aipUpdatesCreatorRequest.getState());
        clearRequest();
        // END ------- Test AIPUpdatesCreatorRequest


        // BEGIN ------- Test AIPUpdateRequest
        createIngestRequest(aips.get(0));
        createAIPUpdatesCreatorRequest();
        createStoreMetaDataRequest(aips);
        updateRequest = createUpdateRequest(aips);
        for (AIPUpdateRequest request : updateRequest) {
            Assert.assertEquals("The request should not be blocked", InternalRequestState.CREATED, request.getState());
        }
        clearRequest();
        // END ------- Test AIPUpdateRequest

        // AIPStoreMetaDataRequest does not deserve more tests

        // BEGIN ------- Test OAISDeletionRequest
        createIngestRequest(aips.get(0));
        createStorageDeletionRequest(aips);
        oaisDeletionRequest = createOAISDeletionRequest();
        Assert.assertEquals("The request should not be blocked", InternalRequestState.CREATED, oaisDeletionRequest.getState());
        clearRequest();
        // END ------- Test OAISDeletionRequest


        // BEGIN ------- Test StorageDeletionRequest
        createIngestRequest(aips.get(0));
        createOAISDeletionRequest();
        storageDeletionRequest = createStorageDeletionRequest(aips);
        Assert.assertEquals("The request should not be blocked", InternalRequestState.RUNNING, storageDeletionRequest.getState());
        clearRequest();
        // END ------- Test StorageDeletionRequest
    }

    @Test
    public void testScheduleAsBlockRequest() {
        clearRequest();
        prepareOAISEntities();


        // BEGIN ------- Test AIPUpdatesCreatorRequest
        createStoreMetaDataRequest(aips);
        AIPUpdatesCreatorRequest aipUpdatesCreatorRequest = createAIPUpdatesCreatorRequest();
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, aipUpdatesCreatorRequest.getState());
        clearRequest();

        createOAISDeletionRequest();
        aipUpdatesCreatorRequest = createAIPUpdatesCreatorRequest();
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, aipUpdatesCreatorRequest.getState());
        clearRequest();
        // END ------- Test AIPUpdatesCreatorRequest

        // BEGIN ------- Test AIPUpdateRequest
        createStoreMetaDataRequest(aips);
        List<AIPUpdateRequest> updateRequest = createUpdateRequest(aips);
        for (AIPUpdateRequest request : updateRequest) {
            Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, request.getState());
        }
        clearRequest();

        createStorageDeletionRequest(aips);
        updateRequest = createUpdateRequest(aips);
        for (AIPUpdateRequest request : updateRequest) {
            Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, request.getState());
        }
        clearRequest();

        createOAISDeletionRequest();
        updateRequest = createUpdateRequest(aips);
        for (AIPUpdateRequest request : updateRequest) {
            Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, request.getState());
        }
        clearRequest();
        // END ------- Test AIPUpdateRequest


        // BEGIN ------- Test AIPStoreMetaDataRequest
        createUpdateRequest(aips);
        AIPStoreMetaDataRequest storeMetaDataRequest = createStoreMetaDataRequest(aips);
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, storeMetaDataRequest.getState());
        clearRequest();


        createStorageDeletionRequest(aips);
        storeMetaDataRequest = createStoreMetaDataRequest(aips);
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, storeMetaDataRequest.getState());
        clearRequest();

        createAIPUpdatesCreatorRequest();
        storeMetaDataRequest = createStoreMetaDataRequest(aips);
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, storeMetaDataRequest.getState());
        clearRequest();


        createOAISDeletionRequest();
        storeMetaDataRequest = createStoreMetaDataRequest(aips);
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, storeMetaDataRequest.getState());
        clearRequest();
        // END ------- Test AIPStoreMetaDataRequest



        // BEGIN ------- Test OAISDeletionRequest
        createStoreMetaDataRequest(aips);
        OAISDeletionRequest oaisDeletionRequest = createOAISDeletionRequest();
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, oaisDeletionRequest.getState());
        clearRequest();

        createUpdateRequest(aips);
        oaisDeletionRequest = createOAISDeletionRequest();
        Assert.assertEquals("The request should not be blocked", InternalRequestState.BLOCKED, oaisDeletionRequest.getState());
        clearRequest();

        createAIPUpdatesCreatorRequest();
        oaisDeletionRequest = createOAISDeletionRequest();
        Assert.assertEquals("The request should not be blocked", InternalRequestState.BLOCKED, oaisDeletionRequest.getState());
        clearRequest();
        // END ------- Test OAISDeletionRequest



        // BEGIN ------- Test StorageDeletionRequest
        createStoreMetaDataRequest(aips);
        StorageDeletionRequest storageDeletionRequest = createStorageDeletionRequest(aips);
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, storageDeletionRequest.getState());
        clearRequest();

        createUpdateRequest(aips);
        storageDeletionRequest = createStorageDeletionRequest(aips);
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, storageDeletionRequest.getState());
        clearRequest();

        createAIPUpdatesCreatorRequest();
        storageDeletionRequest = createStorageDeletionRequest(aips);
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, storageDeletionRequest.getState());
        clearRequest();
        // END ------- Test StorageDeletionRequest

    }


    @Test
    public void testUnblockAIPUpdatesCreatorMacro() {
        clearRequest();

        prepareOAISEntities();
        AIPUpdatesCreatorRequest aipUpdatesCreatorRequest = createAIPUpdatesCreatorRequest();
        aipUpdatesCreatorRequest.setState(InternalRequestState.BLOCKED);
        aipUpdatesCreatorRequest = (AIPUpdatesCreatorRequest) abstractRequestRepository.save(aipUpdatesCreatorRequest);
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, aipUpdatesCreatorRequest.getState());

        requestService.unblockRequests(RequestTypeEnum.AIP_UPDATES_CREATOR);
        aipUpdatesCreatorRequest = (AIPUpdatesCreatorRequest) abstractRequestRepository.findById(aipUpdatesCreatorRequest.getId()).get();
        Assert.assertEquals("The request should not be blocked", InternalRequestState.RUNNING, aipUpdatesCreatorRequest.getState());
    }


    @Test
    public void testUnblockStorageDeletionMicro() {
        clearRequest();

        prepareOAISEntities();
        StorageDeletionRequest storageDeletionRequest = createStorageDeletionRequest(aips);
        storageDeletionRequest.setState(InternalRequestState.BLOCKED);
        storageDeletionRequest = (StorageDeletionRequest) abstractRequestRepository.save(storageDeletionRequest);
        Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, storageDeletionRequest.getState());

        requestService.unblockRequests(RequestTypeEnum.STORAGE_DELETION);
        storageDeletionRequest = (StorageDeletionRequest) abstractRequestRepository.findById(storageDeletionRequest.getId()).get();
        Assert.assertEquals("The request should not be blocked", InternalRequestState.CREATED, storageDeletionRequest.getState());
    }

}