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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.gravitee.rest.api.management.v2.rest.model.SourceConfiguration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */
class PageMapperTest extends AbstractMapperTest {

    private PageMapper pageMapper;

    @BeforeEach
    void setUp() {
        pageMapper = PageMapper.INSTANCE;
    }

    @Test
    void shouldMapSourceConfigurationToPageSource() throws Exception {
        var sourceConfiguration = new SourceConfiguration();
        sourceConfiguration.setType("http-fetcher");

        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("useSystemProxy", false);
        configMap.put("autoFetch", false);
        configMap.put("url", "https://apim-master-api.team-apim.gravitee.dev/management/openapi.yaml");
        sourceConfiguration.setConfiguration(configMap);

        var pageSource = pageMapper.mapSourceConfigurationToPageSource(sourceConfiguration);

        assertNotNull(pageSource);
        assertEquals("http-fetcher", pageSource.getType());

        var expectedConfig =
            """
                {
                  "useSystemProxy" : false,
                  "autoFetch" : false,
                  "url" : "https://apim-master-api.team-apim.gravitee.dev/management/openapi.yaml"
                }""";
        assertNotNull(pageSource.getConfiguration());
        assertEquals(expectedConfig, pageSource.getConfiguration().toString());
    }
}
