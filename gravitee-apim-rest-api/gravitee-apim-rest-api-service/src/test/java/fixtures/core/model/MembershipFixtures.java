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
package fixtures.core.model;

import static fixtures.core.model.RoleFixtures.apiPrimaryOwnerRoleId;
import static fixtures.core.model.RoleFixtures.applicationPrimaryOwnerRoleId;

import io.gravitee.apim.core.membership.model.Membership;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;

public class MembershipFixtures {

    private static final Supplier<Membership.MembershipBuilder> BASE = () ->
        Membership.builder()
            .id("membership-id")
            .source("system")
            .memberId("user-id")
            .memberType(Membership.Type.USER)
            .roleId("role-id")
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()));

    public static Membership anApiMembership() {
        return BASE.get().referenceType(Membership.ReferenceType.API).referenceId("api-id").build();
    }

    public static Membership anApiMembership(String apiId) {
        return BASE.get().referenceType(Membership.ReferenceType.API).referenceId(apiId).build();
    }

    public static Membership anApiPrimaryOwnerUserMembership(String apiId, String userId, String organizationId) {
        return BASE.get()
            .referenceType(Membership.ReferenceType.API)
            .referenceId(apiId)
            .memberId(userId)
            .roleId(apiPrimaryOwnerRoleId(organizationId))
            .build();
    }

    public static Membership anApplicationPrimaryOwnerUserMembership(String applicationId, String userId, String organizationId) {
        return BASE.get()
            .referenceType(Membership.ReferenceType.APPLICATION)
            .referenceId(applicationId)
            .memberId(userId)
            .roleId(applicationPrimaryOwnerRoleId(organizationId))
            .build();
    }
}
