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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.HttpResponse;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
public class DebugApiV4 implements DebugApiProxy, Serializable {

    private Api apiDefinition;
    private HttpRequest request;
    private HttpResponse response;
    private HttpResponse backendResponse;

    private List<DebugStep> debugSteps;
    private PreprocessorStep preprocessorStep;

    private DebugMetrics metrics;

    public DebugApiV4(Api apiDefinition, HttpRequest request) {
        this.apiDefinition = apiDefinition;
        this.request = request;
    }

    public DebugApiV4(Api apiDefinition, HttpRequest request, HttpResponse response) {
        this.apiDefinition = apiDefinition;
        this.request = request;
        this.response = response;
    }

    @Override
    @JsonIgnore
    public String getId() {
        return apiDefinition.getId();
    }

    @Override
    @JsonIgnore
    public DefinitionVersion getDefinitionVersion() {
        return apiDefinition.getDefinitionVersion();
    }

    @Override
    @JsonIgnore
    public ApiType getType() {
        return apiDefinition.getType();
    }

    @Override
    @JsonIgnore
    public Set<String> getTags() {
        return apiDefinition.getTags();
    }
}
