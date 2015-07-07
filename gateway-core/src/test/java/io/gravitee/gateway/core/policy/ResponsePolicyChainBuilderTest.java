/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.PolicyConfiguration;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.builder.ApiBuilder;
import io.gravitee.gateway.core.policy.builder.ResponsePolicyChainBuilder;
import io.gravitee.gateway.core.policy.impl.PolicyBuilderImpl;
import io.gravitee.model.Api;
import io.gravitee.model.Policy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ResponsePolicyChainBuilderTest {

    @Mock
    private PolicyRegistry policyRegistry;

    private PolicyBuilder policyBuilder = new PolicyBuilderImpl();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(policyRegistry.getPolicy("dummy-policy")).thenReturn(new PolicyDefinition() {
            @Override
            public String name() {
                return "my-policy";
            }

            @Override
            public String description() {
                return null;
            }

            @Override
            public Class<? extends io.gravitee.gateway.api.Policy> policy() {
                return DummyPolicy.class;
            }

            @Override
            public Class<PolicyConfiguration> configuration() {
                return null;
            }
        });
    }

    @Test
    public void createEmptyChainBuilder() {
        ResponsePolicyChainBuilder builder = new ResponsePolicyChainBuilder();

        builder.setPolicyRegistry(policyRegistry);
        builder.setPolicyBuilder(policyBuilder);

        Api api = new ApiBuilder()
                .name("my-team-api")
                .origin("http://localhost/team")
                .target("http://localhost:8083/myapi")
                .build();

        builder.setApi(api);

        Request request = mock(Request.class);

        ResponsePolicyChain chain = builder.newPolicyChain(request);
        Assert.assertTrue(chain.getPolicies().isEmpty());
    }

    @Test
    public void createChainBuilderWithNullPolicy() {
        ResponsePolicyChainBuilder builder = new ResponsePolicyChainBuilder();

        builder.setPolicyRegistry(policyRegistry);
        builder.setPolicyBuilder(policyBuilder);

        Api api = new ApiBuilder()
                .name("my-team-api")
                .origin("http://localhost/team")
                .target("http://localhost:8083/myapi")
                .build();

        Map<String, Policy> policies = new HashMap<>();
        policies.put("response-time", null);
        api.setPolicies(policies);

        builder.setApi(api);

        Request request = mock(Request.class);

        ResponsePolicyChain chain = builder.newPolicyChain(request);
        Assert.assertTrue(chain.getPolicies().isEmpty());
    }

    @Test
    public void createChainBuilderWithNullPolicyName() {
        ResponsePolicyChainBuilder builder = new ResponsePolicyChainBuilder();

        builder.setPolicyRegistry(policyRegistry);
        builder.setPolicyBuilder(policyBuilder);

        Api api = new ApiBuilder()
                .name("my-team-api")
                .origin("http://localhost/team")
                .target("http://localhost:8083/myapi")
                .build();

        Map<String, Policy> policies = new HashMap<>();
        policies.put(null, null);
        api.setPolicies(policies);

        builder.setApi(api);

        Request request = mock(Request.class);

        ResponsePolicyChain chain = builder.newPolicyChain(request);
        Assert.assertTrue(chain.getPolicies().isEmpty());
    }

    @Test
    public void createChainBuilderWithKnownPolicy() {
        ResponsePolicyChainBuilder builder = new ResponsePolicyChainBuilder();

        builder.setPolicyRegistry(policyRegistry);
        builder.setPolicyBuilder(policyBuilder);

        Api api = new ApiBuilder()
                .name("my-team-api")
                .origin("http://localhost/team")
                .target("http://localhost:8083/myapi")
                .build();

        Map<String, Policy> policies = new HashMap<>();

        Policy policy = new Policy();
        policy.setName("dummy-policy");
        policy.setConfiguration("{}");
        policies.put("dummy-policy", policy);

        api.setPolicies(policies);

        builder.setApi(api);

        Request request = mock(Request.class);

        ResponsePolicyChain chain = builder.newPolicyChain(request);
        Assert.assertTrue(chain.getPolicies().size() == 1);
    }

    @Test
    public void createChainBuilderWithUnknownPolicy() {
        ResponsePolicyChainBuilder builder = new ResponsePolicyChainBuilder();

        builder.setPolicyRegistry(policyRegistry);
        builder.setPolicyBuilder(policyBuilder);

        Api api = new ApiBuilder()
                .name("my-team-api")
                .origin("http://localhost/team")
                .target("http://localhost:8083/myapi")
                .build();

        Map<String, Policy> policies = new HashMap<>();

        Policy policy = new Policy();
        policy.setName("unknown-policy");
        policy.setConfiguration("{}");
        policies.put("unknown-policy", policy);

        api.setPolicies(policies);

        builder.setApi(api);

        Request request = mock(Request.class);

        ResponsePolicyChain chain = builder.newPolicyChain(request);
        Assert.assertTrue(chain.getPolicies().size() == 0);
    }
}
