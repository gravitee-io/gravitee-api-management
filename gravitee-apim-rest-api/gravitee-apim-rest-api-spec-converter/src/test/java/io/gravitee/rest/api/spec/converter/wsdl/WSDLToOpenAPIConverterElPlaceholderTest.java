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
package io.gravitee.rest.api.spec.converter.wsdl;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import org.junit.Test;

public class WSDLToOpenAPIConverterElPlaceholderTest {

    private final WSDLToOpenAPIConverter converter = new WSDLToOpenAPIConverter();

    @Test
    public void should_generate_json_path_el_placeholders_in_soap_envelope() {
        OpenAPI openApi = converter.toOpenAPI(getClass().getResourceAsStream("/calculator.asmx"));

        // The Add operation has two integer parameters: intA and intB
        // Multiple ports exist, so path includes the port name
        PathItem addPath = openApi.getPaths().get("/Calculator/CalculatorSoap/Add");
        assertThat(addPath).isNotNull();

        Operation addOp = addPath.getPost();
        assertThat(addOp).isNotNull();

        String envelope = (String) addOp.getExtensions().get(WSDLToOpenAPIConverter.SOAP_EXTENSION_ENVELOPE);
        assertThat(envelope).isNotNull();
        assertThat(envelope).contains("{#xmlEscape(#jsonPath(#request.content, '$.intA'))}");
        assertThat(envelope).contains("{#xmlEscape(#jsonPath(#request.content, '$.intB'))}");
        assertThat(envelope).doesNotContain("1.051732E7");
        assertThat(envelope).doesNotContain("#request.params");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_generate_request_body_schema_from_wsdl() {
        OpenAPI openApi = converter.toOpenAPI(getClass().getResourceAsStream("/calculator.asmx"));

        PathItem addPath = openApi.getPaths().get("/Calculator/CalculatorSoap/Add");
        assertThat(addPath).isNotNull();

        Operation addOp = addPath.getPost();
        assertThat(addOp).isNotNull();
        assertThat(addOp.getRequestBody()).isNotNull();
        assertThat(addOp.getRequestBody().getRequired()).isTrue();

        Schema<?> schema = addOp.getRequestBody().getContent().get("application/json").getSchema();
        assertThat(schema).isNotNull();
        assertThat(schema.getType()).isEqualTo("object");

        Map<String, Schema> properties = schema.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).containsKey("intA");
        assertThat(properties).containsKey("intB");
        assertThat(properties.get("intA").getType()).isEqualTo("integer");
        assertThat(properties.get("intB").getType()).isEqualTo("integer");
    }
}
