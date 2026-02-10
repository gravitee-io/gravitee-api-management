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
package io.gravitee.apim.infra.domain_service.subscription_form;

import io.gravitee.apim.core.subscription_form.domain_service.HtmlAttributeParser;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

/**
 * Jsoup-based implementation of {@link HtmlAttributeParser}.
 * Uses a real HTML parser — no regex bypasses possible.
 *
 * @author Gravitee.io Team
 */
@Service
public class HtmlAttributeParserJsoupImpl implements HtmlAttributeParser {

    @Override
    public List<ElementAttribute> parseAllAttributes(String htmlFragment) {
        Document doc = Jsoup.parseBodyFragment(htmlFragment);
        List<ElementAttribute> result = new ArrayList<>();

        for (Element element : doc.getAllElements()) {
            String tagName = element.tagName().toLowerCase();
            for (Attribute attr : element.attributes()) {
                result.add(new ElementAttribute(tagName, attr.getKey().toLowerCase(), attr.getValue()));
            }
        }

        return result;
    }
}
