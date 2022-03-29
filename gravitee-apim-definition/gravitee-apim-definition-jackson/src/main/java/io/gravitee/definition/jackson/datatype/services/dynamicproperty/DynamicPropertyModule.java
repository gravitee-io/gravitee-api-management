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
package io.gravitee.definition.jackson.datatype.services.dynamicproperty;

import io.gravitee.definition.jackson.datatype.GraviteeModule;
import io.gravitee.definition.jackson.datatype.services.dynamicproperty.ser.DynamicPropertySerializer;
import io.gravitee.definition.jackson.datatype.services.dynamicproperty.ser.http.HttpDynamicPropertyProviderConfigurationSerializer;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DynamicPropertyModule extends GraviteeModule {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public DynamicPropertyModule() {
        super(DynamicPropertyService.SERVICE_KEY);
        // first deserializers
        addDeserializer(
            DynamicPropertyService.class,
            new io.gravitee.definition.jackson.datatype.services.dynamicproperty.deser.DynamicPropertyDeserializer(
                DynamicPropertyService.class
            )
        );
        addDeserializer(
            HttpDynamicPropertyProviderConfiguration.class,
            new io.gravitee.definition.jackson.datatype.services.dynamicproperty.deser.http.HttpDynamicPropertyProviderConfigurationDeserializer(
                HttpDynamicPropertyProviderConfiguration.class
            )
        );

        // then serializers:
        addSerializer(DynamicPropertyService.class, new DynamicPropertySerializer(DynamicPropertyService.class));
        addSerializer(
            HttpDynamicPropertyProviderConfiguration.class,
            new HttpDynamicPropertyProviderConfigurationSerializer(HttpDynamicPropertyProviderConfiguration.class)
        );
    }
}
