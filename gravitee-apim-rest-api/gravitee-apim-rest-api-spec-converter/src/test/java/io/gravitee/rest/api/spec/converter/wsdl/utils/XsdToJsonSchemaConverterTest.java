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
package io.gravitee.rest.api.spec.converter.wsdl.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.spec.converter.wsdl.WSDLToOpenAPIConverter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link XsdToJsonSchemaConverter} through the WSDL converter which uses it
 * to generate JSON request body schemas from WSDL input messages.
 */
public class XsdToJsonSchemaConverterTest {

    private final WSDLToOpenAPIConverter converter = new WSDLToOpenAPIConverter();

    @Test
    @SuppressWarnings("unchecked")
    public void should_map_integer_params_to_integer_schema_with_required() {
        OpenAPI api = converter.toOpenAPI(getClass().getResourceAsStream("/calculator.asmx"));
        Schema<?> schema = getRequestBodySchema(api, "/Calculator/CalculatorSoap/Add");

        assertThat(schema.getType()).isEqualTo("object");
        Map<String, Schema> props = schema.getProperties();
        assertThat(props).containsKeys("intA", "intB");
        assertThat(props.get("intA").getType()).isEqualTo("integer");
        assertThat(props.get("intB").getType()).isEqualTo("integer");
        assertThat(schema.getRequired()).containsExactly("intA", "intB");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_generate_schema_for_multiple_operations() {
        OpenAPI api = converter.toOpenAPI(getClass().getResourceAsStream("/calculator.asmx"));
        Schema<?> subtractSchema = getRequestBodySchema(api, "/Calculator/CalculatorSoap/Subtract");

        assertThat(subtractSchema.getType()).isEqualTo("object");
        Map<String, Schema> props = subtractSchema.getProperties();
        assertThat(props).containsKeys("intA", "intB");
    }

    @Test
    public void should_set_request_body_as_required_with_json_content() {
        OpenAPI api = converter.toOpenAPI(getClass().getResourceAsStream("/calculator.asmx"));
        Operation op = api.getPaths().get("/Calculator/CalculatorSoap/Add").getPost();

        assertThat(op.getRequestBody()).isNotNull();
        assertThat(op.getRequestBody().getRequired()).isTrue();
        assertThat(op.getRequestBody().getContent()).containsKey("application/json");
        assertThat(op.getRequestBody().getContent().get("application/json").getSchema()).isNotNull();
        assertThat(op.getRequestBody().getContent().get("application/json").getSchema().getType()).isEqualTo("object");
    }

    private Schema<?> getRequestBodySchema(OpenAPI api, String path) {
        Operation op = api.getPaths().get(path).getPost();
        assertThat(op).isNotNull();
        assertThat(op.getRequestBody()).isNotNull();
        return op.getRequestBody().getContent().get("application/json").getSchema();
    }
}
