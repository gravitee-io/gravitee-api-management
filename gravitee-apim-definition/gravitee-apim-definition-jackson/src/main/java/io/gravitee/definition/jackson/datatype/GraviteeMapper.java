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
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.gravitee.definition.jackson.datatype.api.ApiModule;
import io.gravitee.definition.jackson.datatype.api.DebugApiModule;
import io.gravitee.definition.jackson.datatype.plugins.resource.ResourceModule;
import io.gravitee.definition.jackson.datatype.services.core.ServiceModule;
import io.gravitee.definition.jackson.datatype.services.discovery.EndpointDiscoveryModule;
import io.gravitee.definition.jackson.datatype.services.dynamicproperty.DynamicPropertyModule;
import io.gravitee.definition.jackson.datatype.services.healthcheck.HealthCheckModule;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeMapper extends ObjectMapper {

    private static final long serialVersionUID = 1L;

    public GraviteeMapper() {
        registerModule(new ApiModule(this));
        registerModule(new ServiceModule());
        registerModule(new HealthCheckModule());
        registerModule(new DynamicPropertyModule());
        registerModule(new EndpointDiscoveryModule());
        registerModule(new ResourceModule());
        registerModule(new DebugApiModule());

        enable(SerializationFeature.INDENT_OUTPUT);
        enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        setSerializationInclusion(JsonInclude.Include.NON_NULL);
        enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
