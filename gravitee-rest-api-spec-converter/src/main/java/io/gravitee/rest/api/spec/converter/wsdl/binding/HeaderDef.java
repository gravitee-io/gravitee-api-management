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

import javax.xml.namespace.QName;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HeaderDef {

    private QName message;
    private String use;
    private boolean required;
    private String part;

    // add here the Fault element

    public HeaderDef(QName message, String use, String part, Boolean required) {
        this.message = message;
        this.use = use;
        this.part = part;
        this.required = required == null ? true : required;
    }

    public boolean useEncoded() {
        return "encoded".equals(use);
    }

    public QName getMessage() {
        return message;
    }

    public String getUse() {
        return use;
    }

    public boolean isRequired() {
        return required;
    }

    public String getPart() {
        return part;
    }
}
