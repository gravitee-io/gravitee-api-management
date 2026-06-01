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
package io.gravitee.gamma.authorization.infra.service_provider;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.am.sdk.management.model.Group;
import io.gravitee.am.sdk.management.model.Role;
import io.gravitee.am.sdk.management.model.User;
import io.gravitee.gamma.authorization.core.am.model.AmGroup;
import io.gravitee.gamma.authorization.core.am.model.AmRole;
import io.gravitee.gamma.authorization.core.am.model.AmUser;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmSdkDirectoryClientTest {

    @Test
    void maps_user_group_and_role_ids() {
        User user = new User();
        user.setId("u-1");
        user.setGroups(List.of("g-1", "g-2"));
        user.setRoles(List.of("r-1"));

        AmUser mapped = AmSdkDirectoryClient.toAmUser(user);

        assertThat(mapped.groups()).containsExactly("g-1", "g-2");
        assertThat(mapped.roles()).containsExactly("r-1");
    }

    @Test
    void defaults_missing_groups_and_roles_to_empty_lists() {
        User user = new User();
        user.setId("u-1");

        AmUser mapped = AmSdkDirectoryClient.toAmUser(user);

        assertThat(mapped.groups()).isEmpty();
        assertThat(mapped.roles()).isEmpty();
    }

    @Test
    void maps_group_id_and_name() {
        Group group = new Group();
        group.setId("g-1");
        group.setName("Engineering");

        AmGroup mapped = AmSdkDirectoryClient.toAmGroup(group);

        assertThat(mapped.id()).isEqualTo("g-1");
        assertThat(mapped.name()).isEqualTo("Engineering");
    }

    @Test
    void maps_role_id_and_name() {
        Role role = new Role();
        role.setId("r-1");
        role.setName("ADMIN");

        AmRole mapped = AmSdkDirectoryClient.toAmRole(role);

        assertThat(mapped.id()).isEqualTo("r-1");
        assertThat(mapped.name()).isEqualTo("ADMIN");
    }
}
