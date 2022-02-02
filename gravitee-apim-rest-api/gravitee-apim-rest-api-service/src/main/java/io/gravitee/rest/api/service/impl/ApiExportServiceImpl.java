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
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.service.ApiExportService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PageConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class ApiExportServiceImpl extends AbstractService implements ApiExportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExportServiceImpl.class);

    private final ObjectMapper objectMapper;
    private final PageService pageService;
    private final PlanService planService;
    private final ApiService apiService;
    private final ApiConverter apiConverter;
    private final PlanConverter planConverter;
    private final PageConverter pageConverter;

    public ApiExportServiceImpl(
        ObjectMapper objectMapper,
        PageService pageService,
        PlanService planService,
        ApiService apiService,
        ApiConverter apiConverter,
        PlanConverter planConverter,
        PageConverter pageConverter
    ) {
        this.objectMapper = objectMapper;
        this.pageService = pageService;
        this.planService = planService;
        this.apiService = apiService;
        this.apiConverter = apiConverter;
        this.planConverter = planConverter;
        this.pageConverter = pageConverter;
    }

    @Override
    public String exportAsJson(final String apiId, String exportVersion, String... filteredFields) {
        ApiEntity apiEntity = apiService.findById(apiId);
        generateAndSaveCrossId(apiEntity);
        // set metadata for serialize process
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ApiSerializer.METADATA_EXPORT_VERSION, exportVersion);
        metadata.put(ApiSerializer.METADATA_FILTERED_FIELDS_LIST, Arrays.asList(filteredFields));
        apiEntity.setMetadata(metadata);

        try {
            return objectMapper.writeValueAsString(apiEntity);
        } catch (final Exception e) {
            LOGGER.error("An error occurs while trying to JSON serialize the API {}", apiEntity, e);
        }
        return "";
    }

    private void generateAndSaveCrossId(ApiEntity api) {
        if (StringUtils.isEmpty(api.getCrossId())) {
            api.setCrossId(UuidString.generateRandom());
            apiService.update(api.getId(), apiConverter.toUpdateApiEntity(api));
        }
        planService.findByApi(api.getId()).forEach(this::generateAndSaveCrossId);
        pageService.findByApi(api.getId()).forEach(this::generateAndSaveCrossId);
    }

    private void generateAndSaveCrossId(PlanEntity plan) {
        if (StringUtils.isEmpty(plan.getCrossId())) {
            plan.setCrossId(UuidString.generateRandom());
            planService.update(planConverter.toUpdatePlanEntity(plan));
        }
    }

    private void generateAndSaveCrossId(PageEntity page) {
        if (StringUtils.isEmpty(page.getCrossId())) {
            page.setCrossId(UuidString.generateRandom());
            pageService.update(page.getId(), pageConverter.toUpdatePageEntity(page));
        }
    }
}
