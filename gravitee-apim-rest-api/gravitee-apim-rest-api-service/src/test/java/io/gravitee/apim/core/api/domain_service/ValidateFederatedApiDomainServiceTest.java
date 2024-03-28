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
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.DefinitionVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ValidateFederatedApiDomainServiceTest {

    ValidateFederatedApiDomainService service = new ValidateFederatedApiDomainService();

    @Test
    void should_return_the_api_when_valid() {
        var api = ApiFixtures.aFederatedApi();

        var result = service.validateAndSanitizeForCreation(api);

        assertThat(result).isSameAs(api);
    }

    @Test
    void should_reset_lifecycle_state_when_defined() {
        var api = ApiFixtures.aFederatedApi().toBuilder().lifecycleState(Api.LifecycleState.STARTED).build();

        var result = service.validateAndSanitizeForCreation(api);

        assertThat(result).extracting(Api::getLifecycleState).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = DefinitionVersion.class, mode = EnumSource.Mode.EXCLUDE, names = { "FEDERATED" })
    void should_throw_when_definition_version_is_incorrect(DefinitionVersion definitionVersion) {
        var api = ApiFixtures.aFederatedApi().toBuilder().definitionVersion(definitionVersion).build();

        var throwable = catchThrowable(() -> service.validateAndSanitizeForCreation(api));

        assertThat(throwable).isInstanceOf(ValidationDomainException.class);
    }
}
