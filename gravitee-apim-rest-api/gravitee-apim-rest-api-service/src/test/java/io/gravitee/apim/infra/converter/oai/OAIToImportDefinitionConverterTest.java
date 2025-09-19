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
package io.gravitee.apim.infra.converter.oai;

import static io.gravitee.rest.api.service.impl.swagger.converter.api.OAIToAPIConverter.X_GRAVITEEIO_DEFINITION_VENDOR_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpoint;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.rest.api.service.impl.swagger.parser.OAIParser;
import io.gravitee.rest.api.service.swagger.converter.extension.XGraviteeIODefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OAIToImportDefinitionConverterTest {

    private static final GraviteeMapper MAPPER = new GraviteeMapper();

    // Tests of data coming from OpenAPI file "info" section
    @Nested
    class BasicInfo {

        @Test
        void should_convert_oai_to_import_definition_with_basic_information() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();

                var api = result.getApiExport();
                softly.assertThat(api.getName()).isEqualTo("Gravitee.io Swagger API");
                softly.assertThat(api.getApiVersion()).isEqualTo("1.2.3");
                softly.assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
                softly.assertThat(api.getType()).isEqualTo(ApiType.PROXY);
            });
        }

        @Nested
        class Description {

            @ParameterizedTest
            @NullSource
            @EmptySource
            void should_generate_api_description(String description) throws IOException {
                // Given
                var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi.json");
                apiInput.getInfo().setDescription(description);

                // When
                var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

                // Then
                assertSoftly(softly -> {
                    softly.assertThat(result).isNotNull();
                    softly.assertThat(result.getApiExport().getDescription()).isEqualTo("Description of Gravitee.io Swagger API");
                });
            }

            @Test
            void should_map_api_description() throws IOException {
                // Given
                String description = "My API description";
                var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi.json");
                apiInput.getInfo().setDescription(description);

                // When
                var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

                // Then
                assertSoftly(softly -> {
                    softly.assertThat(result).isNotNull();
                    softly.assertThat(result.getApiExport().getDescription()).isEqualTo(description);
                });
            }
        }
    }

    @Nested
    class Listeners {

        @ParameterizedTest
        @MethodSource("getApiWithServers")
        void should_map_servers_path_into_listeners_paths(String source, List<Path> paths) throws IOException {
            // Given
            var apiInput = toOpenApi(source);

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();

                var api = result.getApiExport();
                softly.assertThat(api.getListeners()).hasSize(1);

                var listener = (HttpListener) api.getListeners().get(0);
                softly.assertThat(listener.getType()).isEqualTo(ListenerType.HTTP);
                softly.assertThat(listener.getPaths()).containsExactlyElementsOf(paths);
                softly.assertThat(listener.getEntrypoints()).hasSize(1).extracting(Entrypoint::getType).containsExactly("http-proxy");
            });
        }

        @Test
        void should_map_vhost_extension_into_listeners_paths() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-vhost.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();

                var api = result.getApiExport();
                softly.assertThat(api.getListeners()).hasSize(1);

                var listener = (HttpListener) api.getListeners().get(0);
                softly.assertThat(listener.getType()).isEqualTo(ListenerType.HTTP);
                softly
                    .assertThat(listener.getPaths())
                    .containsExactlyElementsOf(
                        List.of(
                            Path.builder().host("host1.example.com").path("path-1").build(),
                            Path.builder().host("host1.example.com").path("path-2").overrideAccess(true).build(),
                            Path.builder().host("host2.example.com").path("path-3").build(),
                            Path.builder().host("host3.example.com").build()
                        )
                    );
            });
        }

        @Test
        void should_map_extension_without_vhost_into_listeners_paths() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-vhost.json");
            apiInput.getExtensions().put("x-graviteeio-definition", new XGraviteeIODefinition());

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();

                var api = result.getApiExport();
                softly.assertThat(api.getListeners()).hasSize(1);

                var listener = (HttpListener) api.getListeners().get(0);
                softly.assertThat(listener.getType()).isEqualTo(ListenerType.HTTP);
                softly.assertThat(listener.getPaths()).isEmpty();
            });
        }

        @Test
        void should_map_servers_variables_into_listeners_paths() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-servers-variables.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();

                var api = result.getApiExport();
                softly.assertThat(api.getListeners()).hasSize(1);

                var listener = (HttpListener) api.getListeners().get(0);
                softly.assertThat(listener.getType()).isEqualTo(ListenerType.HTTP);
                softly.assertThat(listener.getPaths()).containsExactlyElementsOf(List.of(Path.builder().path("/gateway/v1").build()));
            });
        }

        private static Stream<Arguments> getApiWithServers() {
            return Stream.of(
                Arguments.of("io/gravitee/rest/api/management/service/openapi.json", List.of(Path.builder().path("/gateway/echo").build())),
                Arguments.of(
                    "io/gravitee/rest/api/management/service/openapi-no-basepath.json",
                    List.of(Path.builder().path("gravitee.ioswaggerapi").build())
                ),
                Arguments.of(
                    "io/gravitee/rest/api/management/service/openapi-multi-servers.json",
                    List.of(Path.builder().path("/echo").build()) // To have multiple path the user needs to use "virtualHosts" attribute of "x-graviteeio-definition" extension
                )
            );
        }
    }

    @Nested
    class EndpointGroups {

        @ParameterizedTest
        @NullSource
        @EmptySource
        void should_return_empty_list_when_no_servers(List<Server> servers) throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi.json");
            apiInput.setServers(servers);

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getEndpointGroups()).isEmpty();
            });
        }

        @ParameterizedTest
        @MethodSource("getEndpointsData")
        void should_convert_servers_into_endpoint_groups(String file, List<Tuple> endpointData) throws Exception {
            // Given
            var apiInput = toOpenApi(file);

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getApiExport().getEndpointGroups()).hasSize(1);

            var group = result.getApiExport().getEndpointGroups().get(0);
            assertThat(group.getName()).isEqualTo("default-group");
            assertThat(group.getType()).isEqualTo("http-proxy");
            assertThat(group.getEndpoints())
                .extracting(AbstractEndpoint::getName, endpoint -> getEndpointTarget(endpoint.getConfiguration()))
                .containsExactlyElementsOf(endpointData);
        }

        private String getEndpointTarget(String configuration) {
            try {
                var configurationNode = MAPPER.readTree(configuration);
                return configurationNode.get("target").asText();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        private static Stream<Arguments> getEndpointsData() {
            return Stream.of(
                Arguments.of(
                    "io/gravitee/rest/api/management/service/openapi.json",
                    List.of(tuple("default", "https://demo.gravitee.io/gateway/echo"))
                ),
                Arguments.of(
                    "io/gravitee/rest/api/management/service/openapi-multi-servers.json",
                    List.of(tuple("server1", "https://example.gravitee.io/echo"), tuple("server2", "https://example.gravitee.io/whoami"))
                ),
                Arguments.of(
                    "io/gravitee/rest/api/management/service/openapi-servers-variables.json",
                    List.of(
                        tuple("server1", "https://demo.gravitee.io:8443/gateway/v1"),
                        tuple("server2", "https://demo.gravitee.io:443/gateway/v1"),
                        tuple("server3", "https://demo.gravitee.io:8443/gateway/v2"),
                        tuple("server4", "https://demo.gravitee.io:443/gateway/v2")
                    )
                )
            );
        }
    }

    @Nested
    class Flows {

        @Test
        void should_convert_oai_to_import_definition_with_flows() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();

                var api = result.getApiExport();
                softly
                    .assertThat((List<Flow>) api.getFlows())
                    .flatExtracting(Flow::getSelectors)
                    .containsExactlyElementsOf(
                        List.of(
                            ConditionSelector.builder().condition("").build(),
                            HttpSelector.builder()
                                .path("/pets")
                                .pathOperator(Operator.EQUALS)
                                .type(SelectorType.HTTP)
                                .methods(Set.of(HttpMethod.GET))
                                .build(),
                            ConditionSelector.builder().condition("").build(),
                            HttpSelector.builder()
                                .path("/pets")
                                .pathOperator(Operator.EQUALS)
                                .type(SelectorType.HTTP)
                                .methods(Set.of(HttpMethod.POST))
                                .build(),
                            ConditionSelector.builder().condition("").build(),
                            HttpSelector.builder()
                                .path("/pets/:petId")
                                .pathOperator(Operator.EQUALS)
                                .type(SelectorType.HTTP)
                                .methods(Set.of(HttpMethod.GET))
                                .build(),
                            ConditionSelector.builder().condition("").build(),
                            HttpSelector.builder()
                                .path("/pets/:petId")
                                .pathOperator(Operator.EQUALS)
                                .type(SelectorType.HTTP)
                                .methods(Set.of(HttpMethod.DELETE))
                                .build()
                        )
                    );
            });
        }
    }

    @Nested
    class Categories {

        @Test
        void should_be_null_when_definition_is_null() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getCategories()).isNull();
            });
        }

        @Test
        void should_be_empty_when_there_is_no_categories_in_the_definition() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-vhost.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getCategories()).isEmpty();
            });
        }

        @Test
        void should_map_categories() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-withExtensions.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getCategories()).containsExactlyInAnyOrder("cat1", "cat2");
            });
        }
    }

    @Nested
    class Labels {

        @Test
        void should_be_null_when_definition_is_null() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getLabels()).isNull();
            });
        }

        @Test
        void should_be_empty_when_there_is_no_labels_in_the_definition() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-vhost.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getLabels()).isEmpty();
            });
        }

        @Test
        void should_map_labels() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-withExtensions.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getLabels()).containsExactlyInAnyOrder("label1", "label2");
            });
        }
    }

    @Nested
    class Groups {

        @Test
        void should_be_null_when_definition_is_null() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getGroups()).isNull();
            });
        }

        @Test
        void should_be_empty_when_there_is_no_groups_in_the_definition() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-vhost.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getGroups()).isEmpty();
            });
        }

        @Test
        void should_map_group_names() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-withExtensions.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getGroups()).containsExactlyInAnyOrder("group1", "group2");
            });
        }
    }

    @Nested
    class Picture {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = { "invalid", "" })
        void should_be_null_when_definition_is_invalid(String picture) throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi.json");
            var graviteeExtension = new XGraviteeIODefinition();
            graviteeExtension.setPicture(picture);
            apiInput.getInfo().setExtensions(Map.of(X_GRAVITEEIO_DEFINITION_VENDOR_EXTENSION, graviteeExtension));

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getPicture()).isNull();
            });
        }

        @Test
        void should_map_picture() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-withExtensions.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getPicture()).isEqualTo("data:image/png;base64,XXXXXXX");
            });
        }
    }

    @Nested
    class Visibility {

        @ParameterizedTest
        @ValueSource(
            strings = {
                "io/gravitee/rest/api/management/service/openapi.json", "io/gravitee/rest/api/management/service/openapi-vhost.json",
            }
        )
        void should_be_null_when_no_definition_or_null(String filePath) throws IOException {
            // Given
            var apiInput = toOpenApi(filePath);

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getVisibility()).isNull();
            });
        }

        @Test
        void should_map_visibility() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-withExtensions.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getVisibility()).isEqualTo(io.gravitee.rest.api.model.Visibility.PRIVATE);
            });
        }
    }

    @Nested
    class Metadata {

        @ParameterizedTest
        @ValueSource(
            strings = {
                "io/gravitee/rest/api/management/service/openapi.json", "io/gravitee/rest/api/management/service/openapi-vhost.json",
            }
        )
        void should_be_null_when_no_definition_or_null() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getMetadata()).isNull();
            });
        }

        @Test
        void should_map_metadata() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-withExtensions.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly
                    .assertThat(result.getMetadata())
                    .containsExactlyInAnyOrderElementsOf(
                        Set.of(
                            NewApiMetadata.builder()
                                .key("meta1")
                                .name("meta1")
                                .value("1234")
                                .format(io.gravitee.apim.core.metadata.model.Metadata.MetadataFormat.NUMERIC)
                                .build(),
                            NewApiMetadata.builder()
                                .key("meta2")
                                .name("meta2")
                                .value("metaValue2")
                                .format(io.gravitee.apim.core.metadata.model.Metadata.MetadataFormat.STRING)
                                .build()
                        )
                    );
            });
        }
    }

    @Nested
    class Properties {

        @ParameterizedTest
        @ValueSource(
            strings = {
                "io/gravitee/rest/api/management/service/openapi.json", "io/gravitee/rest/api/management/service/openapi-vhost.json",
            }
        )
        void should_be_empty_when_no_definition_or_null(String filePath) throws IOException {
            // Given
            var apiInput = toOpenApi(filePath);

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getProperties()).isEmpty();
            });
        }

        @Test
        void should_map_properties() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-withExtensions.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly
                    .assertThat(result.getApiExport().getProperties())
                    .containsExactlyInAnyOrderElementsOf(
                        List.of(
                            Property.builder().key("prop1").value("propValue1").build(),
                            Property.builder().key("prop2").value("propValue2").build()
                        )
                    );
            });
        }
    }

    @Nested
    class Tags {

        @ParameterizedTest
        @ValueSource(
            strings = {
                "io/gravitee/rest/api/management/service/openapi.json", "io/gravitee/rest/api/management/service/openapi-vhost.json",
            }
        )
        void should_be_empty_when_no_definition_or_null(String filePath) throws IOException {
            // Given
            var apiInput = toOpenApi(filePath);

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getTags()).isEmpty();
            });
        }

        @Test
        void should_map_tags() throws IOException {
            // Given
            var apiInput = toOpenApi("io/gravitee/rest/api/management/service/openapi-withExtensions.json");

            // When
            var result = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(apiInput, null);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getApiExport().getTags()).containsExactlyInAnyOrder("tag1", "tag2");
            });
        }
    }

    protected OpenAPI toOpenApi(String file) throws IOException {
        var resource = Resources.getResource(file);
        var content = Resources.toString(resource, Charsets.UTF_8);
        var options = new ParseOptions();
        options.setResolveFully(true);
        var descriptor = new OAIParser().parse(content, options);
        return descriptor.getSpecification();
    }
}
