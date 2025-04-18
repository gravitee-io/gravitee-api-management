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
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.installation.model.RestrictedDomain;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.mcp.Tool;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@DomainService
@AllArgsConstructor
public class VerifyMCPToolDomainService implements Validator<VerifyMCPToolDomainService.Input> {

    public record Input(String environmentId, String apiId, List<Tool> tools) implements Validator.Input {}

    private final ApiQueryService apiSearchService;

    @Override
    public Result<VerifyMCPToolDomainService.Input> validateAndSanitize(VerifyMCPToolDomainService.Input input) {
        var sanitizedBuilder = input.tools.stream().map(Tool::toBuilder).toList();

        var errors = new ArrayList<Error>();

        errors.addAll(duplicateNameErrors(sanitizedBuilder));

        errors.addAll(unavailablePathErrors(input, sanitizedBuilder));

        var sanitized = sanitizedBuilder.stream().map(Tool.ToolBuilder::build).toList();

        return Result.ofBoth(new Input(input.environmentId, input.apiId, sanitized), errors);
    }

    private List<Error> unavailablePathErrors(Input input, List<Tool.ToolBuilder> sanitizedBuilder) {
        var errors = new ArrayList<Error>();
        var toolNames = sanitizedBuilder.stream().map(Tool.ToolBuilder::build).map(Tool::getName).toList();

        apiSearchService
            .search(
                ApiSearchCriteria.builder().environmentId(input.environmentId).definitionVersion(List.of(DefinitionVersion.V4)).build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).build()
            )
            .filter(api -> !api.getId().equals(input.apiId))
            .map(VerifyMCPToolDomainService::extractTools)
            .filter(CollectionUtils::isNotEmpty)
            .forEach(existingTools -> {
                existingTools.forEach(tool -> {
                    var toolName = tool.getName();
                    if (toolNames.contains(toolName)) {
                        errors.add(Error.severe("Tool name [%s] already exists", toolName));
                    }
                });
            });

        return errors;
    }

    private List<Error> duplicateNameErrors(List<Tool.ToolBuilder> sanitizedBuilder) {
        var seen = new HashSet<>();
        return sanitizedBuilder
            .stream()
            .map(Tool.ToolBuilder::build)
            .filter(path -> !seen.add(path))
            .map(duplicate -> Error.severe("Name [%s] is duplicated", duplicate.getName()))
            .toList();
    }

    private static List<Tool> extractTools(Api api) {
        return (
                api.getDefinitionVersion() == DefinitionVersion.V4 &&
                ApiType.PROXY.equals(api.getType()) &&
                api.getApiDefinitionHttpV4().getMcp() != null &&
                api.getApiDefinitionHttpV4().getMcp().getTools() != null
            )
            ? api.getApiDefinitionHttpV4().getMcp().getTools()
            : new ArrayList<>();
    }
}
