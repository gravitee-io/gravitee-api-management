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

import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.domain_service.CreateUserDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateUserDomainServiceImpl implements CreateUserDomainService {

    private final UserCrudService userCrudService;

    @Override
    public BaseUserEntity createExternalUser(
        ExecutionContext executionContext,
        String email,
        Optional<String> firstname,
        Optional<String> lastname
    ) {
        var now = Date.from(TimeProvider.now().toInstant());
        var user = BaseUserEntity.builder()
            .id(UuidString.generateRandom())
            .organizationId(executionContext.getOrganizationId())
            .source("gravitee")
            .sourceId(email)
            .email(email)
            .firstname(firstname.orElse(null))
            .lastname(lastname.orElse(null))
            .status("ACTIVE")
            .createdAt(now)
            .updatedAt(now)
            .build();
        return userCrudService.create(user);
    }
}
