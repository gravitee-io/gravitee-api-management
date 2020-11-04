/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.spec.converter.wsdl;

import com.ibm.wsdl.xml.WSDLReaderImpl;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.spec.converter.OpenAPIConverter;
import io.gravitee.rest.api.spec.converter.wsdl.exception.WsdlDescriptorException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.wsdl.*;
import javax.wsdl.extensions.schema.Schema;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import static io.gravitee.rest.api.spec.converter.wsdl.WSDLUtils.*;

/**
 * @author GraviteeSource Team
 */
public class WSDLToOpenAPIConverter implements OpenAPIConverter {
    public static final Logger LOGGER = LoggerFactory.getLogger(WSDLToOpenAPIConverter.class);

    public static final String SOAP_EXTENSION_ENVELOPE = "x-graviteeio-soap-envelope";
    public static final String SOAP_EXTENSION_ACTION = "x-graviteeio-soap-action";

    private OpenAPI openAPI;
    private Definition wsdlDefinition;
    private SoapMessageBuilder soapBuilder;

    private Set<String> serverCache = new HashSet<>();

    @Override
    public OpenAPI toOpenAPI(String content) {
        return toOpenAPI(new ByteArrayInputStream(content.getBytes()));
    }

    @Override
    public OpenAPI toOpenAPI(InputStream stream) {

        Objects.requireNonNull(stream, "WSDL input source is required");
        this.wsdlDefinition = loadWSDL(stream);
        // create the SoapBuilder with namespaces declared in the Definition element
        // this allows Apache XmlBeans to load additional Namespaces if required to parse the XSDs
        this.soapBuilder = new SoapMessageBuilder(wsdlDefinition.getNamespaces());

        this.openAPI = new OpenAPI();
        buildInfo();
        createSchemaObjects();
        processServices();

        return openAPI;
    }

    private Definition loadWSDL(InputStream stream) {
        try {
            WSDLReaderImpl reader = new WSDLReaderImpl();
            reader.setFeature("javax.wsdl.importDocuments", true);
            return reader.readWSDL(null, new InputSource(stream));
        } catch (WSDLException e) {
            LOGGER.info("Unable to read WSDL", e);
            e.printStackTrace();
            throw new WsdlDescriptorException("Unable to read WSDL");
        }
    }

    private void buildInfo() {
        Info info = new Info();
        info.version(DEFAULT_API_VERSION);
        if (wsdlDefinition.getQName() != null) {
            info.setTitle(wsdlDefinition.getQName().getLocalPart());
        } else {
            info.setTitle(wsdlDefinition.getTargetNamespace());
        }

        Element documentation = wsdlDefinition.getDocumentationElement();
        if (documentation != null) {
            info.setDescription(documentation.getTextContent());
        }

        this.openAPI.setInfo(info);
    }

    private void createSchemaObjects() {
        // Types element contains a list of XmlSchema
        Types types = wsdlDefinition.getTypes();
        if (types != null) {
            types.getExtensibilityElements().forEach((o) -> soapBuilder.addSchema((Schema) o));
        }
    }

    private void processServices() {
        Map<QName, Service> services = wsdlDefinition.getServices();
        services.values().stream().forEach(this::processPorts);
    }

    private void processPorts(Service serviceDefinition) {
        Map<String, Port> ports = serviceDefinition.getPorts();
        Iterator<Entry<String, Port>> portIterator = ports.entrySet().iterator();

        final boolean singlePort = (ports.size() == 1);
        while (portIterator.hasNext()) {
            Entry<String, Port> entry = portIterator.next();

            String portName = entry.getKey();
            Port portDefinition = entry.getValue();

            // create Server section for the given port
            createServer(openAPI, portDefinition);

            final Binding binding = wsdlDefinition.getBinding(portDefinition.getBinding().getQName());
            for (int i = 0; i < binding.getBindingOperations().size(); ++i) {
                final BindingOperation bindingOperation = (BindingOperation) binding.getBindingOperations().get(i);
                final Operation operation = bindingOperation.getOperation();
                final String operationName = operation.getName();

                PathItem pathItem = new PathItem();
                io.swagger.v3.oas.models.Operation openApiOperation = new io.swagger.v3.oas.models.Operation();
                openApiOperation.operationId(String.join("_", binding.getQName().getLocalPart(),operationName));
                if (operation.getDocumentationElement() != null) {
                    openApiOperation.description(operation.getDocumentationElement().getTextContent());
                }

                Input input = operation.getInput();
                if (input != null) {
                    extractSOAPAction(bindingOperation)
                            .ifPresent(action -> openApiOperation.addExtension(SOAP_EXTENSION_ACTION, action));
                    this.soapBuilder.generateSoapEnvelop(wsdlDefinition, binding,bindingOperation)
                            .ifPresent((envelope) -> openApiOperation.addExtension(SOAP_EXTENSION_ENVELOPE, envelope));
                }

                // create an empty Content definition used by each response description
                Content nodefContent = new Content();
                io.swagger.v3.oas.models.media.MediaType item = new io.swagger.v3.oas.models.media.MediaType();
                io.swagger.v3.oas.models.media.Schema schema = new io.swagger.v3.oas.models.media.Schema();
                schema.setType("object");
                item.setSchema(schema);
                nodefContent.addMediaType(MediaType.APPLICATION_JSON, item);

                Output output = operation.getOutput();
                ApiResponse successResp = new ApiResponse();
                successResp.content(nodefContent);
                successResp.setDescription(""); // description is mandatory
                if (output != null) {
                    Message msg = output.getMessage();
                    if (msg != null) {
                        if (msg.getDocumentationElement() != null) {
                            successResp.description(operation.getDocumentationElement().getTextContent());
                        }
                    }
                }

                ApiResponse errorResp = new ApiResponse();
                errorResp.description("Error throws by the Backend Service");
                errorResp.content(nodefContent);

                ApiResponses responses = new ApiResponses();
                responses.addApiResponse(Integer.toString(HttpStatusCode.OK_200), successResp);
                responses.addApiResponse(Integer.toString(HttpStatusCode.INTERNAL_SERVER_ERROR_500), errorResp);

                openApiOperation.setResponses(responses);

                // TODO use the http:binding element to extract the HTTP method (§4.4) ?
                switch (detectHttpMethod(operationName)) {
                    case GET:
                        pathItem.get(openApiOperation);
                        break;
                    case DELETE:
                        pathItem.delete(openApiOperation);
                        break;
                    case PUT:
                        pathItem.put(openApiOperation);
                        break;
                    default:
                        pathItem.post(openApiOperation);
                }

                if (singlePort) {
                    openAPI.path(String.join("/", "", serviceDefinition.getQName().getLocalPart(), operationName), pathItem);
                } else {
                    openAPI.path(String.join("/", "", serviceDefinition.getQName().getLocalPart(), portName, operationName), pathItem);
                }
            }
        }
    }

    private HttpMethod detectHttpMethod(String operationName) {
        String name = operationName.toLowerCase();
        if (name.startsWith("get") || name.startsWith("find")  || name.startsWith("search") ) {
            return HttpMethod.GET;
        }

        if (name.startsWith("delete") || name.startsWith("remove")) {
            return HttpMethod.DELETE;
        }

        if (name.startsWith("update")) {
            return HttpMethod.PUT;
        }

        return HttpMethod.POST;
    }

    private void createServer(OpenAPI openAPI, Port port) {
        String address = extractSOAPAddress(port);
        if (!serverCache.contains(address)) {
            Server server = new Server();
            server.setUrl(address);
            openAPI.addServersItem(server);
            serverCache.add(address);
        }
    }

}
