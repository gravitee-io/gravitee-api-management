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
package io.gravitee.rest.api.spec.converter.wsdl.binding;

import static io.gravitee.rest.api.spec.converter.wsdl.WSDLUtils.extractAllElements;
import static io.gravitee.rest.api.spec.converter.wsdl.WSDLUtils.extractFirstElement;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.wsdl.BindingOperation;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.xml.namespace.QName;

public class SoapBinding extends AbstractBinding {

    public static final String SOAP_ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String SOAP_ENVELOPE_PREFIXS = "soapenv";

    public SoapBinding(boolean rpc) {
        super(SOAP_ENVELOPE_NS, SOAP_ENVELOPE_PREFIXS, rpc);
    }

    @Override
    public boolean hasBodyElement(List<Object> elements) {
        Optional<SOAPBody> optBody = extractFirstElement(elements, SOAPBody.class);
        return optBody.isPresent();
    }

    @Override
    public BobyParts extractBodyParts(List<Object> elements) {
        Optional<SOAPBody> optBody = extractFirstElement(elements, SOAPBody.class);
        return new BobyParts(optBody.get().getParts() == null ? Collections.EMPTY_LIST : optBody.get().getParts(), optBody.get().getUse());
    }

    @Override
    public boolean hasHeadersElement(List<Object> elements) {
        Optional<SOAPHeader> optHeader = extractFirstElement(elements, SOAPHeader.class);
        return optHeader.isPresent();
    }

    @Override
    public List<HeaderDef> extractHeaderParts(List<Object> elements) {
        return extractAllElements(elements, SOAPHeader.class)
            .map(header -> new HeaderDef(header.getMessage(), header.getUse(), header.getPart(), header.getRequired()))
            .collect(Collectors.toList());
    }

    @Override
    public String getRpcNamespace(BindingOperation bindingOperation) {
        Optional<SOAPBody> optional = extractFirstElement(bindingOperation.getBindingInput().getExtensibilityElements(), SOAPBody.class);
        return optional.get().getNamespaceURI();
    }

    @Override
    public QName buildAttribute(String localPart) {
        return new QName(SOAP_ENVELOPE_NS, localPart, SOAP_ENVELOPE_PREFIXS);
    }
}
