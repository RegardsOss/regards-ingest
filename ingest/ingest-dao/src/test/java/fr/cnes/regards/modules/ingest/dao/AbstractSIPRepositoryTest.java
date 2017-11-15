package fr.cnes.regards.modules.ingest.dao;

import java.time.OffsetDateTime;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.oais.builder.InformationPackagePropertiesBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPSessionBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema:ingest_dao" })
public abstract class AbstractSIPRepositoryTest extends AbstractDaoTransactionalTest {

    @Autowired
    protected ISIPRepository sipRepository;

    @Autowired
    protected ISIPSessionRepository sipSessionRepository;

    protected static final String PROCESSING_CHAIN = "processing";

    protected static final String PROCESSING_CHAIN2 = "processing2";

    @Before
    public void init() {

        SIPSession session = sipSessionRepository.save(SIPSessionBuilder.build("sessionId"));
        SIPSession session2 = sipSessionRepository.save(SIPSessionBuilder.build("sessionId2"));
        SIPSession session3 = sipSessionRepository.save(SIPSessionBuilder.build("otherSession"));

        SIPEntity sip1 = new SIPEntity();
        SIPBuilder b = new SIPBuilder("SIP_001");
        sip1.setSip(b.build());
        sip1.setIpId("URN:SIP:Collection:DEFAULT:12345678901:V1");
        sip1.setSipId("SIP_001");
        sip1.setIngestDate(OffsetDateTime.now());
        sip1.setOwner("admin");
        sip1.setProcessing(PROCESSING_CHAIN);
        sip1.setSession(session);
        sip1.setState(SIPState.CREATED);
        sip1.setVersion(1);
        sip1.setChecksum("1234567890");

        sip1 = sipRepository.save(sip1);

        SIPEntity sip2 = new SIPEntity();
        b = new SIPBuilder("SIP_002");
        sip2.setSip(b.build());
        sip2.setIpId("URN:SIP:Collection:DEFAULT:12345678902:V1");
        sip2.setSipId("SIP_002");
        sip2.setIngestDate(OffsetDateTime.now().minusHours(6));
        sip2.setOwner("admin");
        sip2.setProcessing(PROCESSING_CHAIN);
        sip2.setSession(session);
        sip2.setState(SIPState.CREATED);
        sip2.setVersion(1);
        sip2.setChecksum("12345678902");

        sip2 = sipRepository.save(sip2);

        SIPEntity sip3 = new SIPEntity();
        b = new SIPBuilder("SIP_003");
        sip3.setSip(b.build());
        sip3.setIpId("URN:SIP:Collection:DEFAULT:12345678903:V1");
        sip3.setSipId("SIP_003");
        sip3.setIngestDate(OffsetDateTime.now().minusHours(6));
        sip3.setOwner("admin2");
        sip3.setProcessing(PROCESSING_CHAIN);
        sip3.setSession(session2);
        sip3.setState(SIPState.STORED);
        sip3.setVersion(1);
        sip3.setChecksum("12345678903");

        sip3 = sipRepository.save(sip3);

        SIPEntity sip4 = new SIPEntity();
        InformationPackagePropertiesBuilder ippb = new InformationPackagePropertiesBuilder();
        ippb.addDescriptiveInformation("version", "2");
        b = new SIPBuilder("SIP_003");
        sip4.setSip(b.build(ippb.build()));
        sip4.setIpId("URN:SIP:Collection:DEFAULT:123456789032:V2");
        sip4.setSipId("SIP_003");
        sip4.setIngestDate(OffsetDateTime.now().minusHours(6));
        sip4.setOwner("admin2");
        sip4.setProcessing(PROCESSING_CHAIN2);
        sip4.setSession(session2);
        sip4.setState(SIPState.STORED);
        sip4.setVersion(2);
        sip4.setChecksum("123456789032");

        sip4 = sipRepository.save(sip4);
    }
}