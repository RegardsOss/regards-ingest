/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.ingest.service.settings;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dao.IAIPNotificationSettingsRepository;
import fr.cnes.regards.modules.ingest.domain.settings.AIPNotificationSettings;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;


/**
 * Test for {@link AIPNotificationSettingsService}
 * @author Iliana Ghazali
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=aip_notification_settings_service_it" },
        locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = {"noschedule"})
public class AIPNotificationSettingsServiceIT extends AbstractMultitenantServiceTest {

    @Autowired
    IAIPNotificationSettingsService notificationSettingsService;

    @Autowired
    IAIPNotificationSettingsRepository notificationSettingsRepository;

    @Test
    @Purpose("Check notification settings are retrieved")
    public void testRetrieve() {
        // simulate application started event for notification settings
        simulateApplicationStartedEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // init new configuration
        AIPNotificationSettings notificationSettings = notificationSettingsService.retrieve();

        // check configuration was saved in db
        Optional<AIPNotificationSettings> settingsOpt = notificationSettingsRepository.findFirstBy();
        Assert.assertTrue("Settings were not initialized properly",
                          settingsOpt.isPresent() && notificationSettings.getId().equals(settingsOpt.get().getId()));
        Assert.assertEquals("active_notifications was initialized with default value", notificationSettings.isActiveNotification(),
                          settingsOpt.get().isActiveNotification());
    }

    @Test
    @Purpose("Check the update of existing notification settings")
    public void testUpdate() throws EntityNotFoundException {
        // init new configuration
        AIPNotificationSettings notificationSettings = notificationSettingsService.retrieve();

        // Change notification to false
        notificationSettings.setActiveNotification(false);
        notificationSettingsService.update(notificationSettings);
        Assert.assertEquals(false, notificationSettingsRepository.findFirstBy().get().isActiveNotification());
    }
}
