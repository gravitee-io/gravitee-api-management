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

import java.util.function.BiConsumer;
import org.apache.xmlbeans.SchemaLocalElement;
import org.apache.xmlbeans.SchemaParticle;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;

/**
 * Generates EL placeholder expressions instead of sample values for SOAP envelope elements.
 * Leaf elements get {@code {#xmlEscape(#jsonPath(#request.content, '$.path'))}} while
 * complex types are traversed recursively, building the JSON path from the element hierarchy.
 */
public class ElPlaceholderXmlUtil {

    private static final Runnable SKIP_NON_COMPLEX_CONTENT = () -> {};
    private static final Runnable SKIP_UNSUPPORTED_PARTICLE_TYPE = () -> {};

    private ElPlaceholderXmlUtil() {}

    /**
     * Returns a {@link BiConsumer} that writes EL expressions for leaf elements
     * and traverses complex types recursively. The JSON path starts at {@code $}
     * and skips the root XML element name (the WSDL operation wrapper).
     */
    public static BiConsumer<SchemaType, XmlCursor> typeContentWriter() {
        return (stype, xmlc) -> {
            // The document type wraps a root element (e.g. AddRequest).
            // Insert the root element in XML but start the JSON path at "$"
            // so children get "$.a", "$.b" instead of "$.AddRequest.a".
            if (stype.getContentModel() != null && stype.getContentModel().getParticleType() == SchemaParticle.ELEMENT) {
                SchemaLocalElement rootElement = (SchemaLocalElement) stype.getContentModel();
                xmlc.insertElement(rootElement.getName().getLocalPart(), rootElement.getName().getNamespaceURI());
                xmlc.toPrevToken();
                createPlaceholderForType(rootElement.getType(), xmlc, "$");
                xmlc.toNextToken();
            } else {
                createPlaceholderForType(stype, xmlc, "$");
            }
        };
    }

    private static void createPlaceholderForType(SchemaType stype, XmlCursor xmlc, String jsonPath) {
        if (stype.isSimpleType() || stype.isURType()) {
            writeElPlaceholder(xmlc, jsonPath);
            return;
        }

        switch (stype.getContentType()) {
            case SchemaType.SIMPLE_CONTENT -> writeElPlaceholder(xmlc, jsonPath);
            case SchemaType.ELEMENT_CONTENT, SchemaType.MIXED_CONTENT -> {
                if (stype.getContentModel() != null) {
                    processParticle(stype.getContentModel(), xmlc, jsonPath);
                }
            }
            default -> SKIP_NON_COMPLEX_CONTENT.run();
        }
    }

    private static void processParticle(SchemaParticle sp, XmlCursor xmlc, String jsonPath) {
        switch (sp.getParticleType()) {
            case SchemaParticle.ELEMENT -> processElement(sp, xmlc, jsonPath);
            case SchemaParticle.SEQUENCE, SchemaParticle.CHOICE, SchemaParticle.ALL -> {
                for (SchemaParticle child : sp.getParticleChildren()) {
                    processParticle(child, xmlc, jsonPath);
                }
            }
            default -> SKIP_UNSUPPORTED_PARTICLE_TYPE.run();
        }
    }

    private static void processElement(SchemaParticle sp, XmlCursor xmlc, String parentPath) {
        SchemaLocalElement element = (SchemaLocalElement) sp;
        String elementName = element.getName().getLocalPart();
        String currentPath = parentPath + "." + elementName;

        xmlc.insertElement(elementName, element.getName().getNamespaceURI());
        xmlc.toPrevToken();
        createPlaceholderForType(element.getType(), xmlc, currentPath);
        xmlc.toNextToken();
    }

    private static void writeElPlaceholder(XmlCursor xmlc, String jsonPath) {
        xmlc.insertChars("{#xmlEscape(#jsonPath(#request.content, '" + jsonPath + "'))}");
    }
}
