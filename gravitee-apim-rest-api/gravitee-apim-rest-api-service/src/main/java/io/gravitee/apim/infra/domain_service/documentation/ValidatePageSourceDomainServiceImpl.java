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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.documentation.domain_service.ValidatePageSourceDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageSourceException;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.apim.core.validation.Validator;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.rxjava3.core.Vertx;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
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
    private static final String VERSION_HEADER = "application/vnd.github.v3+json";
    private static final String GITHUB_REPOSITORY_BRANCH_OR_TAG = "branchOrTag";
    private static final String GITHUB_REPOSITORY_OWNER = "owner";
    private static final String GITHUB_REPOSITORY = "repository";
    private static final String GITHUB_FILEPATH = "filepath";
    private static final String GITHUB_USERNAME = "username";
    private static final String GITHUB_PERSONAL_ACCESS_TOKEN = "personalAccessToken";
    private static final String DEFAULT_GITHUB_API_URL = "https://api.github.com";

    private static final Set<String> REQUIRED_GITHUB_PROPERTIES = new HashSet<>(
        Set.of(GITHUB_REPOSITORY_OWNER, GITHUB_REPOSITORY, GITHUB_FILEPATH)
    );
    private static final Set<String> OPTIONAL_GITHUB_PROPERTIES = new HashSet<>(
        Set.of(GITHUB_URL_PROPERTY, FETCH_CRON_PROPERTY, GITHUB_REPOSITORY_BRANCH_OR_TAG, "autoFetch", "useSystemProxy")
    );

    private static final String HTTP_URL_PROPERTY = "url";

    private static final Set<String> REQUIRED_HTTP_PROPERTIES = Set.of(HTTP_URL_PROPERTY);

    private static final Set<String> OPTIONAL_HTTP_PROPERTIES = Set.of(FETCH_CRON_PROPERTY, "autoFetch", "useSystemProxy");

    private final ObjectMapper objectMapper;
    private final Vertx vertx;

    public ValidatePageSourceDomainServiceImpl(ObjectMapper objectMapper, Vertx vertx) {
        this.objectMapper = objectMapper;
        this.vertx = vertx;
    }

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
        try {
            var sanitizedBuilder = input.source().toBuilder();
            var errors = new ArrayList<Error>();

            var config = new HashMap<>(input.source().getConfigurationMap());

            config.putIfAbsent(GITHUB_URL_PROPERTY, DEFAULT_GITHUB_API_URL);

            validatePageSourceURL(input.pageName(), GITHUB_URL_PROPERTY, GITHUB_SOURCE_TYPE, config).errors().ifPresent(errors::addAll);

            checkRequiredProperties(input.pageName(), config, GITHUB_SOURCE_TYPE, REQUIRED_GITHUB_PROPERTIES)
                .peek(sanitizedBuilder::configurationMap, errors::addAll);

            if (errors.stream().anyMatch(Error::isSevere)) {
                return Result.ofBoth(input.sanitized(sanitizedBuilder.build()), errors);
            }

            Set<String> githubFetcherCredentialsFields = new HashSet<>();
            if (githubPageSourceIsPrivate(config)) {
                githubFetcherCredentialsFields.add(GITHUB_USERNAME);
                githubFetcherCredentialsFields.add(GITHUB_PERSONAL_ACCESS_TOKEN);
            } else {
                OPTIONAL_GITHUB_PROPERTIES.add(GITHUB_USERNAME);
                OPTIONAL_GITHUB_PROPERTIES.add(GITHUB_PERSONAL_ACCESS_TOKEN);
            }

            if (!githubFetcherCredentialsFields.isEmpty()) {
                checkRequiredProperties(input.pageName(), config, GITHUB_SOURCE_TYPE, githubFetcherCredentialsFields)
                    .peek(sanitizedBuilder::configurationMap, errors::addAll);

                if (errors.stream().anyMatch(Error::isSevere)) {
                    return Result.ofBoth(input.sanitized(sanitizedBuilder.build()), errors);
                }

                if (!githubPageSourceCredentialsIsValid(config)) {
                    errors.add(Error.severe("Page [%s] Github fetcher credentials is invalid", input.pageName()));
                }
            }

            REQUIRED_GITHUB_PROPERTIES.addAll(githubFetcherCredentialsFields);
            checkUnknownProperties(input.pageName(), config, GITHUB_SOURCE_TYPE, REQUIRED_GITHUB_PROPERTIES, OPTIONAL_GITHUB_PROPERTIES)
                .peek(sanitizedBuilder::configurationMap, errors::addAll);

            validateFetchCronProperty(input.pageName(), GITHUB_SOURCE_TYPE, config).errors().ifPresent(errors::addAll);

            sanitizedBuilder.configuration(objectMapper.writeValueAsString(config));
            sanitizedBuilder.configurationMap(config);
            return Result.ofBoth(input.sanitized(sanitizedBuilder.build()), errors);
        } catch (JsonProcessingException e) {
            log.error("An error occurred while parsing the page source configuration", e);
            throw new InvalidPageSourceException("An error occurred while parsing the page source configuration");
        }
    }

    private boolean githubPageSourceIsPrivate(Map<String, Object> config) {
        return vertx
            .createHttpClient()
            .rxRequest(getGithubRequestOptions(config))
            .flatMap(req -> req.rxSend().flatMap(resp -> Single.just(resp.statusCode() == 403)))
            .blockingGet();
    }

    private boolean githubPageSourceCredentialsIsValid(Map<String, Object> config) {
        final RequestOptions reqOptions = getGithubRequestOptions(config);
        String auth = config.get(GITHUB_USERNAME) + ":" + config.get(GITHUB_PERSONAL_ACCESS_TOKEN);
        reqOptions.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes()));

        return vertx
            .createHttpClient()
            .rxRequest(reqOptions)
            .flatMap(req -> req.rxSend().flatMap(resp -> Single.just(resp.statusCode() == 200)))
            .blockingGet();
    }

    private RequestOptions getGithubRequestOptions(Map<String, Object> config) {
        Object branchOrTag = config.get(GITHUB_REPOSITORY_BRANCH_OR_TAG);
        String url =
            config.get(GITHUB_URL_PROPERTY) +
            "/repos" +
            "/" +
            config.get(GITHUB_REPOSITORY_OWNER) +
            "/" +
            config.get(GITHUB_REPOSITORY) +
            "/contents" +
            config.get(GITHUB_FILEPATH) +
            (branchOrTag != null && StringUtils.isNotEmpty(branchOrTag.toString()) ? ("?ref=" + branchOrTag) : "");

        return new RequestOptions()
            .setMethod(HttpMethod.GET)
            .setAbsoluteURI(url)
            .putHeader("User-Agent", config.get(GITHUB_REPOSITORY_OWNER).toString())
            .putHeader("Accept", VERSION_HEADER)
            .setFollowRedirects(true);
    }

    private Validator.Result<ValidatePageSourceDomainService.Input> validateAndSanitizeHTTPSource(
        ValidatePageSourceDomainService.Input input
    ) {
        var sanitizedBuilder = input.source().toBuilder();
        var errors = new ArrayList<Error>();

        var config = new HashMap<>(input.source().getConfigurationMap());

        checkRequiredProperties(input.pageName(), config, HTTP_SOURCE_TYPE, REQUIRED_HTTP_PROPERTIES)
            .peek(sanitizedBuilder::configurationMap, errors::addAll);

        checkUnknownProperties(input.pageName(), config, HTTP_SOURCE_TYPE, REQUIRED_HTTP_PROPERTIES, OPTIONAL_HTTP_PROPERTIES)
            .peek(sanitizedBuilder::configurationMap, errors::addAll);

        validatePageSourceURL(input.pageName(), HTTP_URL_PROPERTY, HTTP_SOURCE_TYPE, config).errors().ifPresent(errors::addAll);

        validateFetchCronProperty(input.pageName(), HTTP_SOURCE_TYPE, config).errors().ifPresent(errors::addAll);

        return Result.ofBoth(input.sanitized(sanitizedBuilder.build()), errors);
    }

    private Validator.Result<ValidatePageSourceDomainService.Input> bypassValidation(ValidatePageSourceDomainService.Input input) {
        log.debug("Bypassing validation for source {} as it has not been implemented", input.source().getType());
        return Result.ofValue(input);
    }

    private Validator.Result<String> validatePageSourceURL(
        String pageName,
        String urlFieldName,
        String sourceType,
        Map<String, Object> config
    ) {
        try {
            var property = (String) config.get(urlFieldName);
            return Result.ofValue(URI.create(property).toURL().toString());
        } catch (Exception e) {
            return Result.withError(
                Error.severe("property [%s] of source [%s] must be a valid URL for page [%s]", urlFieldName, sourceType, pageName)
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

    private Validator.Result<Map<String, Object>> checkRequiredProperties(
        String pageName,
        Map<String, Object> config,
        String sourceType,
        Set<String> required
    ) {
        var errors = new ArrayList<Error>();

        var requiredFields = new HashSet<>(required);

        requiredFields.removeAll(config.keySet());

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

        return Result.ofBoth(config, errors);
    }

    private Validator.Result<Map<String, Object>> checkUnknownProperties(
        String pageName,
        Map<String, Object> config,
        String sourceType,
        Set<String> required,
        Set<String> optional
    ) {
        var errors = new ArrayList<Error>();

        var givenFields = new HashMap<>(config);

        givenFields.keySet().removeAll(required);
        givenFields.keySet().removeAll(optional);

        for (var field : givenFields.keySet()) {
            errors.add(
                Error.warning("page [%s] contains unknown configuration property [%s] for [%s] source", pageName, field, sourceType)
            );
            config.remove(field);
        }

        return Result.ofBoth(config, errors);
    }
}
