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
package io.gravitee.rest.api.service.sanitizer;

import java.util.List;
import org.owasp.html.Handler;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.HtmlStreamEventReceiver;
import org.owasp.html.HtmlStreamRenderer;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
final class AllowAllSanitizer {

    private AllowAllSanitizer() {}

    public static String sanitize(String content) {
        return new AllowAllPolicy().sanitize(content);
    }

    /**
     * Policy that just do nothing that delegate all calls to an {@link HtmlStreamEventReceiver}.
     * This policy is mainly used to generate an html string that can be compared against a sanitized html.
     */
    private static class AllowAllPolicy implements HtmlSanitizer.Policy {

        private final StringBuilder htmlOutput = new StringBuilder();
        private final HtmlStreamEventReceiver delegate = HtmlStreamRenderer.create(htmlOutput, Handler.DO_NOTHING);

        public void openDocument() {
            delegate.openDocument();
        }

        public void openTag(String tagName, List<String> attrs) {
            delegate.openTag(tagName, attrs);
        }

        public void text(String text) {
            delegate.text(text);
        }

        public void closeTag(String tagName) {
            delegate.closeTag(tagName);
        }

        public void closeDocument() {
            delegate.closeDocument();
        }

        public String sanitize(String content) {
            HtmlSanitizer.sanitize(content, this);
            return htmlOutput.toString();
        }
    }
}
