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
package io.gravitee.definition.jackson.datatype;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.definition.jackson.datatype.api.ser.DeploymentRequiredRuleSerializer;
import io.gravitee.definition.jackson.datatype.api.ser.PolicySerializer;
import io.gravitee.definition.jackson.datatype.filter.GraviteeFilterProvider;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DeploymentRequiredMapper extends ObjectMapper {

    private static final long serialVersionUID = 1L;

    public DeploymentRequiredMapper() {
        this.setFilterProvider(new GraviteeFilterProvider(true));
        enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        setSerializationInclusion(JsonInclude.Include.NON_NULL);
        enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        SimpleModule module = new SimpleModule();
        module.addSerializer(Rule.class, new DeploymentRequiredRuleSerializer(Rule.class));
        module.addSerializer(Policy.class, new PolicySerializer(Policy.class));
        registerModule(module);
    }
}
