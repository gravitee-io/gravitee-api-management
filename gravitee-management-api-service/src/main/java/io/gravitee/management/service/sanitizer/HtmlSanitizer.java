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
package io.gravitee.management.service.sanitizer;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.lang3.StringUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class HtmlSanitizer {

    private static final Parser mdParser = Parser.builder(new MutableDataSet()).build();
    private static final HtmlRenderer htmlRenderer = HtmlRenderer.builder(new MutableDataSet()).build();

    private static final PolicyFactory factory = Sanitizers.BLOCKS
            .and(Sanitizers.FORMATTING)
            .and(Sanitizers.IMAGES)
            .and(new HtmlPolicyBuilder()
                    .allowStandardUrlProtocols().allowElements("a")
                    .allowAttributes("href").onElements("a")
                    .toFactory())
            .and(Sanitizers.STYLES)
            .and(Sanitizers.TABLES)
            .and(new HtmlPolicyBuilder()
                    .allowElements("pre", "hr").toFactory())
            .and(new HtmlPolicyBuilder()
                    .allowElements("img")
                    .allowAttributes("title")
                    .globally()
                    .toFactory())
            .and(new HtmlPolicyBuilder()
                    .allowElements("code")
                    .allowAttributes("class")
                    .globally()
                    .toFactory());

    private HtmlSanitizer() {
    }

    public static String sanitize(String content) {

        if (content == null || content.isEmpty()) {
            return content;
        }

        return factory.sanitize(content);
    }

    public static SanitizeInfos isSafe(String content) {

        if (content == null || content.isEmpty()) {
            return new SanitizeInfos(true);
        }

        String toSanitize = htmlRenderer.render(mdParser.parse(content));
        String initialHtml = AllowAllSanitizer.sanitize(toSanitize);
        String sanitizedHtml = HtmlSanitizer.sanitize(toSanitize);

        String[] initialLines = initialHtml.split("\n");
        String[] sanitizedLines = sanitizedHtml.split("\n");


        String diffMessage = null;

        for (int i = 0; i < initialLines.length; i++) {

            if (i < sanitizedLines.length) {
                String difference = StringUtils.difference(sanitizedLines[i], initialLines[i]);

                if (!difference.isEmpty()) {
                    diffMessage = "The content [" + difference + "] is not allowed (~line " + (i + 1)+ ")";
                }
            } else {
                diffMessage = "The content [" + initialLines[i] + "] is not allowed";
            }

            if(diffMessage != null) {
                break;
            }
        }

        if (diffMessage == null) {
            return new SanitizeInfos(true);
        }

        return new SanitizeInfos(false, diffMessage);
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
}