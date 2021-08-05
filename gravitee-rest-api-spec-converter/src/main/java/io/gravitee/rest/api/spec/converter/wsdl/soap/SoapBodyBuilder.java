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
package io.gravitee.rest.api.spec.converter.wsdl.soap;

import static io.gravitee.rest.api.spec.converter.wsdl.WSDLUtils.formatQName;

import io.gravitee.rest.api.spec.converter.wsdl.binding.BobyParts;
import io.gravitee.rest.api.spec.converter.wsdl.binding.SoapVersion;
import io.gravitee.rest.api.spec.converter.wsdl.utils.SampleXmlUtil;
import java.util.Collection;
import java.util.Objects;
import javax.wsdl.*;
import javax.xml.namespace.QName;
import org.apache.xmlbeans.XmlCursor;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SoapBodyBuilder extends AbstractSoapBuilder {

    @Override
    public XmlCursor build() {
        BobyParts bodyParts = null;
        if (version.hasBodyElement(bindingOperation.getBindingInput().getExtensibilityElements())) {
            bodyParts = version.extractBodyParts(bindingOperation.getBindingInput().getExtensibilityElements());
        }

        Input input = bindingOperation.getOperation().getInput();
        xmlCursor.beginElement(version.getBodyQName());

        if (bodyParts != null) {
            if (bodyParts.useEncoded()) {
                xmlCursor.insertAttributeWithValue(SampleXmlUtil.XSI_TYPE, formatQName(version.getBodyQName()));
            }

            if (!version.isRpcStyle()) {
                // Document Style exchange
                // body contains XML description of part elements
                xmlCursor.toFirstChild();

                if (Objects.isNull(bodyParts.getPartNames()) || bodyParts.getPartNames().isEmpty()) {
                    // process all the parts
                    for (Part part : (Collection<Part>) input.getMessage().getParts().values()) {
                        generateXml(part, xmlCursor, bodyParts.useEncoded());
                    }
                } else {
                    // process only parts requested by the soap:body elt
                    for (Object partName : bodyParts.getPartNames()) {
                        generateXml((Part) input.getMessage().getParts().get(partName), xmlCursor, bodyParts.useEncoded());
                    }
                }

                // all part of the body element are generated, go back to the SoapEnvelope element
                xmlCursor.toParent(); // go to body elt
                xmlCursor.toParent(); // go to envelope elt
            } else if (version.isRpcStyle()) {
                // root element in rpc style is related to the operation
                // https://www.w3.org/TR/2007/REC-soap12-part2-20070427/#soapforrpc
                String ns = version.getRpcNamespace(bindingOperation);
                xmlCursor.toFirstChild();
                xmlCursor.beginElement(new QName(ns, bindingOperation.getName()));
                if (bodyParts.useEncoded()) {
                    xmlCursor.insertAttributeWithValue(version.getEncodingStyleQName(), SoapVersion.ENCODING_STYLE_URI);
                }

                if (Objects.isNull(bodyParts.getPartNames()) || bodyParts.getPartNames().isEmpty()) {
                    // process all the parts
                    for (Part part : (Collection<Part>) input.getMessage().getParts().values()) {
                        generateRpcPart(xmlCursor, part, bodyParts.useEncoded());
                    }
                } else {
                    // process only parts requested by the soap:body elt
                    for (Object partName : bodyParts.getPartNames()) {
                        Part part = (Part) input.getMessage().getParts().get(partName);
                        generateRpcPart(xmlCursor, part, bodyParts.useEncoded());
                    }
                }
            }
        } else {
            // no part for body, close the element
            xmlCursor.toParent();
        }

        return xmlCursor;
    }

    private void generateRpcPart(XmlCursor envelopeCursor, Part part, boolean encoded) {
        envelopeCursor.toLastChild();
        envelopeCursor.insertElement(part.getName());
        envelopeCursor.toPrevToken();
        generateXml(part, envelopeCursor, encoded);
        envelopeCursor.toParent();
    }
}
