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
package io.gravitee.rest.api.service.sanitizer;

import com.vladsch.flexmark.ast.HtmlCommentBlock;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.owasp.html.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public final class HtmlSanitizer {

    private final Parser mdParser = Parser.builder(new MutableDataSet()).build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder(new MutableDataSet().set(HtmlRenderer.SUPPRESSED_LINKS, "")).build();

    private final PolicyFactory factory;

    public HtmlSanitizer(Environment environment) {
        this.factory = this.initPolicyFactory(environment);
    }

    public String sanitize(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        return factory.sanitize(content);
    }

    public <CTX> String sanitize(String content, HtmlChangeListener<CTX> listener, CTX context) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        return factory.sanitize(content, listener, context);
    }

    public SanitizeInfos isSafe(String content) {
        if (content == null || content.isEmpty()) {
            return new SanitizeInfos(true);
        }
        Document document = mdParser.parse(content);

        // Create a custom visitor to remove invalid comments
        InvalidCommentVisitor visitor = new InvalidCommentVisitor();
        visitor.visit(document);

        String toSanitize = htmlRenderer.render(document);
        List<String> sanitizedChanges = new ArrayList<>();
        HtmlChangeListener<List<String>> listener = new HtmlChangeListener<>() {
            @Override
            public void discardedTag(@Nullable List<String> context, String elementName) {
                context.add("Tag not allowed: " + elementName);
            }

            @Override
            public void discardedAttributes(@Nullable List<String> context, String tagName, String... attributeNames) {
                context.add("Attribute not allowed: [" + tagName + "]" + Arrays.toString(attributeNames));
            }
        };

        this.sanitize(toSanitize, listener, sanitizedChanges);
        return new SanitizeInfos(sanitizedChanges.isEmpty(), sanitizedChanges.toString());
    }

    @Getter
    public static class SanitizeInfos {

        boolean safe;
        String rejectedMessage;

        public SanitizeInfos(boolean safe) {
            this.safe = safe;
        }

        public SanitizeInfos(boolean safe, String rejectedMessage) {
            this.safe = safe;
            this.rejectedMessage = rejectedMessage;
        }
    }

    static class InvalidCommentVisitor extends NodeVisitor {

        InvalidCommentVisitor() {
            super(
                new VisitHandler<>(HtmlCommentBlock.class, htmlBlock -> {
                    String removedInvalidComments = htmlBlock.getChars().toString().replaceAll("<!-{2,3}>", "");
                    htmlBlock.setChars(BasedSequence.of(removedInvalidComments));
                })
            );
        }
    }

    private PolicyFactory initPolicyFactory(Environment environment) {
        AttributePolicy IntegerAttributePolicy = (elementName, attributeName, value) -> {
            int n = value.length();
            if (n == 0) {
                return null;
            }
            for (int i = 0; i < n; ++i) {
                char ch = value.charAt(i);
                if (ch == '.') {
                    if (i == 0) {
                        return null;
                    }
                    return value.substring(0, i); // truncate to integer.
                } else if (!('0' <= ch && ch <= '9')) {
                    return null;
                }
            }
            return value;
        };

        PolicyFactory htmlImagesSanitizer = new HtmlPolicyBuilder()
            .allowUrlProtocols("data", "http", "https")
            .allowElements("img")
            .allowAttributes("alt", "title", "src")
            .onElements("img")
            .allowAttributes("border", "height", "width")
            .matching(IntegerAttributePolicy)
            .onElements("img")
            .toFactory();

        PolicyFactory htmlCssSanitizer = new HtmlPolicyBuilder()
            .allowStyling(CssSchema.union(CssSchema.DEFAULT, CssSchema.withProperties(Collections.singleton("float"))))
            .toFactory();

        /*
         * Allow a set of HTML tags to support GitHub Flavoured Markdown. Spec is available at: <a href="https://github.github.com/gfm">https://github.github.com/gfm</a>
         */
        PolicyFactory githubFlavouredMarkdownSanitizer = new HtmlPolicyBuilder().allowElements("summary", "details").toFactory();

        PolicyFactory policyFactory = Sanitizers.BLOCKS.and(Sanitizers.FORMATTING)
            .and(
                new HtmlPolicyBuilder()
                    .allowStandardUrlProtocols()
                    .allowElements("a")
                    .allowAttributes("href", "title")
                    .onElements("a")
                    .toFactory()
            )
            .and(htmlCssSanitizer)
            .and(Sanitizers.TABLES)
            .and(new HtmlPolicyBuilder().allowElements("pre", "hr").toFactory())
            .and(new HtmlPolicyBuilder().allowElements("ol").allowAttributes("start").onElements("ol").toFactory())
            .and(htmlImagesSanitizer)
            .and(new HtmlPolicyBuilder().allowElements("code").allowAttributes("class").globally().toFactory())
            .and(githubFlavouredMarkdownSanitizer);

        PolicyFactory additionalElementsSanitizer = configureAdditionalElements(policyFactory, environment);
        if (additionalElementsSanitizer != null) {
            return policyFactory.and(additionalElementsSanitizer);
        }
        return policyFactory;
    }

    private PolicyFactory configureAdditionalElements(PolicyFactory factory, Environment environment) {
        Map<String, List<String>> allowedElements = getAllowedElementsFromProperties(environment);
        if (allowedElements.isEmpty()) {
            return null;
        }
        HtmlPolicyBuilder policyBuilder = new HtmlPolicyBuilder();
        for (Map.Entry<String, List<String>> entry : allowedElements.entrySet()) {
            String element = entry.getKey();
            List<String> attributes = entry.getValue();
            policyBuilder.allowElements(element);

            for (String attribute : attributes) {
                policyBuilder.allowAttributes(attribute).onElements(element);
            }
        }
        return policyBuilder.toFactory();
    }

    private Map<String, List<String>> getAllowedElementsFromProperties(Environment environment) {
        Map<String, List<String>> allowedElements = new HashMap<>();
        boolean found = true;
        int idx = 0;
        while (found) {
            String element = environment.getProperty("documentation.markdown.additional_allowed_elements[" + idx + "].element");
            if (element == null) {
                found = false;
            } else {
                List<String> attributes = readConfiguredAttributes(environment, idx);
                if (!attributes.isEmpty()) {
                    allowedElements.put(element, attributes);
                }
            }
            idx++;
        }

        return allowedElements;
    }

    private List<String> readConfiguredAttributes(Environment environment, int idx) {
        List<String> attributes = new ArrayList<>();
        boolean found = true;
        int attributeIdx = 0;
        while (found) {
            String attribute = environment.getProperty(
                "documentation.markdown.additional_allowed_elements[" + idx + "].attributes[" + attributeIdx + "]"
            );
            if (attribute == null) {
                found = false;
            } else {
                attributes.add(attribute);
            }
            attributeIdx++;
        }

        return attributes;
    }
}
