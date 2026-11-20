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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * {@code Api.guardians} indexes the Guardians an API runs, and it has a single writer: the write that replaces a
 * definition, which carries the Guardians that definition names. Every path that writes a whole repository API must
 * therefore leave the index unset — {@code null}, which both stores read as "not provided" — rather than derive it
 * from the model it writes, since a read may have filtered the definition out of that model and the index would then
 * be emptied by a save that never saw a Guardian.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiGuardiansIndexWritePathsTest {

    private static final List<String> WRITE_PATHS_OF_A_REPOSITORY_API = List.of(
        "ApiAdapterDecorator.toRepository",
        "ApiMapper.toRepository"
    );

    @Test
    @DisplayName("no path writing a repository API derives the guardian index from the definition it writes")
    void no_write_path_of_an_api_touches_the_guardian_index() {
        var withGuardianSteps = io.gravitee.apim.core.api.model.Api.builder()
            .id("api-1")
            .environmentId("environment-id")
            .originContext(new io.gravitee.rest.api.model.context.OriginContext.Management())
            .apiDefinitionHttpV4(
                io.gravitee.definition.model.v4.Api.builder()
                    .id("api-1")
                    .internalFlow(Flow.builder().request(List.of(GuardianSteps.of("guardian-1", "Credential leakage"))).build())
                    .build()
            )
            .build();

        assertSoftly(softly -> {
            softly
                .assertThat(ApiAdapter.INSTANCE.toRepository(withGuardianSteps).getGuardians())
                .as("index written by ApiAdapterDecorator.toRepository")
                .isNull();
            softly
                .assertThat(methodsWritingARepositoryApi())
                .as("paths writing a repository API, each of which must leave the index alone")
                .containsExactlyInAnyOrderElementsOf(WRITE_PATHS_OF_A_REPOSITORY_API);
        });
    }

    private static List<String> methodsWritingARepositoryApi() {
        return Stream.of(ApiAdapterDecorator.class, ApiMapper.class)
            .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
            .filter(ApiGuardiansIndexWritePathsTest::writesARepositoryApi)
            .map(method -> method.getDeclaringClass().getSimpleName() + "." + method.getName())
            .distinct()
            .toList();
    }

    private static boolean writesARepositoryApi(Method method) {
        return Modifier.isPublic(method.getModifiers()) && method.getReturnType() == Api.class;
    }
}
