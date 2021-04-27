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

import java.util.List;
import javax.wsdl.BindingOperation;
import javax.xml.namespace.QName;

public interface SoapVersion {
    String ENCODING_STYLE_URI = "http://schemas.xmlsoap.org/soap/encoding/";

    QName getEncodingStyleQName();

    QName getEnvelopeQName();

    QName getBodyQName();

    QName getHeaderQName();

    QName getFaultQName();

    boolean isRpcStyle();

    boolean hasBodyElement(List<Object> elements);

    BobyParts extractBodyParts(List<Object> elements);

    boolean hasHeadersElement(List<Object> elements);

    List<HeaderDef> extractHeaderParts(List<Object> elements);

    String getRpcNamespace(BindingOperation bindingOperation);

    boolean useEncoded(BindingOperation bindingOperation);

    QName buildAttribute(String localPart);
}
