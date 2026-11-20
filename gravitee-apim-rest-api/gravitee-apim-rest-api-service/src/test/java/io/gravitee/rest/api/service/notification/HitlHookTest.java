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
package io.gravitee.rest.api.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HitlHookTest {

    @Test
    void should_expose_exactly_the_five_hitl_hooks_with_their_labels() {
        Map<String, String> labelByName = Map.of(
            "HITL_APPROVAL_REQUIRED",
            "Approval required",
            "HITL_APPROVAL_REMINDER",
            "Approval reminder",
            "HITL_APPROVED",
            "Approved",
            "HITL_REJECTED",
            "Rejected",
            "HITL_EXPIRED",
            "Expired"
        );

        assertThat(HitlHook.values()).extracting(HitlHook::name).containsExactlyInAnyOrderElementsOf(labelByName.keySet());
        for (HitlHook hook : HitlHook.values()) {
            assertThat(hook.getLabel()).isEqualTo(labelByName.get(hook.name()));
            assertThat(hook.getDescription()).isNotBlank();
        }
    }

    @Test
    void should_scope_every_hitl_hook_to_hitl_and_name_its_template() {
        for (HitlHook hook : HitlHook.values()) {
            assertThat(hook.getScope()).isEqualTo(HookScope.HITL);
            assertThat(hook.getTemplate()).isEqualTo("HITL." + hook.name());
            assertThat(hook.isHidden()).isFalse();
        }
    }

    @Test
    void should_not_send_portal_notification_for_hitl_scope() {
        assertThat(HookScope.HITL.hasPortalNotification()).isFalse();
    }
}
