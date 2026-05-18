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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import fixtures.definition.ApiDefinitionFixtures;
import inmemory.ApiHostValidatorDomainServiceGoogleImpl;
import io.gravitee.apim.core.api.domain_service.ApiPathIndex;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.installation.model.RestrictedDomain;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerifyApiPathDomainServiceTest {

    public static final String ENVIRONMENT_ID = "environment-id";
    public static final String API_ID = "api-id";

    @Mock
    ApiQueryService apiSearchService;

    @Mock
    InstallationAccessQueryService installationAccessQueryService;

    VerifyApiPathDomainService service;

    public static Stream<Arguments> sanitizePathParams() {
        return Stream.of(
            Arguments.of(null, "/"),
            Arguments.of("", "/"),
            Arguments.of("path", "/path/"),
            Arguments.of("/path", "/path/"),
            Arguments.of("path/", "/path/"),
            Arguments.of("//path/", "/path/")
        );
    }

    public static Stream<Arguments> sanitizeHostParams() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of("domain.com:123", "domain.com:123"),
            Arguments.of("domain.net", "domain.net"),
            Arguments.of("sub.domain.com", "sub.domain.com"),
            Arguments.of("lower.sub.domain.net:443", "lower.sub.domain.net:443")
        );
    }

    @BeforeEach
    void setup() {
        service = new VerifyApiPathDomainService(
            apiSearchService,
            installationAccessQueryService,
            new ApiHostValidatorDomainServiceGoogleImpl(),
            new ApiPathIndex(),
            Duration.ofSeconds(10)
        );
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @ParameterizedTest
    @MethodSource("sanitizePathParams")
    public void should_sanitize_path(String path, String expectedPath) {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, null);

        var result = service.validateAndSanitize(
            new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path(path).overrideAccess(true).build()))
        );
        var optionalPaths = result.value().map(VerifyApiPathDomainService.Input::paths);
        assertThat(optionalPaths).isNotEmpty();
        var paths = optionalPaths.get();
        assertThat(paths).hasSize(1);
        assertThat(paths.get(0).getPath()).isEqualTo(expectedPath);
        assertThat(paths.get(0).getHost()).isNull();
        assertThat(paths.get(0).isOverrideAccess()).isTrue();
    }

    @Test
    public void should_return_severe_errors_if_path_is_invalid() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, null);

        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path("invalid>path").build()))
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [invalid>path] is invalid")));
    }

    @ParameterizedTest
    @MethodSource("sanitizeHostParams")
    public void should_check_domain_restrictions_if_host_is_present(String host, String expectedHost) {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, List.of("domain.com", "domain.net", "domain.com:123"));

        var res = service.validateAndSanitize(
            new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().host(host).path("/path/").build()))
        );

        var paths = res.value().map(VerifyApiPathDomainService.Input::paths);

        assertThat(paths).isPresent().hasValue(List.of(Path.builder().path("/path/").host(expectedHost).overrideAccess(true).build()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong-domain.com", "not-same-port:8082" })
    public void should_return_severe_error_exception_if_domain_is_invalid(String host) {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, List.of("domain.com", "domain.net", "not-same-port:1234"));

        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().host(host).path("/path/").build()))
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Domain [%s] is invalid", host)));
    }

    @Test
    public void should_return_severe_error_if_duplicate_paths() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, null);

        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(
                    ENVIRONMENT_ID,
                    null,
                    List.of(
                        Path.builder().path("/abc/").build(),
                        Path.builder().path("/path/").build(),
                        Path.builder().path("/path/").build()
                    )
                )
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [/path/] is duplicated")));
    }

    @Test
    public void should_return_severe_error_if_path_already_covered_by_other_api_for_api_creation() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, null);

        givenExistingApis(ENVIRONMENT_ID, Stream.of(buildApiV2WithPaths(ENVIRONMENT_ID, "api1", List.of(Pair.of("", "/path/")))));

        var errors = service
            .validateAndSanitize(new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, null, List.of(Path.builder().path("/path/").build())))
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [/path/] already exists")));
    }

    @Test
    public void should_return_severe_error_if_path_already_used_by_api_v2() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, null);

        givenExistingApis(ENVIRONMENT_ID, Stream.of(buildApiV2WithPaths(ENVIRONMENT_ID, "api1", List.of(Pair.of("", "/path/")))));

        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path("/path/").build()))
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [/path/] already exists")));
    }

    @Test
    public void should_return_severe_error_if_path_already_used_by_api_v2_with_host() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, List.of("domain.com", "domain.net"));

        givenExistingApis(ENVIRONMENT_ID, Stream.of(buildApiV2WithPaths(ENVIRONMENT_ID, "api1", List.of(Pair.of("domain.com", "/path/")))));

        // If domain is not set, first domain of domain restriction is used
        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(
                    ENVIRONMENT_ID,
                    API_ID,
                    List.of(Path.builder().host("domain.com").path("/path/").build())
                )
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [/path/] already exists")));
    }

    @Test
    public void should_ignore_path_used_by_same_api_v2_with_host() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, List.of("domain.com", "domain.net"));

        givenExistingApis(ENVIRONMENT_ID, Stream.of(buildApiV2WithPaths(ENVIRONMENT_ID, API_ID, List.of(Pair.of("domain.com", "/path/")))));

        // Check should be ok if same path but different domain
        var paths = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(
                    ENVIRONMENT_ID,
                    API_ID,
                    List.of(Path.builder().host("domain.com").path("/path/").build())
                )
            )
            .map(VerifyApiPathDomainService.Input::paths)
            .value();

        assertThat(paths).isPresent().hasValue(List.of(Path.builder().path("/path/").host("domain.com").overrideAccess(true).build()));
    }

    @Test
    public void should_return_severe_error_if_path_is_sub_path_of_another_api_v2() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, List.of());

        givenExistingApis(ENVIRONMENT_ID, Stream.of(buildApiV2WithPaths(ENVIRONMENT_ID, "api1", List.of(Pair.of(null, "/path/")))));

        // If domain is not set, first domain of domain restriction is used
        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path("/path/subpath").build()))
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [/path/] already exists")));
    }

    @Test
    public void should_throw_exception_if_path_is_sub_path_of_another_api_v2_with_host() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, List.of("domain.com", "domain.net"));

        givenExistingApis(ENVIRONMENT_ID, Stream.of(buildApiV2WithPaths(ENVIRONMENT_ID, "api1", List.of(Pair.of("domain.com", "/path/")))));

        // If domain is not set, first domain of domain restriction is used
        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(
                    ENVIRONMENT_ID,
                    API_ID,
                    List.of(Path.builder().host("domain.com").path("/path/subpath").build())
                )
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [/path/] already exists")));
    }

    @Test
    public void should_throw_exception_if_path_already_used_by_api_v4() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, null);

        givenExistingApis(ENVIRONMENT_ID, Stream.of(buildApiHttpV4WithPaths(ENVIRONMENT_ID, "api1", List.of(Pair.of("", "/path/")))));

        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().host("").path("/path/").build()))
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [/path/] already exists")));
    }

    @Test
    public void should_return_error_if_path_already_used_by_secondary_api_v4_path() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, null);

        givenExistingApis(
            ENVIRONMENT_ID,
            Stream.of(buildApiHttpV4WithPaths(ENVIRONMENT_ID, "api1", List.of(Pair.of("", "/path1/"), Pair.of("", "/path2/"))))
        );

        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().host("").path("/path2/").build()))
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [/path2/] already exists")));
    }

    @Test
    public void should_ignore_path_already_used_by_same_api_v4() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, null);

        givenExistingApis(ENVIRONMENT_ID, Stream.of(buildApiHttpV4WithPaths(ENVIRONMENT_ID, API_ID, List.of(Pair.of("", "/path/")))));

        var paths = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path("/path/").build()))
            )
            .map(VerifyApiPathDomainService.Input::paths)
            .value();

        assertThat(paths).isPresent().hasValue(List.of(Path.builder().path("/path/").host(null).build()));
    }

    @Test
    public void should_return_error_if_path_already_used_by_api_v4_with_host() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, List.of("domain.com", "domain.net"));

        givenExistingApis(
            ENVIRONMENT_ID,
            Stream.of(buildApiHttpV4WithPaths(ENVIRONMENT_ID, "api1", List.of(Pair.of("domain.com", "/path/"))))
        );

        // If domain is not set, first domain of domain restriction is used
        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(
                    ENVIRONMENT_ID,
                    API_ID,
                    List.of(Path.builder().host("domain.com").path("/path/").build())
                )
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [/path/] already exists")));
    }

    @Test
    public void should_ignore_if_path_already_used_by_api_v4_with_different_host() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, List.of("domain.com", "domain.net"));

        givenExistingApis(
            ENVIRONMENT_ID,
            Stream.of(buildApiHttpV4WithPaths(ENVIRONMENT_ID, "api1", List.of(Pair.of("domain.net", "/path/"))))
        );

        var paths = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(
                    ENVIRONMENT_ID,
                    API_ID,
                    List.of(Path.builder().host("domain.com").path("/path/").build())
                )
            )
            .map(VerifyApiPathDomainService.Input::paths)
            .value();

        assertThat(paths).isPresent().hasValue(List.of(Path.builder().path("/path/").host("domain.com").overrideAccess(true).build()));
    }

    @Test
    public void should_ignore_path_used_by_same_api_v4_with_host() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, List.of("domain.com", "domain.net"));

        givenExistingApis(
            ENVIRONMENT_ID,
            Stream.of(buildApiHttpV4WithPaths(ENVIRONMENT_ID, API_ID, List.of(Pair.of("domain.com", "/path/"))))
        );

        // Check should be ok if same path but different domain
        var paths = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(
                    ENVIRONMENT_ID,
                    API_ID,
                    List.of(Path.builder().host("domain.com").path("/path/").build())
                )
            )
            .map(VerifyApiPathDomainService.Input::paths)
            .value();

        assertThat(paths).isPresent().hasValue(List.of(Path.builder().path("/path/").host("domain.com").overrideAccess(true).build()));
    }

    @Test
    public void should_return_severe_error_if_path_is_sub_path_of_another_api_v4() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, List.of());

        givenExistingApis(ENVIRONMENT_ID, Stream.of(buildApiHttpV4WithPaths(ENVIRONMENT_ID, "api1", List.of(Pair.of(null, "/path/")))));

        // If domain is not set, first domain of domain restriction is used
        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path("/path/subpath").build()))
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [/path/] already exists")));
    }

    @Test
    public void should_throw_exception_if_path_is_sub_path_of_another_api_v4_with_host() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, List.of("domain.com", "domain.net"));

        givenExistingApis(
            ENVIRONMENT_ID,
            Stream.of(buildApiHttpV4WithPaths(ENVIRONMENT_ID, "api1", List.of(Pair.of("domain.com", "/path/"))))
        );

        // If domain is not set, first domain of domain restriction is used
        var errors = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(
                    ENVIRONMENT_ID,
                    API_ID,
                    List.of(Path.builder().host("domain.com").path("/path/subpath").build())
                )
            )
            .severe();

        assertThat(errors).isPresent().hasValue(List.of(Validator.Error.severe("Path [/path/] already exists")));
    }

    @Test
    public void should_handle_native_api() {
        givenExistingRestrictedDomains(ENVIRONMENT_ID, null);

        givenExistingApis(ENVIRONMENT_ID, Stream.of(buildApiNativeV4(ENVIRONMENT_ID, "another-api")));

        var paths = service
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path("/path/").build()))
            )
            .map(VerifyApiPathDomainService.Input::paths)
            .value();

        assertThat(paths).isPresent().hasValue(List.of(Path.builder().path("/path/").host(null).build()));
    }

    private void givenExistingRestrictedDomains(String environmentId, List<String> domainRestrictions) {
        lenient().when(installationAccessQueryService.getGatewayRestrictedDomains(any())).thenReturn(List.of());
        lenient()
            .when(installationAccessQueryService.getGatewayRestrictedDomains(eq(environmentId)))
            .thenReturn(
                domainRestrictions != null
                    ? domainRestrictions
                        .stream()
                        .map(domain -> RestrictedDomain.builder().domain(domain).build())
                        .toList()
                    : List.of()
            );
    }

    private void givenExistingApis(String environmentId, Stream<Api> apis) {
        var apiList = apis.toList();
        lenient()
            .when(
                apiSearchService.search(
                    argThat(
                        (ApiSearchCriteria c) -> c != null && environmentId.equals(c.getEnvironmentId()) && c.getDefinitionVersion() != null
                    ),
                    eq(null),
                    eq(ApiFieldFilter.builder().pictureExcluded(true).build())
                )
            )
            .thenAnswer(invocation -> apiList.stream());
    }

    @SneakyThrows
    private Api buildApiV2WithPaths(String environmentId, String apiId, List<Pair<String, String>> paths) {
        io.gravitee.definition.model.Api apiDefV2 = ApiDefinitionFixtures.anApiV2()
            .toBuilder()
            .proxy(
                Proxy.builder()
                    .virtualHosts(
                        paths
                            .stream()
                            .map(p -> {
                                VirtualHost virtualHost = new VirtualHost();
                                virtualHost.setHost(p.getLeft());
                                virtualHost.setPath(p.getRight());
                                return virtualHost;
                            })
                            .toList()
                    )
                    .build()
            )
            .build();

        return Api.builder().id(apiId).environmentId(environmentId).apiDefinition(apiDefV2).build();
    }

    @SneakyThrows
    private Api buildApiHttpV4WithPaths(String environmentId, String apiId, List<Pair<String, String>> paths) {
        io.gravitee.definition.model.v4.Api apiDefV4 = ApiDefinitionFixtures.anApiV4()
            .toBuilder()
            .id((apiId))
            .listeners(
                List.of(
                    HttpListener.builder()
                        .paths(
                            paths
                                .stream()
                                .map(p ->
                                    io.gravitee.definition.model.v4.listener.http.Path.builder()
                                        .host(p.getLeft())
                                        .path(p.getRight())
                                        .build()
                                )
                                .toList()
                        )
                        .build()
                )
            )
            .build();
        return Api.builder().id(apiId).environmentId(environmentId).apiDefinitionHttpV4(apiDefV4).build();
    }

    @SneakyThrows
    private Api buildApiNativeV4(String environmentId, String apiId) {
        io.gravitee.definition.model.v4.nativeapi.NativeApi apiDefV4 = ApiDefinitionFixtures.aNativeApiV4()
            .toBuilder()
            .id((apiId))
            .listeners(List.of(KafkaListener.builder().build()))
            .build();
        return Api.builder().id(apiId).environmentId(environmentId).apiDefinitionNativeV4(apiDefV4).build();
    }

    /**
     * Scenarios that exercise the supplementary-query + per-conflict-recheck mitigation:
     * cross-pod create race, override of stale snapshot entries, delete race, real conflicts surviving recheck,
     * and the clock-skew belt-and-suspenders path. Each test wires the seeder and supplementary calls separately
     * so we can drive divergent views.
     */
    @org.junit.jupiter.api.Nested
    class WatermarkAndRecheck {

        @Test
        void cross_pod_create_race_detected_via_supplementary_query() {
            // Seeded snapshot: empty (a brand-new pod just spun up; no APIs locally yet).
            // Supplementary query: returns a foreign API that another pod just wrote with conflicting path.
            givenExistingRestrictedDomains(ENVIRONMENT_ID, null);
            var foreign = buildApiHttpV4WithPaths(ENVIRONMENT_ID, "foreign-api", List.of(Pair.of(null, "/foo")));
            stubSeederResponse(ENVIRONMENT_ID, List.of());
            stubSupplementaryResponse(ENVIRONMENT_ID, List.of(foreign));

            var errors = service
                .validateAndSanitize(
                    new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path("/foo/bar").build()))
                )
                .severe();

            assertThat(errors).isPresent();
            assertThat(errors.get().get(0).getMessage()).contains("/foo/");
        }

        @Test
        void override_drops_stale_snapshot_conflict_when_supplementary_says_paths_changed() {
            // Seeded snapshot has X owning /foo. Supplementary returns X with /bar (X was updated, /foo is now free).
            // Candidate /foo should be accepted: snapshot says conflict, supplementary overrides with current paths.
            givenExistingRestrictedDomains(ENVIRONMENT_ID, null);
            var staleX = buildApiHttpV4WithPaths(ENVIRONMENT_ID, "X", List.of(Pair.of(null, "/foo")));
            var freshX = buildApiHttpV4WithPaths(ENVIRONMENT_ID, "X", List.of(Pair.of(null, "/bar")));
            stubSeederResponse(ENVIRONMENT_ID, List.of(staleX));
            stubSupplementaryResponse(ENVIRONMENT_ID, List.of(freshX));

            var errors = service
                .validateAndSanitize(
                    new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path("/foo").build()))
                )
                .severe();

            assertThat(errors).isNotPresent();
        }

        @Test
        void delete_race_dropped_via_recheck_when_batch_search_returns_empty() {
            // Seeded snapshot has X owning /foo. Supplementary is empty (X was deleted; no row to find).
            // The batch recheck (ids=[X]) returns empty. Recheck drops the conflict — /foo is actually free.
            givenExistingRestrictedDomains(ENVIRONMENT_ID, null);
            var deletedX = buildApiHttpV4WithPaths(ENVIRONMENT_ID, "X", List.of(Pair.of(null, "/foo")));
            stubSeederResponse(ENVIRONMENT_ID, List.of(deletedX));
            stubSupplementaryResponse(ENVIRONMENT_ID, List.of());
            stubRecheckResponse(ENVIRONMENT_ID, List.of());

            var errors = service
                .validateAndSanitize(
                    new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path("/foo").build()))
                )
                .severe();

            assertThat(errors).isNotPresent();
        }

        @Test
        void real_conflict_survives_recheck_when_batch_search_confirms_paths() {
            // Seeded snapshot has X owning /foo. Supplementary empty. The batch recheck returns X still owning /foo.
            // Rescan re-confirms the conflict against the live paths.
            givenExistingRestrictedDomains(ENVIRONMENT_ID, null);
            var realX = buildApiHttpV4WithPaths(ENVIRONMENT_ID, "X", List.of(Pair.of(null, "/foo")));
            stubSeederResponse(ENVIRONMENT_ID, List.of(realX));
            stubSupplementaryResponse(ENVIRONMENT_ID, List.of());
            stubRecheckResponse(ENVIRONMENT_ID, List.of(realX));

            var errors = service
                .validateAndSanitize(
                    new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path("/foo").build()))
                )
                .severe();

            assertThat(errors).isPresent();
            assertThat(errors.get().get(0).getMessage()).contains("/foo/");
        }

        @Test
        void clock_skew_belt_and_suspenders_recheck_drops_conflict_when_paths_have_changed() {
            // Snapshot has X owning /foo (stale due to an update that escaped the watermark window — e.g. extreme
            // clock skew where the update's updatedAt was older than our refreshedAt). Supplementary returns nothing
            // for X. But the batch recheck returns X with new paths (/bar). Rescan drops the conflict against /foo.
            givenExistingRestrictedDomains(ENVIRONMENT_ID, null);
            var staleX = buildApiHttpV4WithPaths(ENVIRONMENT_ID, "X", List.of(Pair.of(null, "/foo")));
            var currentX = buildApiHttpV4WithPaths(ENVIRONMENT_ID, "X", List.of(Pair.of(null, "/bar")));
            stubSeederResponse(ENVIRONMENT_ID, List.of(staleX));
            stubSupplementaryResponse(ENVIRONMENT_ID, List.of());
            stubRecheckResponse(ENVIRONMENT_ID, List.of(currentX));

            var errors = service
                .validateAndSanitize(
                    new VerifyApiPathDomainService.Input(ENVIRONMENT_ID, API_ID, List.of(Path.builder().path("/foo").build()))
                )
                .severe();

            assertThat(errors).isNotPresent();
        }

        @Test
        void batch_recheck_fires_a_single_search_for_multiple_conflicting_apiIds() {
            // Two snapshot entries each conflict with a different candidate path. Both apiIds need recheck.
            // We expect a single batch search to be issued (covering both ids), not one per id.
            givenExistingRestrictedDomains(ENVIRONMENT_ID, null);
            var staleX = buildApiHttpV4WithPaths(ENVIRONMENT_ID, "X", List.of(Pair.of(null, "/foo")));
            var staleY = buildApiHttpV4WithPaths(ENVIRONMENT_ID, "Y", List.of(Pair.of(null, "/bar")));
            stubSeederResponse(ENVIRONMENT_ID, List.of(staleX, staleY));
            stubSupplementaryResponse(ENVIRONMENT_ID, List.of());
            stubRecheckResponse(ENVIRONMENT_ID, List.of(staleX, staleY));

            var errors = service
                .validateAndSanitize(
                    new VerifyApiPathDomainService.Input(
                        ENVIRONMENT_ID,
                        API_ID,
                        List.of(Path.builder().path("/foo").build(), Path.builder().path("/bar").build())
                    )
                )
                .severe();

            assertThat(errors).isPresent();
            assertThat(errors.get()).hasSize(2);
            // Verify a single batch call with both ids — not one call per id.
            org.mockito.Mockito.verify(apiSearchService, org.mockito.Mockito.times(1)).search(
                argThat(
                    (ApiSearchCriteria c) ->
                        c != null && c.getIds() != null && c.getIds().containsAll(java.util.Set.of("X", "Y")) && c.getIds().size() == 2
                ),
                eq(null),
                eq(ApiFieldFilter.builder().pictureExcluded(true).build())
            );
        }

        private void stubSeederResponse(String environmentId, List<Api> apis) {
            lenient()
                .when(
                    apiSearchService.search(
                        argThat(
                            (ApiSearchCriteria c) ->
                                c != null &&
                                environmentId.equals(c.getEnvironmentId()) &&
                                c.getUpdatedAtFrom() == null &&
                                (c.getIds() == null || c.getIds().isEmpty())
                        ),
                        eq(null),
                        eq(ApiFieldFilter.builder().pictureExcluded(true).build())
                    )
                )
                .thenAnswer(invocation -> apis.stream());
        }

        private void stubRecheckResponse(String environmentId, List<Api> apis) {
            lenient()
                .when(
                    apiSearchService.search(
                        argThat(
                            (ApiSearchCriteria c) ->
                                c != null &&
                                environmentId.equals(c.getEnvironmentId()) &&
                                c.getUpdatedAtFrom() == null &&
                                c.getIds() != null &&
                                !c.getIds().isEmpty()
                        ),
                        eq(null),
                        eq(ApiFieldFilter.builder().pictureExcluded(true).build())
                    )
                )
                .thenAnswer(invocation -> apis.stream());
        }

        private void stubSupplementaryResponse(String environmentId, List<Api> apis) {
            lenient()
                .when(
                    apiSearchService.search(
                        argThat(
                            (ApiSearchCriteria c) -> c != null && environmentId.equals(c.getEnvironmentId()) && c.getUpdatedAtFrom() != null
                        ),
                        eq(null),
                        eq(ApiFieldFilter.builder().pictureExcluded(true).build())
                    )
                )
                .thenAnswer(invocation -> apis.stream());
        }
    }
}
