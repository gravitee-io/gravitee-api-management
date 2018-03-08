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

import static org.junit.Assert.assertEquals;

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

    @Test
    public void shouldPrepareAPIFromSwaggerV2_json() throws IOException {
        URL url =  Resources.getResource("io/gravitee/management/service/swagger-petstore.json");
        String descriptor = Resources.toString(url, Charsets.UTF_8);
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setPayload(descriptor);

        NewApiEntity api = swaggerService.prepare(swaggerDescriptor);

        assertEquals("1.0.0", api.getVersion());
        assertEquals("Swagger Petstore (Simple)", api.getName());
        assertEquals("http://petstore.swagger.io/api", api.getEndpoint());
        assertEquals(2, api.getPaths().size());
        assertEquals("/pets/:id", api.getPaths().get(1));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_yaml() throws IOException {
        URL url =  Resources.getResource("io/gravitee/management/service/swagger-petstore.yaml");
        String descriptor = Resources.toString(url, Charsets.UTF_8);
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setPayload(descriptor);

        NewApiEntity api = swaggerService.prepare(swaggerDescriptor);

        assertEquals("1.0.0", api.getVersion());
        assertEquals("Swagger Petstore (Simple)", api.getName());
        assertEquals("http://petstore.swagger.io/api", api.getEndpoint());
        assertEquals(2, api.getPaths().size());
        assertEquals("/pets/:id", api.getPaths().get(1));
    }
}
