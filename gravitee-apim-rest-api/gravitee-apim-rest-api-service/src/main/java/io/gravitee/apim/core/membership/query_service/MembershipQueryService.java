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
package io.gravitee.apim.core.membership.query_service;

import io.gravitee.apim.core.membership.model.Membership;
import java.util.Collection;
import java.util.List;

public interface MembershipQueryService {
    Collection<Membership> findByReferenceAndRoleId(Membership.ReferenceType referenceType, String referenceId, String roleId);
    Collection<Membership> findByReferencesAndRoleId(Membership.ReferenceType referenceType, List<String> referenceIds, String roleId);
    Collection<Membership> findByReference(Membership.ReferenceType referenceType, String referenceId);

    Collection<Membership> findGroupsThatUserBelongsTo(String userId);
    List<String> findClustersIdsThatUserBelongsTo(String userId);
}
