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
package io.gravitee.gateway.handlers.sharedpolicygroup.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.event.SharedPolicyGroupEvent;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SharedPolicyGroupManagerImplTest {

    public static final String SHARED_POLICY_GROUP_ID = "shared-policy-group-id";
    private SharedPolicyGroupManagerImpl cut;

    @Mock
    private EventManager eventManager;

    @Mock
    private LicenseManager licenseManager;

    @BeforeEach
    void setUp() {
        cut = new SharedPolicyGroupManagerImpl(eventManager, licenseManager);
    }

    @Test
    void should_deploy_shared_policy_group() {
        final ReactableSharedPolicyGroup sharedPolicyGroup = new SharedPolicyGroupBuilder().id(SHARED_POLICY_GROUP_ID).build();
        cut.register(sharedPolicyGroup);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), any(SecretDiscoveryEvent.class));
        verify(eventManager).publishEvent(SharedPolicyGroupEvent.DEPLOY, sharedPolicyGroup);
        assertThat(cut.sharedPolicyGroups()).hasSize(1);
    }

    @Test
    void should_update_shared_policy_group() {
        final ReactableSharedPolicyGroup sharedPolicyGroup = new SharedPolicyGroupBuilder().id(SHARED_POLICY_GROUP_ID).build();
        sharedPolicyGroup.setDeployedAt(new Date());
        cut.register(sharedPolicyGroup);
        verify(eventManager).publishEvent(SharedPolicyGroupEvent.DEPLOY, sharedPolicyGroup);
        assertThat(cut.sharedPolicyGroups()).hasSize(1);

        final ReactableSharedPolicyGroup sharedPolicyGroup2 = new SharedPolicyGroupBuilder().id(SHARED_POLICY_GROUP_ID).build();
        Instant deployDateInst = sharedPolicyGroup.getDeployedAt().toInstant().plus(Duration.ofHours(1));
        sharedPolicyGroup2.setDeployedAt(Date.from(deployDateInst));

        cut.register(sharedPolicyGroup2);
        verify(eventManager).publishEvent(SharedPolicyGroupEvent.UPDATE, sharedPolicyGroup2);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), any(SecretDiscoveryEvent.class));
        assertThat(cut.sharedPolicyGroups()).hasSize(1);
    }

    @Test
    void should_not_update_shared_policy_group() {
        final ReactableSharedPolicyGroup sharedPolicyGroup = new SharedPolicyGroupBuilder().id(SHARED_POLICY_GROUP_ID).build();
        sharedPolicyGroup.setDeployedAt(new Date());
        cut.register(sharedPolicyGroup);
        verify(eventManager).publishEvent(SharedPolicyGroupEvent.DEPLOY, sharedPolicyGroup);
        assertThat(cut.sharedPolicyGroups()).hasSize(1);

        final ReactableSharedPolicyGroup sharedPolicyGroup2 = new SharedPolicyGroupBuilder().id(SHARED_POLICY_GROUP_ID).build();
        Instant deployDateInst = sharedPolicyGroup.getDeployedAt().toInstant().minus(Duration.ofHours(1));
        sharedPolicyGroup2.setDeployedAt(Date.from(deployDateInst));

        cut.register(sharedPolicyGroup2);
        verify(eventManager, never()).publishEvent(SharedPolicyGroupEvent.UPDATE, sharedPolicyGroup2);
        assertThat(cut.sharedPolicyGroups()).hasSize(1);
    }

    @Test
    void should_undeploy_shared_policy_group() {
        final ReactableSharedPolicyGroup sharedPolicyGroup = new SharedPolicyGroupBuilder().id(SHARED_POLICY_GROUP_ID).build();
        sharedPolicyGroup.setDeployedAt(new Date());
        cut.register(sharedPolicyGroup);
        verify(eventManager).publishEvent(SharedPolicyGroupEvent.DEPLOY, sharedPolicyGroup);
        assertThat(cut.sharedPolicyGroups()).hasSize(1);

        cut.unregister(sharedPolicyGroup.getId(), null);
        verify(eventManager).publishEvent(SharedPolicyGroupEvent.UNDEPLOY, sharedPolicyGroup);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), any(SecretDiscoveryEvent.class));
        assertThat(cut.sharedPolicyGroups()).isEmpty();
    }

    @Test
    void should_deploy_same_id_in_two_environments_without_revoking_the_first() {
        Date firstDeployedAt = new Date();
        ReactableSharedPolicyGroup environmentA = sharedPolicyGroup("env-a", firstDeployedAt, "128");
        ReactableSharedPolicyGroup environmentB = sharedPolicyGroup(
            "env-b",
            Date.from(firstDeployedAt.toInstant().plus(Duration.ofHours(1))),
            "5"
        );

        cut.register(environmentA);
        cut.register(environmentB);

        verify(eventManager, times(2)).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), any(SecretDiscoveryEvent.class));
        verify(eventManager).publishEvent(SharedPolicyGroupEvent.DEPLOY, environmentA);
        verify(eventManager).publishEvent(SharedPolicyGroupEvent.DEPLOY, environmentB);
        verify(eventManager, never()).publishEvent(eq(SecretDiscoveryEventType.REVOKE), any());
        verify(eventManager, never()).publishEvent(eq(SharedPolicyGroupEvent.UPDATE), any());
        assertThat(cut.sharedPolicyGroups()).containsExactlyInAnyOrder(environmentA, environmentB);
    }

    @Test
    void should_deploy_older_copy_in_another_environment() {
        Date firstDeployedAt = new Date();
        ReactableSharedPolicyGroup environmentA = sharedPolicyGroup("env-a", firstDeployedAt, "128");
        ReactableSharedPolicyGroup environmentB = sharedPolicyGroup(
            "env-b",
            Date.from(firstDeployedAt.toInstant().minus(Duration.ofHours(1))),
            "5"
        );

        cut.register(environmentA);
        cut.register(environmentB);

        verify(eventManager).publishEvent(SharedPolicyGroupEvent.DEPLOY, environmentB);
        verify(eventManager, never()).publishEvent(eq(SecretDiscoveryEventType.REVOKE), any());
        assertThat(cut.sharedPolicyGroups()).containsExactlyInAnyOrder(environmentA, environmentB);
    }

    @Test
    void should_update_only_the_copy_of_the_same_environment() {
        Date firstDeployedAt = new Date();
        ReactableSharedPolicyGroup environmentA = sharedPolicyGroup("env-a", firstDeployedAt, "1");
        ReactableSharedPolicyGroup environmentB = sharedPolicyGroup(
            "env-b",
            Date.from(firstDeployedAt.toInstant().plus(Duration.ofHours(1))),
            "5"
        );
        ReactableSharedPolicyGroup environmentAUpdated = sharedPolicyGroup(
            "env-a",
            Date.from(firstDeployedAt.toInstant().plus(Duration.ofHours(2))),
            "2"
        );

        cut.register(environmentA);
        cut.register(environmentB);
        cut.register(environmentAUpdated);

        verify(eventManager).publishEvent(SharedPolicyGroupEvent.UPDATE, environmentAUpdated);
        verify(eventManager, never()).publishEvent(SharedPolicyGroupEvent.UPDATE, environmentB);
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().envId()).isEqualTo("env-a");
        assertThat(revokeCaptor.getValue().metadata().revision()).isEqualTo("1");
        assertThat(cut.sharedPolicyGroups()).containsExactlyInAnyOrder(environmentAUpdated, environmentB);
    }

    @Test
    void should_unregister_only_the_copy_of_the_given_environment() {
        Date firstDeployedAt = new Date();
        ReactableSharedPolicyGroup environmentA = sharedPolicyGroup("env-a", firstDeployedAt, "128");
        ReactableSharedPolicyGroup environmentB = sharedPolicyGroup(
            "env-b",
            Date.from(firstDeployedAt.toInstant().plus(Duration.ofHours(1))),
            "5"
        );
        cut.register(environmentA);
        cut.register(environmentB);

        cut.unregister(SHARED_POLICY_GROUP_ID, "env-a");

        verify(eventManager).publishEvent(SharedPolicyGroupEvent.UNDEPLOY, environmentA);
        verify(eventManager, never()).publishEvent(SharedPolicyGroupEvent.UNDEPLOY, environmentB);
        ArgumentCaptor<SecretDiscoveryEvent> revokeCaptor = ArgumentCaptor.forClass(SecretDiscoveryEvent.class);
        verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.REVOKE), revokeCaptor.capture());
        assertThat(revokeCaptor.getValue().envId()).isEqualTo("env-a");
        assertThat(cut.get(SHARED_POLICY_GROUP_ID, "env-b")).isEqualTo(environmentB);
        assertThat(cut.get(SHARED_POLICY_GROUP_ID, "env-a")).isNull();
    }

    @Test
    void should_unregister_every_copy_of_the_cross_id() {
        Date firstDeployedAt = new Date();
        ReactableSharedPolicyGroup environmentA = sharedPolicyGroup("env-a", firstDeployedAt, "128");
        ReactableSharedPolicyGroup environmentB = sharedPolicyGroup(
            "env-b",
            Date.from(firstDeployedAt.toInstant().plus(Duration.ofHours(1))),
            "5"
        );
        cut.register(environmentA);
        cut.register(environmentB);

        cut.unregisterAll(SHARED_POLICY_GROUP_ID);

        verify(eventManager).publishEvent(SharedPolicyGroupEvent.UNDEPLOY, environmentA);
        verify(eventManager).publishEvent(SharedPolicyGroupEvent.UNDEPLOY, environmentB);
        assertThat(cut.get(SHARED_POLICY_GROUP_ID, "env-a")).isNull();
        assertThat(cut.get(SHARED_POLICY_GROUP_ID, "env-b")).isNull();
    }

    @Test
    void should_update_only_matching_environment_on_secret_value_changed() {
        Date firstDeployedAt = new Date();
        ReactableSharedPolicyGroup environmentB = sharedPolicyGroup("env-b", firstDeployedAt, "5");
        ReactableSharedPolicyGroup environmentA = sharedPolicyGroup(
            "env-a",
            Date.from(firstDeployedAt.toInstant().plus(Duration.ofHours(1))),
            "128"
        );
        cut.register(environmentB);
        cut.register(environmentA);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventListener<SecretDiscoveryEventType, SecretDiscoveryEvent>> listenerCaptor = ArgumentCaptor.forClass(
            EventListener.class
        );
        verify(eventManager).subscribeForEvents(listenerCaptor.capture(), eq(SecretDiscoveryEventType.VALUE_CHANGED));

        listenerCaptor
            .getValue()
            .onEvent(
                new SimpleEvent<>(
                    SecretDiscoveryEventType.VALUE_CHANGED,
                    new SecretDiscoveryEvent(
                        "env-b",
                        new Definition("shared-policy-group", SHARED_POLICY_GROUP_ID),
                        new DefinitionMetadata("5")
                    )
                )
            );

        verify(eventManager).publishEvent(SharedPolicyGroupEvent.UPDATE, environmentB);
        verify(eventManager, never()).publishEvent(SharedPolicyGroupEvent.UPDATE, environmentA);
    }

    private static ReactableSharedPolicyGroup sharedPolicyGroup(String environmentId, Date deployedAt, String version) {
        return new SharedPolicyGroupBuilder()
            .id(SHARED_POLICY_GROUP_ID)
            .environmentId(environmentId)
            .version(version)
            .deployedAt(deployedAt)
            .build();
    }

    static class SharedPolicyGroupBuilder {

        private final SharedPolicyGroup definition = new SharedPolicyGroup();
        private final ReactableSharedPolicyGroup reactableSharedPolicyGroup = new ReactableSharedPolicyGroup();

        {
            reactableSharedPolicyGroup.setDefinition(definition);
        }

        public SharedPolicyGroupManagerImplTest.SharedPolicyGroupBuilder id(String id) {
            reactableSharedPolicyGroup.setId(id);
            this.definition.setId(id);
            return this;
        }

        public SharedPolicyGroupManagerImplTest.SharedPolicyGroupBuilder name(String name) {
            this.definition.setName(name);
            return this;
        }

        public SharedPolicyGroupManagerImplTest.SharedPolicyGroupBuilder environmentId(String environmentId) {
            reactableSharedPolicyGroup.setEnvironmentId(environmentId);
            definition.setEnvironmentId(environmentId);
            return this;
        }

        public SharedPolicyGroupManagerImplTest.SharedPolicyGroupBuilder version(String version) {
            definition.setVersion(version);
            return this;
        }

        public SharedPolicyGroupManagerImplTest.SharedPolicyGroupBuilder deployedAt(Date deployedAt) {
            reactableSharedPolicyGroup.setDeployedAt(deployedAt);
            return this;
        }

        public ReactableSharedPolicyGroup build() {
            return this.reactableSharedPolicyGroup;
        }
    }
}
