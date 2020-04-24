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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HtmlSanitizerTest {

    @Test
    public void sanitize() {

        String html = getSafe();
        assertEquals(html, HtmlSanitizer.sanitize(html));
    }


    @Test
    public void sanitizeExcludeSensitive() {

        String html = getNotSafe();
        assertEquals("", HtmlSanitizer.sanitize(html));
    }

    @Test
    public void isSafe() {

        HtmlSanitizer.SanitizeInfos sanitizeInfos = HtmlSanitizer.isSafe(getSafe());

        assertTrue(sanitizeInfos.isSafe());
        assertNull(sanitizeInfos.getRejectedMessage());
    }


    @Test
    public void isNotSafe() {

        HtmlSanitizer.SanitizeInfos sanitizeInfos = HtmlSanitizer.isSafe(getNotSafe());

        assertFalse(sanitizeInfos.isSafe());
        assertEquals("The content [<script src=\"/external.jpg\"><div onClick=\"alert('test');\">onclick alert<div>] is not allowed (~line 1)", sanitizeInfos.getRejectedMessage());
    }

    private String getSafe() {

        String html = "";
        html += "<div>Test div</div>";
        html += "<a href=\"/internal\">Test internal link</a>";
        html += "<a href=\"https://external.com\">Test external link</a>";
        html += "<table><tbody><tr><th>Data1</th><th>Data2</th></tr><tr><td>Calcutta</td><td>Orange</td></tr><tr><td>Robots</td><td>Jazz</td></tr></tbody></table>";
        html += "<ul><li>Test ul/li</li></ul>";
        html += "<img src=\"/internal.jpg\" title=\"test\" />";
        html += "<img src=\"/external.jpg\" title=\"test\" />";

        return html;
    }

    private String getNotSafe() {

        String html = "";
        html += "<script src=\"/external.jpg\" />";
        html += "<div onClick=\"alert('test');\">onclick alert<div>";

        return html;
    }
}