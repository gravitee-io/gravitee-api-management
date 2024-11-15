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
package io.gravitee.apim.infra.query_service.specgen;

import static io.gravitee.definition.model.DefinitionVersion.V2;
import static io.gravitee.definition.model.DefinitionVersion.V4;
import static io.gravitee.definition.model.v4.ApiType.MESSAGE;
import static io.gravitee.definition.model.v4.ApiType.NATIVE;
import static io.gravitee.definition.model.v4.ApiType.PROXY;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;
import static io.gravitee.rest.api.service.common.UuidString.generateRandom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.infra.query_service.specgen.ApiSpecGenQueryServiceImpl;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class ApiSpecGenQueryServiceTest {

    @Mock
    ApiRepository repository;

    ApiSpecGenQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        queryService = new ApiSpecGenQueryServiceImpl(repository);
    }

    public static Stream<Arguments> params_that_must_return_optional_api() {
        return Stream.of(
            Arguments.of(generateRandom(), "api-env", "ctx-env", NATIVE, V2, false, false),
            Arguments.of(generateRandom(), "api-env", "ctx-env", MESSAGE, V2, false, false),
            Arguments.of(generateRandom(), "api-env", "ctx-env", PROXY, V2, false, false),
            Arguments.of(generateRandom(), "api-env", "api-env", NATIVE, V2, false, false),
            Arguments.of(generateRandom(), "api-env", "api-env", MESSAGE, V2, false, false),
            Arguments.of(generateRandom(), "api-env", "api-env", PROXY, V2, false, false),
            Arguments.of(generateRandom(), "api-env", "ctx-env", NATIVE, V4, false, false),
            Arguments.of(generateRandom(), "api-env", "ctx-env", MESSAGE, V4, false, false),
            Arguments.of(generateRandom(), "api-env", "ctx-env", PROXY, V4, false, false),
            Arguments.of(generateRandom(), "api-env", "api-env", NATIVE, V4, true, false),
            Arguments.of(generateRandom(), "api-env", "api-env", MESSAGE, V4, true, false),
            Arguments.of(generateRandom(), "api-env", "api-env", PROXY, V4, true, true),
            Arguments.of(generateRandom(), "api-env", "api-env", PROXY, V4, true, true)
        );
    }

    @ParameterizedTest
    @MethodSource("params_that_must_return_optional_api")
    void must_return_optional_api(
        String apiId,
        String apiEnv,
        String contextEnv,
        ApiType apiType,
        DefinitionVersion version,
        boolean isPresent,
        boolean expected
    ) throws TechnicalException {
        when(repository.findById(apiId)).thenReturn(getOptionalToReturn(apiId, apiType, apiEnv, version, isPresent));
        GraviteeContext.setCurrentEnvironment(contextEnv);

        assertThat(queryService.findByIdAndType(getExecutionContext(), apiId, PROXY).isPresent()).isEqualTo(expected);
    }

    private Optional<Api> getOptionalToReturn(String id, ApiType apiType, String apiEnv, DefinitionVersion version, boolean present) {
        if (present) {
            return Optional.of(Api.builder().id(id).type(apiType).environmentId(apiEnv).definitionVersion(version).build());
        }
        return Optional.empty();
    }

    @Test
    void must_not_return_optional_api_due_to_exception() throws TechnicalException {
        when(repository.findById(any())).thenThrow(new TechnicalException("An error has occurred"));

        assertThat(queryService.findByIdAndType(getExecutionContext(), generateRandom(), PROXY).isEmpty()).isTrue();
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }
}
