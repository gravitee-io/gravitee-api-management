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

import io.gravitee.rest.api.spec.converter.wsdl.binding.Soap12Binding;
import io.gravitee.rest.api.spec.converter.wsdl.binding.SoapBinding;
import io.gravitee.rest.api.spec.converter.wsdl.binding.SoapVersion;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.wsdl.BindingOperation;
import javax.wsdl.Port;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.xml.namespace.QName;

public class WSDLUtils {

    public static <T> Optional<T> extractFirstElement(List<Object> elements, Class<T> type) {
        return elements.stream().filter(elt -> type.isAssignableFrom(elt.getClass())).map(e -> (T) e).findFirst();
    }

    public static <T> Stream<T> extractAllElements(List<Object> elements, Class<T> type) {
        return elements.stream().filter(elt -> type.isAssignableFrom(elt.getClass())).map(e -> (T) e);
    }

    public static Optional<SoapVersion> detectSoapVersion(List<Object> elements) {
        Optional<SOAP12Binding> optSoap12 = extractFirstElement(elements, SOAP12Binding.class);
        if (optSoap12.isPresent()) {
            return Optional.of(new Soap12Binding("rpc".equalsIgnoreCase(optSoap12.get().getStyle())));
        }
        Optional<SOAPBinding> optSoap = extractFirstElement(elements, SOAPBinding.class);
        if (optSoap.isPresent()) {
            return Optional.of(new SoapBinding("rpc".equalsIgnoreCase(optSoap.get().getStyle())));
        }
        return Optional.empty();
    }

    public static String extractSOAPAddress(Port port) {
        Optional<SOAPAddress> optAddr = extractFirstElement(port.getExtensibilityElements(), SOAPAddress.class);
        Optional<SOAP12Address> optAddr12 = extractFirstElement(port.getExtensibilityElements(), SOAP12Address.class);
        Optional<HTTPAddress> optAddrHttp = extractFirstElement(port.getExtensibilityElements(), HTTPAddress.class);
        if (optAddr.isPresent()) {
            return optAddr.get().getLocationURI();
        } else if (optAddr12.isPresent()) {
            return optAddr12.get().getLocationURI();
        } else {
            return optAddrHttp.map(addr -> addr.getLocationURI()).orElse("");
        }
    }

    public static Optional<String> extractSOAPAction(BindingOperation operation) {
        Optional<SOAPOperation> optAddr = extractFirstElement(operation.getExtensibilityElements(), SOAPOperation.class);
        if (optAddr.isPresent()) {
            return optAddr.map(SOAPOperation::getSoapActionURI);
        } else {
            Optional<SOAP12Operation> optAddr12 = extractFirstElement(operation.getExtensibilityElements(), SOAP12Operation.class);
            return optAddr12.map(SOAP12Operation::getSoapActionURI);
        }
    }

    public static String formatQName(QName qname) {
        if (qname.getPrefix() != null) {
            return qname.getPrefix() + ":" + qname.getLocalPart();
        } else {
            return qname.getLocalPart();
        }
    }
}
