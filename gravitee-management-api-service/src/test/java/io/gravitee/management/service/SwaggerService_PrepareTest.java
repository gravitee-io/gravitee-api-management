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
package io.gravitee.management.service;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.management.model.ImportSwaggerDescriptorEntity;
import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.service.impl.SwaggerServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SwaggerService_PrepareTest {

    private SwaggerService swaggerService;

    @Before
    public void setUp() {
        swaggerService = new SwaggerServiceImpl();
    }

    // Swagger v1
    @Test
    public void shouldPrepareAPIFromSwaggerV1_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/management/service/swagger-v1.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV1_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/management/service/swagger-v1.json"));
    }

    // Swagger v2
    @Test
    public void shouldPrepareAPIFromSwaggerV2_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/management/service/swagger-v2.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/management/service/swagger-v2.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_URL_yaml() throws IOException {
        validate(prepareUrl("io/gravitee/management/service/swagger-v2.yaml"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_Inline_yaml() throws IOException {
        validate(prepareInline("io/gravitee/management/service/swagger-v2.yaml"));
    }


    // OpenAPI
    @Test
    public void shouldPrepareAPIFromSwaggerV3_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/management/service/openapi.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/management/service/openapi.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_URL_yaml() throws IOException {
        validate(prepareUrl("io/gravitee/management/service/openapi.yaml"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_Inline_yaml() throws IOException {
        validate(prepareInline("io/gravitee/management/service/openapi.yaml"));
    }

    private void validate(NewApiEntity api) {
        assertEquals("1.2.3", api.getVersion());
        assertEquals("Gravitee.io Swagger API", api.getName());
        assertEquals("https://demo.gravitee.io/gateway/echo", api.getEndpoint());
        assertEquals(2, api.getPaths().size());
        assertTrue(api.getPaths().containsAll(Arrays.asList("/pets", "/pets/:petId")));
    }

    private NewApiEntity prepareInline(String file) throws IOException {
        URL url =  Resources.getResource(file);
        String descriptor = Resources.toString(url, Charsets.UTF_8);
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setType(ImportSwaggerDescriptorEntity.Type.INLINE);
        swaggerDescriptor.setPayload(descriptor);

        return swaggerService.prepare(swaggerDescriptor);
    }

    private NewApiEntity prepareUrl(String file) throws IOException {
        URL url =  Resources.getResource(file);
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setType(ImportSwaggerDescriptorEntity.Type.URL);
        swaggerDescriptor.setPayload(url.getPath());

        return swaggerService.prepare(swaggerDescriptor);
    }
}
