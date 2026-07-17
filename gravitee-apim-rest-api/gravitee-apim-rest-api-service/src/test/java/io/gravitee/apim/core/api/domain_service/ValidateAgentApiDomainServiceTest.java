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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import inmemory.ApiHostValidatorDomainServiceGoogleImpl;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.installation.model.RestrictedDomain;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.definition.model.v4.agent.AgentApi;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ValidateAgentApiDomainServiceTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_ID = "agent-id";

    @Mock
    ApiQueryService apiQueryService;

    @Mock
    InstallationAccessQueryService installationAccessQueryService;

    ValidateAgentApiDomainService service;

    @BeforeEach
    void setUp() {
        service = new ValidateAgentApiDomainService(
            new VerifyApiPathDomainService(
                apiQueryService,
                installationAccessQueryService,
                new ApiHostValidatorDomainServiceGoogleImpl(),
                new ApiPathIndex()
            )
        );
        lenient()
            .when(apiQueryService.search(any(), any(), any()))
            .thenAnswer(invocation -> Stream.of());
    }

    @Test
    void should_sanitize_paths_back_into_the_listener() {
        givenRestrictedDomains(List.of());
        var api = anAgentOn(null, "chat//x");

        service.validateAndSanitize(api, ENVIRONMENT_ID);

        assertThat(pathsOf(api)).extracting("path", "overrideAccess").containsExactly(tuple("/chat/x/", false));
    }

    @Test
    void should_write_back_override_access_on_a_restricted_domain_environment() {
        givenRestrictedDomains(List.of("agent.gravitee.io"));
        var api = anAgentOn("agent.gravitee.io", "/chat");

        service.validateAndSanitize(api, ENVIRONMENT_ID);

        assertThat(pathsOf(api)).extracting("host", "path", "overrideAccess").containsExactly(tuple("agent.gravitee.io", "/chat/", true));
    }

    @Test
    void should_leave_a_listener_without_path_untouched() {
        givenRestrictedDomains(List.of());
        var api = anAgentWith(HttpListener.builder().paths(List.of()).build());

        service.validateAndSanitize(api, ENVIRONMENT_ID);

        assertThat(pathsOf(api)).isEmpty();
    }

    private void givenRestrictedDomains(List<String> domains) {
        lenient()
            .when(installationAccessQueryService.getGatewayRestrictedDomains(eq(ENVIRONMENT_ID)))
            .thenReturn(
                domains
                    .stream()
                    .map(d -> RestrictedDomain.builder().domain(d).build())
                    .toList()
            );
    }

    private static List<Path> pathsOf(Api api) {
        return ((HttpListener) ((AgentApi) api.getApiDefinitionValue()).getListeners().get(0)).getPaths();
    }

    private static Api anAgentOn(String host, String path) {
        return anAgentWith(HttpListener.builder().paths(List.of(Path.builder().host(host).path(path).build())).build());
    }

    private static Api anAgentWith(Listener listener) {
        return Api.builder()
            .id(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .apiDefinitionValue(AgentApi.builder().id(API_ID).kind("standalone").listeners(List.of(listener)).build())
            .build();
    }
}
