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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.event.SharedPolicyGroupEvent;
import io.gravitee.node.api.license.LicenseManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

        cut.unregister(sharedPolicyGroup.getId());
        verify(eventManager).publishEvent(SharedPolicyGroupEvent.UNDEPLOY, sharedPolicyGroup);
        assertThat(cut.sharedPolicyGroups()).isEmpty();
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

        public ReactableSharedPolicyGroup build() {
            return this.reactableSharedPolicyGroup;
        }
    }
}
