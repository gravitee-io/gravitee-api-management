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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.repository.management.model.Api.AuditEvent.API_UPDATED;
import static java.util.Optional.of;

import com.google.common.base.Strings;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiImagesService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiService;
import jakarta.xml.bind.DatatypeConverter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiImagesServiceImpl implements ApiImagesService {

    @Value("${configuration.default-api-icon:}")
    private String defaultApiIcon;

    private final ApiRepository apiRepository;
    private final AuditService auditService;

    public ApiImagesServiceImpl(@Lazy ApiRepository apiRepository, AuditService auditService) {
        this.apiRepository = apiRepository;
        this.auditService = auditService;
    }

    @Override
    public InlinePictureEntity getApiPicture(ExecutionContext executionContext, String apiId) {
        Api api;
        try {
            api = findApi(executionContext, apiId);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find an API using its ID: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API using its ID: " + apiId, ex);
        }

        InlinePictureEntity imageEntity = new InlinePictureEntity();
        String picture = api.getPicture();
        if (picture != null) {
            convertImage(imageEntity, picture);
        } else {
            getDefaultPicture().ifPresent(content -> {
                imageEntity.setType("image/png");
                imageEntity.setContent(content);
            });
        }
        return imageEntity;
    }

    @Override
    public void updateApiPicture(ExecutionContext executionContext, String apiId, String picture) {
        try {
            Api apiToUpdate = findApi(executionContext, apiId);
            Api newApi = apiToUpdate.withPicture(picture).withUpdatedAt(new Date());

            apiRepository.update(newApi);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                apiId,
                Collections.emptyMap(),
                API_UPDATED,
                newApi.getUpdatedAt(),
                apiToUpdate,
                newApi
            );
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find an API using its ID: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API using its ID: " + apiId, ex);
        }
    }

    @Override
    public InlinePictureEntity getApiBackground(ExecutionContext executionContext, String apiId) {
        Api api;
        try {
            api = findApi(executionContext, apiId);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find an API using its ID: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API using its ID: " + apiId, ex);
        }

        InlinePictureEntity imageEntity = new InlinePictureEntity();
        String background = api.getBackground();
        if (background != null) {
            convertImage(imageEntity, background);
        }

        return imageEntity;
    }

    @Override
    public void updateApiBackground(ExecutionContext executionContext, String apiId, String background) {
        try {
            Api apiToUpdate = findApi(executionContext, apiId);
            Api newApi = apiToUpdate.withBackground(background).withUpdatedAt(new Date());

            apiRepository.update(newApi);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                apiId,
                Collections.emptyMap(),
                API_UPDATED,
                newApi.getUpdatedAt(),
                apiToUpdate,
                newApi
            );
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find an API using its ID: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API using its ID: " + apiId, ex);
        }
    }

    private Api findApi(ExecutionContext executionContext, String apiId) throws TechnicalException {
        log.debug("Find API by ID: {}", apiId);

        Optional<Api> optApi = apiRepository.findById(apiId);

        if (executionContext.hasEnvironmentId()) {
            optApi = optApi.filter(result -> executionContext.getEnvironmentId().equals(result.getEnvironmentId()));
        }

        Api api = optApi.orElseThrow(() -> new ApiNotFoundException(apiId));
        return api;
    }

    private static void convertImage(InlinePictureEntity imageEntity, String picture) {
        String[] parts = picture.split(";", 2);
        imageEntity.setType(parts[0].split(":")[1]);
        String base64Content = picture.split(",", 2)[1];
        imageEntity.setContent(DatatypeConverter.parseBase64Binary(base64Content));
    }

    private Optional<byte[]> getDefaultPicture() {
        Optional<byte[]> content = Optional.empty();
        if (!Strings.isNullOrEmpty(defaultApiIcon)) {
            try {
                content = of(IOUtils.toByteArray(new FileInputStream(defaultApiIcon)));
            } catch (IOException ioe) {
                log.error("Default icon for API does not exist", ioe);
            }
        }
        return content;
    }
}
