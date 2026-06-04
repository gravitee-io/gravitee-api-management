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

import io.gravitee.am.sdk.management.model.FilteredApplication;
import io.gravitee.am.sdk.management.model.Group;
import io.gravitee.am.sdk.management.model.Role;
import io.gravitee.am.sdk.management.model.User;
import io.gravitee.gamma.authorization.core.am.model.AmAgent;
import io.gravitee.gamma.authorization.core.am.model.AmGroup;
import io.gravitee.gamma.authorization.core.am.model.AmRole;
import io.gravitee.gamma.authorization.core.am.model.AmUser;
import java.util.List;
import java.util.Map;
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

        AmUser mapped = AmSdkDirectoryClient.toAmUser(user, Map.of());

        assertThat(mapped.groups()).containsExactly("g-1", "g-2");
        assertThat(mapped.roles()).containsExactly("r-1");
    }

    @Test
    void defaults_missing_groups_and_roles_to_empty_lists() {
        User user = new User();
        user.setId("u-1");

        AmUser mapped = AmSdkDirectoryClient.toAmUser(user, Map.of());

        assertThat(mapped.groups()).isEmpty();
        assertThat(mapped.roles()).isEmpty();
    }

    @Test
    void resolves_source_name_back_to_the_idp_id() {
        // listUsers returns the IdP *name* in `source`; the sync must key the entity on the IdP *id*
        // so it matches AM's token `sub` (UUID of "idpId:externalId").
        User user = new User();
        user.setId("u-1");
        user.setSource("Default Identity Provider");

        AmUser mapped = AmSdkDirectoryClient.toAmUser(user, Map.of("Default Identity Provider", "default-idp-3e149b8b"));

        assertThat(mapped.source()).isEqualTo("default-idp-3e149b8b");
    }

    @Test
    void keeps_source_as_is_when_idp_name_is_not_in_the_map() {
        User user = new User();
        user.setId("u-1");
        user.setSource("default-idp-3e149b8b");

        AmUser mapped = AmSdkDirectoryClient.toAmUser(user, Map.of());

        assertThat(mapped.source()).isEqualTo("default-idp-3e149b8b");
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

    @Test
    void maps_agent_id_client_id_name_and_type() {
        FilteredApplication application = new FilteredApplication();
        application.setId("app-1");
        application.setClientId("agent-client");
        application.setName("Booking bot");
        application.setKind(FilteredApplication.KindEnum.AUTONOMOUS);

        AmAgent mapped = AmSdkDirectoryClient.toAmAgent(application);

        assertThat(mapped.id()).isEqualTo("app-1");
        assertThat(mapped.clientId()).isEqualTo("agent-client");
        assertThat(mapped.name()).isEqualTo("Booking bot");
        assertThat(mapped.agentType()).isEqualTo(FilteredApplication.KindEnum.AUTONOMOUS.getValue());
    }

    @Test
    void maps_agent_type_to_null_when_kind_is_absent() {
        FilteredApplication application = new FilteredApplication();
        application.setId("app-1");
        application.setClientId("agent-client");

        AmAgent mapped = AmSdkDirectoryClient.toAmAgent(application);

        assertThat(mapped.agentType()).isNull();
    }
}
