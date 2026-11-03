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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import io.gravitee.rest.api.spec.converter.wsdl.exception.WsdlDescriptorException;
import io.gravitee.rest.api.spec.converter.wsdl.reader.WsdlImportResolutionException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXParseException;

public class WSDLToOpenAPIConverterExceptionTest {

    private final WSDLToOpenAPIConverter converter = new WSDLToOpenAPIConverter();

    @Test
    public void convertWsdlWithException() {
        assertThrows(WsdlDescriptorException.class, () -> converter.toOpenAPI(this.getClass().getResourceAsStream("/root_pwd.wsdl")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "netdoc:/tmp/x.wsdl", "gopher://127.0.0.1/a", "//evil.example/a.wsdl" })
    public void should_reject_malformed_import_locations_on_the_production_path(String location) {
        String parent = """
            <?xml version="1.0"?>
            <definitions name="Malformed" targetNamespace="http://example.com/malformed" xmlns="http://schemas.xmlsoap.org/wsdl/">
              <import namespace="http://example.com/child" location="%s"/>
            </definitions>
            """.formatted(location);

        assertThatThrownBy(() -> converter.toOpenAPI(parent))
            .isInstanceOf(WsdlDescriptorException.class)
            .hasRootCauseInstanceOf(WsdlImportResolutionException.class);
    }

    @Test
    public void should_reject_file_scheme_import_location() {
        assertThatThrownBy(() -> converter.toOpenAPI(getClass().getResourceAsStream("/file_scheme_import.wsdl")))
            .isInstanceOf(WsdlDescriptorException.class)
            .hasRootCauseInstanceOf(WsdlImportResolutionException.class)
            .rootCause()
            .hasMessageContaining("file");
    }

    @Test
    public void should_reject_relative_import_on_inline_wsdl_without_http_base() {
        // Production loadWSDL uses a null document base. Relative imports then resolve via
        // wsdl4j's File fallback to file: and must be rejected by SecureWSDLLocator.
        assertThatThrownBy(() -> converter.toOpenAPI(getClass().getResourceAsStream("/relative_import_parent.wsdl")))
            .isInstanceOf(WsdlDescriptorException.class)
            .hasRootCauseInstanceOf(WsdlImportResolutionException.class)
            .rootCause()
            .hasMessageContaining("file");
    }

    @Test
    public void should_reject_nested_xxe_in_http_imported_document_with_doctype_cause() throws Exception {
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String base = "http://127.0.0.1:" + port;

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            requests.add(path);
            byte[] body = nestedXxeChildWsdl().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/xml");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            String parent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions name="poc" targetNamespace="http://example.com/poc" xmlns="http://schemas.xmlsoap.org/wsdl/">
                  <import location="%s/evil.wsdl" namespace="http://example.com/evil"/>
                </definitions>
                """.formatted(base);

            assertThatThrownBy(() -> converter.toOpenAPI(parent))
                .isInstanceOf(WsdlDescriptorException.class)
                .hasRootCauseInstanceOf(SAXParseException.class)
                .rootCause()
                .hasMessageContaining("DOCTYPE");

            assertThat(requests).anyMatch(path -> path.contains("evil.wsdl"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void should_allow_relative_wsdl_import_against_http_parent_base() throws Exception {
        byte[] child = getClass().getResourceAsStream("/relative_import_child.wsdl").readAllBytes();
        byte[] parentTemplate = getClass().getResourceAsStream("/relative_import_parent.wsdl").readAllBytes();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String base = "http://127.0.0.1:" + port;

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            byte[] body = path.endsWith("relative_import_child.wsdl") ? child : parentTemplate;
            exchange.getResponseHeaders().add("Content-Type", "text/xml");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            String parent = """
                <?xml version="1.0"?>
                <definitions name="RelativeImportParent"
                             targetNamespace="http://example.com/relative-import/parent"
                             xmlns:tns="http://example.com/relative-import/parent"
                             xmlns:child="http://example.com/relative-import/child"
                             xmlns="http://schemas.xmlsoap.org/wsdl/">
                    <import namespace="http://example.com/relative-import/child" location="%s/relative_import_child.wsdl"/>
                    <binding name="StockQuoteSoapBinding" type="child:StockQuotePortType">
                        <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"
                                      xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                        <operation name="GetLastTradePrice">
                            <soap:operation soapAction="http://example.com/GetLastTradePrice"
                                            xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                            <input>
                                <soap:body use="literal" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                            </input>
                            <output>
                                <soap:body use="literal" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                            </output>
                        </operation>
                    </binding>
                    <service name="StockQuoteService">
                        <port name="StockQuotePort" binding="tns:StockQuoteSoapBinding">
                            <soap:address location="http://example.com/stockquote"
                                          xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                        </port>
                    </service>
                </definitions>
                """.formatted(base);

            assertThat(converter.toOpenAPI(parent)).isNotNull();
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void should_reject_schema_include_with_doctype() throws Exception {
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String base = "http://127.0.0.1:" + port;

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            requests.add(path);
            byte[] body = """
                <?xml version="1.0"?>
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <schema xmlns="http://www.w3.org/2001/XMLSchema" targetNamespace="http://example.com/evil-xsd">
                  <element name="TradePrice" type="string"/>
                </schema>
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/xml");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            String parent = """
                <?xml version="1.0"?>
                <definitions name="SchemaIncludeXxe"
                             targetNamespace="http://example.com/schema-include"
                             xmlns:tns="http://example.com/schema-include"
                             xmlns:xsd1="http://example.com/evil-xsd"
                             xmlns="http://schemas.xmlsoap.org/wsdl/"
                             xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                  <types>
                    <schema targetNamespace="http://example.com/evil-xsd" xmlns="http://www.w3.org/2001/XMLSchema">
                      <include schemaLocation="%s/evil.xsd"/>
                    </schema>
                  </types>
                  <message name="In"><part name="body" element="xsd1:TradePrice"/></message>
                  <message name="Out"><part name="body" type="xsd:string"/></message>
                  <portType name="P">
                    <operation name="Op">
                      <input message="tns:In"/>
                      <output message="tns:Out"/>
                    </operation>
                  </portType>
                  <binding name="B" type="tns:P">
                    <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"
                                  xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                    <operation name="Op">
                      <soap:operation soapAction="http://example.com/Op" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                      <input><soap:body use="literal" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/></input>
                      <output><soap:body use="literal" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/></output>
                    </operation>
                  </binding>
                  <service name="S">
                    <port name="Port" binding="tns:B">
                      <soap:address location="http://example.com/s" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                    </port>
                  </service>
                </definitions>
                """.formatted(base);

            assertThatThrownBy(() -> converter.toOpenAPI(parent))
                .isInstanceOf(WsdlDescriptorException.class)
                .hasRootCauseInstanceOf(SAXParseException.class)
                .rootCause()
                .hasMessageContaining("DOCTYPE");

            assertThat(requests).anyMatch(path -> path.contains("evil.xsd"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void should_allow_relative_schema_include_against_http_parent_base() throws Exception {
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String base = "http://127.0.0.1:" + port;

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            requests.add(path);
            byte[] body = ("/types.xsd".equals(path) ? includedTypesSchema() : schemaThatIncludesTypes()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/xml");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            String parent = """
                <?xml version="1.0"?>
                <definitions name="RelativeSchemaInclude"
                             targetNamespace="http://example.com/relative-schema"
                             xmlns:tns="http://example.com/relative-schema"
                             xmlns:xsd1="http://example.com/ns"
                             xmlns="http://schemas.xmlsoap.org/wsdl/"
                             xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                  <types>
                    <schema targetNamespace="http://example.com/ns" xmlns="http://www.w3.org/2001/XMLSchema">
                      <include schemaLocation="%s/base.xsd"/>
                    </schema>
                  </types>
                  <message name="In"><part name="body" element="xsd1:TradePrice"/></message>
                  <message name="Out"><part name="body" type="xsd:string"/></message>
                  <portType name="P">
                    <operation name="Op">
                      <input message="tns:In"/>
                      <output message="tns:Out"/>
                    </operation>
                  </portType>
                  <binding name="B" type="tns:P">
                    <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"
                                  xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                    <operation name="Op">
                      <soap:operation soapAction="http://example.com/Op" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                      <input><soap:body use="literal" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/></input>
                      <output><soap:body use="literal" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/></output>
                    </operation>
                  </binding>
                  <service name="S">
                    <port name="Port" binding="tns:B">
                      <soap:address location="http://example.com/s" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                    </port>
                  </service>
                </definitions>
                """.formatted(base);

            // XmlBeans compiles schemas with network downloads disabled, so envelope generation
            // can fail after a successful fetch. The import itself must still allow the relative include.
            Throwable thrown = catchThrowable(() -> converter.toOpenAPI(parent));
            assertThat(requests).contains("/base.xsd", "/types.xsd");
            assertThat(thrown).isNotInstanceOf(WsdlDescriptorException.class);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void should_reject_schema_include_with_file_scheme() {
        String parent = """
            <?xml version="1.0"?>
            <definitions name="SchemaIncludeFile"
                         targetNamespace="http://example.com/schema-include-file"
                         xmlns:tns="http://example.com/schema-include-file"
                         xmlns="http://schemas.xmlsoap.org/wsdl/"
                         xmlns:xsd="http://www.w3.org/2001/XMLSchema">
              <types>
                <schema targetNamespace="http://example.com/local-xsd" xmlns="http://www.w3.org/2001/XMLSchema">
                  <include schemaLocation="file:///etc/passwd"/>
                </schema>
              </types>
              <message name="In"><part name="body" type="xsd:string"/></message>
              <message name="Out"><part name="body" type="xsd:string"/></message>
              <portType name="P">
                <operation name="Op">
                  <input message="tns:In"/>
                  <output message="tns:Out"/>
                </operation>
              </portType>
              <binding name="B" type="tns:P">
                <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"
                              xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                <operation name="Op">
                  <soap:operation soapAction="http://example.com/Op" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                  <input><soap:body use="literal" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/></input>
                  <output><soap:body use="literal" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/></output>
                </operation>
              </binding>
              <service name="S">
                <port name="Port" binding="tns:B">
                  <soap:address location="http://example.com/s" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"/>
                </port>
              </service>
            </definitions>
            """;

        assertThatThrownBy(() -> converter.toOpenAPI(parent))
            .isInstanceOf(WsdlDescriptorException.class)
            .hasMessageContaining("Unable to read WSDL")
            .hasRootCauseInstanceOf(WsdlImportResolutionException.class)
            .rootCause()
            .hasMessageContaining("Unsupported WSDL import location scheme");
    }

    @Test
    public void should_reject_local_file_imports_that_bypass_uri_parsing_on_the_production_path() throws Exception {
        Path dir = Files.createTempDirectory("pen105-bypass");
        Path child = dir.resolve("child.wsdl");
        Path note = dir.resolve("note.xml");
        Path schema = dir.resolve("types.xsd");
        Path relative = Path.of("pen105-relative-child.wsdl");
        Files.writeString(child, localChildWsdl());
        Files.writeString(note, "<note>not-a-wsdl</note>");
        Files.writeString(schema, localSchema());
        Files.writeString(relative, localChildWsdl());
        try {
            String fileUrl = child.toUri().toString();
            List<String> locations = new ArrayList<>();
            locations.add(fileUrl);
            locations.add(fileUrl + "#a b");
            locations.add("jar:" + fileUrl + "!/a.wsdl#a b");
            locations.add("netdoc:" + child.toAbsolutePath() + "#a b");
            locations.add(child.toAbsolutePath().toString());
            locations.add(relative.getFileName().toString());
            locations.add(note.toAbsolutePath().toString());
            locations.add(schema.toAbsolutePath().toString());

            for (String location : locations) {
                assertLocalLocationRejected(wsdlImporting(location), location);
                assertLocalLocationRejected(wsdlIncludingSchema(location), location);
                assertLocalLocationRejected(wsdlImportingSchema(location), location);
            }
        } finally {
            Files.deleteIfExists(relative);
            Files.deleteIfExists(child);
            Files.deleteIfExists(note);
            Files.deleteIfExists(schema);
            Files.deleteIfExists(dir);
        }
    }

    private void assertLocalLocationRejected(String wsdl, String location) {
        assertThatThrownBy(() -> converter.toOpenAPI(wsdl))
            .as(location)
            .isInstanceOf(WsdlDescriptorException.class)
            .hasRootCauseInstanceOf(WsdlImportResolutionException.class)
            .rootCause()
            .hasMessageContaining("Unsupported WSDL import location scheme");
    }

    private static String wsdlImporting(String location) {
        return """
        <?xml version="1.0"?>
        <definitions name="LocalImport" targetNamespace="http://example.com/local-import" xmlns="http://schemas.xmlsoap.org/wsdl/">
          <import namespace="http://example.com/child" location="%s"/>
        </definitions>
        """.formatted(xmlAttr(location));
    }

    private static String wsdlIncludingSchema(String location) {
        return """
        <?xml version="1.0"?>
        <definitions name="LocalInclude" targetNamespace="http://example.com/local-include" xmlns="http://schemas.xmlsoap.org/wsdl/">
          <types>
            <schema xmlns="http://www.w3.org/2001/XMLSchema">
              <include schemaLocation="%s"/>
            </schema>
          </types>
        </definitions>
        """.formatted(xmlAttr(location));
    }

    private static String wsdlImportingSchema(String location) {
        return """
        <?xml version="1.0"?>
        <definitions name="LocalSchemaImport" targetNamespace="http://example.com/local-schema-import" xmlns="http://schemas.xmlsoap.org/wsdl/">
          <types>
            <schema xmlns="http://www.w3.org/2001/XMLSchema">
              <import namespace="http://example.com/imported" schemaLocation="%s"/>
            </schema>
          </types>
        </definitions>
        """.formatted(xmlAttr(location));
    }

    private static String xmlAttr(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
    }

    private static String localChildWsdl() {
        return """
        <?xml version="1.0"?>
        <definitions name="LocalChild" targetNamespace="http://example.com/child" xmlns="http://schemas.xmlsoap.org/wsdl/"/>
        """;
    }

    private static String localSchema() {
        return """
        <?xml version="1.0"?>
        <schema xmlns="http://www.w3.org/2001/XMLSchema"/>
        """;
    }

    private static String schemaThatIncludesTypes() {
        return """
        <?xml version="1.0"?>
        <schema xmlns="http://www.w3.org/2001/XMLSchema" targetNamespace="http://example.com/ns">
          <include schemaLocation="types.xsd"/>
        </schema>
        """;
    }

    private static String includedTypesSchema() {
        return """
        <?xml version="1.0"?>
        <schema xmlns="http://www.w3.org/2001/XMLSchema" targetNamespace="http://example.com/ns">
          <element name="TradePrice" type="string"/>
        </schema>
        """;
    }

    private static String nestedXxeChildWsdl() {
        return """
        <?xml version="1.0"?>
        <!DOCTYPE root [<!ENTITY test SYSTEM "file:///etc/passwd">]>
        <definitions name="NestedXxeChild"
                     targetNamespace="http://example.com/evil"
                     xmlns="http://schemas.xmlsoap.org/wsdl/">
            <documentation>&test;</documentation>
        </definitions>
        """;
    }
}
