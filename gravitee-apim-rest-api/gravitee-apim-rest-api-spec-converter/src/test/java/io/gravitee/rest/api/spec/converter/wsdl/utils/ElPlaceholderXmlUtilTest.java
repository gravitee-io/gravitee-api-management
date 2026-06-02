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
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ElPlaceholderXmlUtil} through the WSDL converter which uses it
 * to generate EL placeholders in SOAP envelopes.
 */
public class ElPlaceholderXmlUtilTest {

    private final WSDLToOpenAPIConverter converter = new WSDLToOpenAPIConverter();

    @Test
    public void should_generate_json_path_for_integer_params() {
        OpenAPI api = converter.toOpenAPI(getClass().getResourceAsStream("/calculator.asmx"));
        String envelope = getEnvelope(api, "/Calculator/CalculatorSoap/Add");

        assertThat(envelope).contains("{#xmlEscape(#jsonPath(#request.content, '$.intA'))}");
        assertThat(envelope).contains("{#xmlEscape(#jsonPath(#request.content, '$.intB'))}");
    }

    @Test
    public void should_generate_json_path_for_string_params() {
        OpenAPI api = converter.toOpenAPI(getClass().getResourceAsStream("/tempconvert.wsdl"));
        String envelope = getEnvelope(api, "/TempConvert/TempConvertSoap/FahrenheitToCelsius");

        assertThat(envelope).contains("{#xmlEscape(#jsonPath(#request.content, '$.Fahrenheit'))}");
    }

    private String getEnvelope(OpenAPI api, String path) {
        Operation op = api.getPaths().get(path).getPost();
        assertThat(op).isNotNull();
        return (String) op.getExtensions().get(WSDLToOpenAPIConverter.SOAP_EXTENSION_ENVELOPE);
    }
}
