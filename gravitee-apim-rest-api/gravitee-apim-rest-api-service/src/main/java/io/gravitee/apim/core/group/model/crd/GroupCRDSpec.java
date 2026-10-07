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
package io.gravitee.apim.core.group.model.crd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.member.model.RoleScope;
import io.gravitee.definition.model.Origin;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class GroupCRDSpec {

    private String id;

    private String hrid;

    private String name;

    private SequencedSet<Member> members;

    private boolean notifyMembers;

    private String apiRole;

    private String applicationRole;

    private String apiProductRole;

    /**
     * Default role per scope, from the Automation API. {@code null} means not declared: the platform's defaults are
     * left untouched. When declared, it is authoritative for API, APPLICATION and API_PRODUCT; a missing scope is cleared.
     * The flat {@code apiRole}, {@code applicationRole} and {@code apiProductRole} remain for the Management API import.
     */
    private Map<RoleScope, String> defaultMemberRoles;

    /**
     * Set by the Automation API from the {@code ignoreMembers} query parameter, never read from the payload.
     * When true, {@code members} is not applied: the group's memberships are left as the platform holds them.
     */
    @JsonIgnore
    private boolean ignoreMembers;

    @Builder.Default
    private String origin = Origin.KUBERNETES.name();

    public Group toGroup(String environmentId) {
        return Group.builder()
            .origin(origin)
            .id(id)
            .hrid(hrid)
            .name(name)
            .environmentId(environmentId)
            .apiPrimaryOwner(null)
            .apiProductPrimaryOwner(null)
            .eventRules(List.of())
            .lockApiRole(false)
            .lockApplicationRole(false)
            .maxInvitation(Integer.MAX_VALUE)
            .systemInvitation(false)
            .emailInvitation(false)
            .disableMembershipNotifications(!notifyMembers)
            .build();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder(toBuilder = true)
    public static class Member {

        private String id;

        private String source;

        private String sourceId;

        private Map<RoleScope, String> roles;
    }
}
