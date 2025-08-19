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
package io.gravitee.apim.core.cluster.use_case.members;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class DeleteClusterMemberUseCase {

    private final MembershipCrudService membershipCrudService;
    private final MembershipQueryService membershipQueryService;

    public record Input(String clusterId, String memberId) {}

    public record Output() {}

    public Output execute(Input input) {
        Membership membershipToDelete = membershipQueryService
            .findByReference(Membership.ReferenceType.CLUSTER, input.clusterId)
            .stream()
            .filter(membership -> membership.getMemberType().equals(Membership.Type.USER))
            .filter(membership -> membership.getMemberId().equals(input.memberId))
            .findFirst()
            .orElseThrow();
        membershipCrudService.delete(membershipToDelete.getId());

        return new Output();
    }
}
