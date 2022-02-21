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
package io.gravitee.gateway.debug.reactor.handler.context;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathTransformerTest {

    private static final String EVENT_ID = "18199-511ff-15fdv-156fdv";

    @Test
    public void shouldAddEventIdToPath() {
        final String result = PathTransformer.computePathWithEventId(EVENT_ID, "/api/");
        assertThat(result).isEqualTo("/" + EVENT_ID + "-api/");
    }

    @Test
    public void shouldAddEventIdToBiggerPath() {
        final String result = PathTransformer.computePathWithEventId(EVENT_ID, "/api/chicken/");
        assertThat(result).isEqualTo("/" + EVENT_ID + "-api/chicken/");
    }

    @Test
    public void shouldRemoveEventIdFromPath() {
        final String result = PathTransformer.removeEventIdFromPath(EVENT_ID, "/" + EVENT_ID + "-api/chicken/");
        assertThat(result).isEqualTo("/api/chicken/");
    }

    @Test
    public void shouldNotRemoveEventIdFromPathIfAbsent() {
        final String result = PathTransformer.removeEventIdFromPath(EVENT_ID, "/anotherEventId-api/chicken/");
        assertThat(result).isEqualTo("/anotherEventId-api/chicken/");
    }
}
