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
package io.gravitee.apim.infra.domain_service.user;

import io.gravitee.apim.core.user.domain_service.UserDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.adapter.UserAdapter;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserDomainServiceLegacyWrapper implements UserDomainService {

    private final UserService userService;

    private final IdentityService identityService;

    @Override
    public Optional<BaseUserEntity> findBySource(String organizationId, String source, String sourceId) {
        try {
            return Optional.ofNullable(UserAdapter.INSTANCE.fromUser(userService.findBySource(organizationId, source, sourceId, false)));
        } catch (UserNotFoundException e) {
            return Optional.empty();
        }
    }
}
