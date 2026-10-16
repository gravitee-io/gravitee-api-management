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
package io.gravitee.apim.core.environment.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.EnvironmentCrudServiceInMemory;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidateEnvironmentInOrganizationDomainServiceTest {

    private static final String ORGANIZATION_ID = "org-1";
    private static final String OTHER_ORGANIZATION_ID = "org-2";
    private static final String ENVIRONMENT_ID = "env-1";

    EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    ValidateEnvironmentInOrganizationDomainService service;

    @BeforeEach
    void setUp() {
        service = new ValidateEnvironmentInOrganizationDomainService(environmentCrudService);
    }

    @AfterEach
    void tearDown() {
        environmentCrudService.reset();
    }

    @Test
    void should_not_throw_when_environment_belongs_to_organization() {
        environmentCrudService.initWith(List.of(Environment.builder().id(ENVIRONMENT_ID).organizationId(ORGANIZATION_ID).build()));

        assertThatCode(() -> service.validate(ENVIRONMENT_ID, ORGANIZATION_ID.toUpperCase(Locale.ROOT))).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_environment_belongs_to_another_organization() {
        environmentCrudService.initWith(List.of(Environment.builder().id(ENVIRONMENT_ID).organizationId(OTHER_ORGANIZATION_ID).build()));

        assertThatThrownBy(() -> service.validate(ENVIRONMENT_ID, ORGANIZATION_ID)).isInstanceOf(EnvironmentNotFoundException.class);
    }

    @Test
    void should_skip_when_environment_id_is_blank() {
        assertThatCode(() -> service.validate("", ORGANIZATION_ID)).doesNotThrowAnyException();
    }

    @Test
    void should_skip_when_environment_id_is_null() {
        assertThatCode(() -> service.validate(null, ORGANIZATION_ID)).doesNotThrowAnyException();
    }
}
