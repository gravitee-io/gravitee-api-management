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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageSource;
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

        var expectedConfig = """
            {
              "useSystemProxy" : false,
              "autoFetch" : false,
              "url" : "https://apim-master-api.team-apim.gravitee.dev/management/openapi.yaml"
            }""";
        assertNotNull(pageSource.getConfiguration());
        assertEquals(expectedConfig, pageSource.getConfiguration().toString());
    }

    @Test
    void shouldMaskSensitiveDataInPageConfiguration() throws Exception {
        // Create a page with sensitive data in the configuration
        String configurationWithSensitiveData = """
            {
              "gitlabUrl": "https://gitlab.com/api/v4",
              "useSystemProxy": false,
              "namespace": "group",
              "project": "project",
              "branchOrTag": "main",
              "filepath": "/petstore",
              "privateToken": "exposedTokenHere",
              "apiVersion": "V4",
              "editLink": null,
              "fetchCron": null,
              "autoFetch": false
            }""";

        PageSource pageSource = PageSource.builder().type("gitlab-fetcher").configuration(configurationWithSensitiveData).build();

        Page page = Page.builder().id("page-id").name("Test Page").type(Page.Type.MARKDOWN).source(pageSource).build();

        // Map the page using the mapper
        var mappedPage = pageMapper.mapPage(page);

        // Verify that sensitive data is masked
        assertNotNull(mappedPage);
        assertNotNull(mappedPage.getSource());
        assertNotNull(mappedPage.getSource().getConfiguration());

        // The configuration should be a LinkedHashMap with masked sensitive data
        var config = mappedPage.getSource().getConfiguration();
        assertNotNull(config);

        // Parse the configuration to verify sensitive data is masked
        if (config instanceof LinkedHashMap) {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> configMap = (LinkedHashMap<String, Object>) config;

            // Verify that privateToken is masked
            assertThat(configMap.get("privateToken")).isEqualTo("********");

            // Verify that other non-sensitive data is preserved
            assertEquals("https://gitlab.com/api/v4", configMap.get("gitlabUrl"));
            assertEquals(false, configMap.get("useSystemProxy"));
            assertEquals("group", configMap.get("namespace"));
            assertEquals("project", configMap.get("project"));
            assertEquals("main", configMap.get("branchOrTag"));
            assertEquals("/petstore", configMap.get("filepath"));
            assertEquals("V4", configMap.get("apiVersion"));
            assertEquals(false, configMap.get("autoFetch"));
        }
    }

    @Test
    void shouldMaskMultipleSensitiveFields() throws Exception {
        // Create a configuration with multiple sensitive fields
        String configurationWithMultipleSensitiveData = """
            {
              "url": "https://api.example.com",
              "username": "testuser",
              "password": "secretPassword",
              "apiKey": "myApiKey123",
              "accessToken": "accessTokenValue",
              "clientSecret": "clientSecretValue",
              "personalAccessToken": "pat123456",
              "authToken": "authTokenValue",
              "useSystemProxy": false
            }""";

        PageSource pageSource = PageSource.builder().type("http-fetcher").configuration(configurationWithMultipleSensitiveData).build();

        Page page = Page.builder().id("page-id").name("Test Page").type(Page.Type.MARKDOWN).source(pageSource).build();

        // Map the page using the mapper
        var mappedPage = pageMapper.mapPage(page);

        // Verify that all sensitive data is masked
        assertNotNull(mappedPage);
        assertNotNull(mappedPage.getSource());
        assertNotNull(mappedPage.getSource().getConfiguration());

        var config = mappedPage.getSource().getConfiguration();
        assertNotNull(config);

        if (config instanceof LinkedHashMap) {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> configMap = (LinkedHashMap<String, Object>) config;

            // Verify that all sensitive fields are masked
            assertEquals("********", configMap.get("password"));
            assertEquals("********", configMap.get("apiKey"));
            assertEquals("********", configMap.get("accessToken"));
            assertEquals("********", configMap.get("clientSecret"));
            assertEquals("********", configMap.get("personalAccessToken"));
            assertEquals("********", configMap.get("authToken"));

            // Verify that non-sensitive data is preserved
            assertEquals("https://api.example.com", configMap.get("url"));
            assertEquals("testuser", configMap.get("username"));
            assertEquals(false, configMap.get("useSystemProxy"));
        }
    }

    @Test
    void shouldHandleNullConfiguration() throws Exception {
        PageSource pageSource = PageSource.builder().type("http-fetcher").configuration(null).build();

        Page page = Page.builder().id("page-id").name("Test Page").type(Page.Type.MARKDOWN).source(pageSource).build();

        // Map the page using the mapper
        var mappedPage = pageMapper.mapPage(page);

        // Verify that null configuration is handled gracefully
        assertNotNull(mappedPage);
        assertNotNull(mappedPage.getSource());
        assertNull(mappedPage.getSource().getConfiguration());
    }

    @Test
    void shouldHandleNonJsonConfiguration() throws Exception {
        // Create a page with non-JSON configuration
        PageSource pageSource = PageSource.builder().type("http-fetcher").configuration("not-a-json-string").build();

        Page page = Page.builder().id("page-id").name("Test Page").type(Page.Type.MARKDOWN).source(pageSource).build();

        // Map the page using the mapper
        var mappedPage = pageMapper.mapPage(page);

        // Verify that non-JSON configuration is returned as-is
        assertNotNull(mappedPage);
        assertNotNull(mappedPage.getSource());
        assertEquals("not-a-json-string", mappedPage.getSource().getConfiguration());
    }

    @Test
    void shouldUnmaskSensitiveDataInConfiguration() {
        // Given
        String newConfiguration = """
            {
                "gitlabUrl": "https://gitlab.com/api/v4",
                "useSystemProxy": false,
                "namespace": "test",
                "project": null,
                "branchOrTag": "master",
                "filepath": null,
                "privateToken": "********",
                "apiVersion": "V4",
                "editLink": null,
                "fetchCron": null,
                "autoFetch": false
            }
            """;

        String originalConfiguration = """
            {
                "gitlabUrl": "https://gitlab.com/api/v4",
                "useSystemProxy": false,
                "namespace": "test",
                "project": null,
                "branchOrTag": "master",
                "filepath": null,
                "privateToken": "glpat-real-token-abc123",
                "apiVersion": "V4",
                "editLink": null,
                "fetchCron": null,
                "autoFetch": false
            }
            """;

        // When
        Object result = pageMapper.unmaskSensitiveDataInConfiguration(newConfiguration, originalConfiguration);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(LinkedHashMap.class);

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> configMap = (LinkedHashMap<String, Object>) result;

        // Verify that masked sensitive data is restored
        assertThat(configMap.get("privateToken")).isEqualTo("glpat-real-token-abc123");
        assertThat(configMap.get("gitlabUrl")).isEqualTo("https://gitlab.com/api/v4");
        assertThat(configMap.get("namespace")).isEqualTo("test");
    }

    @Test
    void shouldNotUnmaskNonMaskedValues() {
        // Given
        String newConfiguration = """
            {
                "gitlabUrl": "https://gitlab.com/api/v4",
                "useSystemProxy": false,
                "namespace": "test",
                "project": null,
                "branchOrTag": "master",
                "filepath": null,
                "privateToken": "new-token-value",
                "apiVersion": "V4",
                "editLink": null,
                "fetchCron": null,
                "autoFetch": false
            }
            """;

        String originalConfiguration = """
            {
                "gitlabUrl": "https://gitlab.com/api/v4",
                "useSystemProxy": false,
                "namespace": "test",
                "project": null,
                "branchOrTag": "master",
                "filepath": null,
                "privateToken": "glpat-real-token-abc123",
                "apiVersion": "V4",
                "editLink": null,
                "fetchCron": null,
                "autoFetch": false
            }
            """;

        // When
        Object result = pageMapper.unmaskSensitiveDataInConfiguration(newConfiguration, originalConfiguration);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(LinkedHashMap.class);

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> configMap = (LinkedHashMap<String, Object>) result;

        // Verify that non-masked values are preserved
        assertThat(configMap.get("privateToken")).isEqualTo("new-token-value");
        assertThat(configMap.get("gitlabUrl")).isEqualTo("https://gitlab.com/api/v4");
        assertThat(configMap.get("namespace")).isEqualTo("test");
    }
}
