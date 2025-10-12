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
package io.gravitee.rest.api.management.v2.rest.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.model.crd.PageCRD;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.use_case.ApiUpdateDocumentationPageUseCase;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.management.v2.rest.model.Breadcrumb;
import io.gravitee.rest.api.management.v2.rest.model.Media;
import io.gravitee.rest.api.management.v2.rest.model.Page;
import io.gravitee.rest.api.management.v2.rest.model.PageMedia;
import io.gravitee.rest.api.management.v2.rest.model.PageSource;
import io.gravitee.rest.api.management.v2.rest.model.Revision;
import io.gravitee.rest.api.management.v2.rest.model.SourceConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.UpdateDocumentationAsyncApi;
import io.gravitee.rest.api.management.v2.rest.model.UpdateDocumentationFolder;
import io.gravitee.rest.api.management.v2.rest.model.UpdateDocumentationMarkdown;
import io.gravitee.rest.api.management.v2.rest.model.UpdateDocumentationSwagger;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageMediaEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { ConfigurationSerializationMapper.class, DateMapper.class })
public interface PageMapper {
    PageMapper INSTANCE = Mappers.getMapper(PageMapper.class);
    ObjectMapper mapper = new GraviteeMapper();
    Logger logger = LoggerFactory.getLogger(PageMapper.class);
    String SENSITIVE_DATA_REPLACEMENT = "********";

    @Mapping(target = "source.configuration", qualifiedByName = "deserializeConfigurationWithSensitiveDataMasking")
    Page mapPage(io.gravitee.apim.core.documentation.model.Page page);

    List<Page> mapPageList(List<io.gravitee.apim.core.documentation.model.Page> page);

    @Mapping(target = "name", source = "mediaName")
    @Mapping(target = "hash", source = "mediaHash")
    PageMedia mapPageMedia(PageMediaEntity pageMediaEntity);

    @Mapping(target = "mediaName", source = "name")
    @Mapping(target = "mediaHash", source = "hash")
    PageMediaEntity mapPageMedia(PageMedia pageMedia);

    @Mapping(target = "id", source = "pageId")
    Revision mapPageRevision(PageEntity.PageRevisionId pageRevisionId);

    @Mapping(target = "pageId", source = "id")
    PageEntity.PageRevisionId mapPageRevision(Revision revision);

    @Mapping(target = "contentRevision", source = "contentRevisionId")
    @Mapping(target = "updatedAt", source = "lastModificationDate")
    Page map(PageEntity pageEntity);

    @Mapping(target = "contentRevisionId", source = "contentRevision")
    @Mapping(target = "lastModificationDate", source = "updatedAt")
    PageEntity map(Page page);

    io.gravitee.apim.core.documentation.model.Page toCorePage(Page page);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "configurationMap", source = "configuration", qualifiedByName = "convertToMapConfiguration")
    io.gravitee.apim.core.documentation.model.PageSource toCorePageSource(PageSource source);

    @Mapping(target = "mediaName", source = "name")
    @Mapping(target = "mediaHash", source = "hash")
    io.gravitee.apim.core.documentation.model.PageMedia map(PageMedia pageMedia);

    @Mapping(target = "configuration", qualifiedByName = "deserializeJsonConfiguration")
    PageSourceEntity mapPageSource(PageSource source);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    PageSource mapPageSource(PageSourceEntity sourceEntity);

    Set<Page> mapToSet(List<PageEntity> pageEntityList);

    @Mapping(target = "createdAt", source = "createAt")
    Media mapMedia(MediaEntity mediaEntity);

    @Mapping(target = "uploadDate", source = "createdAt")
    MediaEntity mapMedia(Media media);

    @Mapping(target = "type", source = "type")
    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    PageSource mapSourceConfigurationToPageSource(SourceConfiguration sourceConfiguration);

    io.gravitee.apim.core.documentation.model.Page map(
        io.gravitee.rest.api.management.v2.rest.model.CreateDocumentationMarkdown createDocumentationMarkdown
    );
    io.gravitee.apim.core.documentation.model.Page map(
        io.gravitee.rest.api.management.v2.rest.model.CreateDocumentationFolder createDocumentationFolder
    );

    io.gravitee.apim.core.documentation.model.Page map(
        io.gravitee.rest.api.management.v2.rest.model.CreateDocumentationSwagger createDocumentationSwagger
    );
    io.gravitee.apim.core.documentation.model.Page map(
        io.gravitee.rest.api.management.v2.rest.model.CreateDocumentationAsyncApi createDocumentationAsyncApi
    );

    Breadcrumb map(io.gravitee.apim.core.documentation.model.Breadcrumb breadcrumb);
    List<Breadcrumb> map(List<io.gravitee.apim.core.documentation.model.Breadcrumb> breadcrumbList);

    io.gravitee.apim.core.documentation.model.Page map(PageCRD pageCRD);

    // UPDATE
    ApiUpdateDocumentationPageUseCase.Input map(
        UpdateDocumentationMarkdown updateDocumentationMarkdown,
        String apiId,
        String pageId,
        AuditInfo auditInfo
    );

    ApiUpdateDocumentationPageUseCase.Input map(
        UpdateDocumentationSwagger updateDocumentationSwagger,
        String apiId,
        String pageId,
        AuditInfo auditInfo
    );

    ApiUpdateDocumentationPageUseCase.Input map(
        UpdateDocumentationAsyncApi updateDocumentationAsyncApi,
        String apiId,
        String pageId,
        AuditInfo auditInfo
    );

    ApiUpdateDocumentationPageUseCase.Input map(
        UpdateDocumentationFolder updateDocumentationFolder,
        String apiId,
        String pageId,
        AuditInfo auditInfo
    );

    @Named("deserializeJsonConfiguration")
    default JsonNode deserializeJsonConfiguration(Object configuration) throws JsonProcessingException {
        if (Objects.isNull(configuration)) {
            return null;
        }
        if (configuration instanceof LinkedHashMap) {
            try {
                return mapper.valueToTree(configuration);
            } catch (IllegalArgumentException e) {
                throw new TechnicalManagementException("An error occurred while trying to parse connector configuration " + e);
            }
        }
        return null;
    }

    /**
     * Deserialize configuration and mask sensitive data using field names.
     * This method masks known sensitive field names like privateToken, password, etc.
     * This is a simple and reliable approach that doesn't require custom annotations.
     */
    @Named("deserializeConfigurationWithSensitiveDataMasking")
    default Object deserializeConfigurationWithSensitiveDataMasking(String configuration) {
        if (Objects.isNull(configuration)) {
            return null;
        }

        try {
            LinkedHashMap<String, Object> config = mapper.readValue(configuration, LinkedHashMap.class);
            maskSensitiveFieldsInMap(config);

            return config;
        } catch (JsonProcessingException jse) {
            logger.debug(jse.getMessage());
        }

        try {
            return mapper.readValue(configuration, List.class);
        } catch (JsonProcessingException jse) {
            logger.debug(jse.getMessage());
        }

        return configuration;
    }

    /**
     * Get the list of sensitive field names that should be masked.
     * This centralizes the sensitive field definitions to avoid duplication.
     *
     * @return array of sensitive field names
     */
    private String[] getSensitiveFieldNames() {
        return new String[] {
            "privateToken", // GitLab fetcher
            "password", // HTTP, Git fetchers
            "secret", // Various fetchers
            "token", // Various fetchers
            "apiKey", // API fetchers
            "accessToken", // OAuth fetchers
            "clientSecret", // OAuth fetchers
            "personalAccessToken", // GitHub, GitLab
            "authToken", // Generic auth tokens
        };
    }

    /**
     * Mask sensitive fields in a configuration map.
     * This method checks for known sensitive field names and replaces them with "********".
     */
    default void maskSensitiveFieldsInMap(LinkedHashMap<String, Object> configMap) {
        for (String fieldName : getSensitiveFieldNames()) {
            if (configMap.containsKey(fieldName)) {
                configMap.put(fieldName, SENSITIVE_DATA_REPLACEMENT);
            }
        }
    }

    /**
     * Parse configuration JSON string to LinkedHashMap.
     *
     * @param configuration the configuration JSON string
     * @return parsed configuration map or null if parsing fails
     */
    private LinkedHashMap<String, Object> parseConfigurationToMap(String configuration) {
        if (Objects.isNull(configuration)) {
            return null;
        }

        try {
            LinkedHashMap<String, Object> config = mapper.readValue(configuration, LinkedHashMap.class);
            return config;
        } catch (JsonProcessingException e) {
            logger.debug("Cannot parse configuration: " + e.getMessage());
        }
        return null;
    }

    /**
     * Unmasks sensitive data in configuration by restoring original values from the original configuration.
     * This method is used during updates to prevent masked values from overwriting real sensitive data.
     *
     * @param newConfiguration the new configuration JSON string (may contain masked values)
     * @param originalConfiguration the original configuration JSON string (contains real values)
     * @return the configuration map with sensitive data unmasked
     */
    @Named("unmaskSensitiveDataInConfiguration")
    default Object unmaskSensitiveDataInConfiguration(String newConfiguration, String originalConfiguration) {
        LinkedHashMap<String, Object> newConfigMap = parseConfigurationToMap(newConfiguration);
        LinkedHashMap<String, Object> originalConfigMap = parseConfigurationToMap(originalConfiguration);

        if (newConfigMap != null && originalConfigMap != null) {
            unmaskSensitiveFieldsInMap(newConfigMap, originalConfigMap);
            return newConfigMap;
        }

        return newConfigMap;
    }

    /**
     * Unmasks sensitive fields in the configuration map by restoring original values.
     * This method checks if new values are masked and restores them from the original configuration.
     *
     * @param newConfigMap the new configuration map (may contain masked values)
     * @param originalConfigMap the original configuration map (contains real values)
     */
    default void unmaskSensitiveFieldsInMap(LinkedHashMap<String, Object> newConfigMap, LinkedHashMap<String, Object> originalConfigMap) {
        for (String fieldName : getSensitiveFieldNames()) {
            if (newConfigMap.containsKey(fieldName)) {
                Object newValue = newConfigMap.get(fieldName);

                // If new value is masked, restore original value
                if (SENSITIVE_DATA_REPLACEMENT.equals(newValue)) {
                    Object originalValue = originalConfigMap.get(fieldName);
                    if (originalValue != null) {
                        newConfigMap.put(fieldName, originalValue);
                    }
                }
            }
        }
    }
}
