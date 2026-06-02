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

import static org.junit.jupiter.api.Assertions.*;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class WSDLToOpenAPIConverterTest {

    private WSDLToOpenAPIConverter converter = new WSDLToOpenAPIConverter();

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                { "/calculator.asmx", 8, 8, 1 },
                { "/example.wsdl", 1, 1, 1 },
                { "/rpc_style.wsdl", 1, 1, 1 },
                { "/rpc_style_2args.wsdl", 1, 1, 1 },
                { "/tempconvert.wsdl", 6, 4, 3 },
            }
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    public void convertWsdl(String wsdl, int expectedPaths, int expectedSoapEnvelopes, int expectedServers) {
        System.out.println("Execute on " + wsdl);
        OpenAPI openApi = converter.toOpenAPI(this.getClass().getResourceAsStream(wsdl));
        assertNotNull(openApi, "OpenAPI should be generated");

        Info info = openApi.getInfo();
        assertNotNull(info, "Info is required");
        assertFalse(isNullOrEmpty(info.getTitle()), "Title is required");
        assertFalse(isNullOrEmpty(info.getVersion()), "Version is required");

        List<Server> servers = openApi.getServers();
        assertNotNull(servers, "Servers is required");
        assertEquals(expectedServers, servers.size(), "Servers is required");

        Paths paths = openApi.getPaths();
        assertNotNull(paths, "Paths is required");
        assertEquals(expectedPaths, paths.size(), "Not enough paths");
        int soapEnvelopes = 0;
        for (PathItem path : paths.values()) {
            Optional<State> optState = Arrays.asList(
                checkOperation(path.getPost()),
                checkOperation(path.getGet()),
                checkOperation(path.getPut()),
                checkOperation(path.getDelete())
            )
                .stream()
                .filter(state -> state != State.NO_OP)
                .findFirst();
            if (optState.isPresent()) {
                if (optState.get().equals(State.ENVELOPE)) {
                    ++soapEnvelopes;
                }
            } else {
                fail("operation is missing for path");
            }
        }
        assertEquals(expectedSoapEnvelopes, soapEnvelopes, "Not enough SoapEnvelopes");
    }

    private State checkOperation(Operation operation) {
        State state = State.NO_OP;
        if (operation != null) {
            Map<String, Object> extensions = operation.getExtensions();
            if (extensions != null && extensions.keySet().contains(WSDLToOpenAPIConverter.SOAP_EXTENSION_ENVELOPE)) {
                state = State.ENVELOPE;
            } else {
                state = State.NO_ENVELOPE;
            }

            assertNotNull(operation.getResponses().get("200"));
            assertNotNull(operation.getResponses().get("200").getDescription());
        }
        return state;
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || "".equals(value);
    }

    private static enum State {
        NO_OP,
        ENVELOPE,
        NO_ENVELOPE,
    }
}
