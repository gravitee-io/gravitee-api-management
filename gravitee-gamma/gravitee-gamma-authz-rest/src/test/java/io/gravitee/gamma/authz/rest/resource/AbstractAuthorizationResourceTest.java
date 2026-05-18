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
package io.gravitee.gamma.authz.rest.resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import io.gravitee.apim.authorization.api.EntityAdminApi;
import io.gravitee.apim.authorization.api.PolicyAdminApi;
import io.gravitee.apim.authorization.api.SchemaAdminApi;
import io.gravitee.gamma.authz.rest.exception.CascadeTooLargeExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.EntityNotFoundExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.ForbiddenAccessExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.IllegalArgumentExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.InvalidEntityIdExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.InvalidStatusTransitionExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.PolicyNotFoundExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.UnauthorizedAccessExceptionMapper;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.support.GenericApplicationContext;

abstract class AbstractAuthorizationResourceTest extends JerseyTest {

    protected PolicyAdminApi policyService;
    protected EntityAdminApi entityService;
    protected SchemaAdminApi schemaService;

    @BeforeEach
    void resetMocks() {
        reset(policyService, entityService, schemaService);
    }

    @Override
    protected Application configure() {
        policyService = mock(PolicyAdminApi.class);
        entityService = mock(EntityAdminApi.class);
        schemaService = mock(SchemaAdminApi.class);
        GenericApplicationContext emptySpringContext = new GenericApplicationContext();
        emptySpringContext.refresh();
        ResourceConfig config = new ResourceConfig()
            .register(TestGraviteeContextFilter.class)
            .register(new PoliciesResource(policyService))
            .register(new EntitiesResource(entityService))
            .register(new SchemaResource(schemaService))
            .register(new HealthResource())
            .register(PolicyNotFoundExceptionMapper.class)
            .register(EntityNotFoundExceptionMapper.class)
            .register(CascadeTooLargeExceptionMapper.class)
            .register(InvalidStatusTransitionExceptionMapper.class)
            .register(InvalidEntityIdExceptionMapper.class)
            .register(IllegalArgumentExceptionMapper.class)
            .register(UnauthorizedAccessExceptionMapper.class)
            .register(ForbiddenAccessExceptionMapper.class)
            .register(JacksonFeature.class);
        config.property("contextConfig", emptySpringContext);
        return config;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(JacksonFeature.class);
    }
}
