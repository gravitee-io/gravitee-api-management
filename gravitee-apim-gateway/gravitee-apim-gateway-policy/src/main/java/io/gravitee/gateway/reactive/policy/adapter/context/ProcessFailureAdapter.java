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
package io.gravitee.gateway.reactive.policy.adapter.context;

import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.policy.api.PolicyResult;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProcessFailureAdapter implements ProcessorFailure {

    private int statusCode;
    private String message;
    private String key;
    private Map<String, Object> parameters;
    private String contentType;

    public ProcessFailureAdapter(final ExecutionFailure executionFailure) {
        this.statusCode = executionFailure.statusCode();
        this.message = executionFailure.message();
        this.key = executionFailure.key();
        this.parameters = executionFailure.parameters();
        this.contentType = executionFailure.contentType();
    }

    public ProcessFailureAdapter(final ProcessorFailure processorFailure) {
        this.statusCode = processorFailure.statusCode();
        this.message = processorFailure.message();
        this.key = processorFailure.key();
        this.parameters = processorFailure.parameters();
        this.contentType = processorFailure.contentType();
    }

    public ProcessFailureAdapter(final PolicyResult policyResult) {
        this.statusCode = policyResult.statusCode();
        this.message = policyResult.message();
        this.key = policyResult.key();
        this.parameters = policyResult.parameters();
        this.contentType = policyResult.contentType();
    }

    public ExecutionFailure toExecutionFailure() {
        return new ExecutionFailure().key(key).statusCode(statusCode).message(message).parameters(parameters).contentType(contentType);
    }

    public int statusCode() {
        return statusCode;
    }

    public ProcessFailureAdapter statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String message() {
        return message;
    }

    public ProcessFailureAdapter message(String message) {
        this.message = message;
        return this;
    }

    public String key() {
        return key;
    }

    public ProcessFailureAdapter key(String key) {
        this.key = key;
        return this;
    }

    public Map<String, Object> parameters() {
        return parameters;
    }

    public ProcessFailureAdapter parameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        return this;
    }

    public String contentType() {
        return contentType;
    }

    public ProcessFailureAdapter contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }
}
