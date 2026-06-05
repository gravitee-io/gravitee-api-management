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
package io.gravitee.rest.api.management.rest.spring;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.use_case.PatchApiUseCase.ApiV4Deserializer;
import io.gravitee.apim.core.api.use_case.PatchApiUseCase.ApiV4Fields;
import io.gravitee.apim.core.plan.use_case.PatchPlanUseCase.PlanFlowsConverter;
import io.gravitee.apim.infra.spring.UsecaseSpringConfiguration;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.el.ExpressionLanguageInitializer;
import io.gravitee.plugin.core.spring.PluginConfiguration;
import io.gravitee.rest.api.idp.core.spring.IdentityProviderPluginConfiguration;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import({ ServiceConfiguration.class, UsecaseSpringConfiguration.class })
@EnableAsync
public class RestManagementConfiguration {

    @Bean
    public ApiV4Deserializer apiV4Deserializer() {
        return new V4PatchNotSupportedDeserializer();
    }

    @Bean
    public PlanFlowsConverter planFlowsDeserializer() {
        return new PlanFlowsPatchNotSupportedDeserializer();
    }

    @Bean
    public ExpressionLanguageInitializer expressionLanguageInitializer() {
        return new ExpressionLanguageInitializer();
    }

    private static class V4PatchNotSupportedDeserializer implements ApiV4Deserializer {

        private static final String MESSAGE = "v4 PATCH is served only by management-v2 REST API";

        @Override
        public JsonNode toCurrentStateNode(Api api) {
            throw new UnsupportedOperationException(MESSAGE + " (apiId=" + api.getId() + ")");
        }

        @Override
        public ApiV4Fields fromPatchedNode(JsonNode patchedNode) {
            throw new UnsupportedOperationException(MESSAGE);
        }
    }

    private static class PlanFlowsPatchNotSupportedDeserializer implements PlanFlowsConverter {

        private static final String MESSAGE = "plan flows PATCH is served only by management-v2 REST API";

        @Override
        public JsonNode toCurrentFlowsNode(List<Flow> flows) {
            throw new UnsupportedOperationException(MESSAGE);
        }

        @Override
        public List<Flow> fromPatchedFlowsNode(JsonNode flowsNode) {
            throw new UnsupportedOperationException(MESSAGE);
        }
    }
}
