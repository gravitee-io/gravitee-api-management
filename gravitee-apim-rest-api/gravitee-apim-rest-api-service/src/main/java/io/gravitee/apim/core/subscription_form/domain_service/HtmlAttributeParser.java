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
package io.gravitee.apim.core.subscription_form.domain_service;

import java.util.List;

/**
 * Parses HTML/GMD content and extracts all element attributes.
 * Core-layer abstraction — infrastructure implements this with a real HTML parser.
 *
 * @author Gravitee.io Team
 */
public interface HtmlAttributeParser {
    /**
     * Parses the given HTML fragment and returns a flat list of every attribute
     * found on every element.
     */
    List<ElementAttribute> parseAllAttributes(String htmlFragment);

    /**
     * A single attribute on an element.
     */
    record ElementAttribute(String tagName, String attributeName, String attributeValue) {}
}
