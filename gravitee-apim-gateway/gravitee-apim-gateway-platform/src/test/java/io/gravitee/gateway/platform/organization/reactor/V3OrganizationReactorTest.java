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
package io.gravitee.gateway.platform.organization.reactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.gravitee.definition.model.Organization;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.platform.organization.policy.OrganizationPolicyManager;
import io.gravitee.gateway.reactive.platform.organization.reactor.DefaultOrganizationReactor;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class V3OrganizationReactorTest {

    @Mock
    private PolicyChainFactory policyChainFactory;

    @Mock
    private ResourceLifecycleManager resourceLifecycleManager;

    @Mock
    private OrganizationPolicyManager organizationPolicyManager;

    @Test
    void should_create_default_organization_reactor() {
        Organization organization = new Organization();
        organization.setId("id");
        ReactableOrganization reactableOrganization = new ReactableOrganization(organization);
        V3OrganizationReactor cut = new V3OrganizationReactor(
            reactableOrganization,
            policyChainFactory,
            resourceLifecycleManager,
            organizationPolicyManager
        );

        assertThat(cut.id()).isEqualTo(organization.getId());
        assertThat(cut.reactableOrganization()).isEqualTo(reactableOrganization);
        assertThat(cut.policyChainFactory()).isEqualTo(policyChainFactory);
    }

    @Test
    void should_start_organization_reactor() throws Exception {
        Organization organization = new Organization();
        organization.setId("id");
        ReactableOrganization reactableOrganization = new ReactableOrganization(organization);
        V3OrganizationReactor cut = new V3OrganizationReactor(
            reactableOrganization,
            policyChainFactory,
            resourceLifecycleManager,
            organizationPolicyManager
        );

        cut.start();
        verify(organizationPolicyManager).start();
    }

    @Test
    void should_stop_organization_reactor() throws Exception {
        Organization organization = new Organization();
        organization.setId("id");
        ReactableOrganization reactableOrganization = new ReactableOrganization(organization);
        V3OrganizationReactor cut = new V3OrganizationReactor(
            reactableOrganization,
            policyChainFactory,
            resourceLifecycleManager,
            organizationPolicyManager
        );

        cut.stop();
        verify(organizationPolicyManager).stop();
    }
}
