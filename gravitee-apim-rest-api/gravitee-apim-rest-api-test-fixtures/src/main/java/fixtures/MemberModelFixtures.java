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

import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.List;

public class MemberModelFixtures {

    private MemberModelFixtures() {}

    private static final MemberEntity.MemberEntityBuilder BASE = MemberEntity
        .builder()
        .id("member-id")
        .displayName("John Doe")
        .roles(List.of(RoleEntity.builder().name("OWNER").scope(RoleScope.GROUP).build()))
        .type(MembershipMemberType.USER)
        .email("john@doe.com");

    public static MemberEntity aGroupMember(String groupId) {
        return BASE.referenceType(MembershipReferenceType.GROUP).referenceId(groupId).build();
    }
}
