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
package io.gravitee.apim.core.api.use_case;

import static fixtures.core.model.ApiFixtures.aTcpApiV4;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.VerifyApiHostsDomainService;
import io.gravitee.apim.core.api.exception.DuplicatedHostException;
import io.gravitee.apim.core.api.exception.HostAlreadyExistsException;
import io.gravitee.apim.core.api.exception.InvalidHostException;
import io.gravitee.apim.core.api.model.Api;
import java.util.List;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VerifyApiHostsUseCaseTest {

    private static final String ENVIRONMENT_ID = "envId";
    private static final Api TCP_API = aTcpApiV4().toBuilder().environmentId(ENVIRONMENT_ID).build();

    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final VerifyApiHostsDomainService verifyApiHostsDomainService = new VerifyApiHostsDomainService(apiQueryService);

    private VerifyApiHostsUseCase verifyApiHostsUseCase;

    @BeforeEach
    void setUp() {
        verifyApiHostsUseCase = new VerifyApiHostsUseCase(verifyApiHostsDomainService);
        apiQueryService.initWith(List.of(TCP_API));
    }

    @AfterEach
    void tearDown() {
        apiQueryService.reset();
    }

    @Test
    void should_return_valid_hosts() {
        // given
        VerifyApiHostsUseCase.Input input = new VerifyApiHostsUseCase.Input(
            ENVIRONMENT_ID,
            "apiId",
            List.of("foo-2.example.com", "bar-2.example.com")
        );

        // when
        var result = verifyApiHostsUseCase.execute(input);

        // then
        assertThat(result).isNotNull();
        assertThat(result.hosts()).containsExactly("foo-2.example.com", "bar-2.example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void should_throw_invalid_host_exception(List<String> hosts) {
        // given
        VerifyApiHostsUseCase.Input input = new VerifyApiHostsUseCase.Input(ENVIRONMENT_ID, "apiId", hosts);

        // when
        var throwable = catchThrowable(() -> verifyApiHostsUseCase.execute(input));

        // then
        AssertionsForClassTypes.assertThat(throwable).isInstanceOf(InvalidHostException.class);
    }

    @Test
    void should_throw_invalid_host_exception_with_blank_host() {
        // given
        VerifyApiHostsUseCase.Input input = new VerifyApiHostsUseCase.Input(ENVIRONMENT_ID, "apiId", List.of(" "));

        // when
        var throwable = catchThrowable(() -> verifyApiHostsUseCase.execute(input));

        // then
        AssertionsForClassTypes.assertThat(throwable).isInstanceOf(InvalidHostException.class);
    }

    @Test
    void should_throw_host_already_exist_exception() {
        // given
        VerifyApiHostsUseCase.Input input = new VerifyApiHostsUseCase.Input(
            ENVIRONMENT_ID,
            "apiId",
            List.of("foo.example.com", "bar.example.com")
        );

        // when
        var throwable = catchThrowable(() -> verifyApiHostsUseCase.execute(input));

        // then
        AssertionsForClassTypes.assertThat(throwable).isInstanceOf(HostAlreadyExistsException.class);
    }

    @Test
    void should_throw_duplicated_host_exception() {
        // given
        VerifyApiHostsUseCase.Input input = new VerifyApiHostsUseCase.Input(
            ENVIRONMENT_ID,
            "apiId",
            List.of("foo-2.example.com", "foo-2.example.com")
        );

        // when
        var throwable = catchThrowable(() -> verifyApiHostsUseCase.execute(input));

        // then
        AssertionsForClassTypes.assertThat(throwable).isInstanceOf(DuplicatedHostException.class);
    }
}
