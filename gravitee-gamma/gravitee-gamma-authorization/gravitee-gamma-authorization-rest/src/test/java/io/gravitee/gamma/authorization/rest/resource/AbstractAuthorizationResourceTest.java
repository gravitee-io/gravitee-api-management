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
package io.gravitee.gamma.authorization.rest.resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import io.gravitee.gamma.authorization.api.AuthzEntityAdminApi;
import io.gravitee.gamma.authorization.api.AuthzPolicyAdminApi;
import io.gravitee.gamma.authorization.api.AuthzSchemaAdminApi;
import io.gravitee.gamma.authorization.rest.exception.AuthzApiExceptionMapper;
import io.gravitee.gamma.authorization.rest.exception.IllegalArgumentExceptionMapper;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.support.GenericApplicationContext;

abstract class AbstractAuthorizationResourceTest extends JerseyTest {

    protected AuthzPolicyAdminApi policyService;
    protected AuthzEntityAdminApi entityService;
    protected AuthzSchemaAdminApi schemaService;

    @BeforeEach
    void resetMocks() {
        reset(policyService, entityService, schemaService);
    }

    @Override
    protected Application configure() {
        policyService = mock(AuthzPolicyAdminApi.class);
        entityService = mock(AuthzEntityAdminApi.class);
        schemaService = mock(AuthzSchemaAdminApi.class);
        GenericApplicationContext emptySpringContext = new GenericApplicationContext();
        emptySpringContext.refresh();
        ResourceConfig config = new ResourceConfig()
            .register(TestGraviteeContextFilter.class)
            .register(new AuthzPoliciesResource(policyService))
            .register(new AuthzEntitiesResource(entityService))
            .register(new AuthzSchemaResource(schemaService))
            .register(AuthzApiExceptionMapper.class)
            .register(IllegalArgumentExceptionMapper.class)
            .register(JacksonFeature.class);
        config.property("contextConfig", emptySpringContext);
        return config;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(JacksonFeature.class);
    }
}
