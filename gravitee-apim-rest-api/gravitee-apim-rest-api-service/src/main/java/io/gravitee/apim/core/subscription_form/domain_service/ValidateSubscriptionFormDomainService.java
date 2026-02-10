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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.core.sanitizer.SanitizeResult;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormContentUnsafeException;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * Domain service for validating GMD (Gravitee Markdown) content in subscription forms.
 *
 * <p>GMD is a mix of Markdown + custom web components (gmd-*) + HTML.
 * This validator:
 * <ul>
 *   <li>Whitelists GMD custom elements (gmd-input, gmd-card, etc.)</li>
 *   <li>Uses {@link HtmlAttributeParser} to parse and validate all element attributes (no regex bypasses)</li>
 *   <li>Validates markdown/HTML content using HtmlSanitizer</li>
 *   <li>Blocks dangerous attributes (onclick, onerror, javascript:, etc.)</li>
 * </ul>
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@DomainService
public class ValidateSubscriptionFormDomainService {

    // Whitelist of allowed GMD custom elements
    private static final Set<String> ALLOWED_GMD_ELEMENTS = Set.of(
        "gmd-grid",
        "gmd-card",
        "gmd-card-title",
        "gmd-md",
        "gmd-input",
        "gmd-textarea",
        "gmd-select",
        "gmd-checkbox",
        "gmd-radio"
    );

    // Blacklist of dangerous HTML event handler attributes
    private static final Set<String> DANGEROUS_ATTRIBUTES = Set.of(
        "onclick",
        "ondblclick",
        "onmousedown",
        "onmouseup",
        "onmouseover",
        "onmousemove",
        "onmouseout",
        "onmouseenter",
        "onmouseleave",
        "onkeydown",
        "onkeypress",
        "onkeyup",
        "onload",
        "onerror",
        "onabort",
        "onblur",
        "onchange",
        "onfocus",
        "onreset",
        "onselect",
        "onsubmit",
        "onunload",
        "oncontextmenu",
        "ondrag",
        "ondrop"
    );

    // URL-like attributes where we restrict allowed protocols to http/https
    private static final Set<String> URL_ATTRIBUTES = Set.of("href", "src", "action", "data", "formaction");

    // Pattern to detect JavaScript: protocol (case-insensitive, allows whitespace)
    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN = Pattern.compile("^\\s*javascript\\s*:", Pattern.CASE_INSENSITIVE);

    // Allowed URL protocols (case-insensitive)
    private static final Pattern ALLOWED_URL_PROTOCOL_PATTERN = Pattern.compile("^\\s*(https?):", Pattern.CASE_INSENSITIVE);

    private final HtmlSanitizer htmlSanitizer;
    private final HtmlAttributeParser htmlAttributeParser;

    /**
     * Validates that the GMD content is safe (no malicious scripts, XSS attacks, etc.).
     *
     * @param gmdContent the GMD content to validate
     * @throws ValidationDomainException if the content is empty
     * @throws SubscriptionFormContentUnsafeException if the content contains malicious code
     */
    public void validateContent(String gmdContent) {
        // 1. Check not empty
        if (gmdContent == null || gmdContent.trim().isEmpty()) {
            throw new ValidationDomainException(
                "Subscription form content cannot be empty. Please disable the form if you don’t want to display it."
            );
        }

        // 2. Parse and validate all attributes (event handlers, javascript:)
        validateAttributes(gmdContent);

        // 3. Strip GMD tags and validate markdown/HTML using HtmlSanitizer
        String contentWithoutGmdTags = stripGmdTags(gmdContent);
        validateMarkdownAndHtml(contentWithoutGmdTags);
    }

    /**
     * Uses {@link HtmlAttributeParser} to parse content and validate all element attributes.
     * The parser handles HTML encoding, whitespace in values, nested quotes, etc. -
     * no regex bypasses possible.
     */
    private void validateAttributes(String gmdContent) {
        for (HtmlAttributeParser.ElementAttribute attr : htmlAttributeParser.parseAllAttributes(gmdContent)) {
            // Check for dangerous event handler attributes
            if (DANGEROUS_ATTRIBUTES.contains(attr.attributeName())) {
                throw new SubscriptionFormContentUnsafeException(
                    String.format(
                        "Dangerous attribute '%s' not allowed on <%s> tag. Event handlers pose XSS security risk.",
                        attr.attributeName(),
                        attr.tagName()
                    )
                );
            }

            // Check URL attributes: block javascript:, allow only http/https (or empty / relative URLs)
            if (URL_ATTRIBUTES.contains(attr.attributeName())) {
                String value = attr.attributeValue();
                if (containsJavaScriptProtocol(value)) {
                    throw new SubscriptionFormContentUnsafeException(
                        String.format(
                            "JavaScript protocol not allowed in '%s' attribute on <%s> tag. Use http/https URLs only.",
                            attr.attributeName(),
                            attr.tagName()
                        )
                    );
                }
                if (!isAllowedUrlValue(value)) {
                    throw new SubscriptionFormContentUnsafeException(
                        String.format(
                            "Only http and https URLs are allowed in '%s' attribute on <%s> tag. Other protocols (e.g. file:, data:, mailto:) are not permitted.",
                            attr.attributeName(),
                            attr.tagName()
                        )
                    );
                }
            }
        }
    }

    /**
     * Returns true if the value is allowed for a URL attribute: empty, relative (no scheme), or http/https.
     */
    private boolean isAllowedUrlValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        String trimmed = value.trim();
        // Has a protocol (contains ':')
        if (trimmed.contains(":")) {
            return ALLOWED_URL_PROTOCOL_PATTERN.matcher(trimmed).lookingAt();
        }
        // No scheme: relative URL (e.g. /path, page.html) - allow
        return true;
    }

    /**
     * Strips GMD custom elements from content, leaving Markdown and HTML.
     * This allows us to validate the markdown/HTML portions using HtmlSanitizer.
     */
    private String stripGmdTags(String content) {
        String result = content;

        for (String gmdElement : ALLOWED_GMD_ELEMENTS) {
            // Remove self-closing tags: <gmd-input ... />
            result = result.replaceAll("<" + gmdElement + "[^>]*/\\s*>", "");

            // Remove opening tags: <gmd-input ...>
            result = result.replaceAll("<" + gmdElement + "[^>]*>", "");

            // Remove closing tags: </gmd-input>
            result = result.replaceAll("</" + gmdElement + "\\s*>", "");
        }

        return result;
    }

    /**
     * Validates Markdown and HTML content using HtmlSanitizer.
     * This catches malicious HTML tags like a script, iframe, etc.
     */
    private void validateMarkdownAndHtml(String contentWithoutGmdTags) {
        final SanitizeResult sanitizeResult = htmlSanitizer.isSafe(contentWithoutGmdTags);
        if (!sanitizeResult.isSafe()) {
            throw new SubscriptionFormContentUnsafeException(
                "GMD content contains unsafe HTML/Markdown: " + sanitizeResult.getRejectedMessage()
            );
        }
    }

    /**
     * Checks if value contains javascript: protocol.
     */
    private boolean containsJavaScriptProtocol(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return JAVASCRIPT_PROTOCOL_PATTERN.matcher(value).find();
    }
}
