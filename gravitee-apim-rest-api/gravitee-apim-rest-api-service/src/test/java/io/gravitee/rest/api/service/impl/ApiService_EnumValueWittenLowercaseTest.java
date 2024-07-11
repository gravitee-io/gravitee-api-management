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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.jackson.ser.api.ApiCompositeSerializer;
import io.gravitee.rest.api.service.jackson.ser.api.ApiDefaultSerializer;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

/**
 * @author Guillaume Gillon
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_EnumValueWittenLowercaseTest {

    private static final String API_ID = "id-api";

    private ObjectMapper objectMapper;

    @Mock
    private GroupService groupService;

    @Mock
    private ApplicationContext applicationContext;

    @Before
    public void setUp() {
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        objectMapper = serviceConfiguration.objectMapper();

        when(applicationContext.getBean(GroupService.class)).thenReturn(groupService);

        ApiCompositeSerializer apiSerializer = (ApiCompositeSerializer) serviceConfiguration.apiSerializer();
        apiSerializer.afterPropertiesSet();

        ApiSerializer apiDefaultSerializer = new ApiDefaultSerializer();
        apiDefaultSerializer.setApplicationContext(applicationContext);
        apiSerializer.setSerializers(Arrays.asList(apiDefaultSerializer));

        SimpleModule module = new SimpleModule();
        module.addSerializer(ApiEntity.class, apiSerializer);
        objectMapper.registerModule(module);
    }

    @Test
    public void shouldConvertAsJsonForV2ExportWithUppercaseEnum() throws IOException {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setName("test");
        apiEntity.setDescription("Gravitee.io");
        apiEntity.setVisibility(Visibility.PUBLIC);
        apiEntity.setGraviteeDefinitionVersion("2.0.0");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ApiSerializer.METADATA_EXPORT_VERSION, ApiSerializer.Version.DEFAULT.getVersion());
        metadata.put(
            ApiSerializer.METADATA_FILTERED_FIELDS_LIST,
            Arrays.asList("groups", "members", "pages", "plans", "metadata", "media")
        );
        apiEntity.setMetadata(metadata);

        String result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiEntity);
        assertThat(result)
            .isEqualTo(
                "{\n" +
                "  \"name\" : \"test\",\n" +
                "  \"execution_mode\" : \"v3\",\n" +
                "  \"description\" : \"Gravitee.io\",\n" +
                "  \"visibility\" : \"PUBLIC\",\n" +
                "  \"flows\" : [ ],\n" +
                "  \"gravitee\" : \"2.0.0\",\n" +
                "  \"resources\" : [ ],\n" +
                "  \"properties\" : [ ],\n" +
                "  \"id\" : \"id-api\",\n" +
                "  \"path_mappings\" : [ ],\n" +
                "  \"disable_membership_notifications\" : false\n" +
                "}"
            );
    }

    @Test
    public void shouldConvertAsJsonForV1ExportWithUppercaseEnum() throws IOException {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setName("test");
        apiEntity.setDescription("Gravitee.io");
        apiEntity.setVisibility(Visibility.PUBLIC);
        apiEntity.setGraviteeDefinitionVersion("1.0.0");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ApiSerializer.METADATA_EXPORT_VERSION, ApiSerializer.Version.DEFAULT.getVersion());
        metadata.put(
            ApiSerializer.METADATA_FILTERED_FIELDS_LIST,
            Arrays.asList("groups", "members", "pages", "plans", "metadata", "media")
        );
        apiEntity.setMetadata(metadata);

        String result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiEntity);
        assertThat(result)
            .isEqualTo(
                "{\n" +
                "  \"name\" : \"test\",\n" +
                "  \"execution_mode\" : \"v3\",\n" +
                "  \"description\" : \"Gravitee.io\",\n" +
                "  \"visibility\" : \"PUBLIC\",\n" +
                "  \"paths\" : { },\n" +
                "  \"gravitee\" : \"1.0.0\",\n" +
                "  \"resources\" : [ ],\n" +
                "  \"properties\" : [ ],\n" +
                "  \"id\" : \"id-api\",\n" +
                "  \"path_mappings\" : [ ],\n" +
                "  \"disable_membership_notifications\" : false\n" +
                "}"
            );
    }

    @Test
    public void shouldConvertAsObjectForImportWithLowercaseEnum() throws IOException {
        final String lowercaseApiDefinition =
            "{\n" +
            "  \"name\" : \"test\",\n" +
            "  \"description\" : \"Gravitee.io\",\n" +
            "  \"visibility\" : \"public\",\n" +
            "  \"paths\" : { },\n" +
            "  \"resources\" : [ ],\n" +
            "  \"path_mappings\" : [ ]\n" +
            "}";

        final ApiEntity apiEntity = objectMapper.readValue(lowercaseApiDefinition, ApiEntity.class);
        assertEquals(Visibility.PUBLIC, apiEntity.getVisibility());
    }

    @Test
    public void shouldConvertAsObjectForImportWithUppercaseEnum() throws IOException {
        final String lowercaseApiDefinition =
            "{\n" +
            "  \"name\" : \"test\",\n" +
            "  \"description\" : \"Gravitee.io\",\n" +
            "  \"visibility\" : \"PUBLIC\",\n" +
            "  \"paths\" : { },\n" +
            "  \"gravitee\" : \"1.0.0\",\n" +
            "  \"resources\" : [ ],\n" +
            "  \"path_mappings\" : [ ]\n" +
            "}";

        final ApiEntity apiEntity = objectMapper.readValue(lowercaseApiDefinition, ApiEntity.class);
        assertEquals(Visibility.PUBLIC, apiEntity.getVisibility());
    }
}
