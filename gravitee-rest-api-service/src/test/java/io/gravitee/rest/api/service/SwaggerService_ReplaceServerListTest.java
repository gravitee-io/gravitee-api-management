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
package io.gravitee.rest.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.impl.SwaggerServiceImpl;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SwaggerService_ReplaceServerListTest {
    
    private SwaggerService swaggerService = new SwaggerServiceImpl();
    
    private List<String> graviteeUrls = Arrays.asList("http://foo.bar/contextpath", "http://foo.bar/tag/contextpath");
    
    @Before
    public void setUp() {
        setField(swaggerService, "mapper", new ObjectMapper());
    }
    
    // Swagger v1
    @Test
    public void shouldNotReplaceUrlFromSwaggerV1_json() throws IOException {
        String payload = prepareInline("io/gravitee/rest/api/management/service/swagger-v1.json");
        String result = swaggerService.replaceServerList(payload, graviteeUrls);
        assertEquals(payload, result);
    }
    
    // Swagger v2
    @Test
    public void shouldNotReplaceUrlFromSwaggerV2_json() throws IOException {
        String payload = prepareInline("io/gravitee/rest/api/management/service/swagger-v2.json");
        String result = swaggerService.replaceServerList(payload, graviteeUrls);
        assertEquals(payload, result);
    }
    
    @Test
    public void shouldNotReplaceUrlFromSwaggerV2_yaml() throws IOException {
        String payload = prepareInline("io/gravitee/rest/api/management/service/swagger-v2.yaml");
        String result = swaggerService.replaceServerList(payload, graviteeUrls);
        assertEquals(payload, result);
    }
    
    // OpenAPI
    @Test
    public void shouldReplaceUrlFromSwaggerV3_json() throws IOException {
        String payload = prepareInline("io/gravitee/rest/api/management/service/openapi.json");
        String result = swaggerService.replaceServerList(payload, graviteeUrls);
        
        assertFalse(result.contains("https://demo.gravitee.io/gateway/echo"));
        assertTrue(result.contains(graviteeUrls.get(0)));
        assertTrue(result.contains(graviteeUrls.get(1)));
    }
    
    @Test
    public void shouldReplaceUrlFromSwaggerV3_yaml() throws IOException {
        String payload = prepareInline("io/gravitee/rest/api/management/service/openapi.yaml");
        String result = swaggerService.replaceServerList(payload, graviteeUrls);
        
        assertFalse(result.contains("https://demo.gravitee.io/gateway/echo"));
        assertTrue(result.contains(graviteeUrls.get(0)));
        assertTrue(result.contains(graviteeUrls.get(1)));
    }
    
    private String prepareInline(String file) throws IOException {
        URL url = Resources.getResource(file);
        return Resources.toString(url, Charsets.UTF_8);
    }
    
}
