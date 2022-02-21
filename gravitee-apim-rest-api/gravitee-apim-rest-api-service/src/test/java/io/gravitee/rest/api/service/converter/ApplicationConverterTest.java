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
package io.gravitee.rest.api.service.converter;

import static org.junit.Assert.assertSame;

import io.gravitee.repository.management.model.Application;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import org.junit.Test;

public class ApplicationConverterTest {

    ApplicationConverter applicationConverter = new ApplicationConverter();

    @Test
    public void newApplicationEntity_toApplication_should_convert_ApiKeyMode() {
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setApiKeyMode(ApiKeyMode.EXCLUSIVE);

        Application application = applicationConverter.toApplication(newApplicationEntity);

        assertSame(io.gravitee.repository.management.model.ApiKeyMode.EXCLUSIVE, application.getApiKeyMode());
    }

    @Test
    public void newApplicationEntity_toApplication_should_set_unspecified_byDefault_if_null_ApiKeyMode() {
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setApiKeyMode(null);

        Application application = applicationConverter.toApplication(newApplicationEntity);

        assertSame(io.gravitee.repository.management.model.ApiKeyMode.UNSPECIFIED, application.getApiKeyMode());
    }

    @Test
    public void updateApplicationEntity_toApplication_should_convert_ApiKeyMode() {
        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        updateApplicationEntity.setApiKeyMode(ApiKeyMode.EXCLUSIVE);

        Application application = applicationConverter.toApplication(updateApplicationEntity);

        assertSame(io.gravitee.repository.management.model.ApiKeyMode.EXCLUSIVE, application.getApiKeyMode());
    }

    @Test
    public void updateApplicationEntity_toApplication_should_set_unspecified_byDefault_if_null_ApiKeyMode() {
        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        updateApplicationEntity.setApiKeyMode(null);

        Application application = applicationConverter.toApplication(updateApplicationEntity);

        assertSame(io.gravitee.repository.management.model.ApiKeyMode.UNSPECIFIED, application.getApiKeyMode());
    }
}
