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

import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.service.exception.AuthzEntityIdValidationCode;
import io.gravitee.gamma.authorization.service.exception.AuthzInvalidEntityIdException;
import org.junit.jupiter.api.Test;

class AuthzEntityIdValidatorTest {

    private final AuthzEntityIdValidator validator = new AuthzEntityIdValidator();

    @Test
    void global_with_null_entityId_is_accepted() {
        assertThatCode(() -> validator.validate(AuthzPolicyKind.GLOBAL, null)).doesNotThrowAnyException();
    }

    @Test
    void global_with_non_null_entityId_is_rejected_with_FORBIDDEN_ON_GLOBAL() {
        assertThatThrownBy(() -> validator.validate(AuthzPolicyKind.GLOBAL, "api.x"))
            .isInstanceOf(AuthzInvalidEntityIdException.class)
            .extracting(e -> ((AuthzInvalidEntityIdException) e).code())
            .isEqualTo(AuthzEntityIdValidationCode.ENTITY_ID_FORBIDDEN_ON_GLOBAL);
    }

    @Test
    void policyKind_resource_with_null_entityId_is_rejected_with_REQUIRED_FOR_RESOURCE() {
        assertThatThrownBy(() -> validator.validate(AuthzPolicyKind.RESOURCE, null))
            .isInstanceOf(AuthzInvalidEntityIdException.class)
            .extracting(e -> ((AuthzInvalidEntityIdException) e).code())
            .isEqualTo(AuthzEntityIdValidationCode.ENTITY_ID_REQUIRED_FOR_RESOURCE);
    }

    @Test
    void policyKind_resource_with_blank_entityId_is_rejected_with_REQUIRED_FOR_RESOURCE() {
        assertThatThrownBy(() -> validator.validate(AuthzPolicyKind.RESOURCE, "   "))
            .isInstanceOf(AuthzInvalidEntityIdException.class)
            .extracting(e -> ((AuthzInvalidEntityIdException) e).code())
            .isEqualTo(AuthzEntityIdValidationCode.ENTITY_ID_REQUIRED_FOR_RESOURCE);
    }

    @Test
    void entityKind_with_uppercase_entityId_is_rejected_as_MALFORMED() {
        assertThatThrownBy(() -> validator.validate(AuthzEntityKind.RESOURCE, "api.MyApi"))
            .isInstanceOf(AuthzInvalidEntityIdException.class)
            .extracting(e -> ((AuthzInvalidEntityIdException) e).code())
            .isEqualTo(AuthzEntityIdValidationCode.ENTITY_ID_MALFORMED);
    }

    @Test
    void entityKind_with_space_in_entityId_is_rejected_as_MALFORMED() {
        assertThatThrownBy(() -> validator.validate(AuthzEntityKind.RESOURCE, "api.my api"))
            .isInstanceOf(AuthzInvalidEntityIdException.class)
            .extracting(e -> ((AuthzInvalidEntityIdException) e).code())
            .isEqualTo(AuthzEntityIdValidationCode.ENTITY_ID_MALFORMED);
    }

    @Test
    void entityKind_with_leading_or_trailing_or_consecutive_dots_is_rejected_as_MALFORMED() {
        assertThatThrownBy(() -> validator.validate(AuthzEntityKind.RESOURCE, ".api.x")).isInstanceOf(AuthzInvalidEntityIdException.class);
        assertThatThrownBy(() -> validator.validate(AuthzEntityKind.RESOURCE, "api.x.")).isInstanceOf(AuthzInvalidEntityIdException.class);
        assertThatThrownBy(() -> validator.validate(AuthzEntityKind.RESOURCE, "api..x")).isInstanceOf(AuthzInvalidEntityIdException.class);
    }

    @Test
    void entityKind_with_null_entityId_is_rejected_with_REQUIRED_FOR_RESOURCE() {
        assertThatThrownBy(() -> validator.validate(AuthzEntityKind.RESOURCE, null))
            .isInstanceOf(AuthzInvalidEntityIdException.class)
            .extracting(e -> ((AuthzInvalidEntityIdException) e).code())
            .isEqualTo(AuthzEntityIdValidationCode.ENTITY_ID_REQUIRED_FOR_RESOURCE);
    }

    @Test
    void api_prefix_is_accepted_for_any_apiId_format() {
        assertThatCode(() -> validator.validate(AuthzEntityKind.RESOURCE, "api.abc-123_xyz")).doesNotThrowAnyException();
    }

    @Test
    void mcp_prefix_with_three_or_more_segments_is_accepted() {
        assertThatCode(() -> validator.validate(AuthzEntityKind.RESOURCE, "mcp.any-api.tool-x")).doesNotThrowAnyException();
        assertThatCode(() ->
            validator.validate(AuthzEntityKind.RESOURCE, "mcp.any-api.tools-call.get-bookings")
        ).doesNotThrowAnyException();
    }
}
