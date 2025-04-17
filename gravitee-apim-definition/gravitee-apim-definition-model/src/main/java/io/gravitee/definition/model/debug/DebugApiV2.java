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
package io.gravitee.definition.model.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.HttpResponse;
import java.io.Serializable;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
public class DebugApiV2 extends Api implements Serializable {

    @JsonProperty("request")
    private HttpRequest request;

    @JsonProperty("response")
    private HttpResponse response;

    @JsonProperty("debugSteps")
    private List<DebugStep> debugSteps;

    @JsonProperty("preprocessorStep")
    private PreprocessorStep preprocessorStep;

    @JsonProperty("backendResponse")
    private HttpResponse backendResponse;

    @JsonProperty("metrics")
    private DebugMetrics metrics;

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    public List<DebugStep> getDebugSteps() {
        return debugSteps;
    }

    public void setDebugSteps(List<DebugStep> debugSteps) {
        this.debugSteps = debugSteps;
    }

    public HttpResponse getBackendResponse() {
        return backendResponse;
    }

    public void setBackendResponse(HttpResponse backendResponse) {
        this.backendResponse = backendResponse;
    }

    public PreprocessorStep getPreprocessorStep() {
        return preprocessorStep;
    }

    public void setPreprocessorStep(PreprocessorStep preprocessorStep) {
        this.preprocessorStep = preprocessorStep;
    }

    public DebugMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(DebugMetrics metrics) {
        this.metrics = metrics;
    }
}
