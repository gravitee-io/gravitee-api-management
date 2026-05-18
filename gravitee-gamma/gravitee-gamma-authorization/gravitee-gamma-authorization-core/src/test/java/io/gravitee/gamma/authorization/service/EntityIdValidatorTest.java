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
package io.gravitee.gamma.authorization.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.authorization.service.exception.EntityIdValidationCode;
import io.gravitee.gamma.authorization.service.exception.InvalidEntityIdException;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyKind;
import org.junit.jupiter.api.Test;

class EntityIdValidatorTest {

    private final EntityIdValidator validator = new EntityIdValidator();

    @Test
    void global_with_null_entityId_is_accepted() {
        assertThatCode(() -> validator.validate(AuthorizationPolicyKind.GLOBAL, null)).doesNotThrowAnyException();
    }

    @Test
    void global_with_non_null_entityId_is_rejected_with_FORBIDDEN_ON_GLOBAL() {
        assertThatThrownBy(() -> validator.validate(AuthorizationPolicyKind.GLOBAL, "api.x"))
            .isInstanceOf(InvalidEntityIdException.class)
            .extracting(e -> ((InvalidEntityIdException) e).code())
            .isEqualTo(EntityIdValidationCode.ENTITY_ID_FORBIDDEN_ON_GLOBAL);
    }

    @Test
    void resource_with_null_entityId_is_rejected_with_REQUIRED_FOR_RESOURCE() {
        assertThatThrownBy(() -> validator.validate(AuthorizationPolicyKind.RESOURCE, null))
            .isInstanceOf(InvalidEntityIdException.class)
            .extracting(e -> ((InvalidEntityIdException) e).code())
            .isEqualTo(EntityIdValidationCode.ENTITY_ID_REQUIRED_FOR_RESOURCE);
    }

    @Test
    void resource_with_blank_entityId_is_rejected_with_REQUIRED_FOR_RESOURCE() {
        assertThatThrownBy(() -> validator.validate(AuthorizationPolicyKind.RESOURCE, "   "))
            .isInstanceOf(InvalidEntityIdException.class)
            .extracting(e -> ((InvalidEntityIdException) e).code())
            .isEqualTo(EntityIdValidationCode.ENTITY_ID_REQUIRED_FOR_RESOURCE);
    }

    @Test
    void resource_with_uppercase_entityId_is_rejected_as_MALFORMED() {
        assertThatThrownBy(() -> validator.validate(AuthorizationPolicyKind.RESOURCE, "api.MyApi"))
            .isInstanceOf(InvalidEntityIdException.class)
            .extracting(e -> ((InvalidEntityIdException) e).code())
            .isEqualTo(EntityIdValidationCode.ENTITY_ID_MALFORMED);
    }

    @Test
    void resource_with_space_in_entityId_is_rejected_as_MALFORMED() {
        assertThatThrownBy(() -> validator.validate(AuthorizationPolicyKind.RESOURCE, "api.my api"))
            .isInstanceOf(InvalidEntityIdException.class)
            .extracting(e -> ((InvalidEntityIdException) e).code())
            .isEqualTo(EntityIdValidationCode.ENTITY_ID_MALFORMED);
    }

    @Test
    void api_prefix_is_accepted_for_any_apiId_format() {
        assertThatCode(() -> validator.validate(AuthorizationPolicyKind.RESOURCE, "api.abc-123_xyz")).doesNotThrowAnyException();
    }

    @Test
    void mcp_prefix_with_three_or_more_segments_is_accepted() {
        assertThatCode(() -> validator.validate(AuthorizationPolicyKind.RESOURCE, "mcp.any-api.tool-x")).doesNotThrowAnyException();
        assertThatCode(() ->
            validator.validate(AuthorizationPolicyKind.RESOURCE, "mcp.any-api.tools-call.get-bookings")
        ).doesNotThrowAnyException();
    }
}
