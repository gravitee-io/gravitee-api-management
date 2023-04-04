/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;        http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */
package io.gravitee.repository.bridge.server.handler.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AbstractHandlerTest {

    TestHandler cut = new TestHandler();

    @Test
    public void shouldReadListParamOneItem() {
        final Set<String> items = cut.readListParam("item");

        assertEquals(1, items.size());
        assertTrue(items.contains("item"));
    }

    @Test
    public void shouldReadListParamSeveralItems() {
        final Set<String> items = cut.readListParam("item1,item2,item3");

        assertEquals(3, items.size());
        assertTrue(items.contains("item1"));
        assertTrue(items.contains("item2"));
        assertTrue(items.contains("item3"));
    }

    @Test
    public void shouldReadListParamEmptyString() {
        final Set<String> items = cut.readListParam("");

        assertEquals(0, items.size());
    }

    @Test
    public void shouldReadListParamNullString() {
        final Set<String> items = cut.readListParam(null);

        assertEquals(0, items.size());
    }

    private static class TestHandler extends AbstractHandler {

        protected TestHandler() {
            super(null);
        }
    }
}
