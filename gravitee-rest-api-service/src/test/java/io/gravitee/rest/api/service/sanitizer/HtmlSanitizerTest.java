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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HtmlSanitizerTest {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    private static String ONLY_OPENED_IMG_TAG = "<img src=\"myPic.png\">";
    private static String SELF_CLOSING_IMG_TAG = "<img src=\"myPic.png\"/>";
    private static String CLOSED_IMG_TAG = "<img src=\"myPic.png\"></img>";
    private static String BASE64_IMG_TAG = "<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==\"/>";

    private static String DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD = "<div style=\"margin:auto\"></div>";
    private static String DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD_WITH_SPACES = "<div style=\" margin : auto \"></div>";
    private static String DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD_WITH_SEMICOLON = "<div style=\"margin:auto;\"></div>";
    private static String DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD_WITH_SPACES_AND_SEMICOLON = "<div style=\" margin : auto ; \"></div>";

    private static String DIV_TAG_WITH_STYLE_ATT_WITH_FLOAT_FIELD = "<div style=\"float:left\"></div>";

    private static String DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD = "<div style=\"margin:auto;width:100px\"></div>";
    private static String DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD_WITH_SPACES = "<div style=\" margin : auto ; width :   100px    \"></div>";
    private static String DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD_WITH_SEMICOLON = "<div style=\"margin:auto;width:100px;\"></div>";
    private static String DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD_WITH_SPACES_AND_SEMICOLON = "<div style=\" margin : auto ; width :   100px   ;  \"></div>";

    private static String DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_QUOTE = "<div style='margin:auto'></div>";
    private static String DIV_TAG_WITH_STYLE_ATT_WITH_TWO_SEMICOLON = "<div style=\"margin:auto;;\"></div>";


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
        assertEquals("[]", sanitizeInfos.getRejectedMessage());
    }


    @Test
    public void isNotSafe() {

        HtmlSanitizer.SanitizeInfos sanitizeInfos = HtmlSanitizer.isSafe(getNotSafe());

        assertFalse(sanitizeInfos.isSafe());
        assertEquals("[Tag not allowed: script]", sanitizeInfos.getRejectedMessage());
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
        html += "<div onClick=\"alert('test');\" style=\"margin: auto\">onclick alert<div>";

        return html;
    }


    @Test
    public void shouldBeSafe() {
        collector.checkThat("ONLY_OPENED_IMG_TAG", HtmlSanitizer.isSafe(ONLY_OPENED_IMG_TAG).isSafe(), is(true));
        collector.checkThat("SELF_CLOSING_IMG_TAG", HtmlSanitizer.isSafe(SELF_CLOSING_IMG_TAG).isSafe(), is(true));
        collector.checkThat("CLOSED_IMG_TAG", HtmlSanitizer.isSafe(CLOSED_IMG_TAG).isSafe(), is(true));
        collector.checkThat("BASE64_IMG_TAG", HtmlSanitizer.isSafe(BASE64_IMG_TAG).isSafe(), is(true));
        collector.checkThat("DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD", HtmlSanitizer.isSafe(DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD).isSafe(), is(true));
        collector.checkThat("DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD_WITH_SPACES", HtmlSanitizer.isSafe(DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD_WITH_SPACES).isSafe(), is(true));
        collector.checkThat("DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD_WITH_SEMICOLON", HtmlSanitizer.isSafe(DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD_WITH_SEMICOLON).isSafe(), is(true));
        collector.checkThat("DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD_WITH_SPACES_AND_SEMICOLON", HtmlSanitizer.isSafe(DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_FIELD_WITH_SPACES_AND_SEMICOLON).isSafe(), is(true));
        collector.checkThat("DIV_TAG_WITH_STYLE_ATT_WITH_FLOAT_FIELD", HtmlSanitizer.isSafe(DIV_TAG_WITH_STYLE_ATT_WITH_FLOAT_FIELD).isSafe(), is(true));
        collector.checkThat("DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD", HtmlSanitizer.isSafe(DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD).isSafe(), is(true));
        collector.checkThat("DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD_WITH_SPACES", HtmlSanitizer.isSafe(DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD_WITH_SPACES).isSafe(), is(true));
        collector.checkThat("DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD_WITH_SEMICOLON", HtmlSanitizer.isSafe(DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD_WITH_SEMICOLON).isSafe(), is(true));
        collector.checkThat("DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD_WITH_SPACES_AND_SEMICOLON", HtmlSanitizer.isSafe(DIV_TAG_WITH_STYLE_ATT_WITH_TWO_FIELD_WITH_SPACES_AND_SEMICOLON).isSafe(), is(true));
        collector.checkThat("DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_QUOTE", HtmlSanitizer.isSafe(DIV_TAG_WITH_STYLE_ATT_WITH_SINGLE_QUOTE).isSafe(), is(true));
        collector.checkThat("DIV_TAG_WITH_STYLE_ATT_WITH_TWO_SEMICOLON", HtmlSanitizer.isSafe(DIV_TAG_WITH_STYLE_ATT_WITH_TWO_SEMICOLON).isSafe(), is(true));
    }
}