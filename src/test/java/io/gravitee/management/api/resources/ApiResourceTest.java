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
package io.gravitee.management.api.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.gravitee.management.api.builder.ApiBuilder;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.model.Api;

import java.util.Date;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiResourceTest extends AbstractResourceTest {

    @Autowired
    private ApiRepository apiRepository;

    @Test
    public void testGetApi() {
        Api api = new ApiBuilder()
                .name("my-api")
                .origin("http://localhost/my-api")
                .target("http://remote_api/context")
                .createdAt(new Date())
                .build();

        Mockito.doReturn(api).when(apiRepository).findByName(api.getName());

        final Response response = target("/apis/" + api.getName()).request().get();

        // Check HTTP response
        assertEquals(200, response.getStatus());

        // Check Response content
        Api responseApi = response.readEntity(Api.class);
        assertNotNull(responseApi);
    }
}
