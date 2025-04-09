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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiExposedEntrypointDomainServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ExposedEntrypoint;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiResource_ExposedEntrypointsTest extends ApiResourceTest {

    @Inject
    private ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Inject
    private ApiExposedEntrypointDomainServiceInMemory apiExposedEntrypointDomainServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/exposedEntrypoints";
    }

    @Test
    public void should_get_ExposedEntrypoints() {
        ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
        deployEntity.setDeploymentLabel("label");

        Api api = ApiFixtures.aProxyApiV4();
        apiCrudServiceInMemory.initWith(List.of(api));
        apiExposedEntrypointDomainServiceInMemory.initWith(List.of(new ExposedEntrypoint("http://myapi.domain.com")));

        final Response response = rootTarget().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        final List<io.gravitee.rest.api.management.v2.rest.model.ExposedEntrypoint> exposedEntrypoints = response.readEntity(
            new GenericType<>() {}
        );

        assertNotNull(exposedEntrypoints);
        assertEquals(1, exposedEntrypoints.size());
        assertEquals("http://myapi.domain.com", exposedEntrypoints.get(0).getValue());
    }
}
