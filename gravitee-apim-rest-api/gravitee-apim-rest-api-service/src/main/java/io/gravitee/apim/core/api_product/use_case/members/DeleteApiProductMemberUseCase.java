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
package io.gravitee.apim.core.api_product.use_case.members;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.member.model.MembershipMemberType;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.query_service.MemberQueryService;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class DeleteApiProductMemberUseCase {

    private final MemberQueryService memberQueryService;

    public record Input(String apiProductId, String memberId) {}

    public record Output() {}

    public Output execute(Input input) {
        memberQueryService.deleteReferenceMember(
            MembershipReferenceType.API_PRODUCT,
            input.apiProductId,
            MembershipMemberType.USER,
            input.memberId
        );
        return new Output();
    }
}
