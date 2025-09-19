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
package fixtures;

import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import java.time.Instant;
import java.util.Date;

public class MembershipModelFixtures {

    private MembershipModelFixtures() {}

    private static final MembershipEntity.MembershipEntityBuilder BASE_MODEL = MembershipEntity.builder()
        .id("membership-id")
        .id("m1")
        .roleId("role1")
        .createdAt(Date.from(Instant.parse("2020-01-01T00:00:00.00Z")))
        .updatedAt(Date.from(Instant.parse("2020-01-01T00:00:00.00Z")));

    public static MembershipEntity aUserMembershipForApi() {
        return BASE_MODEL.memberId("user-id")
            .memberType(MembershipMemberType.USER)
            .referenceType(MembershipReferenceType.API)
            .referenceId("api-id")
            .build();
    }

    public static MembershipEntity aGroupMembershipForApi() {
        return BASE_MODEL.memberType(MembershipMemberType.GROUP)
            .memberId("group-id")
            .referenceType(MembershipReferenceType.API)
            .referenceId("api-id")
            .build();
    }
}
