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

import javax.wsdl.BindingOperation;
import javax.xml.namespace.QName;

public abstract class AbstractBinding implements SoapVersion {

    private QName envelopeQName;
    private QName bodyQName;
    private QName headerQName;
    private QName faultQName;
    private QName encodingStyleQName;

    private boolean rpcStyle = false;

    AbstractBinding(String namespace, String prefix, boolean rpc) {
        this.envelopeQName = new QName(namespace, "Envelope", prefix);
        this.bodyQName = new QName(namespace, "Body", prefix);
        this.headerQName = new QName(namespace, "Header", prefix);
        this.faultQName = new QName(namespace, "Fault", prefix);
        this.encodingStyleQName = new QName(namespace, "encodingStyle", prefix);
        this.rpcStyle = rpc;
    }

    @Override
    public QName getEncodingStyleQName() {
        return this.encodingStyleQName;
    }

    @Override
    public QName getEnvelopeQName() {
        return this.envelopeQName;
    }

    @Override
    public QName getBodyQName() {
        return this.bodyQName;
    }

    @Override
    public QName getHeaderQName() {
        return this.headerQName;
    }

    @Override
    public QName getFaultQName() {
        return this.faultQName;
    }

    @Override
    public boolean isRpcStyle() {
        return this.rpcStyle;
    }

    @Override
    public boolean useEncoded(BindingOperation bindingOperation) {
        if (hasBodyElement(bindingOperation.getBindingInput().getExtensibilityElements())) {
            return extractBodyParts(bindingOperation.getBindingInput().getExtensibilityElements()).useEncoded();
        }
        return false;
    }
}
