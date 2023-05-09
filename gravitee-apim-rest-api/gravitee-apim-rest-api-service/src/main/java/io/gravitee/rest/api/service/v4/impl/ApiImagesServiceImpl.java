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

import static java.util.Optional.of;

import com.google.common.base.Strings;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiImagesService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiImagesServiceImpl implements ApiImagesService {

    @Value("${configuration.default-api-icon:}")
    private String defaultApiIcon;

    private ApiSearchService apiSearchService;

    public ApiImagesServiceImpl(ApiSearchService apiSearchService) {
        this.apiSearchService = apiSearchService;
    }

    @Override
    public InlinePictureEntity getApiPicture(ExecutionContext executionContext, String apiId) {
        Api api = apiSearchService.findRepositoryApiById(executionContext, apiId);
        InlinePictureEntity imageEntity = new InlinePictureEntity();
        String picture = api.getPicture();
        if (picture != null) {
            convertImage(imageEntity, picture);
        } else {
            getDefaultPicture()
                .ifPresent(content -> {
                    imageEntity.setType("image/png");
                    imageEntity.setContent(content);
                });
        }
        return imageEntity;
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

    @Override
    public InlinePictureEntity getApiBackground(ExecutionContext executionContext, String apiId) {
        Api api = apiSearchService.findRepositoryApiById(executionContext, apiId);
        InlinePictureEntity imageEntity = new InlinePictureEntity();
        String background = api.getBackground();
        if (background != null) {
            convertImage(imageEntity, background);
        }

        return imageEntity;
    }
}
