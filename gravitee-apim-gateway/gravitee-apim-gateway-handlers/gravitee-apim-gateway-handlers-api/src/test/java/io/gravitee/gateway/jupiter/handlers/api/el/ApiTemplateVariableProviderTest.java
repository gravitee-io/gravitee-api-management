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
package io.gravitee.gateway.jupiter.handlers.api.el;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.el.TemplateEngine;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiTemplateVariableProviderTest {

    @Test
    void should_provide_api_id_in_EL() {
        var apiDefinition = Api.builder().id("api#id").build();

        TemplateEngine engine = buildTemplateEngine(apiDefinition);
        engine.eval("{#api.id}", String.class).test().assertValue("api#id");
    }

    @Test
    void should_provide_api_name_in_EL() {
        var apiDefinition = Api.builder().name("api#name").build();

        TemplateEngine engine = buildTemplateEngine(apiDefinition);
        engine.eval("{#api.name}", String.class).test().assertValue("api#name");
    }

    @Test
    void should_provide_api_version_in_EL() {
        var apiDefinition = Api.builder().apiVersion("api#version").build();

        TemplateEngine engine = buildTemplateEngine(apiDefinition);
        engine.eval("{#api.version}", String.class).test().assertValue("api#version");
    }

    @Test
    void should_provide_api_properties_in_EL() {
        var apiDefinition = Api.builder().properties(Map.of("prop1", "value1", "prop2", "value2")).build();

        TemplateEngine engine = buildTemplateEngine(apiDefinition);
        engine.eval("{#api.properties[prop1]}", String.class).test().assertValue("value1");
        engine.eval("{#api.properties[prop2]}", String.class).test().assertValue("value2");
    }

    private static TemplateEngine buildTemplateEngine(Api apiDefinition) {
        var engine = TemplateEngine.templateEngine();
        var apiContextProvider = new ApiTemplateVariableProvider(new io.gravitee.gateway.jupiter.handlers.api.v4.Api(apiDefinition));
        apiContextProvider.provide(engine.getTemplateContext());
        return engine;
    }
}
