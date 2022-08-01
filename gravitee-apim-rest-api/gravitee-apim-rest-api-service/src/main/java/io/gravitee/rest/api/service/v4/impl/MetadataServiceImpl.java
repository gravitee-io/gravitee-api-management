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
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.v4.MetadataService;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("MetadataServiceImplV4")
public class MetadataServiceImpl extends TransactionalService implements MetadataService {

    private final ApiMetadataService apiMetadataService;
    private final NotificationTemplateService notificationTemplateService;

    public MetadataServiceImpl(final ApiMetadataService apiMetadataService, final NotificationTemplateService notificationTemplateService) {
        this.apiMetadataService = apiMetadataService;
        this.notificationTemplateService = notificationTemplateService;
    }

    @Override
    public Map<String, Object> getMetadataForApi(ExecutionContext executionContext, ApiEntity apiEntity) {
        List<ApiMetadataEntity> metadataList = apiMetadataService.findAllByApi(apiEntity.getId());
        final Map<String, Object> mapMetadata = new HashMap<>(metadataList.size());

        metadataList.forEach(metadata ->
            mapMetadata.put(metadata.getKey(), metadata.getValue() == null ? metadata.getDefaultValue() : metadata.getValue())
        );

        String decodedValue =
            this.notificationTemplateService.resolveInlineTemplateWithParam(
                    executionContext.getOrganizationId(),
                    apiEntity.getId(),
                    new StringReader(mapMetadata.toString()),
                    Collections.singletonMap("api", apiEntity)
                );
        return Arrays
            .stream(decodedValue.substring(1, decodedValue.length() - 1).split(", "))
            .map(entry -> entry.split("="))
            .collect(Collectors.toMap(entry -> entry[0], entry -> entry.length > 1 ? entry[1] : ""));
    }
}
