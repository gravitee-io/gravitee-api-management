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
package io.gravitee.apim.core.user.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.InMemoryAlternative;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidateUserInOrganizationDomainServiceTest {

    private static final String ORGANIZATION_ID = "org-1";
    private static final String OTHER_ORGANIZATION_ID = "org-2";
    private static final String USER_ID = "user-1";

    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    ValidateUserInOrganizationDomainService service;

    @BeforeEach
    void setUp() {
        userCrudService.initWith(List.of(BaseUserEntity.builder().id(USER_ID).organizationId(ORGANIZATION_ID).build()));
        service = new ValidateUserInOrganizationDomainService(userCrudService);
    }

    @AfterEach
    void tearDown() {
        userCrudService.reset();
    }

    @Test
    void should_not_throw_when_user_belongs_to_organization() {
        assertThatCode(() -> service.validate(USER_ID, ORGANIZATION_ID)).doesNotThrowAnyException();
    }

    @Test
    void should_match_organization_case_insensitively() {
        assertThatCode(() -> service.validate(USER_ID, ORGANIZATION_ID.toUpperCase(Locale.ROOT))).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_user_belongs_to_another_organization() {
        assertThatThrownBy(() -> service.validate(USER_ID, OTHER_ORGANIZATION_ID)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void should_throw_when_user_does_not_exist() {
        assertThatThrownBy(() -> service.validate("unknown", ORGANIZATION_ID)).isInstanceOf(UserNotFoundException.class);
    }
}
