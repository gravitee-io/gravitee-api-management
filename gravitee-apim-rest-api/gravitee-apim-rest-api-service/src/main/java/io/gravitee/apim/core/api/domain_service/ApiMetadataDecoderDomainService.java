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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.apim.core.documentation.model.PrimaryOwnerApiTemplateData;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.template.TemplateProcessor;
import io.gravitee.apim.core.template.TemplateProcessorException;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DomainService
public class ApiMetadataDecoderDomainService {

    private final ApiMetadataQueryService metadataQueryService;
    private final TemplateProcessor templateProcessor;

    public ApiMetadataDecoderDomainService(ApiMetadataQueryService metadataQueryService, TemplateProcessor templateProcessor) {
        this.metadataQueryService = metadataQueryService;
        this.templateProcessor = templateProcessor;
    }

    public Map<String, String> decodeMetadata(String environmentId, String apiId, ApiMetadataDecodeContext context) {
        var metadata = metadataQueryService
            .findApiMetadata(environmentId, apiId)
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getValue() != null)
            .map(entry -> Map.entry(entry.getKey(), entry.getValue().getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return decode(metadata, context);
    }

    public String decodeMetadataValue(String value, ApiMetadataDecodeContext context) {
        try {
            return templateProcessor.processInlineTemplate(value, Collections.singletonMap("api", context));
        } catch (TemplateProcessorException e) {
            log.warn("Error while validating template '{}' from reader:\n{}", e.getTemplate(), e.getMessage());
            return value;
        }
    }

    private Map<String, String> decode(Map<String, String> metadata, ApiMetadataDecodeContext context) {
        if (metadata.isEmpty()) {
            return metadata;
        }

        try {
            var decodedValue = templateProcessor.processInlineTemplate(metadata.toString(), Collections.singletonMap("api", context));

            return Arrays.stream(decodedValue.substring(1, decodedValue.length() - 1).split(", "))
                .map(entry -> entry.split("=", 2))
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry.length > 1 ? entry[1] : ""));
        } catch (TemplateProcessorException e) {
            log.warn("Error while creating template '{}' from reader:\n{}", e.getTemplate(), e.getMessage());
            return metadata;
        } catch (Exception ex) {
            throw new TechnicalDomainException("An error occurs while evaluating API metadata", ex);
        }
    }

    /**
     * Data exposed as {@code api} to metadata templates. It is deliberately flat: metadata values are written by any API or
     * environment metadata editor, so it must not carry the API definition (endpoints, SSL options, proxies, properties).
     */
    @Builder
    @Getter
    @AllArgsConstructor
    public static class ApiMetadataDecodeContext {

        private String id;
        private String name;
        private String description;
        private String version;
        private DefinitionVersion definitionVersion;

        @Builder.Default
        private ApiType type = ApiType.PROXY;

        private String picture;
        private PrimaryOwnerApiTemplateData primaryOwner;

        @Builder.Default
        private Api.Visibility visibility = Api.Visibility.PRIVATE;

        @Builder.Default
        private Api.LifecycleState state = Api.LifecycleState.STOPPED;

        @Builder.Default
        private Api.ApiLifecycleState lifecycleState = Api.ApiLifecycleState.CREATED;

        @Builder.Default
        private Set<String> tags = new HashSet<>();

        @Builder.Default
        private Map<String, String> metadata = new HashMap<>();

        private Date deployedAt;
        private Date createdAt;
        private Date updatedAt;

        public ApiMetadataDecodeContext(Api api, Map<String, String> metadata, PrimaryOwnerEntity primaryOwner) {
            this.id = api.getId();
            this.name = api.getName();
            this.description = api.getDescription();
            this.version = api.getVersion();
            this.definitionVersion = api.getDefinitionVersion();
            this.type = api.getType();
            this.picture = api.getPicture();
            this.primaryOwner = PrimaryOwnerApiTemplateData.from(primaryOwner);
            this.visibility = api.getVisibility();
            this.state = api.getLifecycleState();
            this.lifecycleState = api.getApiLifecycleState();
            this.tags = api.getTags();
            this.metadata = metadata;
            this.deployedAt = toDate(api.getDeployedAt());
            this.createdAt = toDate(api.getCreatedAt());
            this.updatedAt = toDate(api.getUpdatedAt());
        }

        private static Date toDate(ZonedDateTime dateTime) {
            return dateTime == null ? null : Date.from(dateTime.toInstant());
        }
    }
}
