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
package io.gravitee.apim.core.analytics_engine.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics.domain_service.AiWorkspaceAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.query_service.FilterValuesQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ResolveFilterLabelsUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId("org-id").environmentId("env-id").build();

    /** The APIs the caller reads, all in the current environment. */
    private static final AnalyticsQueryContext READABLE = new AnalyticsQueryContext(
        AUDIT_INFO,
        new ExecutionContext("org-id", "env-id"),
        Set.of("api-id"),
        Map.of("api-id", "My API"),
        Map.of(),
        Map.of(),
        Optional.empty()
    );

    @Mock
    private FilterValueNameResolver filterValueNameResolver;

    @Mock
    private AnalyticsQueryContextLoaderResolver contextLoader;

    @Mock
    private FilterValuesQueryService filterValuesQueryService;

    @Mock
    private AiWorkspaceAnalyticsApisDomainService workspaceApis;

    private ResolveFilterLabelsUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(contextLoader.load(AUDIT_INFO, AnalyticsScope.MANAGEMENT, ApiDataKind.AGGREGATES)).thenReturn(READABLE);
        useCase = new ResolveFilterLabelsUseCase(filterValueNameResolver, contextLoader, filterValuesQueryService, workspaceApis);
    }

    @Test
    void should_label_the_readable_apis_only() {
        var output = useCase.execute(
            new ResolveFilterLabelsUseCase.Input(
                AUDIT_INFO,
                List.of(new ResolveFilterLabelsUseCase.Entry("API", List.of("api-id", "unreadable-api-id", "other-environment-api-id")))
            )
        );

        assertThat(output.entries()).containsExactly(new ResolveFilterLabelsUseCase.ResolvedEntry("API", Map.of("api-id", "My API")));
        verifyNoInteractions(filterValueNameResolver);
    }

    @ParameterizedTest
    @EnumSource(value = FilterSpec.Name.class, names = { "APPLICATION", "PLAN", "API_PRODUCT" })
    void should_label_only_the_ids_found_in_the_calls_of_the_readable_apis(FilterSpec.Name filterName) {
        when(
            filterValuesQueryService.findValuesInCalls(
                "org-id",
                "env-id",
                filterName,
                null,
                null,
                Set.of("in-calls-id", "not-in-calls-id"),
                Set.of("api-id")
            )
        ).thenReturn(Set.of("in-calls-id"));
        when(filterValueNameResolver.resolveNames("env-id", filterName, List.of("in-calls-id"))).thenReturn(
            Map.of("in-calls-id", "Found in calls")
        );

        var output = useCase.execute(
            new ResolveFilterLabelsUseCase.Input(
                AUDIT_INFO,
                List.of(new ResolveFilterLabelsUseCase.Entry(filterName.name(), List.of("in-calls-id", "not-in-calls-id")))
            )
        );

        assertThat(output.entries()).containsExactly(
            new ResolveFilterLabelsUseCase.ResolvedEntry(filterName.name(), Map.of("in-calls-id", "Found in calls"))
        );
    }

    /** The name resolver always adds the keyless application: the answer keeps the ids found in the calls only. */
    @Test
    void should_not_label_an_id_the_calls_do_not_hold_even_when_the_name_resolver_adds_it() {
        when(
            filterValuesQueryService.findValuesInCalls(any(), any(), eq(FilterSpec.Name.APPLICATION), any(), any(), any(), any())
        ).thenReturn(Set.of("app-id"));
        when(filterValueNameResolver.resolveNames("env-id", FilterSpec.Name.APPLICATION, List.of("app-id"))).thenReturn(
            Map.of("app-id", "My App", "1", "Unknown")
        );

        var output = useCase.execute(
            new ResolveFilterLabelsUseCase.Input(
                AUDIT_INFO,
                List.of(new ResolveFilterLabelsUseCase.Entry("APPLICATION", List.of("app-id")))
            )
        );

        assertThat(output.entries()).containsExactly(
            new ResolveFilterLabelsUseCase.ResolvedEntry("APPLICATION", Map.of("app-id", "My App"))
        );
    }

    @Test
    void should_label_nothing_when_the_caller_reads_no_api() {
        when(contextLoader.load(AUDIT_INFO, AnalyticsScope.MANAGEMENT, ApiDataKind.AGGREGATES)).thenReturn(
            new AnalyticsQueryContext(
                AUDIT_INFO,
                new ExecutionContext("org-id", "env-id"),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Optional.empty()
            )
        );

        var output = useCase.execute(
            new ResolveFilterLabelsUseCase.Input(
                AUDIT_INFO,
                List.of(
                    new ResolveFilterLabelsUseCase.Entry("API", List.of("api-id")),
                    new ResolveFilterLabelsUseCase.Entry("APPLICATION", List.of("app-id"))
                )
            )
        );

        assertThat(output.entries()).containsExactly(
            new ResolveFilterLabelsUseCase.ResolvedEntry("API", Map.of()),
            new ResolveFilterLabelsUseCase.ResolvedEntry("APPLICATION", Map.of())
        );
        verify(filterValueNameResolver, never()).resolveNames(any(), any(), eq(List.of("app-id")));
    }

    @Test
    void should_ignore_unsupported_filter_names() {
        var output = useCase.execute(
            new ResolveFilterLabelsUseCase.Input(AUDIT_INFO, List.of(new ResolveFilterLabelsUseCase.Entry("HTTP_STATUS", List.of("200"))))
        );

        assertThat(output.entries()).containsExactly(new ResolveFilterLabelsUseCase.ResolvedEntry("HTTP_STATUS", Map.of()));
        verifyNoInteractions(filterValueNameResolver, filterValuesQueryService);
    }

    @Test
    void should_reject_too_many_entries() {
        var entries = IntStream.range(0, 11)
            .mapToObj(index -> new ResolveFilterLabelsUseCase.Entry("API", List.of("api-" + index)))
            .toList();

        assertThatThrownBy(() -> useCase.execute(new ResolveFilterLabelsUseCase.Input(AUDIT_INFO, entries))).isInstanceOf(
            ValidationDomainException.class
        );
    }

    @Test
    void should_reject_too_many_ids_per_entry() {
        var ids = IntStream.range(0, 101)
            .mapToObj(index -> "api-" + index)
            .toList();

        assertThatThrownBy(() ->
            useCase.execute(new ResolveFilterLabelsUseCase.Input(AUDIT_INFO, List.of(new ResolveFilterLabelsUseCase.Entry("API", ids))))
        ).isInstanceOf(ValidationDomainException.class);
    }
}
