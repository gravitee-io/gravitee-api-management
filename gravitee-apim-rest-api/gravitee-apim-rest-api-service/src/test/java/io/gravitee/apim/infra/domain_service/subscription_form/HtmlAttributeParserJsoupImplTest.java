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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.subscription_form.domain_service.HtmlAttributeParser;
import io.gravitee.apim.core.subscription_form.domain_service.HtmlAttributeParser.ElementAttribute;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class HtmlAttributeParserJsoupImplTest {

    private HtmlAttributeParser parser;

    @BeforeEach
    void setUp() {
        parser = new HtmlAttributeParserJsoupImpl();
    }

    @Nested
    class SafeAttributes {

        @Test
        void should_parse_simple_gmd_attributes() {
            var result = parser.parseAllAttributes("<gmd-input name=\"email\" label=\"Email\" required=\"true\"/>");

            assertThat(result).contains(
                new ElementAttribute("gmd-input", "name", "email"),
                new ElementAttribute("gmd-input", "label", "Email"),
                new ElementAttribute("gmd-input", "required", "true")
            );
        }

        @Test
        void should_parse_nested_elements() {
            var result = parser.parseAllAttributes(
                """
                <gmd-grid columns="2">
                  <gmd-card>
                    <gmd-input name="field"/>
                  </gmd-card>
                </gmd-grid>
                """
            );

            assertThat(result).contains(
                new ElementAttribute("gmd-grid", "columns", "2"),
                new ElementAttribute("gmd-input", "name", "field")
            );
        }

        @Test
        void should_return_empty_for_elements_without_attributes() {
            var result = parser.parseAllAttributes("<p>Just text</p>");

            // Jsoup adds html, head, body wrapper elements — filter to check no "p" attrs
            assertThat(
                result
                    .stream()
                    .filter(a -> a.tagName().equals("p"))
                    .toList()
            ).isEmpty();
        }
    }

    @Nested
    class DangerousAttributes {

        @ParameterizedTest
        @MethodSource
        void should_parse_event_handler_attributes(EventHandlerCase testCase) {
            var result = parser.parseAllAttributes(testCase.html());

            assertThat(result).contains(new ElementAttribute(testCase.expectedTag(), testCase.expectedAttr(), testCase.expectedValue()));
        }

        static Stream<EventHandlerCase> should_parse_event_handler_attributes() {
            return Stream.of(
                new EventHandlerCase("<gmd-input onclick=\"alert('xss')\" name=\"email\"/>", "gmd-input", "onclick", "alert('xss')"),
                new EventHandlerCase("<gmd-card onerror=\"alert('xss')\">X</gmd-card>", "gmd-card", "onerror", "alert('xss')"),
                new EventHandlerCase("<img src=\"x\" onload=\"alert('xss')\"/>", "img", "onload", "alert('xss')"),
                new EventHandlerCase("<div onmouseover=\"steal()\">hover</div>", "div", "onmouseover", "steal()"),
                new EventHandlerCase("<gmd-input onfocus=\"alert(1)\" name=\"t\"/>", "gmd-input", "onfocus", "alert(1)")
            );
        }

        record EventHandlerCase(String html, String expectedTag, String expectedAttr, String expectedValue) {}

        @Test
        void should_parse_event_handler_with_spaces_in_value() {
            // This was the regex bypass case — Jsoup handles it correctly
            var result = parser.parseAllAttributes("<gmd-input onclick=\"alert( document.cookie )\" name=\"email\"/>");

            assertThat(result).contains(new ElementAttribute("gmd-input", "onclick", "alert( document.cookie )"));
        }
    }

    @Nested
    class JavaScriptProtocol {

        @ParameterizedTest
        @MethodSource
        void should_parse_javascript_protocol_in_url_attributes(JavaScriptCase testCase) {
            var result = parser.parseAllAttributes(testCase.html());

            assertThat(result).anyMatch(
                a ->
                    a.tagName().equals(testCase.expectedTag()) &&
                    a.attributeName().equals(testCase.expectedAttr()) &&
                    a.attributeValue().contains("javascript")
            );
        }

        static Stream<JavaScriptCase> should_parse_javascript_protocol_in_url_attributes() {
            return Stream.of(
                new JavaScriptCase("<a href=\"javascript:alert('xss')\">Click</a>", "a", "href"),
                new JavaScriptCase("<img src=\"javascript:alert('xss')\"/>", "img", "src"),
                new JavaScriptCase("<form action=\"javascript:void(0)\">X</form>", "form", "action")
            );
        }

        record JavaScriptCase(String html, String expectedTag, String expectedAttr) {}

        @Test
        void should_preserve_leading_spaces_in_javascript_protocol() {
            var result = parser.parseAllAttributes("<a href=\"  javascript : alert(1)\">Click</a>");

            assertThat(result).contains(new ElementAttribute("a", "href", "  javascript : alert(1)"));
        }

        @Test
        void should_parse_mailto_href_without_modification() {
            var result = parser.parseAllAttributes("<a href=\"mailto:security@example.com\">Mail</a>");

            assertThat(result).contains(new ElementAttribute("a", "href", "mailto:security@example.com"));
        }

        @Test
        void should_parse_tcp_href_without_modification() {
            var result = parser.parseAllAttributes("<a href=\"tcp://example.com:1234\">TCP</a>");

            assertThat(result).contains(new ElementAttribute("a", "href", "tcp://example.com:1234"));
        }
    }

    @Nested
    class LowercaseNormalization {

        @Test
        void should_lowercase_tag_names() {
            var result = parser.parseAllAttributes("<GMD-INPUT NAME=\"test\"/>");

            assertThat(result).anyMatch(a -> a.tagName().equals("gmd-input") && a.attributeName().equals("name"));
        }

        @Test
        void should_lowercase_attribute_names() {
            var result = parser.parseAllAttributes("<div OnClick=\"alert(1)\">X</div>");

            assertThat(result).anyMatch(a -> a.attributeName().equals("onclick"));
        }
    }
}
