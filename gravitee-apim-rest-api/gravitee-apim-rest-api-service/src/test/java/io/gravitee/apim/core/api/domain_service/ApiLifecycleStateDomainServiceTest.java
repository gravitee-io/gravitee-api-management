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

import inmemory.WorkflowQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.InvalidApiLifecycleStateException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.workflow.model.Workflow;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiLifecycleStateDomainServiceTest {

    WorkflowQueryServiceInMemory workflowQueryService = new WorkflowQueryServiceInMemory();

    private ApiLifecycleStateDomainService cut;

    private static final String API_ID_DRAFT = "api-id-draft";
    private static final String API_ID_REVIEW = "api-id-review";

    @BeforeEach
    void setUp() {
        workflowQueryService.initWith(
            List.of(
                Workflow
                    .builder()
                    .id("workflow-draft")
                    .referenceId(API_ID_DRAFT)
                    .referenceType(Workflow.ReferenceType.API)
                    .type(Workflow.Type.REVIEW)
                    .state(Workflow.State.DRAFT)
                    .build(),
                Workflow
                    .builder()
                    .id("workflow-review")
                    .referenceId(API_ID_REVIEW)
                    .referenceType(Workflow.ReferenceType.API)
                    .type(Workflow.Type.REVIEW)
                    .state(Workflow.State.IN_REVIEW)
                    .build()
            )
        );
        cut = new ApiLifecycleStateDomainService(workflowQueryService);
    }

    @ParameterizedTest
    @MethodSource("provideParametersNoError")
    void can_validate_and_sanitize_without_errors(
        Api.ApiLifecycleState currentState,
        Api.ApiLifecycleState newState,
        Api.ApiLifecycleState expectedResult
    ) {
        var result = cut.validateAndSanitizeForUpdate(API_ID_DRAFT, currentState, newState);
        assertThat(result).isEqualTo(expectedResult);
    }

    public static Stream<Arguments> provideParametersNoError() {
        return Stream.of(
            // CREATED
            Arguments.of(Api.ApiLifecycleState.CREATED, null, Api.ApiLifecycleState.CREATED),
            Arguments.of(Api.ApiLifecycleState.CREATED, Api.ApiLifecycleState.CREATED, Api.ApiLifecycleState.CREATED),
            Arguments.of(Api.ApiLifecycleState.CREATED, Api.ApiLifecycleState.PUBLISHED, Api.ApiLifecycleState.PUBLISHED),
            Arguments.of(Api.ApiLifecycleState.CREATED, Api.ApiLifecycleState.UNPUBLISHED, Api.ApiLifecycleState.UNPUBLISHED),
            Arguments.of(Api.ApiLifecycleState.CREATED, Api.ApiLifecycleState.ARCHIVED, Api.ApiLifecycleState.ARCHIVED),
            Arguments.of(Api.ApiLifecycleState.CREATED, Api.ApiLifecycleState.DEPRECATED, Api.ApiLifecycleState.DEPRECATED),
            // UNPUBLISHED
            Arguments.of(Api.ApiLifecycleState.UNPUBLISHED, null, Api.ApiLifecycleState.UNPUBLISHED),
            Arguments.of(Api.ApiLifecycleState.UNPUBLISHED, Api.ApiLifecycleState.PUBLISHED, Api.ApiLifecycleState.PUBLISHED),
            Arguments.of(Api.ApiLifecycleState.UNPUBLISHED, Api.ApiLifecycleState.UNPUBLISHED, Api.ApiLifecycleState.UNPUBLISHED),
            Arguments.of(Api.ApiLifecycleState.UNPUBLISHED, Api.ApiLifecycleState.ARCHIVED, Api.ApiLifecycleState.ARCHIVED),
            Arguments.of(Api.ApiLifecycleState.UNPUBLISHED, Api.ApiLifecycleState.DEPRECATED, Api.ApiLifecycleState.DEPRECATED),
            // PUBLISHED
            Arguments.of(Api.ApiLifecycleState.PUBLISHED, null, Api.ApiLifecycleState.PUBLISHED),
            Arguments.of(Api.ApiLifecycleState.PUBLISHED, Api.ApiLifecycleState.PUBLISHED, Api.ApiLifecycleState.PUBLISHED),
            Arguments.of(Api.ApiLifecycleState.PUBLISHED, Api.ApiLifecycleState.UNPUBLISHED, Api.ApiLifecycleState.UNPUBLISHED),
            Arguments.of(Api.ApiLifecycleState.PUBLISHED, Api.ApiLifecycleState.ARCHIVED, Api.ApiLifecycleState.ARCHIVED),
            Arguments.of(Api.ApiLifecycleState.PUBLISHED, Api.ApiLifecycleState.DEPRECATED, Api.ApiLifecycleState.DEPRECATED),
            // ARCHIVED
            Arguments.of(Api.ApiLifecycleState.ARCHIVED, null, Api.ApiLifecycleState.ARCHIVED),
            Arguments.of(Api.ApiLifecycleState.ARCHIVED, Api.ApiLifecycleState.ARCHIVED, Api.ApiLifecycleState.ARCHIVED),
            // DEPRECATED
            Arguments.of(Api.ApiLifecycleState.DEPRECATED, null, Api.ApiLifecycleState.DEPRECATED)
        );
    }

    @ParameterizedTest
    @MethodSource("provideParametersWithError")
    void validate_and_sanitize_throw_error(String apiId, Api.ApiLifecycleState currentState, Api.ApiLifecycleState newState) {
        var throwable = catchThrowable(() -> cut.validateAndSanitizeForUpdate(apiId, currentState, newState));
        assertThat(throwable).isInstanceOf(InvalidApiLifecycleStateException.class);
        assertThat(throwable.getMessage()).contains(newState.name());
    }

    public static Stream<Arguments> provideParametersWithError() {
        return Stream.of(
            // CREATED
            Arguments.of(API_ID_REVIEW, Api.ApiLifecycleState.CREATED, Api.ApiLifecycleState.UNPUBLISHED),
            // UNPUBLISHED
            Arguments.of(API_ID_DRAFT, Api.ApiLifecycleState.UNPUBLISHED, Api.ApiLifecycleState.CREATED),
            // PUBLISHED
            Arguments.of(API_ID_DRAFT, Api.ApiLifecycleState.PUBLISHED, Api.ApiLifecycleState.CREATED),
            // ARCHIVED
            Arguments.of(API_ID_DRAFT, Api.ApiLifecycleState.ARCHIVED, Api.ApiLifecycleState.CREATED),
            Arguments.of(API_ID_DRAFT, Api.ApiLifecycleState.ARCHIVED, Api.ApiLifecycleState.UNPUBLISHED),
            Arguments.of(API_ID_DRAFT, Api.ApiLifecycleState.ARCHIVED, Api.ApiLifecycleState.PUBLISHED),
            Arguments.of(API_ID_DRAFT, Api.ApiLifecycleState.ARCHIVED, Api.ApiLifecycleState.DEPRECATED),
            // DEPRECATED
            Arguments.of(API_ID_DRAFT, Api.ApiLifecycleState.DEPRECATED, Api.ApiLifecycleState.CREATED),
            Arguments.of(API_ID_DRAFT, Api.ApiLifecycleState.DEPRECATED, Api.ApiLifecycleState.UNPUBLISHED),
            Arguments.of(API_ID_DRAFT, Api.ApiLifecycleState.DEPRECATED, Api.ApiLifecycleState.PUBLISHED),
            Arguments.of(API_ID_DRAFT, Api.ApiLifecycleState.DEPRECATED, Api.ApiLifecycleState.ARCHIVED),
            Arguments.of(API_ID_DRAFT, Api.ApiLifecycleState.DEPRECATED, Api.ApiLifecycleState.DEPRECATED)
        );
    }
}
