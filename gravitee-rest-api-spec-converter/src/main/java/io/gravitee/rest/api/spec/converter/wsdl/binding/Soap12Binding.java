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
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.wsdl.BindingOperation;
import javax.wsdl.extensions.soap12.SOAP12Body;
import javax.wsdl.extensions.soap12.SOAP12Header;
import javax.xml.namespace.QName;

public class Soap12Binding extends AbstractBinding {

    public static final String SOAP12_ENVELOPE_NS = "http://www.w3.org/2003/05/soap-envelope";
    public static final String SOAP12_ENVELOPE_PREFIX = "soapenv";

    public Soap12Binding(boolean rpc) {
        super(SOAP12_ENVELOPE_NS, SOAP12_ENVELOPE_PREFIX, rpc);
    }

    @Override
    public boolean hasBodyElement(List<Object> elements) {
        Optional<SOAP12Body> optBody12 = extractFirstElement(elements, SOAP12Body.class);
        return optBody12.isPresent();
    }

    @Override
    public BobyParts extractBodyParts(List<Object> elements) {
        Optional<SOAP12Body> optBody12 = extractFirstElement(elements, SOAP12Body.class);
        final Optional<BobyParts> optionalBobyParts = optBody12.map(
            body -> new BobyParts(body.getParts() == null ? emptyList() : body.getParts(), body.getUse())
        );
        return optionalBobyParts.orElse(null);
    }

    @Override
    public boolean hasHeadersElement(List<Object> elements) {
        Optional<SOAP12Header> optHeader12 = extractFirstElement(elements, SOAP12Header.class);
        return optHeader12.isPresent();
    }

    @Override
    public List<HeaderDef> extractHeaderParts(List<Object> elements) {
        return extractAllElements(elements, SOAP12Header.class)
            .map(header -> new HeaderDef(header.getMessage(), header.getUse(), header.getPart(), header.getRequired()))
            .collect(Collectors.toList());
    }

    @Override
    public String getRpcNamespace(BindingOperation bindingOperation) {
        Optional<SOAP12Body> optional = extractFirstElement(
            bindingOperation.getBindingInput().getExtensibilityElements(),
            SOAP12Body.class
        );
        return optional.get().getNamespaceURI();
    }

    @Override
    public QName buildAttribute(String localPart) {
        return new QName(SOAP12_ENVELOPE_NS, localPart, SOAP12_ENVELOPE_PREFIX);
    }
}
