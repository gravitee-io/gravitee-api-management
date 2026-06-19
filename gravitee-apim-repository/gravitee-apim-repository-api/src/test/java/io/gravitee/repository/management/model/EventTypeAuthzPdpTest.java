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
package io.gravitee.repository.management.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class EventTypeAuthzPdpTest {

    @Test
    void should_expose_publish_authz_pdp_event_type() {
        assertThatCode(() -> EventType.valueOf("PUBLISH_AUTHZ_PDP")).doesNotThrowAnyException();
        assertThat(EventType.PUBLISH_AUTHZ_PDP.name()).isEqualTo("PUBLISH_AUTHZ_PDP");
    }

    @Test
    void should_expose_unpublish_authz_pdp_event_type() {
        assertThatCode(() -> EventType.valueOf("UNPUBLISH_AUTHZ_PDP")).doesNotThrowAnyException();
        assertThat(EventType.UNPUBLISH_AUTHZ_PDP.name()).isEqualTo("UNPUBLISH_AUTHZ_PDP");
    }
}
