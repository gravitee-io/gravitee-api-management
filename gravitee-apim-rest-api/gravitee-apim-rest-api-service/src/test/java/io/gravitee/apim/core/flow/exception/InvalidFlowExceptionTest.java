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
package io.gravitee.apim.core.flow.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.definition.model.v4.ApiType;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class InvalidFlowExceptionTest {

    @Test
    void missingPathOperator_should_not_throw_when_flow_name_is_null() {
        var throwable = catchThrowable(() -> InvalidFlowException.missingPathOperator(null));

        assertThat(throwable).isNull();
        assertThat(InvalidFlowException.missingPathOperator(null).getParameters()).doesNotContainKey("flowName");
    }

    @Test
    void invalidEntrypoint_should_not_throw_when_flow_name_is_null() {
        var throwable = catchThrowable(() -> InvalidFlowException.invalidEntrypoint(null, Set.of("sse")));

        assertThat(throwable).isNull();
        assertThat(InvalidFlowException.invalidEntrypoint(null, Set.of("sse")).getParameters())
            .doesNotContainKey("flowName")
            .containsEntry("invalidEntrypoints", "sse");
    }

    @Test
    void invalidSelector_should_not_throw_when_flow_name_is_null() {
        var throwable = catchThrowable(() -> InvalidFlowException.invalidSelector(null, ApiType.PROXY, Set.of("channel")));

        assertThat(throwable).isNull();
        assertThat(InvalidFlowException.invalidSelector(null, ApiType.PROXY, Set.of("channel")).getParameters())
            .doesNotContainKey("flowName")
            .containsEntry("invalidSelectors", "channel");
    }

    @Test
    void duplicatedSelector_should_not_throw_when_flow_name_is_null() {
        var throwable = catchThrowable(() -> InvalidFlowException.duplicatedSelector(null, Set.of("http")));

        assertThat(throwable).isNull();
        assertThat(InvalidFlowException.duplicatedSelector(null, Set.of("http")).getParameters())
            .doesNotContainKey("flowName")
            .containsEntry("duplicatedSelectors", "http");
    }
}
