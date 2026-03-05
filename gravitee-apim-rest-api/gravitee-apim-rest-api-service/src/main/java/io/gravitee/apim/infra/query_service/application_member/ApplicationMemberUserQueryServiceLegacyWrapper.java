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
package io.gravitee.apim.infra.query_service.application_member;

import io.gravitee.apim.core.application_member.model.ApplicationMemberSearchUser;
import io.gravitee.apim.core.application_member.query_service.ApplicationMemberUserQueryService;
import io.gravitee.rest.api.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationMemberUserQueryServiceLegacyWrapper implements ApplicationMemberUserQueryService {

    private final IdentityService identityService;

    @Override
    public java.util.List<ApplicationMemberSearchUser> search(String query) {
        return identityService
            .search(query)
            .stream()
            .map(user ->
                new ApplicationMemberSearchUser(
                    user.getId(),
                    user.getReference(),
                    user.getFirstname(),
                    user.getLastname(),
                    user.getDisplayName(),
                    user.getEmail()
                )
            )
            .toList();
    }
}
