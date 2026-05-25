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
package io.gravitee.apim.infra.domain_service.mcp_tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.mcp_tool.model.McpTool;
import io.gravitee.apim.core.mcp_tool.model.McpToolGatewayMappingHttp;
import io.gravitee.apim.core.mcp_tool.model.OpenApiToMcpToolsResult;
import io.gravitee.apim.core.mcp_tool.model.ParseError;
import io.swagger.v3.core.util.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenApiToMcpToolsDomainServiceImplTest {

    private final OpenApiToMcpToolsDomainServiceImpl service = new OpenApiToMcpToolsDomainServiceImpl();

    @Test
    void should_return_invalid_format_when_input_is_blank() {
        var result = service.parse("");

        assertThat(result.result()).isEmpty();
        assertThat(result.errors()).singleElement().extracting(ParseError::key).isEqualTo(ParseError.INVALID_FORMAT);
    }

    @Test
    void should_return_invalid_spec_when_payload_is_not_an_openapi_document() {
        var result = service.parse(load("invalid.txt"));

        assertThat(result.result()).isEmpty();
        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors().getFirst().key()).isIn(ParseError.INVALID_SPEC, ParseError.INVALID_FORMAT);
    }

    @Test
    void should_extract_server_description_from_info() {
        var result = service.parse(load("petstore.yaml"));

        assertThat(result.serverDescription()).isEqualTo("Petstore\n\nA minimal petstore API used as parser fixture.");
    }

    @Test
    void should_emit_one_tool_per_operation_with_operation_id_as_name() {
        var result = service.parse(load("petstore.yaml"));

        assertThat(result.result())
            .extracting(t -> t.toolDefinition().name())
            .containsExactlyInAnyOrder("listPets", "createPet", "getPet", "deletePet");
    }

    @Test
    void should_fallback_to_snake_case_method_and_path_when_operation_id_is_absent() {
        var result = service.parse(load("no-operation-id.yaml"));

        assertThat(result.result())
            .extracting(t -> t.toolDefinition().name())
            .containsExactlyInAnyOrder("get_alpha_beta", "post_alpha_beta");
    }

    @Test
    void should_uppercase_http_method_in_gateway_mapping() {
        var result = service.parse(load("petstore.yaml"));

        assertThat(result.result()).allSatisfy(tool -> assertThat(tool.gatewayMapping().http().method()).matches("^[A-Z]+$"));
    }

    @Test
    void should_translate_oas_path_placeholders_into_express_style() {
        var result = service.parse(load("petstore.yaml"));

        var getPet = toolByName(result, "getPet");
        assertThat(getPet.gatewayMapping().http().path()).isEqualTo("/pets/:petId");
    }

    @Test
    void should_translate_multiple_path_placeholders() {
        var result = service.parse(load("multi-params.yaml"));

        var tool = result.result().getFirst();
        assertThat(tool.gatewayMapping().http().path()).isEqualTo("/tenants/:tenantId/users/:userId");
    }

    @Test
    void should_classify_parameters_by_location_in_gateway_mapping() {
        var result = service.parse(load("multi-params.yaml"));

        McpToolGatewayMappingHttp http = result.result().getFirst().gatewayMapping().http();
        assertThat(http.pathParams()).containsExactly("tenantId", "userId");
        assertThat(http.queryParams()).containsExactly("includeDisabled");
        assertThat(http.headers()).containsExactly("X-Tenant-Token");
    }

    @Test
    void should_set_contentType_when_request_body_is_present() {
        var result = service.parse(load("petstore.yaml"));

        var createPet = toolByName(result, "createPet");
        assertThat(createPet.gatewayMapping().http().contentType()).isEqualTo("application/json");
    }

    @Test
    void should_leave_contentType_null_when_no_request_body() {
        var result = service.parse(load("petstore.yaml"));

        var listPets = toolByName(result, "listPets");
        assertThat(listPets.gatewayMapping().http().contentType()).isNull();
    }

    @Test
    void should_build_input_schema_as_object_with_properties_and_required() {
        var result = service.parse(load("petstore.yaml"));

        var getPet = toolByName(result, "getPet");
        var inputSchema = getPet.toolDefinition().inputSchema();
        assertThat(inputSchema.get("type").asText()).isEqualTo("object");
        assertThat(inputSchema.get("properties").has("petId")).isTrue();
        assertThat(inputSchema.get("required")).anySatisfy(node -> assertThat(node.asText()).isEqualTo("petId"));
    }

    @Test
    void should_include_request_body_schema_as_bodySchema_property() {
        var result = service.parse(load("petstore.yaml"));

        var createPet = toolByName(result, "createPet");
        var inputSchema = createPet.toolDefinition().inputSchema();
        assertThat(inputSchema.get("properties").has("bodySchema")).isTrue();
        var bodySchema = inputSchema.get("properties").get("bodySchema");
        assertThat(bodySchema.get("type").asText()).isEqualTo("object");
        assertThat(bodySchema.get("properties").has("name")).isTrue();
    }

    @Test
    void should_extract_response_schema_as_outputSchema_with_bodySchema_property() {
        var result = service.parse(load("petstore.yaml"));

        var listPets = toolByName(result, "listPets");
        var outputSchema = listPets.toolDefinition().outputSchema();
        assertThat(outputSchema).isNotNull();
        assertThat(outputSchema.get("properties").has("bodySchema")).isTrue();
    }

    @Test
    void should_leave_outputSchema_null_when_no_2xx_body_or_headers() {
        var result = service.parse(load("petstore.yaml"));

        var deletePet = toolByName(result, "deletePet");
        assertThat(deletePet.toolDefinition().outputSchema()).isNull();
    }

    @Test
    void should_pick_up_x_mcp_annotations() {
        var result = service.parse(load("petstore.yaml"));

        var getPet = toolByName(result, "getPet");
        assertThat(getPet.toolDefinition().annotations()).isNotNull();
        assertThat(getPet.toolDefinition().annotations().readOnlyHint()).isTrue();
        assertThat(getPet.toolDefinition().annotations().idempotentHint()).isTrue();
        assertThat(getPet.toolDefinition().annotations().destructiveHint()).isNull();

        var deletePet = toolByName(result, "deletePet");
        assertThat(deletePet.toolDefinition().annotations()).isNotNull();
        assertThat(deletePet.toolDefinition().annotations().destructiveHint()).isTrue();
    }

    @Test
    void should_leave_annotations_null_when_no_x_mcp_extension() {
        var result = service.parse(load("petstore.yaml"));

        var listPets = toolByName(result, "listPets");
        assertThat(listPets.toolDefinition().annotations()).isNull();
    }

    @Test
    void should_transparently_convert_swagger_2_to_openapi_3() {
        var result = service.parse(load("swagger2-with-body.yaml"));

        assertThat(result.result())
            .extracting(t -> t.toolDefinition().name())
            .containsExactly("placeOrder");
        var placeOrder = result.result().getFirst();
        assertThat(placeOrder.gatewayMapping().http().method()).isEqualTo("POST");
        assertThat(placeOrder.toolDefinition().inputSchema().get("properties").has("bodySchema")).isTrue();
    }

    @Test
    void should_return_empty_tools_when_paths_object_is_empty() {
        var result = service.parse(load("empty-paths.yaml"));

        assertThat(result.result()).isEmpty();
        assertThat(result.errors()).isEmpty();
        assertThat(result.serverDescription()).isEqualTo("Empty");
    }

    @Test
    void should_default_description_to_method_and_path_when_summary_and_description_are_missing() {
        var result = service.parse(load("no-operation-id.yaml"));

        // we used summary "Listing" / "Creation", so this asserts summary wins
        assertThat(result.result())
            .extracting(t -> t.toolDefinition().description())
            .containsExactlyInAnyOrder("Listing", "Creation");
    }

    /**
     * Each entry pairs a YAML fixture with a JSON snapshot generated by the front-end
     * {@code convertOpenApiToMcpTools} util, captured manually from the APIM console.
     * The Java parser must produce a structurally equivalent JSON tree, otherwise the
     * gateway plugin (which consumes today's TS output) would reject the payload.
     *
     * <p>Known intentional divergences with the front end (Java is the cleaner reference):
     * <ul>
     *   <li>For Swagger 2.0 specs with a {@code required} body parameter, the TS keeps the
     *   body parameter name (e.g. {@code "order"}) in {@code inputSchema.required} even
     *   though no matching property exists in {@code inputSchema.properties} — a quirk of
     *   the TS Swagger-2-to-OpenAPI-3 transform. The Java parser leaves {@code required}
     *   empty in that case.</li>
     *   <li>On collisions, the TS emits the duplicate-named tool twice in {@code result}.
     *   The Java parser appends a {@code _2}, {@code _3}, ... suffix to disambiguate while
     *   still emitting a {@code DUPLICATE_NAME} error. The gateway plugin requires unique
     *   tool names at runtime, so suffixing is the correct behaviour.</li>
     * </ul>
     * Both divergences are safe for the gateway plugin (which routes via
     * {@code gatewayMapping}) and asserted by dedicated unit tests below.
     */
    @ParameterizedTest(name = "matches front-end snapshot: {0}")
    @ValueSource(strings = { "empty-paths", "multi-params", "no-operation-id", "petstore", "swagger2-with-body" })
    void should_match_front_end_snapshot(String fixtureBaseName) throws IOException {
        var oas = load(fixtureBaseName + ".yaml");
        var actual = service.parse(oas);

        JsonNode actualTree = Json.mapper().valueToTree(actual);
        JsonNode expectedTree = Json.mapper().readTree(load("expected/" + fixtureBaseName + ".expected.json"));

        assertThat(actualTree).as("parser output should match the front-end snapshot for %s", fixtureBaseName).isEqualTo(expectedTree);
    }

    @Test
    void should_suffix_colliding_tool_names_and_emit_a_duplicate_error() {
        var result = service.parse(load("duplicate-names.yaml"));

        assertThat(result.result())
            .extracting(t -> t.toolDefinition().name())
            .containsExactly("getUser", "getUser_2", "getUser_3");
        assertThat(result.errors())
            .hasSize(2)
            .allSatisfy(err -> assertThat(err.key()).isEqualTo(ParseError.DUPLICATE_NAME))
            .extracting(ParseError::message)
            .containsExactly(
                "Duplicate tool name 'getUser' resolved to 'getUser_2'",
                "Duplicate tool name 'getUser' resolved to 'getUser_3'"
            );
    }

    private static McpTool toolByName(OpenApiToMcpToolsResult result, String name) {
        return result
            .result()
            .stream()
            .filter(t -> Objects.equals(t.toolDefinition().name(), name))
            .findFirst()
            .orElseThrow();
    }

    private static String load(String fixtureName) {
        try (var stream = OpenApiToMcpToolsDomainServiceImplTest.class.getResourceAsStream("oas-fixtures/" + fixtureName)) {
            if (stream == null) {
                throw new IllegalStateException("Fixture not found on classpath: " + fixtureName);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read fixture: " + fixtureName, e);
        }
    }
}
