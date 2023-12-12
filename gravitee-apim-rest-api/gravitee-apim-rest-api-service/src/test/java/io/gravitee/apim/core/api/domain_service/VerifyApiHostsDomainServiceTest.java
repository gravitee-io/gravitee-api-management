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

import static fixtures.core.model.ApiFixtures.aMessageApiV4;
import static fixtures.core.model.ApiFixtures.aProxyApiV4;
import static fixtures.core.model.ApiFixtures.aTcpApiV4;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.DuplicatedHostException;
import io.gravitee.apim.core.api.exception.HostAlreadyExistsException;
import io.gravitee.apim.core.api.exception.InvalidHostException;
import io.gravitee.apim.core.api.model.Api;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VerifyApiHostsDomainServiceTest {

    private static final String API_ID = "api-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final Api V4_TCP_API_1 = aTcpApiV4().toBuilder().id("tcp-1").environmentId(ENVIRONMENT_ID).build();
    private static final Api V4_TCP_API_2 = aTcpApiV4(List.of("baz.example.com"))
        .toBuilder()
        .id("tcp-2")
        .environmentId(ENVIRONMENT_ID)
        .build();
    private static final Api V4_HTTP_API_2 = aProxyApiV4().toBuilder().id("http-1").environmentId(ENVIRONMENT_ID).build();
    private static final Api V4_MESSAGE_API = aMessageApiV4().toBuilder().id("message-1").environmentId(ENVIRONMENT_ID).build();

    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private VerifyApiHostsDomainService verifyApiHostsDomainService;

    @BeforeEach
    void setUp() {
        verifyApiHostsDomainService = new VerifyApiHostsDomainService(apiQueryService);
        apiQueryService.initWith(List.of(V4_TCP_API_1, V4_TCP_API_2, V4_HTTP_API_2, V4_MESSAGE_API));
    }

    @AfterEach
    void tearDown() {
        apiQueryService.reset();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void should_throw_invalid_hosts_exception_when_no_host(List<String> hosts) {
        // when
        var throwable = catchThrowable(() -> verifyApiHostsDomainService.checkApiHosts(ENVIRONMENT_ID, API_ID, hosts));

        // then
        assertThat(throwable).isInstanceOf(InvalidHostException.class);
    }

    @Test
    void should_throw_hosts_already_exist_exception_when_hosts_are_duplicated() {
        // given
        var hosts = List.of("foo.example.com", "bar.example.com", "foo.example.com");

        // when
        var throwable = catchThrowable(() -> verifyApiHostsDomainService.checkApiHosts(ENVIRONMENT_ID, API_ID, hosts));

        // then
        assertThat(throwable).isInstanceOf(DuplicatedHostException.class);
    }

    @Test
    void should_throw_hosts_already_exist_exception_when_host_is_used_by_another_api() {
        // given
        var hosts = List.of("foo.example.com", "tcp.example.com");

        // when
        var throwable = catchThrowable(() -> verifyApiHostsDomainService.checkApiHosts(ENVIRONMENT_ID, API_ID, hosts));

        // then
        assertThat(throwable).isInstanceOf(HostAlreadyExistsException.class);
    }

    @Test
    void should_throw_invalid_hosts_exception_when_host_is_blank() {
        // given
        var hosts = List.of("foo.example.com", "");

        // when
        var throwable = catchThrowable(() -> verifyApiHostsDomainService.checkApiHosts(ENVIRONMENT_ID, API_ID, hosts));

        // then
        assertThat(throwable).isInstanceOf(InvalidHostException.class);
    }

    @Test
    void should_validate_hosts() {
        // given
        var hosts = List.of("tcp.1.example.com", "tcp.2.example.com");

        // when
        var result = verifyApiHostsDomainService.checkApiHosts(ENVIRONMENT_ID, API_ID, hosts);

        // then
        assertThat(result).isTrue();
    }
}
