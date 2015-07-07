package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.PolicyConfiguration;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.builder.ApiBuilder;
import io.gravitee.gateway.core.policy.builder.RequestPolicyChainBuilder;
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
public class RequestPolicyChainBuilderTest {

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
        RequestPolicyChainBuilder builder = new RequestPolicyChainBuilder();

        builder.setPolicyRegistry(policyRegistry);
        builder.setPolicyBuilder(policyBuilder);

        Api api = new ApiBuilder()
                .name("my-team-api")
                .origin("http://localhost/team")
                .target("http://localhost:8083/myapi")
                .build();

        builder.setApi(api);

        Request request = mock(Request.class);

        RequestPolicyChain chain = builder.newPolicyChain(request);
        Assert.assertTrue(chain.getPolicies().isEmpty());
    }

    @Test
    public void createChainBuilderWithNullPolicy() {
        RequestPolicyChainBuilder builder = new RequestPolicyChainBuilder();

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

        RequestPolicyChain chain = builder.newPolicyChain(request);
        Assert.assertTrue(chain.getPolicies().isEmpty());
    }

    @Test
    public void createChainBuilderWithNullPolicyName() {
        RequestPolicyChainBuilder builder = new RequestPolicyChainBuilder();

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

        RequestPolicyChain chain = builder.newPolicyChain(request);
        Assert.assertTrue(chain.getPolicies().isEmpty());
    }

    @Test
    public void createChainBuilderWithKnownPolicy() {
        RequestPolicyChainBuilder builder = new RequestPolicyChainBuilder();

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

        RequestPolicyChain chain = builder.newPolicyChain(request);
        Assert.assertTrue(chain.getPolicies().size() == 1);
    }

    @Test
    public void createChainBuilderWithUnknownPolicy() {
        RequestPolicyChainBuilder builder = new RequestPolicyChainBuilder();

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

        RequestPolicyChain chain = builder.newPolicyChain(request);
        Assert.assertTrue(chain.getPolicies().size() == 0);
    }
}
