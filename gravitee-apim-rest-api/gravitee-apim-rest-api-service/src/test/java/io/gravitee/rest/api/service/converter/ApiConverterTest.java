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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiConverterTest {

    @InjectMocks
    private ApiConverter apiConverter;

    @Test
    public void toUpdateApiEntity_should_keep_crossId() {
        ApiEntity apiEntity = buildTestApiEntity();
        apiEntity.setCrossId("test-cross-id");

        UpdateApiEntity updateApiEntity = apiConverter.toUpdateApiEntity(apiEntity);

        assertEquals("test-cross-id", updateApiEntity.getCrossId());
    }

    @Test
    public void toUpdateApiEntity_should_reset_crossId_if_param_set_to_true() {
        ApiEntity apiEntity = buildTestApiEntity();
        apiEntity.setCrossId("test-cross-id");

        UpdateApiEntity updateApiEntity = apiConverter.toUpdateApiEntity(apiEntity, true);

        assertNull("test-cross-id", updateApiEntity.getCrossId());
    }

    private ApiEntity buildTestApiEntity() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        return apiEntity;
    }
}
