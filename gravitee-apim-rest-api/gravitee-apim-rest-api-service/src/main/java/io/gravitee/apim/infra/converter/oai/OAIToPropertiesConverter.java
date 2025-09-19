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
package io.gravitee.apim.infra.converter.oai;

import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.definition.model.v4.property.Property;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OAIToPropertiesConverter {

    public static OAIToPropertiesConverter INSTANCE = new OAIToPropertiesConverter();

    public List<Property> convert(List<io.gravitee.rest.api.service.swagger.converter.extension.Property> properties) {
        if (CollectionUtils.isEmpty(properties)) {
            return Collections.emptyList();
        }

        return properties
            .stream()
            .map(oaiProperty ->
                Property.builder()
                    .key(oaiProperty.getKey())
                    .value(oaiProperty.getValue())
                    .encrypted(Boolean.parseBoolean(String.valueOf(oaiProperty.getAdditionalProperties().get("encrypted"))))
                    .build()
            )
            .collect(Collectors.toList());
    }
}
