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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.rest.api.service.exceptions.LifecycleStateChangeNotAllowedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ValidateApiLifecycleServiceTest {

    @ParameterizedTest
    @EnumSource(value = Api.ApiLifecycleState.class)
    void should_return_existing_lifecycle_if_new_is_null(Api.ApiLifecycleState existingLifecycleState) {
        Api.ApiLifecycleState newLifecycleState = null;

        var result = ValidateApiLifecycleService.validateFederatedApiLifecycleState(existingLifecycleState, newLifecycleState);

        assertThat(result).isEqualTo(existingLifecycleState);
    }

    @ParameterizedTest
    @EnumSource(value = Api.ApiLifecycleState.class)
    void should_throw_exception_when_trying_to_change_deprecated_lifecycle_state(Api.ApiLifecycleState newLifecycleState) {
        Api.ApiLifecycleState existingLifecycleState = Api.ApiLifecycleState.DEPRECATED;

        assertThatThrownBy(() -> ValidateApiLifecycleService.validateFederatedApiLifecycleState(existingLifecycleState, newLifecycleState))
            .isInstanceOf(LifecycleStateChangeNotAllowedException.class)
            .hasMessage("The API lifecycle state cannot be changed to " + newLifecycleState + ".");
    }

    @ParameterizedTest
    @EnumSource(value = Api.ApiLifecycleState.class)
    void should_throw_exception_when_trying_to_change_archived_lifecycle_state(Api.ApiLifecycleState newLifecycleState) {
        Api.ApiLifecycleState existingLifecycleState = Api.ApiLifecycleState.ARCHIVED;

        assertThatThrownBy(() -> ValidateApiLifecycleService.validateFederatedApiLifecycleState(existingLifecycleState, newLifecycleState))
            .isInstanceOf(LifecycleStateChangeNotAllowedException.class)
            .hasMessage("The API lifecycle state cannot be changed to " + newLifecycleState + ".");
    }

    @ParameterizedTest
    @EnumSource(value = Api.ApiLifecycleState.class, mode = EnumSource.Mode.EXCLUDE, names = { "ARCHIVED", "DEPRECATED" })
    void should_return_existing_lifecycle_if_new_is_the_same(Api.ApiLifecycleState existingLifecycleState) {
        var result = ValidateApiLifecycleService.validateFederatedApiLifecycleState(existingLifecycleState, existingLifecycleState);

        assertThat(result).isEqualTo(existingLifecycleState);
    }

    @Test
    void should_throw_exception_when_changing_from_unpublished_to_created() {
        Api.ApiLifecycleState existingLifecycleState = Api.ApiLifecycleState.UNPUBLISHED;
        Api.ApiLifecycleState newLifecycleState = Api.ApiLifecycleState.CREATED;

        assertThatThrownBy(() -> ValidateApiLifecycleService.validateFederatedApiLifecycleState(existingLifecycleState, newLifecycleState))
            .isInstanceOf(LifecycleStateChangeNotAllowedException.class)
            .hasMessage("The API lifecycle state cannot be changed to " + newLifecycleState + ".");
    }

    @Test
    void should_return_new_lifecycle_when_changing_from_created_to_published() {
        Api.ApiLifecycleState existingLifecycleState = Api.ApiLifecycleState.CREATED;
        Api.ApiLifecycleState newLifecycleState = Api.ApiLifecycleState.PUBLISHED;

        var result = ValidateApiLifecycleService.validateFederatedApiLifecycleState(existingLifecycleState, newLifecycleState);

        assertThat(result).isEqualTo(newLifecycleState);
    }

    @Test
    void should_return_new_lifecycle_when_changing_from_published_to_unpublished() {
        Api.ApiLifecycleState existingLifecycleState = Api.ApiLifecycleState.PUBLISHED;
        Api.ApiLifecycleState newLifecycleState = Api.ApiLifecycleState.UNPUBLISHED;

        var result = ValidateApiLifecycleService.validateFederatedApiLifecycleState(existingLifecycleState, newLifecycleState);

        assertThat(result).isEqualTo(newLifecycleState);
    }

    @Test
    void should_return_new_lifecycle_when_changing_from_unpublished_to_deprecated() {
        Api.ApiLifecycleState existingLifecycleState = Api.ApiLifecycleState.UNPUBLISHED;
        Api.ApiLifecycleState newLifecycleState = Api.ApiLifecycleState.DEPRECATED;

        var result = ValidateApiLifecycleService.validateFederatedApiLifecycleState(existingLifecycleState, newLifecycleState);

        assertThat(result).isEqualTo(newLifecycleState);
    }
}
