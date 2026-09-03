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
package io.gravitee.gateway.debug.definition;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class DebugApiV4Test {

    private static final String EVENT_ID = "evt-id";

    @Test
    void should_construct_and_compute_new_path() {
        final DebugApiV4 result = new DebugApiV4(EVENT_ID, anApiDefinition());

        assertThat(((HttpListener) result.getDefinition().getListeners().getFirst()).getPaths())
            .hasSize(2)
            .anyMatch(path -> path.getPath().equals("/" + EVENT_ID + "-path1"))
            .anyMatch(path -> path.getPath().equals("/" + EVENT_ID + "-path2"));
    }

    @Test
    void should_not_be_equal_when_debugging_the_same_api_from_another_event() {
        final DebugApiV4 first = new DebugApiV4("evt-1", anApiDefinition());
        final DebugApiV4 second = new DebugApiV4("evt-2", anApiDefinition());

        assertThat(first).isNotEqualTo(second);
        assertThat(first.hashCode()).isNotEqualTo(second.hashCode());
    }

    @Test
    void should_be_equal_when_debugging_the_same_api_from_the_same_event() {
        final DebugApiV4 first = new DebugApiV4(EVENT_ID, anApiDefinition());
        final DebugApiV4 second = new DebugApiV4(EVENT_ID, anApiDefinition());

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

<<<<<<< HEAD
    @Test
    void should_carry_the_event_as_revision_so_two_runs_get_distinct_secret_descriptors() {
        final DebugApiV4 first = new DebugApiV4("evt-1", anApiDefinition());
        final DebugApiV4 second = new DebugApiV4("evt-2", anApiDefinition());

        // The revision is the only per-run discriminator the secret discovery descriptor carries; left
        // null, the first run to finish would revoke the contexts of the one still running.
        assertThat(first.getRevision()).isEqualTo("evt-1");
        assertThat(second.getRevision()).isEqualTo("evt-2");
    }

=======
>>>>>>> 0042aee (fix(gateway): key debug handlers by event id)
    private static io.gravitee.definition.model.debug.DebugApiV4 anApiDefinition() {
        final Api api = Api.builder()
            .id("api-id")
            .name("api")
            .listeners(List.of(HttpListener.builder().paths(List.of(new Path("/path1"), new Path("/path2"))).build()))
            .build();

        return new io.gravitee.definition.model.debug.DebugApiV4(api, new HttpRequest("/path", "GET"));
    }
}
