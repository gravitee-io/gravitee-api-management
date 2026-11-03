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
package io.gravitee.rest.api.model.permissions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EnvironmentPermissionTest {

    @Test
    void every_permission_has_its_own_mask() {
        var permissionsByMask = Arrays.stream(EnvironmentPermission.values()).collect(
            Collectors.groupingBy(EnvironmentPermission::getMask)
        );

        assertThat(permissionsByMask.values()).allSatisfy(permissions -> assertThat(permissions).hasSize(1));
    }

    @Test
    void masks_are_persisted_in_roles_so_they_never_change() {
        assertThat(EnvironmentPermission.CREDENTIAL.getMask()).isEqualTo(5800);
        assertThat(EnvironmentPermission.HITL_RULES.getMask()).isEqualTo(5900);
        assertThat(EnvironmentPermission.HITL_DECISIONS.getMask()).isEqualTo(6000);
        assertThat(EnvironmentPermission.HITL_NOTIFICATIONS.getMask()).isEqualTo(6100);
    }
}
