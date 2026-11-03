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
package io.gravitee.rest.api.spec.converter.wsdl.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wsdl.Constants;
import com.sun.net.httpserver.HttpServer;
import io.gravitee.rest.api.spec.converter.wsdl.WSDLToOpenAPIConverter;
import io.gravitee.rest.api.spec.converter.wsdl.exception.WsdlDescriptorException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

class HardenedWSDLReaderImplTest {

    private static final String TNS = "http://example.com/coverage";

    @Test
    void should_read_wsdl_with_faults_one_way_notification_and_solicit_response() throws Exception {
        Definition definition = read(getClass().getResourceAsStream("/wsdl_with_faults.wsdl"));

        PortType portType = definition.getPortType(new QName(TNS, "CoveragePortType"));
        Operation requestResponse = portType.getOperation("RequestResponse", "Request", "Response");
        assertThat(requestResponse.getFaults()).containsKey("Error");
        assertThat(requestResponse.getParameterOrdering()).containsExactly("body");
        assertThat(requestResponse.getDocumentationElement()).isNotNull();

        Operation oneWay = portType.getOperation("OneWay", Constants.NONE, null);
        assertThat(oneWay.getInput()).isNotNull();
        assertThat(oneWay.getOutput()).isNull();

        Operation notify = portType.getOperation("Notify", null, Constants.NONE);
        assertThat(notify.getOutput()).isNotNull();
        assertThat(notify.getInput()).isNull();

        Operation solicit = portType.getOperation("Solicit", Constants.NONE, Constants.NONE);
        assertThat(solicit.getInput()).isNotNull();
        assertThat(solicit.getOutput()).isNotNull();

        Binding binding = definition.getBinding(new QName(TNS, "CoverageBinding"));
        BindingOperation bindingOperation = binding.getBindingOperation("RequestResponse", "Request", "Response");
        assertThat(bindingOperation.getBindingFaults()).containsKey("Error");
        assertThat(binding.getBindingOperation("Unmatched", "Nope", "Nope")).isNotNull();
        assertThat(definition.getService(new QName(TNS, "CoverageService")).getPort("CoveragePort")).isNotNull();
    }

    @Test
    void should_round_trip_supported_reader_features_and_reject_unknown_ones() throws WSDLException {
        HardenedWSDLReaderImpl reader = new HardenedWSDLReaderImpl();

        reader.setFeature("com.ibm.wsdl.parseXMLSchemas", false);
        assertThat(reader.getFeature("com.ibm.wsdl.parseXMLSchemas")).isFalse();
        reader.setFeature("javax.wsdl.verbose", true);
        assertThat(reader.getFeature("javax.wsdl.verbose")).isTrue();
        reader.setFeature("javax.wsdl.importDocuments", false);
        assertThat(reader.getFeature("javax.wsdl.importDocuments")).isFalse();

        assertThatThrownBy(() -> reader.setFeature(null, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be null");
        assertThatThrownBy(() -> reader.getFeature(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be null");
        assertThatThrownBy(() -> reader.setFeature("com.example.unknown", true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not recognized");
        assertThatThrownBy(() -> reader.getFeature("com.example.unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not recognized");
    }

    @Test
    void should_use_the_configured_wsdl_factory_implementation() throws Exception {
        HardenedWSDLReaderImpl reader = new HardenedWSDLReaderImpl();
        reader.setFeature("javax.wsdl.verbose", false);
        reader.setFactoryImplName("com.ibm.wsdl.factory.WSDLFactoryImpl");
        reader.setFactoryImplName("com.ibm.wsdl.factory.WSDLFactoryImpl");
        assertThat(reader.getFactoryImplName()).isEqualTo("com.ibm.wsdl.factory.WSDLFactoryImpl");

        Definition definition = reader.readWSDL(null, new InputSource(getClass().getResourceAsStream("/wsdl_with_faults.wsdl")));
        assertThat(definition.getQName().getLocalPart()).isEqualTo("Coverage");
    }

    @Test
    void should_read_wsdl_from_an_http_url() throws Exception {
        byte[] body = getClass().getResourceAsStream("/wsdl_with_faults.wsdl").readAllBytes();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/coverage.wsdl", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            String url = "http://127.0.0.1:" + port + "/coverage.wsdl";
            Definition definition = new HardenedWSDLReaderImpl().readWSDL(url);
            assertThat(definition.getServices()).isNotEmpty();

            server.createContext("/child.wsdl", exchange -> {
                byte[] child = """
                    <?xml version="1.0"?>
                    <definitions name="Child" targetNamespace="http://example.com/child" xmlns="http://schemas.xmlsoap.org/wsdl/">
                      <import namespace="http://example.com/coverage" location="http://127.0.0.1:%d/parent.wsdl"/>
                    </definitions>
                    """.formatted(port)
                    .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, child.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(child);
                }
            });
            server.createContext("/parent.wsdl", exchange -> {
                byte[] parent = """
                    <?xml version="1.0"?>
                    <definitions name="Parent" targetNamespace="http://example.com/parent" xmlns="http://schemas.xmlsoap.org/wsdl/">
                      <import namespace="http://example.com/child" location="http://127.0.0.1:%d/child.wsdl"/>
                    </definitions>
                    """.formatted(port)
                    .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, parent.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(parent);
                }
            });
            Definition imported = new HardenedWSDLReaderImpl().readWSDL("http://127.0.0.1:" + port + "/parent.wsdl");
            assertThat(imported.getImports()).isNotEmpty();

            assertThatThrownBy(() -> new HardenedWSDLReaderImpl().readWSDL("http://127.0.0.1:1/missing.wsdl")).isInstanceOf(
                WSDLException.class
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void should_reject_a_wsdl_import_chain_that_exceeds_the_nesting_budget() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String base = "http://127.0.0.1:" + port;
        server.createContext("/", exchange -> {
            String raw = exchange.getRequestURI().getRawPath();
            int level = Integer.parseInt(raw.substring(raw.lastIndexOf('/') + 1));
            String xml = level == 0 ? leafWsdl() : parentWsdl(base + "/level/" + (level - 1));
            byte[] body = xml.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            assertThatThrownBy(() -> new WSDLToOpenAPIConverter().toOpenAPI(parentWsdl(base + "/level/80")))
                .isInstanceOf(WsdlDescriptorException.class)
                .cause()
                .hasMessageContaining("Maximum")
                .hasMessageContaining("nesting depth");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void should_reject_when_the_fetched_document_budget_is_exceeded() throws Exception {
        CopyOnWriteArrayList<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String base = "http://127.0.0.1:" + port;
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestURI().getRawPath());
            byte[] body = """
                <?xml version="1.0"?>
                <schema xmlns="http://www.w3.org/2001/XMLSchema"/>
                """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            StringBuilder includes = new StringBuilder();
            for (int i = 0; i < 200; i++) {
                includes.append("<include schemaLocation=\"").append(base).append("/schema/").append(i).append("\"/>");
            }
            String wsdl = """
                <?xml version="1.0"?>
                <definitions name="Wide" targetNamespace="http://example.com/wide" xmlns="http://schemas.xmlsoap.org/wsdl/">
                  <types>
                    <schema xmlns="http://www.w3.org/2001/XMLSchema">
                      %s
                    </schema>
                  </types>
                </definitions>
                """.formatted(includes);

            assertThatThrownBy(() -> new WSDLToOpenAPIConverter().toOpenAPI(wsdl))
                .isInstanceOf(WsdlDescriptorException.class)
                .cause()
                .hasMessageContaining("Maximum number of fetched");
            assertThat(requests).isNotEmpty().hasSizeLessThanOrEqualTo(128);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void should_reject_a_schema_include_chain_that_exceeds_the_nesting_budget() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String base = "http://127.0.0.1:" + port;
        server.createContext("/", exchange -> {
            String raw = exchange.getRequestURI().getRawPath();
            int level = Integer.parseInt(raw.substring(raw.lastIndexOf('/') + 1));
            String xml = level == 0 ? emptySchema() : schemaIncluding(base + "/level/" + (level - 1));
            byte[] body = xml.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            String wsdl = """
                <?xml version="1.0"?>
                <definitions name="Deep" targetNamespace="http://example.com/deep" xmlns="http://schemas.xmlsoap.org/wsdl/">
                  <types>
                    <schema xmlns="http://www.w3.org/2001/XMLSchema">
                      <include schemaLocation="%s/level/80"/>
                    </schema>
                  </types>
                </definitions>
                """.formatted(base);

            assertThatThrownBy(() -> new WSDLToOpenAPIConverter().toOpenAPI(wsdl))
                .isInstanceOf(WsdlDescriptorException.class)
                .cause()
                .hasMessageContaining("Maximum")
                .hasMessageContaining("nesting depth");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void should_reject_file_urls_when_the_reader_opens_them_without_a_locator() throws Exception {
        Path child = Files.createTempFile("pen105-reader", ".wsdl");
        Files.writeString(
            child,
            """
            <?xml version="1.0"?>
            <definitions name="Local" targetNamespace="http://example.com/local" xmlns="http://schemas.xmlsoap.org/wsdl/"/>
            """
        );
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String fileUrl = child.toUri().toString();
        server.createContext("/parent.wsdl", exchange -> {
            byte[] body = """
                <?xml version="1.0"?>
                <definitions name="Parent" targetNamespace="http://example.com/parent" xmlns="http://schemas.xmlsoap.org/wsdl/">
                  <import namespace="http://example.com/local" location="%s#a b"/>
                </definitions>
                """.formatted(fileUrl)
                .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.createContext("/schema-parent.wsdl", exchange -> {
            byte[] body = """
                <?xml version="1.0"?>
                <definitions name="SchemaParent" targetNamespace="http://example.com/schema-parent" xmlns="http://schemas.xmlsoap.org/wsdl/">
                  <types>
                    <schema xmlns="http://www.w3.org/2001/XMLSchema">
                      <include schemaLocation="%s#a b"/>
                    </schema>
                  </types>
                </definitions>
                """.formatted(fileUrl)
                .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            assertThatThrownBy(() -> new HardenedWSDLReaderImpl().readWSDL(fileUrl))
                .isInstanceOf(WSDLException.class)
                .cause()
                .isInstanceOf(WsdlImportResolutionException.class)
                .hasMessageContaining("Unsupported WSDL import location scheme");

            assertThatThrownBy(() -> new HardenedWSDLReaderImpl().readWSDL("http://127.0.0.1:" + port + "/parent.wsdl"))
                .isInstanceOf(WSDLException.class)
                .cause()
                .isInstanceOf(WsdlImportResolutionException.class)
                .hasMessageContaining("Unsupported WSDL import location scheme");

            assertThatThrownBy(() -> new HardenedWSDLReaderImpl().readWSDL("http://127.0.0.1:" + port + "/schema-parent.wsdl"))
                .isInstanceOf(WSDLException.class)
                .cause()
                .isInstanceOf(WsdlImportResolutionException.class)
                .hasMessageContaining("Unsupported WSDL import location scheme");
        } finally {
            server.stop(0);
            Files.deleteIfExists(child);
        }
    }

    private static Definition read(java.io.InputStream stream) throws WSDLException {
        HardenedWSDLReaderImpl reader = new HardenedWSDLReaderImpl();
        reader.setFeature("javax.wsdl.verbose", false);
        return reader.readWSDL(new SecureWSDLLocator(new InputSource(stream)));
    }

    private static String leafWsdl() {
        return """
        <?xml version="1.0"?>
        <definitions name="Leaf" targetNamespace="http://example.com/leaf" xmlns="http://schemas.xmlsoap.org/wsdl/"/>
        """;
    }

    private static String emptySchema() {
        return """
        <?xml version="1.0"?>
        <schema xmlns="http://www.w3.org/2001/XMLSchema"/>
        """;
    }

    private static String schemaIncluding(String location) {
        return """
        <?xml version="1.0"?>
                    <schema xmlns="http://www.w3.org/2001/XMLSchema">
                      <include schemaLocation="%s#a b"/>
                    </schema>
        """.formatted(location);
    }

    private static String parentWsdl(String location) {
        return """
        <?xml version="1.0"?>
        <definitions name="Parent" targetNamespace="http://example.com/parent" xmlns="http://schemas.xmlsoap.org/wsdl/">
          <import namespace="http://example.com/next" location="%s"/>
        </definitions>
        """.formatted(location);
    }
}
