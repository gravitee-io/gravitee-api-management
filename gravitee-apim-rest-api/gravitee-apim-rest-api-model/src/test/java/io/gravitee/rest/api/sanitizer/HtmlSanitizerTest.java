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
package io.gravitee.rest.api.sanitizer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    @Test
    void should_sanitize() {
        String sanitized = HtmlSanitizer.sanitize("A<img src=\"../../../image.png\"> Text");
        assertEquals("A Text", sanitized);

        sanitized = HtmlSanitizer.sanitize("A<script>alert()</script> Text");
        assertEquals("A Text", sanitized);

        sanitized = HtmlSanitizer.sanitize("<h1>A</h1> Text");
        assertEquals("A Text", sanitized);

        sanitized = HtmlSanitizer.sanitize("A <a href=\"https://www.gravitee.io\">Test</a> Text");
        assertEquals("A Test Text", sanitized);

        sanitized = HtmlSanitizer.sanitize("Allowed chars: (&lt; &gt; &amp; &quot; &apos;) (< > & \" ' + = `)");
        assertEquals("Allowed chars: (< > & \" ') (< > & \" ' + = `)", sanitized);

        sanitized = HtmlSanitizer.sanitize("&lt;a data-bind=&#39;style: alert(1)&#39;&gt;&lt;/a&gt;"); // <a data-bind='style: alert(1)'></a>
        assertEquals("", sanitized);
    }

    @Test
    void should_handle_null() {
        assertNull(HtmlSanitizer.sanitize(null));
    }
}
