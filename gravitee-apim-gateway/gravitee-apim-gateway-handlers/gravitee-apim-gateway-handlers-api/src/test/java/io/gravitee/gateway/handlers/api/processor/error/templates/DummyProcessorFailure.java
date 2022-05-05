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
package io.gravitee.gateway.handlers.api.processor.error.templates;

import io.gravitee.gateway.api.processor.ProcessorFailure;
import java.util.Map;

public class DummyProcessorFailure implements ProcessorFailure {

    private int statusCode;
    private String message;
    private String key;
    private Map<String, Object> parameters;
    private String contentType;

    @Override
    public int statusCode() {
        return this.statusCode;
    }

    @Override
    public String message() {
        return this.message;
    }

    @Override
    public String key() {
        return this.key;
    }

    @Override
    public Map<String, Object> parameters() {
        return this.parameters;
    }

    @Override
    public String contentType() {
        return this.contentType;
    }

    public DummyProcessorFailure statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public DummyProcessorFailure message(String message) {
        this.message = message;
        return this;
    }

    public DummyProcessorFailure key(String key) {
        this.key = key;
        return this;
    }

    public DummyProcessorFailure parameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        return this;
    }

    public DummyProcessorFailure contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }
}
