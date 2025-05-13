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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.CustomUserFieldEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConfigurationCustomFieldsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "configuration/users/custom-fields";
    }

    @Test
    public void shouldListCustomFields() {
        final CustomUserFieldEntity customUserFieldEntity = new CustomUserFieldEntity();
        customUserFieldEntity.setKey("key1");
        customUserFieldEntity.setLabel("label 1");
        customUserFieldEntity.setRequired(true);
        customUserFieldEntity.setValues(Arrays.asList("a", "b"));
        doReturn(List.of(customUserFieldEntity)).when(customUserFieldService).listAllFields(eq(GraviteeContext.getExecutionContext()));

        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        final List<CustomUserFieldEntity> listOfCustomUserFields = response.readEntity(new GenericType<List<CustomUserFieldEntity>>() {});
        assertEquals(1, listOfCustomUserFields.size());
        assertEquals(customUserFieldEntity.getKey(), listOfCustomUserFields.get(0).getKey());
        assertEquals(customUserFieldEntity.getLabel(), listOfCustomUserFields.get(0).getLabel());
        assertEquals(customUserFieldEntity.isRequired(), listOfCustomUserFields.get(0).isRequired());
        assertTrue(listOfCustomUserFields.get(0).getValues().containsAll(customUserFieldEntity.getValues()));
    }
}
