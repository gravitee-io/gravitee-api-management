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
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.owasp.html.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class HtmlSanitizer {

    private static final Parser mdParser = Parser.builder(new MutableDataSet()).build();
    private static final HtmlRenderer htmlRenderer = HtmlRenderer
        .builder(new MutableDataSet().set(HtmlRenderer.SUPPRESSED_LINKS, ""))
        .build();

    private static final AttributePolicy INTEGER = new AttributePolicy() {
        @Override
        public String apply(String elementName, String attributeName, String value) {
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
        }
    };

    private static final PolicyFactory HTML_IMAGES_SANITIZER = new HtmlPolicyBuilder()
        .allowUrlProtocols("data", "http", "https")
        .allowElements("img")
        .allowAttributes("alt", "title", "src")
        .onElements("img")
        .allowAttributes("border", "height", "width")
        .matching(INTEGER)
        .onElements("img")
        .toFactory();

    private static final PolicyFactory HTML_CSS_SANITIZER = new HtmlPolicyBuilder()
        .allowStyling(CssSchema.union(CssSchema.DEFAULT, CssSchema.withProperties(Collections.singleton("float"))))
        .toFactory();

    /**
     * Allow a set of HTML tags to support GitHub Flavoured Markdown. Spec is available at: <a href="https://github.github.com/gfm">https://github.github.com/gfm</a>
     */
    private static final PolicyFactory GITHUB_FLAVOURED_MARKDOWN = new HtmlPolicyBuilder().allowElements("summary", "details").toFactory();

    @Getter
    @Setter
    private static PolicyFactory factory = Sanitizers.BLOCKS
        .and(Sanitizers.FORMATTING)
        .and(
            new HtmlPolicyBuilder()
                .allowStandardUrlProtocols()
                .allowElements("a")
                .allowAttributes("href", "title")
                .onElements("a")
                .toFactory()
        )
        .and(HTML_CSS_SANITIZER)
        .and(Sanitizers.TABLES)
        .and(new HtmlPolicyBuilder().allowElements("pre", "hr").toFactory())
        .and(new HtmlPolicyBuilder().allowElements("ol").allowAttributes("start").onElements("ol").toFactory())
        .and(HTML_IMAGES_SANITIZER)
        .and(new HtmlPolicyBuilder().allowElements("code").allowAttributes("class").globally().toFactory())
        .and(GITHUB_FLAVOURED_MARKDOWN);

    private HtmlSanitizer() {}

    public static String sanitize(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        return factory.sanitize(content);
    }

    public static <CTX> String sanitize(String content, HtmlChangeListener<CTX> listener, CTX context) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        return factory.sanitize(content, listener, context);
    }

    public static SanitizeInfos isSafe(String content) {
        if (content == null || content.isEmpty()) {
            return new SanitizeInfos(true);
        }
        Document document = mdParser.parse(content);

        // Create a custom visitor to remove invalid comments
        InvalidCommentVisitor visitor = new InvalidCommentVisitor();
        visitor.visit(document);

        String toSanitize = htmlRenderer.render(document);
        List<String> sanitizedChanges = new ArrayList<>();
        HtmlChangeListener<List<String>> listener = new HtmlChangeListener<List<String>>() {
            @Override
            public void discardedTag(@Nullable List<String> context, String elementName) {
                context.add("Tag not allowed: " + elementName);
            }

            @Override
            public void discardedAttributes(@Nullable List<String> context, String tagName, String... attributeNames) {
                context.add("Attribute not allowed: [" + tagName + "]" + Arrays.toString(attributeNames));
            }
        };

        HtmlSanitizer.sanitize(toSanitize, listener, sanitizedChanges);
        return new SanitizeInfos(sanitizedChanges.isEmpty(), sanitizedChanges.toString());
    }

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

        public boolean isSafe() {
            return safe;
        }

        public String getRejectedMessage() {
            return rejectedMessage;
        }
    }

    static class InvalidCommentVisitor extends NodeVisitor {

        InvalidCommentVisitor() {
            super(
                new VisitHandler<>(
                    HtmlCommentBlock.class,
                    htmlBlock -> {
                        String removedInvalidComments = htmlBlock.getChars().toString().replaceAll("<!-{2,3}>", "");
                        htmlBlock.setChars(BasedSequence.of(removedInvalidComments));
                    }
                )
            );
        }
    }
}
