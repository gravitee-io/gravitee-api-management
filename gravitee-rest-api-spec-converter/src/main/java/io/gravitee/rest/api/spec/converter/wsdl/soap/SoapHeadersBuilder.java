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

import io.gravitee.rest.api.spec.converter.wsdl.utils.SampleXmlUtil;
import io.gravitee.rest.api.spec.converter.wsdl.binding.HeaderDef;
import org.apache.xmlbeans.XmlCursor;

import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.xml.namespace.QName;
import java.util.List;

import static io.gravitee.rest.api.spec.converter.wsdl.WSDLUtils.formatQName;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SoapHeadersBuilder extends AbstractSoapBuilder {
    private Definition wsdlDef;

    public SoapHeadersBuilder withWsdlDef(Definition wsdlDef) {
        this.wsdlDef = wsdlDef;
        return this;
    }

    @Override
    public XmlCursor build() {
        if (version.hasHeadersElement(bindingOperation.getBindingInput().getExtensibilityElements())) {
            List<HeaderDef> headers = version.extractHeaderParts(bindingOperation.getBindingInput().getExtensibilityElements());
            for (HeaderDef header : headers) {
                generateHeader(header);
            }
        }
        return xmlCursor;
    }

    private void generateHeader(HeaderDef header) {
        Message msg = wsdlDef.getMessage(header.getMessage());
        Part part = msg.getPart(header.getPart());

        xmlCursor.beginElement(version.getHeaderQName());
        if (header.useEncoded()) {
            xmlCursor.insertAttributeWithValue(SampleXmlUtil.XSI_TYPE, formatQName(version.getHeaderQName()));
        }

        xmlCursor.toFirstChild();
        xmlCursor.beginElement(new QName(part.getName()));

        QName mustUnderstandQName = version.buildAttribute("mustUnderstand");
        Object mustUnderstand = part.getExtensionAttribute(mustUnderstandQName);
        if (mustUnderstand != null && mustUnderstand instanceof String) {
            xmlCursor.insertAttributeWithValue(mustUnderstandQName, (String)mustUnderstand);
        }

        QName actorQName = version.buildAttribute("actor");
        Object actor = part.getExtensionAttribute(actorQName);
        if (actor != null && actor instanceof String) {
            xmlCursor.insertAttributeWithValue(actorQName, (String)actor);
        }

        generateXml(part, xmlCursor, header.useEncoded());
        xmlCursor.toParent();
        xmlCursor.toParent();
    }

}
