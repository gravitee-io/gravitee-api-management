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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.HtmlAttributeParserInMemory;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.core.sanitizer.SanitizeResult;
import io.gravitee.apim.core.subscription_form.domain_service.HtmlAttributeParser.ElementAttribute;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormContentUnsafeException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class ValidateSubscriptionFormDomainServiceTest {

    private HtmlSanitizer htmlSanitizer;
    private HtmlAttributeParserInMemory htmlAttributeParser;
    private ValidateSubscriptionFormDomainService validator;

    @BeforeEach
    void setUp() {
        htmlSanitizer = mock(HtmlSanitizer.class);
        htmlAttributeParser = new HtmlAttributeParserInMemory();
        validator = new ValidateSubscriptionFormDomainService(htmlSanitizer, htmlAttributeParser);
        // Default: HtmlSanitizer reports content as safe
        when(htmlSanitizer.isSafe(anyString())).thenReturn(SanitizeResult.builder().safe(true).build());
    }

    @Nested
    class EmptyContent {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "\t", "\n" })
        void should_throw_when_content_is_blank(String content) {
            assertThatThrownBy(() -> validator.validateContent(content))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining(
                    "Subscription form content cannot be empty. Please disable the form if you don’t want to display it."
                );
        }
    }

    @Nested
    class ValidContent {

        @Test
        void should_accept_content_with_safe_attributes() {
            htmlAttributeParser.withAttributes(
                List.of(
                    new ElementAttribute("gmd-grid", "columns", "2"),
                    new ElementAttribute("gmd-input", "name", "email"),
                    new ElementAttribute("gmd-input", "label", "Email"),
                    new ElementAttribute("gmd-input", "required", "true"),
                    new ElementAttribute("gmd-select", "options", "A,B,C")
                )
            );

            assertThatCode(() ->
                validator.validateContent("<gmd-grid columns=\"2\"><gmd-input name=\"email\"/></gmd-grid>")
            ).doesNotThrowAnyException();
        }

        @Test
        void should_accept_content_with_no_attributes() {
            htmlAttributeParser.withAttributes(List.of());

            assertThatCode(() -> validator.validateContent("<gmd-card><p>Text</p></gmd-card>")).doesNotThrowAnyException();
        }

        @Test
        void should_accept_safe_url_attributes() {
            htmlAttributeParser.withAttributes(
                List.of(new ElementAttribute("a", "href", "https://gravitee.io"), new ElementAttribute("img", "src", "/images/logo.png"))
            );

            assertThatCode(() -> validator.validateContent("<a href=\"https://gravitee.io\">Link</a>")).doesNotThrowAnyException();
        }
    }

    @Nested
    class DangerousEventHandlers {

        @ParameterizedTest
        @MethodSource
        void should_reject_dangerous_event_handler_attributes(ElementAttribute dangerousAttr) {
            htmlAttributeParser.withAttributes(List.of(dangerousAttr));

            assertThatThrownBy(() -> validator.validateContent("any content"))
                .isInstanceOf(SubscriptionFormContentUnsafeException.class)
                .hasMessageContaining("Dangerous attribute")
                .hasMessageContaining(dangerousAttr.attributeName())
                .hasMessageContaining("not allowed");
        }

        static Stream<ElementAttribute> should_reject_dangerous_event_handler_attributes() {
            return Stream.of(
                new ElementAttribute("gmd-input", "onclick", "alert('xss')"),
                new ElementAttribute("gmd-card", "onerror", "alert('xss')"),
                new ElementAttribute("img", "onload", "alert('xss')"),
                new ElementAttribute("gmd-input", "onfocus", "alert(1)"),
                new ElementAttribute("div", "onmouseover", "steal()"),
                new ElementAttribute("form", "onsubmit", "steal()"),
                new ElementAttribute("gmd-textarea", "onkeyup", "log(this.value)"),
                new ElementAttribute("gmd-input", "onblur", "track()"),
                new ElementAttribute("div", "ondrag", "exfiltrate()"),
                new ElementAttribute("div", "ondrop", "inject()")
            );
        }
    }

    @Nested
    class JavaScriptProtocol {

        @ParameterizedTest
        @MethodSource
        void should_reject_javascript_protocol_in_url_attributes(ElementAttribute urlAttr) {
            htmlAttributeParser.withAttributes(List.of(urlAttr));

            assertThatThrownBy(() -> validator.validateContent("any content"))
                .isInstanceOf(SubscriptionFormContentUnsafeException.class)
                .hasMessageContaining("JavaScript protocol not allowed");
        }

        static Stream<ElementAttribute> should_reject_javascript_protocol_in_url_attributes() {
            return Stream.of(
                new ElementAttribute("a", "href", "javascript:alert('xss')"),
                new ElementAttribute("img", "src", "javascript:alert('xss')"),
                new ElementAttribute("form", "action", "javascript:void(0)"),
                new ElementAttribute("object", "data", "javascript:alert(1)"),
                new ElementAttribute("button", "formaction", "javascript:submit()"),
                // with leading spaces
                new ElementAttribute("a", "href", "  javascript : alert(1)")
            );
        }

        @Test
        void should_not_reject_safe_url_values() {
            htmlAttributeParser.withAttributes(
                List.of(
                    new ElementAttribute("a", "href", "https://example.com"),
                    new ElementAttribute("img", "src", "/images/photo.jpg"),
                    new ElementAttribute("form", "action", "/api/submit")
                )
            );

            assertThatCode(() -> validator.validateContent("any content")).doesNotThrowAnyException();
        }
    }

    @Nested
    class DisallowedUrlProtocols {

        @ParameterizedTest
        @MethodSource
        void should_reject_non_http_https_protocols_in_url_attributes(ElementAttribute urlAttr) {
            htmlAttributeParser.withAttributes(List.of(urlAttr));

            assertThatThrownBy(() -> validator.validateContent("any content"))
                .isInstanceOf(SubscriptionFormContentUnsafeException.class)
                .hasMessageContaining("Only http and https URLs are allowed")
                .hasMessageContaining("Other protocols");
        }

        static Stream<ElementAttribute> should_reject_non_http_https_protocols_in_url_attributes() {
            return Stream.of(
                new ElementAttribute("a", "href", "file:///etc/passwd"),
                new ElementAttribute("a", "href", "mailto:admin@example.com"),
                new ElementAttribute("img", "src", "data:image/png;base64,abc"),
                new ElementAttribute("a", "href", "ftp://files.example.com"),
                new ElementAttribute("object", "data", "data:application/pdf;base64,xyz")
            );
        }

        @Test
        void should_accept_http_and_https_in_url_attributes() {
            htmlAttributeParser.withAttributes(
                List.of(
                    new ElementAttribute("a", "href", "http://example.com"),
                    new ElementAttribute("a", "href", "https://gravitee.io/docs"),
                    new ElementAttribute("img", "src", "HTTP://cdn.example.com/img.png")
                )
            );

            assertThatCode(() -> validator.validateContent("any content")).doesNotThrowAnyException();
        }
    }

    @Nested
    class UnsafeHtml {

        @Test
        void should_delegate_to_html_sanitizer_for_script_tags() {
            htmlAttributeParser.withAttributes(List.of());

            when(htmlSanitizer.isSafe(anyString())).thenReturn(
                SanitizeResult.builder().safe(false).rejectedMessage("Tag not allowed: script").build()
            );

            assertThatThrownBy(() -> validator.validateContent("<script>alert('xss')</script>"))
                .isInstanceOf(SubscriptionFormContentUnsafeException.class)
                .hasMessageContaining("unsafe HTML/Markdown");

            verify(htmlSanitizer).isSafe(anyString());
        }

        @Test
        void should_strip_gmd_tags_before_html_sanitizer_check() {
            htmlAttributeParser.withAttributes(List.of());

            String content = """
                <gmd-card>
                  <gmd-input name="test"/>
                  <p>Safe content</p>
                </gmd-card>
                """;

            assertThatCode(() -> validator.validateContent(content)).doesNotThrowAnyException();

            // Verify HtmlSanitizer was called (with GMD tags stripped)
            verify(htmlSanitizer).isSafe(anyString());
        }
    }

    @Nested
    class StripGmdTags {

        private String captureStrippedContent(String gmdContent) {
            htmlAttributeParser.withAttributes(List.of());
            validator.validateContent(gmdContent);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(htmlSanitizer).isSafe(captor.capture());
            return captor.getValue();
        }

        @ParameterizedTest
        @MethodSource("should_strip_gmd_tags_source")
        void should_strip_gmd_tags(String gmdContent, String expectedStripped) {
            String stripped = captureStrippedContent(gmdContent);
            assertThat(stripped).isEqualTo(expectedStripped);
        }

        static Stream<Arguments> should_strip_gmd_tags_source() {
            return Stream.of(
                Arguments.of("<gmd-input name=\"email\" required=\"true\"/>", ""),
                Arguments.of("<gmd-input name=\"email\" />", ""),
                Arguments.of("<gmd-card class=\"wide\">content</gmd-card>", "content")
            );
        }

        @Test
        void should_strip_closing_tags_with_whitespace() {
            String stripped = captureStrippedContent("<gmd-card>content</gmd-card  >");

            assertThat(stripped).isEqualTo("content");
        }

        @Test
        void should_strip_tags_with_multiple_attributes() {
            String stripped = captureStrippedContent(
                "<gmd-input name=\"company\" label=\"Company name\" required=\"true\" placeholder=\"Acme Inc.\"/>"
            );

            assertThat(stripped).isEmpty();
        }

        @Test
        void should_preserve_non_gmd_html_and_text() {
            String stripped = captureStrippedContent("<gmd-card><div><p>Hello</p></div></gmd-card>");

            assertThat(stripped).isEqualTo("<div><p>Hello</p></div>");
        }

        @Test
        void should_strip_all_allowed_gmd_elements() {
            String content =
                "<gmd-grid>" +
                "<gmd-card>" +
                "<gmd-card-title>Title</gmd-card-title>" +
                "<gmd-md>## Heading</gmd-md>" +
                "<gmd-input name=\"x\"/>" +
                "<gmd-textarea name=\"y\"/>" +
                "<gmd-select name=\"z\"/>" +
                "<gmd-checkbox name=\"a\"/>" +
                "<gmd-radio name=\"b\"/>" +
                "</gmd-card>" +
                "</gmd-grid>";

            String stripped = captureStrippedContent(content);

            assertThat(stripped)
                .doesNotContain("gmd-grid")
                .doesNotContain("gmd-card-title")
                .doesNotContain("gmd-card")
                .doesNotContain("gmd-md")
                .doesNotContain("gmd-input")
                .doesNotContain("gmd-textarea")
                .doesNotContain("gmd-select")
                .doesNotContain("gmd-checkbox")
                .doesNotContain("gmd-radio")
                .isEqualTo("Title## Heading");
        }

        @Test
        void should_strip_nested_gmd_elements_from_complex_form() {
            String content = """
                <gmd-grid columns="2">
                  <gmd-card>
                    <gmd-card-title>Contact</gmd-card-title>
                    <gmd-input name="email" label="Email" required="true"/>
                    <gmd-textarea name="notes" label="Notes"/>
                  </gmd-card>
                  <gmd-card>
                    <gmd-card-title>Preferences</gmd-card-title>
                    <gmd-select name="plan" options="Free,Pro,Enterprise"/>
                    <gmd-checkbox name="agree" label="I agree"/>
                    <gmd-radio name="contact" options="Email,Phone"/>
                  </gmd-card>
                </gmd-grid>""";

            String stripped = captureStrippedContent(content);

            assertThat(stripped).doesNotContain("gmd-").contains("Contact").contains("Preferences");
        }

        @Test
        void should_not_strip_non_gmd_custom_elements() {
            String stripped = captureStrippedContent("<gmd-card><my-component>text</my-component></gmd-card>");

            assertThat(stripped).isEqualTo("<my-component>text</my-component>");
        }
    }
}
