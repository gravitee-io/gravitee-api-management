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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AddOrgIdNotificationConfigsUpgraderTest {

    @InjectMocks
    private AddOrgIdNotificationConfigsUpgrader upgrader;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private GenericNotificationConfigRepository genericNotificationConfigRepository;

    @Mock
    private PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Test
    public void upgrade() throws TechnicalException {
        when(environmentRepository.findById("env1")).thenReturn(
            Optional.of(Environment.builder().id("env1").organizationId("org1").build())
        );
        when(environmentRepository.findById("env2")).thenReturn(
            Optional.of(Environment.builder().id("env2").organizationId("org1").build())
        );
        when(environmentRepository.findById("env3")).thenReturn(
            Optional.of(Environment.builder().id("env3").organizationId("org2").build())
        );
        when(environmentRepository.findById("env4")).thenReturn(
            Optional.of(Environment.builder().id("env4").organizationId("org2").build())
        );
        when(environmentRepository.findById("env5")).thenReturn(
            Optional.of(Environment.builder().id("env5").organizationId("org3").build())
        );
        when(apiRepository.findById("api1")).thenReturn(Optional.of(Api.builder().id("api1").environmentId("env1").build()));
        when(apiRepository.findById("api2")).thenReturn(Optional.of(Api.builder().id("api2").environmentId("env2").build()));
        when(apiRepository.findById("api3")).thenReturn(Optional.of(Api.builder().id("api3").environmentId("env3").build()));
        when(apiRepository.findById("api5")).thenReturn(Optional.of(Api.builder().id("api5").environmentId("env5").build()));
        when(applicationRepository.findById("app2")).thenReturn(
            Optional.of(Application.builder().id("app2").environmentId("env2").build())
        );
        when(applicationRepository.findById("app5")).thenReturn(
            Optional.of(Application.builder().id("app5").environmentId("env5").build())
        );
        var genericNotifConfigs = Set.of(
            GenericNotificationConfig.builder().id("gn1").referenceType(NotificationReferenceType.PORTAL).referenceId("env1").build(),
            GenericNotificationConfig.builder().id("gn2").referenceType(NotificationReferenceType.API).referenceId("api3").build(),
            GenericNotificationConfig.builder().id("gn3").referenceType(NotificationReferenceType.APPLICATION).referenceId("app5").build(),
            GenericNotificationConfig.builder().id("gn4").referenceType(NotificationReferenceType.PORTAL).referenceId("env2").build(),
            GenericNotificationConfig.builder().id("gn5").referenceType(NotificationReferenceType.API).referenceId("api2").build()
        );
        when(genericNotificationConfigRepository.findAll()).thenReturn(genericNotifConfigs);

        var portalNotifConfigs = Set.of(
            PortalNotificationConfig.builder().referenceType(NotificationReferenceType.PORTAL).referenceId("env4").build(),
            PortalNotificationConfig.builder().referenceType(NotificationReferenceType.API).referenceId("api1").build(),
            PortalNotificationConfig.builder().referenceType(NotificationReferenceType.APPLICATION).referenceId("app2").build(),
            PortalNotificationConfig.builder().referenceType(NotificationReferenceType.PORTAL).referenceId("env2").build(),
            PortalNotificationConfig.builder().referenceType(NotificationReferenceType.API).referenceId("api5").build()
        );
        when(portalNotificationConfigRepository.findAll()).thenReturn(portalNotifConfigs);
        upgrader.upgrade();
        var genericNotifsConfigsById = genericNotifConfigs
            .stream()
            .collect(Collectors.toMap(GenericNotificationConfig::getId, Function.identity()));
        var portalNotifsConfigsByReferenceId = portalNotifConfigs
            .stream()
            .collect(Collectors.toMap(PortalNotificationConfig::getReferenceId, Function.identity()));
        assertAll(
            () -> assertThat(genericNotifsConfigsById.get("gn1").getOrganizationId()).isEqualTo("org1"),
            () -> assertThat(genericNotifsConfigsById.get("gn1").getReferenceType()).isEqualTo(NotificationReferenceType.ENVIRONMENT),
            () -> assertThat(genericNotifsConfigsById.get("gn2").getOrganizationId()).isEqualTo("org2"),
            () -> assertThat(genericNotifsConfigsById.get("gn3").getOrganizationId()).isEqualTo("org3"),
            () -> assertThat(genericNotifsConfigsById.get("gn4").getOrganizationId()).isEqualTo("org1"),
            () -> assertThat(genericNotifsConfigsById.get("gn4").getReferenceType()).isEqualTo(NotificationReferenceType.ENVIRONMENT),
            () -> assertThat(genericNotifsConfigsById.get("gn5").getOrganizationId()).isEqualTo("org1"),
            () -> assertThat(portalNotifsConfigsByReferenceId.get("env4").getOrganizationId()).isEqualTo("org2"),
            () ->
                assertThat(portalNotifsConfigsByReferenceId.get("env4").getReferenceType()).isEqualTo(
                    NotificationReferenceType.ENVIRONMENT
                ),
            () -> assertThat(portalNotifsConfigsByReferenceId.get("api1").getOrganizationId()).isEqualTo("org1"),
            () -> assertThat(portalNotifsConfigsByReferenceId.get("app2").getOrganizationId()).isEqualTo("org1"),
            () -> assertThat(portalNotifsConfigsByReferenceId.get("env2").getOrganizationId()).isEqualTo("org1"),
            () ->
                assertThat(portalNotifsConfigsByReferenceId.get("env2").getReferenceType()).isEqualTo(
                    NotificationReferenceType.ENVIRONMENT
                ),
            () -> assertThat(portalNotifsConfigsByReferenceId.get("api5").getOrganizationId()).isEqualTo("org3")
        );
    }
}
