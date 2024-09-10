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
package io.gravitee.apim.infra.domain_service.documentation;

import io.gravitee.apim.core.documentation.domain_service.ValidatePageSourceDomainService;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.apim.core.validation.Validator;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
public class ValidatePageSourceDomainServiceImpl implements ValidatePageSourceDomainService {

    private static final String GITHUB_SOURCE_TYPE = "github-fetcher";
    private static final String HTTP_SOURCE_TYPE = "http-fetcher";

    private static final String GITHUB_URL_PROPERTY = "githubUrl";

    private static final String FETCH_CRON_PROPERTY = "fetchCron";

    private static final String DEFAULT_GITHUB_API_URL = "https://api.github.com";

    private static final Set<String> REQUIRED_GITHUB_PROPERTIES = Set.of(
        "owner",
        "repository",
        "filepath",
        "username",
        "personalAccessToken"
    );

    private static final Set<String> OPTIONAL_GITHUB_PROPERTIES = Set.of(
        GITHUB_URL_PROPERTY,
        FETCH_CRON_PROPERTY,
        "branchOrTag",
        "autoFetch",
        "useSystemProxy"
    );

    private static final String HTTP_URL_PROPERTY = "url";

    private static final Set<String> REQUIRED_HTTP_PROPERTIES = Set.of(HTTP_URL_PROPERTY);

    private static final Set<String> OPTIONAL_HTTP_PROPERTIES = Set.of(FETCH_CRON_PROPERTY, "autoFetch", "useSystemProxy");

    @Override
    public Validator.Result<ValidatePageSourceDomainService.Input> validateAndSanitize(ValidatePageSourceDomainService.Input input) {
        if (input.source() == null) {
            log.debug("Returning empty result as there is no source to validate");
            return Result.empty();
        }

        return switch (input.source().getType()) {
            case HTTP_SOURCE_TYPE -> validateAndSanitizeHTTPSource(input);
            case GITHUB_SOURCE_TYPE -> validateAndSanitizeGithubSource(input);
            default -> bypassValidation(input);
        };
    }

    private Validator.Result<ValidatePageSourceDomainService.Input> validateAndSanitizeGithubSource(
        ValidatePageSourceDomainService.Input input
    ) {
        var sanitizedBuilder = input.source().toBuilder();
        var errors = new ArrayList<Error>();

        var config = new HashMap<>(input.source().getConfigurationMap());

        config.putIfAbsent(GITHUB_URL_PROPERTY, DEFAULT_GITHUB_API_URL);

        validateAndSanitizeSchema(input.pageName(), config, GITHUB_SOURCE_TYPE, REQUIRED_GITHUB_PROPERTIES, OPTIONAL_GITHUB_PROPERTIES)
            .peek(sanitizedBuilder::configurationMap, errors::addAll);

        validateURLProperty(input.pageName(), GITHUB_URL_PROPERTY, GITHUB_SOURCE_TYPE, config).errors().ifPresent(errors::addAll);

        validateFetchCronProperty(input.pageName(), GITHUB_SOURCE_TYPE, config).errors().ifPresent(errors::addAll);

        return Result.ofBoth(input.sanitized(sanitizedBuilder.build()), errors);
    }

    private Validator.Result<ValidatePageSourceDomainService.Input> validateAndSanitizeHTTPSource(
        ValidatePageSourceDomainService.Input input
    ) {
        var sanitizedBuilder = input.source().toBuilder();
        var errors = new ArrayList<Error>();

        var config = new HashMap<>(input.source().getConfigurationMap());

        validateAndSanitizeSchema(input.pageName(), config, HTTP_SOURCE_TYPE, REQUIRED_HTTP_PROPERTIES, OPTIONAL_HTTP_PROPERTIES)
            .peek(sanitizedBuilder::configurationMap, errors::addAll);

        validateURLProperty(input.pageName(), HTTP_URL_PROPERTY, HTTP_SOURCE_TYPE, config).errors().ifPresent(errors::addAll);

        validateFetchCronProperty(input.pageName(), HTTP_SOURCE_TYPE, config).errors().ifPresent(errors::addAll);

        return Result.ofBoth(input.sanitized(sanitizedBuilder.build()), errors);
    }

    private Validator.Result<ValidatePageSourceDomainService.Input> bypassValidation(ValidatePageSourceDomainService.Input input) {
        log.debug("Bypassing validation for source {} as it has not been implemented", input.source().getType());
        return Result.ofValue(input);
    }

    private Validator.Result<String> validateURLProperty(
        String pageName,
        String propertyName,
        String sourceType,
        Map<String, Object> config
    ) {
        try {
            var property = (String) config.get(propertyName);
            return Result.ofValue(URI.create(property).toURL().toString());
        } catch (Exception e) {
            return Result.withError(
                Error.severe("property [%s] of source [%s] must be a valid URL for page [%s]", propertyName, sourceType, pageName)
            );
        }
    }

    private Validator.Result<String> validateFetchCronProperty(String pageName, String sourceType, Map<String, Object> config) {
        var cronExpression = (String) config.get(FETCH_CRON_PROPERTY);
        if (StringUtils.isEmpty(cronExpression)) {
            return Result.empty();
        }
        return CronExpression.isValidExpression(cronExpression)
            ? Result.ofValue(cronExpression)
            : Result.withError(
                Error.severe("property [fetchCron] of source [%s] must be a valid cron expression for page [%s]", sourceType, pageName)
            );
    }

    private Validator.Result<Map<String, Object>> validateAndSanitizeSchema(
        String pageName,
        Map<String, Object> config,
        String sourceType,
        Set<String> required,
        Set<String> optional
    ) {
        var errors = new ArrayList<Error>();

        var sanitized = new HashMap<>(config);
        var givenFields = new HashMap<>(sanitized);
        var requiredFields = new HashSet<>(required);

        requiredFields.removeAll(givenFields.keySet());

        if (CollectionUtils.isNotEmpty(requiredFields)) {
            errors.add(
                Error.severe(
                    "property [%s] is required in [%s] configuration for page [%s]",
                    requiredFields.iterator().next(),
                    sourceType,
                    pageName
                )
            );
        }

        givenFields.keySet().removeAll(required);
        givenFields.keySet().removeAll(optional);

        for (var field : givenFields.keySet()) {
            errors.add(
                Error.warning("page [%s] contains unknown configuration property [%s] for [%s] source", pageName, field, sourceType)
            );
            sanitized.remove(field);
        }

        return Result.ofBoth(sanitized, errors);
    }
}
