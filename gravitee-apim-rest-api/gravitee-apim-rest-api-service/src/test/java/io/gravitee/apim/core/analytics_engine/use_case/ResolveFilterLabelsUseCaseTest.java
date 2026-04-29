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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ResolveFilterLabelsUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId("org-id").environmentId("env-id").build();

    @Mock
    private FilterValueNameResolver filterValueNameResolver;

    private AutoCloseable closeable;
    private ResolveFilterLabelsUseCase useCase;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        useCase = new ResolveFilterLabelsUseCase(filterValueNameResolver);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void should_resolve_id_based_filter_labels() {
        when(filterValueNameResolver.resolveNames("env-id", FilterSpec.Name.API, List.of("api-id"))).thenReturn(Map.of("api-id", "My API"));

        var output = useCase.execute(
            new ResolveFilterLabelsUseCase.Input(AUDIT_INFO, List.of(new ResolveFilterLabelsUseCase.Entry("API", List.of("api-id"))))
        );

        assertThat(output.entries()).containsExactly(new ResolveFilterLabelsUseCase.ResolvedEntry("API", Map.of("api-id", "My API")));
        verify(filterValueNameResolver).resolveNames("env-id", FilterSpec.Name.API, List.of("api-id"));
    }

    @Test
    void should_ignore_unsupported_filter_names() {
        var output = useCase.execute(
            new ResolveFilterLabelsUseCase.Input(AUDIT_INFO, List.of(new ResolveFilterLabelsUseCase.Entry("HTTP_STATUS", List.of("200"))))
        );

        assertThat(output.entries()).containsExactly(new ResolveFilterLabelsUseCase.ResolvedEntry("HTTP_STATUS", Map.of()));
        verifyNoInteractions(filterValueNameResolver);
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
