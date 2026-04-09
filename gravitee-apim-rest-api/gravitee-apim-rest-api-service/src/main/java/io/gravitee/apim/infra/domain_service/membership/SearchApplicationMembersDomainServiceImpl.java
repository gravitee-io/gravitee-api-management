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
package io.gravitee.apim.infra.domain_service.membership;

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.membership.domain_service.SearchApplicationMembersDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchApplicationMembersDomainServiceImpl implements SearchApplicationMembersDomainService {

    private final MembershipQueryService membershipQueryService;
    private final ApplicationCrudService applicationCrudService;

    @Override
    public Collection<Membership> searchApplicationMembers(String environmentId, String applicationId) {
        applicationCrudService.findById(applicationId, environmentId);
        try {
            return membershipQueryService.findByReference(Membership.ReferenceType.APPLICATION, applicationId);
        } catch (TechnicalDomainException e) {
            throw new TechnicalDomainException(
                String.format("An error occurs while trying to search members for application %s", applicationId),
                e
            );
        }
    }
}
