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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.repository.management.model.Guardian;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GuardianAuditEventAlignmentTest {

    @Test
    void should_list_every_guardian_audit_event_as_a_filter() {
        assertThat(Arrays.stream(Guardian.AuditEvent.values()).map(Enum::name))
            .as("Guardian audit events listed as filters by /audit/events")
            .containsExactlyInAnyOrderElementsOf(Arrays.stream(GuardianAuditEvent.values()).map(Enum::name).toList());
    }
}
