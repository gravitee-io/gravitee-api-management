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
package io.gravitee.apim.infra.sanitizer;

import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.core.sanitizer.SanitizeResult;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@Slf4j
public class HtmlSanitizerImpl implements HtmlSanitizer {

    @Autowired
    private Environment environment;

    private PolicyFactory userDefinedElementsPolicy;

    @Override
    public String sanitize(String content) {
        return io.gravitee.rest.api.service.sanitizer.HtmlSanitizer.sanitize(content);
    }

    @Override
    public SanitizeResult isSafe(String content) {
        var sanitizeInfos = io.gravitee.rest.api.service.sanitizer.HtmlSanitizer.isSafe(content);
        return SanitizeResult.builder().safe(sanitizeInfos.isSafe()).rejectedMessage(sanitizeInfos.getRejectedMessage()).build();
    }

    @PostConstruct
    public void init() {
        configureAdditionalElements();
        updateStaticFactory();
    }

    public void configureAdditionalElements() {
        Map<String, List<String>> allowedElements = getAllowedElementsFromProperties();

        HtmlPolicyBuilder policyBuilder = new HtmlPolicyBuilder();
        for (Map.Entry<String, List<String>> entry : allowedElements.entrySet()) {
            String element = entry.getKey();
            List<String> attributes = entry.getValue();
            policyBuilder.allowElements(element);

            for (String attribute : attributes) {
                policyBuilder.allowAttributes(attribute).onElements(element);
            }
        }
        userDefinedElementsPolicy = policyBuilder.toFactory();
    }

    private Map<String, List<String>> getAllowedElementsFromProperties() {
        Map<String, List<String>> allowedElements = new HashMap<>();
        boolean found = true;
        int idx = 0;
        while (found) {
            String element = environment.getProperty("documentation.markdown.additional_allowed_elements[" + idx + "].element");
            if (element == null) {
                found = false;
            } else {
                List<String> attributes = readConfiguredAttributes(idx);
                if (attributes != null && !attributes.isEmpty()) {
                    allowedElements.put(element, attributes);
                }
            }
            idx++;
        }

        return allowedElements;
    }

    private List<String> readConfiguredAttributes(int idx) {
        List<String> attributes = new ArrayList<>();
        boolean found = true;
        int attributeIdx = 0;
        while (found) {
            String attribute = environment.getProperty(
                "documentation.markdown.additional_allowed_elements[" + idx + "].attributes[" + attributeIdx + "]"
            );
            found = (attribute != null);
            if (found) {
                attributes.add(attribute);
            }
            attributeIdx++;
        }

        return attributes;
    }

    private void updateStaticFactory() {
        PolicyFactory factory = io.gravitee.rest.api.service.sanitizer.HtmlSanitizer.getFactory();
        if (userDefinedElementsPolicy != null) {
            factory = factory.and(userDefinedElementsPolicy);
        }
        io.gravitee.rest.api.service.sanitizer.HtmlSanitizer.setFactory(factory);
    }
}
