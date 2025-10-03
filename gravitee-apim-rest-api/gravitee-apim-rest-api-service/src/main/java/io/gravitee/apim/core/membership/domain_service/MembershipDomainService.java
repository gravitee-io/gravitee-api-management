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
package io.gravitee.apim.core.membership.domain_service;

import io.gravitee.apim.core.membership.model.TransferOwnership;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Map;

public interface MembershipDomainService {
    Map<String, char[]> getUserMemberPermissions(
        ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        String userId
    );

    void transferOwnership(TransferOwnership transferOwnership, RoleScope roleScope, String itemId);

    MemberEntity createNewMembership(
        ExecutionContext executionContext,
        io.gravitee.apim.core.member.model.MembershipReferenceType referenceType,
        String referenceId,
        String userId,
        String externalReference,
        String roleName
    );
}
