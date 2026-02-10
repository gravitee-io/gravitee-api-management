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
package io.gravitee.gateway.reactive.handlers.api.v4.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for v4 HttpSecurityChain with API Product support.
 *
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HttpSecurityChainTest {

    @Mock
    private PolicyManager policyManager;

    @Mock
    private PolicyManager apiProductPlanPolicyManager;

    private Api createMinimalApi() {
        Api api = new Api();
        api.setId("api-1");
        api.setName("Test API");
        api.setApiVersion("1.0");
        api.setType(ApiType.PROXY);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setPlans(Collections.emptyList());
        HttpListener listener = new HttpListener();
        listener.setPaths(List.of(new Path("/")));
        api.setListeners(List.of(listener));
        api.setEndpointGroups(Collections.emptyList());
        return api;
    }

    @Test
    void should_construct_with_api_product_plan_policy_manager() {
        Api api = createMinimalApi();

        HttpSecurityChain chain = new HttpSecurityChain(
            api,
            policyManager,
            ExecutionPhase.REQUEST,
            "env-1",
            null,
            apiProductPlanPolicyManager
        );

        assertThat(chain).isNotNull();
    }

    @Test
    void should_refresh_preserve_api_product_plan_policy_manager() {
        Api api = createMinimalApi();

        HttpSecurityChain chain = new HttpSecurityChain(
            api,
            policyManager,
            ExecutionPhase.REQUEST,
            "env-1",
            null,
            apiProductPlanPolicyManager
        );

        HttpSecurityChain refreshed = chain.refresh();

        assertThat(refreshed).isNotNull().isNotSameAs(chain);
    }
}
