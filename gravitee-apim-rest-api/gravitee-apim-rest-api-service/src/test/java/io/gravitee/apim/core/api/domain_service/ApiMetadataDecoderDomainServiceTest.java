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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiMetadataQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService.ApiMetadataDecodeContext;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.documentation.model.PrimaryOwnerApiTemplateData;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.definition.model.HttpProxy;
import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiMetadataDecoderDomainServiceTest {

    private static final String ENV_ID = "env#1";
    private static final String API_ID = "api-id";

    private static final ApiMetadataDecodeContext CONTEXT = ApiMetadataDecodeContext.builder()
        .name("My Api")
        .description("api-description")
        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
        .primaryOwner(PrimaryOwnerApiTemplateData.builder().displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build())
        .build();

    private static final PrimaryOwnerEntity PRIMARY_OWNER = PrimaryOwnerEntity.builder()
        .id("user-id")
        .displayName("Jane Doe")
        .email("jane.doe@gravitee.io")
        .type(PrimaryOwnerEntity.Type.USER)
        .build();

    ApiMetadataQueryServiceInMemory apiMetadataQueryService = new ApiMetadataQueryServiceInMemory();

    ApiMetadataDecoderDomainService service;

    @BeforeEach
    void setUp() {
        service = new ApiMetadataDecoderDomainService(apiMetadataQueryService, new FreemarkerTemplateProcessor());
    }

    @Test
    public void should_return_simple_metadata() {
        // Given
        givenExistingApiMetadata(
            List.of(
                ApiMetadata.builder().apiId(API_ID).key("key1").value("value1").format(Metadata.MetadataFormat.STRING).build(),
                ApiMetadata.builder().apiId(API_ID).key("key2").value("true").format(Metadata.MetadataFormat.BOOLEAN).build()
            )
        );

        // When
        var result = service.decodeMetadata(ENV_ID, API_ID, CONTEXT);

        // Then
        assertThat(result).isEqualTo(Map.of("key1", "value1", "key2", "true"));
    }

    @Test
    public void should_filter_metadata_with_null_value() {
        // Given
        givenExistingApiMetadata(
            List.of(
                ApiMetadata.builder().apiId(API_ID).key("key1").value("value1").format(Metadata.MetadataFormat.STRING).build(),
                ApiMetadata.builder().apiId(API_ID).key("null_key").value(null).format(Metadata.MetadataFormat.STRING).build()
            )
        );

        // When
        var result = service.decodeMetadata(ENV_ID, API_ID, CONTEXT);

        // Then
        assertThat(result).isEqualTo(Map.of("key1", "value1"));
    }

    @Test
    public void should_decode_metadata_having_el() {
        // Given
        givenExistingApiMetadata(
            List.of(
                ApiMetadata.builder().apiId(API_ID).key("apiName").value("${(api.name)!''}").format(Metadata.MetadataFormat.STRING).build(),
                ApiMetadata.builder()
                    .apiId(API_ID)
                    .key("apiDescription")
                    .value("${(api.description)!''}")
                    .format(Metadata.MetadataFormat.STRING)
                    .build(),
                ApiMetadata.builder()
                    .apiId(API_ID)
                    .key("ownerName")
                    .value("${(api.primaryOwner.displayName)!''}")
                    .format(Metadata.MetadataFormat.STRING)
                    .build(),
                ApiMetadata.builder()
                    .apiId(API_ID)
                    .key("email-support")
                    .value("${(api.primaryOwner.email)!''}")
                    .format(Metadata.MetadataFormat.STRING)
                    .build()
            )
        );

        // When
        var result = service.decodeMetadata(ENV_ID, API_ID, CONTEXT);

        // Then
        assertThat(result).isEqualTo(
            Map.ofEntries(
                Map.entry("apiName", "My Api"),
                Map.entry("apiDescription", "api-description"),
                Map.entry("ownerName", "Jane Doe"),
                Map.entry("email-support", "jane.doe@gravitee.io")
            )
        );
    }

    @Test
    public void should_return_raw_metadata_when_one_of_them_has_invalid_el() {
        // Given
        givenExistingApiMetadata(
            List.of(
                ApiMetadata.builder().apiId(API_ID).key("key1").value("value1").format(Metadata.MetadataFormat.STRING).build(),
                ApiMetadata.builder().apiId(API_ID).key("apiVersion").value("${api.version}").format(Metadata.MetadataFormat.STRING).build()
            )
        );

        // When
        var result = service.decodeMetadata(ENV_ID, API_ID, CONTEXT);

        // Then
        assertThat(result).isEqualTo(Map.ofEntries(Map.entry("key1", "value1"), Map.entry("apiVersion", "${api.version}")));
    }

    @Test
    void should_not_expose_v2_proxy_definition_to_metadata_templates() {
        // Given
        var api = ApiFixtures.aProxyApiV2();
        var httpProxy = new HttpProxy();
        httpProxy.setPassword("proxy-secret");
        api
            .getApiDefinition()
            .getProxy()
            .getGroups()
            .forEach(group -> group.setHttpProxy(httpProxy));
        var context = new ApiMetadataDecodeContext(api, Map.of(), PRIMARY_OWNER);

        // When
        var result = service.decodeMetadataValue("<#list api.proxy.groups as group>${group.httpProxy.password}</#list>", context);

        // Then
        assertThat(result).doesNotContain("proxy-secret");
    }

    @Test
    void should_decode_metadata_value_with_api_properties() {
        // Given
        var context = new ApiMetadataDecodeContext(ApiFixtures.aProxyApiV2(), Map.of("team", "core"), PRIMARY_OWNER);

        // When
        var result = service.decodeMetadataValue(
            "${api.name}|${api.version}|${api.visibility}|${api.primaryOwner.email}|${api.metadata['team']}",
            context
        );

        // Then
        assertThat(result).isEqualTo("My Api|1.0.0|PUBLIC|jane.doe@gravitee.io|core");
    }

    private void givenExistingApiMetadata(List<ApiMetadata> metadata) {
        apiMetadataQueryService.initWithApiMetadata(metadata);
    }
}
