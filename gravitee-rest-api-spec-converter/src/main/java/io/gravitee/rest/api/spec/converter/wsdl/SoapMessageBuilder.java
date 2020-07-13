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

import io.gravitee.rest.api.spec.converter.wsdl.binding.SoapVersion;
import io.gravitee.rest.api.spec.converter.wsdl.soap.SoapBodyBuilder;
import io.gravitee.rest.api.spec.converter.wsdl.soap.SoapHeadersBuilder;
import io.gravitee.rest.api.spec.converter.wsdl.utils.SampleXmlUtil;
import org.apache.xmlbeans.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.wsdl.*;
import javax.wsdl.extensions.schema.Schema;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static io.gravitee.rest.api.spec.converter.wsdl.WSDLUtils.detectSoapVersion;
import static io.gravitee.rest.api.spec.converter.wsdl.WSDLUtils.formatQName;

public class SoapMessageBuilder {
    public static final Logger LOGGER = LoggerFactory.getLogger(SoapMessageBuilder.class);
    public static final String XMLSCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String XSD_PREFIX = "xsd";

    private final Map<Object, Object> namespaceMappings;
    private final Map<Object, Object> prefixToNamespaces;
    private final XmlOptions options;

    private List<XmlObject> schemas = new ArrayList<>();
    private SchemaTypeSystem shemaTypeSystem;
    private boolean compiled = false;

    public SoapMessageBuilder(Map<Object, Object> namespaceMappings) {
        this.namespaceMappings = namespaceMappings;
        this.namespaceMappings.put(SampleXmlUtil.XSI_TYPE.getNamespaceURI(), SampleXmlUtil.XSI_TYPE.getPrefix());
        this.namespaceMappings.put(XMLSCHEMA, XSD_PREFIX);
        this.options = new XmlOptions();
        this.options.setLoadAdditionalNamespaces(this.namespaceMappings);
        this.options.setSavePrettyPrint();
        this.options.setSavePrettyPrintIndent(2);

        // provide prefixes used by the WSDL to keep naming consistency in SoapEnvelope message
        this.prefixToNamespaces = new HashMap();
        for (Map.Entry entry : namespaceMappings.entrySet()) {
            this.prefixToNamespaces.put(entry.getValue(), entry.getKey());
        }
        this.options.setSaveSuggestedPrefixes(this.prefixToNamespaces);
    }

    public void addSchema(Schema schema) {
        try {
            schemas.add(XmlObject.Factory.parse(schema.getElement(), this.options));
        } catch (XmlException e) {
            LOGGER.debug("XSD parsing failed, OpenAPI specification maybe generated without SOAP envelop", e);
        }
    }

    public void compileSchemas() {
        try {
            shemaTypeSystem = XmlBeans.compileXsd(schemas.toArray(new XmlObject[schemas.size()]), XmlBeans.getBuiltinTypeSystem(), options);
            this.compiled = true;
        } catch (XmlException e) {
            LOGGER.debug("Compilation of XSD failed, OpenAPI specification will be generated without SOAP envelop", e);
        }
    }

    public Optional<String> generateSoapEnvelop(Definition wsdlDef, Binding binding, BindingOperation bindingOperation) {
        if (!compiled) {
            compileSchemas();
        }

        try (StringWriter writer = new StringWriter()){

            Optional<SoapVersion> optVersion = detectSoapVersion(binding.getExtensibilityElements());
            if (!optVersion.isPresent()) {
                return Optional.empty();
            }

            SoapVersion version = optVersion.get();

            boolean useEncoded = version.useEncoded(bindingOperation);

            XmlObject soapEnvelope = XmlObject.Factory.newInstance();
            SoapBookmark bookmark = new SoapBookmark();
            XmlCursor envelopeCursor = soapEnvelope.newCursor();
            envelopeCursor.toNextToken();
            envelopeCursor.beginElement(version.getEnvelopeQName());

            if (useEncoded) {
                envelopeCursor.insertNamespace(SampleXmlUtil.XSI_TYPE.getPrefix(), SampleXmlUtil.XSI_TYPE.getNamespaceURI());
                envelopeCursor.insertNamespace(XSD_PREFIX, XMLSCHEMA);
                envelopeCursor.insertAttributeWithValue(SampleXmlUtil.XSI_TYPE, formatQName(version.getEnvelopeQName()));
            }

            envelopeCursor.toLastChild();
            envelopeCursor.setBookmark(bookmark);

            new SoapHeadersBuilder()
                    .withWsdlDef(wsdlDef)
                    .withBindingOperation(bindingOperation)
                    .withCursor(envelopeCursor)
                    .withNamespaceMappings(namespaceMappings)
                    .withShemaTypeSystem(shemaTypeSystem)
                    .withVersion(version)
                    .build();

            envelopeCursor.toBookmark(bookmark);
            envelopeCursor.toLastChild();

            new SoapBodyBuilder()
                    .withBindingOperation(bindingOperation)
                    .withCursor(envelopeCursor)
                    .withNamespaceMappings(namespaceMappings)
                    .withShemaTypeSystem(shemaTypeSystem)
                    .withVersion(version)
                    .build();

            envelopeCursor.dispose();
            soapEnvelope.save(writer, options);
            writer.flush();
            return Optional.ofNullable(writer.toString());
        } catch (IOException e) {
            LOGGER.debug("Generation of Soap Envelope failed for binding : {} ", binding.getQName(), e);
        }
        return Optional.empty();
    }

}
