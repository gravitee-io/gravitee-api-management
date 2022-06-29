/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.healthcheck.eval.assertion;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.services.healthcheck.HealthCheckResponse;
import io.gravitee.gateway.services.healthcheck.eval.EvaluationException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AssertionEvaluationTest {

    @Test
    public void shouldNotValidate_singleCondition() throws EvaluationException {
        String assertion = "'toto' == 'tata'";
        AssertionEvaluation evaluation = new AssertionEvaluation(assertion);

        boolean result = evaluation.validate();
        Assert.assertFalse(result);
    }

    @Test
    public void shouldNotValidate_multipleCondition() throws EvaluationException {
        String assertion = "'toto' == 'toto' && 1 == 2";
        AssertionEvaluation evaluation = new AssertionEvaluation(assertion);

        boolean result = evaluation.validate();
        Assert.assertFalse(result);
    }

    @Test
    public void shouldValidate_simpleHttpCondition() throws EvaluationException {
        String assertion = HealthCheckResponse.DEFAULT_ASSERTION;
        AssertionEvaluation evaluation = new AssertionEvaluation(assertion);
        EvaluableHttpResponse response = new EvaluableHttpResponse();
        response.status = HttpStatusCode.OK_200;

        evaluation.setVariable("response", response);
        boolean result = evaluation.validate();
        Assert.assertTrue(result);
    }

    @Test
    public void shouldValidate_jsonPathCondition() throws EvaluationException {
        String assertion = "#jsonPath(#response.content, '$.status') == 'green'";
        AssertionEvaluation evaluation = new AssertionEvaluation(assertion);
        EvaluableHttpResponse response = new EvaluableHttpResponse();
        response.status = HttpStatusCode.OK_200;
        response.content = "{\"status\": \"green\"}";

        evaluation.setVariable("response", response);
        boolean result = evaluation.validate();
        Assert.assertTrue(result);
    }

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
