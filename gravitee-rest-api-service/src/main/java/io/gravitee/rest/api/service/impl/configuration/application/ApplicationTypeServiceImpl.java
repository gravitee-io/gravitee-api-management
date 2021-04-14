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
package io.gravitee.rest.api.service.impl.configuration.application;

import static java.nio.charset.Charset.defaultCharset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.PortalConfigEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypeEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypesEntity;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.exceptions.ApplicationTypeNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApplicationTypeServiceImpl implements ApplicationTypeService {

    public static final String DEFINITION_PATH = "/applications/types.json";

    @Autowired
    private ConfigService configService;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ApplicationTypesEntity getEnabledApplicationTypes() {
        JsonNode jsonTypes = this.getApplicationTypesConfiguration();
        return getFilteredApplicationTypes(jsonTypes);
    }

    @Override
    public ApplicationTypeEntity getApplicationType(String typeId) {
        return this.getApplicationTypesEntity()
            .getData()
            .stream()
            .filter(typeEntity -> typeEntity.getId().equals(typeId.toLowerCase()))
            .findFirst()
            .orElseThrow(() -> new ApplicationTypeNotFoundException(typeId));
    }

    private ApplicationTypesEntity getApplicationTypesEntity() {
        try {
            InputStream resourceAsStream = this.getClass().getResourceAsStream(DEFINITION_PATH);
            String rawJson = IOUtils.toString(resourceAsStream, defaultCharset());
            return objectMapper.readValue(rawJson, ApplicationTypesEntity.class);
        } catch (IOException e) {
            throw new TechnicalManagementException("An error occurs while trying load application type definition", e);
        }
    }

    public ApplicationTypesEntity getFilteredApplicationTypes(JsonNode jsonTypes) {
        ApplicationTypesEntity applicationTypesEntity = this.getApplicationTypesEntity();

        List<ApplicationTypeEntity> filteredData = applicationTypesEntity
            .getData()
            .stream()
            .filter(typeEntity -> jsonTypes.get(typeEntity.getId()).get("enabled").asBoolean(false))
            .collect(Collectors.toList());

        applicationTypesEntity.setData(filteredData);
        return applicationTypesEntity;
    }

    public JsonNode getApplicationTypesConfiguration() {
        PortalConfigEntity.Application applicationConfig = configService.getPortalConfig().getApplication();
        PortalConfigEntity.Application.ApplicationTypes types = applicationConfig.getTypes();
        if (!applicationConfig.getRegistration().getEnabled()) {
            types.getBrowserType().setEnabled(false);
            types.getBackendToBackendType().setEnabled(false);
            types.getNativeType().setEnabled(false);
            types.getWebType().setEnabled(false);
        }
        return objectMapper.convertValue(types, JsonNode.class);
    }
}
