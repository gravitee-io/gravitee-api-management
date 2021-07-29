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
package io.gravitee.repository.bridge.server.handler;

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

    private static class TestHandler extends AbstractHandler {}
}
