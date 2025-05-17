/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Origin;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalNotificationConfigOriginUpgraderTest {

    private static final ZonedDateTime CREATED_AT = Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault());
    private static final ZonedDateTime UPDATED_AT = Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault());

    @Mock
    private PortalNotificationConfigRepository portalNotificationConfigRepository;

    private PortalNotificationConfigOriginUpgrader upgrader;

    @Before
    public void setUp() {
        upgrader = new PortalNotificationConfigOriginUpgrader(portalNotificationConfigRepository);
    }

    @Test
    public void should_upgrade_only_portal_with_no_origin() throws TechnicalException {
        Set<PortalNotificationConfig> portalNotificationConfigs = Set.of(
            // to be upgraded
            aPortalNotificationConfig("api#1", NotificationReferenceType.API, "user#1", null),
            aPortalNotificationConfig("app#1", NotificationReferenceType.APPLICATION, "user#1", null),
            aPortalNotificationConfig("env#1", NotificationReferenceType.PORTAL, "user#1", null),
            // no upgrade
            aPortalNotificationConfig("api#1", NotificationReferenceType.API, "user#2", Origin.KUBERNETES),
            aPortalNotificationConfig("app#1", NotificationReferenceType.APPLICATION, "user#2", Origin.MANAGEMENT),
            aPortalNotificationConfig("env#1", NotificationReferenceType.PORTAL, "user#2", Origin.MANAGEMENT)
        );
        when(portalNotificationConfigRepository.findAll()).thenReturn(portalNotificationConfigs);

        assertTrue(upgrader.upgrade());
        verify(portalNotificationConfigRepository, never()).delete(any());
        verify(portalNotificationConfigRepository, never()).create(any());
        verify(portalNotificationConfigRepository, atMostOnce())
            .update(aPortalNotificationConfig("api#1", NotificationReferenceType.API, "user#1", null));
        verify(portalNotificationConfigRepository, atMostOnce())
            .update(aPortalNotificationConfig("app#1", NotificationReferenceType.APPLICATION, "user#1", null));
        verify(portalNotificationConfigRepository, atMostOnce())
            .update(aPortalNotificationConfig("env#1", NotificationReferenceType.PORTAL, "user#1", null));
    }

    private PortalNotificationConfig aPortalNotificationConfig(
        String referenceId,
        NotificationReferenceType referenceType,
        String user,
        Origin origin
    ) {
        return PortalNotificationConfig
            .builder()
            .referenceId(referenceId)
            .referenceType(referenceType)
            .user(user)
            .hooks(Collections.emptyList())
            .createdAt(Date.from(CREATED_AT.toInstant()))
            .updatedAt(Date.from(UPDATED_AT.toInstant()))
            .origin(origin)
            .build();
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.PORTAL_NOTIFICATION_CONFIG_ORIGIN_UPGRADER, upgrader.getOrder());
    }
}
