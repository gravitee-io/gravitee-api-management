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
package io.gravitee.apim.infra.domain_service.member;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.member.domain_service.MemberDomainService;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MemberDomainServiceLegacyWrapper implements MemberDomainService {

    private final MembershipService legacyService;

    public Set<MemberEntity> getMembersByReference(
        ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId
    ) {
        return legacyService.getMembersByReference(executionContext, referenceType, referenceId);
    }
}
