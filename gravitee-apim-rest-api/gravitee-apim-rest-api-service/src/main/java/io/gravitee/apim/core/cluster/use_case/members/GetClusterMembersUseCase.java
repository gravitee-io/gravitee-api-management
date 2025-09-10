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
import io.gravitee.apim.core.member.domain_service.MemberDomainService;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@UseCase
public class GetClusterMembersUseCase {

    private final MemberDomainService memberDomainService;

    public record Input(String clusterId) {}

    public record Output(List<MemberEntity> members) {}

    public Output execute(Input input) {
        var members = memberDomainService
            .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.CLUSTER, input.clusterId)
            .stream()
            .filter(memberEntity -> memberEntity.getType() == MembershipMemberType.USER)
            .sorted(Comparator.comparing(MemberEntity::getId))
            .toList();
        return new Output(members);
    }
}
