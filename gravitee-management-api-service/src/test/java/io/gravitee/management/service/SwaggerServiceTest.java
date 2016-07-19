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
import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.service.impl.SwaggerServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SwaggerServiceTest {

    private SwaggerService swaggerService;

    @Before
    public void setUp() {
        swaggerService = new SwaggerServiceImpl();
    }

    @Test
    public void shouldPrepareAPIFromSwagger_json() throws IOException {
        URL url =  Resources.getResource("io/gravitee/management/service/swagger-petstore.json");
        String swagger = Resources.toString(url, Charsets.UTF_8);
        NewApiEntity api = swaggerService.prepare(swagger);

        Assert.assertEquals("1.0.0", api.getVersion());
        Assert.assertEquals("Swagger Petstore (Simple)", api.getName());
        Assert.assertEquals("http://petstore.swagger.io/api", api.getEndpoint());
        Assert.assertEquals(2, api.getPaths().size());
        Assert.assertEquals("/pets/:id", api.getPaths().get(1));
    }

    @Test
    public void shouldPrepareAPIFromSwagger_yaml() throws IOException {
        URL url =  Resources.getResource("io/gravitee/management/service/swagger-petstore.yaml");
        String swagger = Resources.toString(url, Charsets.UTF_8);
        NewApiEntity api = swaggerService.prepare(swagger);

        Assert.assertEquals("1.0.0", api.getVersion());
        Assert.assertEquals("Swagger Petstore (Simple)", api.getName());
        Assert.assertEquals("http://petstore.swagger.io/api", api.getEndpoint());
        Assert.assertEquals(2, api.getPaths().size());
        Assert.assertEquals("/pets/:id", api.getPaths().get(1));
    }
}
