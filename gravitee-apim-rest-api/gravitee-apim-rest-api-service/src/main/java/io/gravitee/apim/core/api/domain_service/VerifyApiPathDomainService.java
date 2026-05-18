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
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.installation.model.RestrictedDomain;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.DefinitionVersion;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
<<<<<<< HEAD
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
=======
import lombok.CustomLog;
<<<<<<< HEAD
<<<<<<< HEAD
>>>>>>> caa802a768 (perf(api): in-memory path index for collision checks (APIM-14052))
=======
import org.springframework.beans.factory.annotation.Value;
>>>>>>> 45bed588dc (fix(api): close cross-pod path-index race window (APIM-14052))
=======
>>>>>>> 69ba820eba (fix(api): inject node Configuration instead of Spring @Value (APIM-14052))

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@DomainService
public class VerifyApiPathDomainService implements Validator<VerifyApiPathDomainService.Input> {

    public record Input(String environmentId, String apiId, List<Path> paths) implements Validator.Input {}

    private static final List<DefinitionVersion> PATH_BEARING_DEFINITIONS = List.of(DefinitionVersion.V2, DefinitionVersion.V4);

    /**
     * Slack subtracted from the snapshot watermark when issuing the supplementary "recently updated" Mongo query.
     * Sized to absorb realistic inter-pod clock skew plus a cron-batch processing window. A too-tight value just
     * widens the per-conflict recheck cohort (still one batch query), so this is a perf knob, not a correctness one.
     */
    private static final Duration SAFETY_MARGIN = Duration.ofSeconds(10);

    private final ApiQueryService apiSearchService;
    private final InstallationAccessQueryService installationAccessQueryService;
    private final ApiHostValidatorDomainService apiHostValidatorDomainService;
    private final ApiPathIndexReader apiPathIndex;

    public VerifyApiPathDomainService(
        ApiQueryService apiSearchService,
        InstallationAccessQueryService installationAccessQueryService,
        ApiHostValidatorDomainService apiHostValidatorDomainService,
        ApiPathIndexReader apiPathIndex
    ) {
        this.apiSearchService = apiSearchService;
        this.installationAccessQueryService = installationAccessQueryService;
        this.apiHostValidatorDomainService = apiHostValidatorDomainService;
        this.apiPathIndex = apiPathIndex;
    }

    @Override
    public Result<VerifyApiPathDomainService.Input> validateAndSanitize(VerifyApiPathDomainService.Input input) {
        if (CollectionUtils.isEmpty(input.paths)) {
            return Result.ofErrors(List.of(Error.severe("HTTP listener requires a minimum of one path")));
        }

        var sanitizedBuilder = input.paths.stream().map(Path::toBuilder).toList();

        var errors = new ArrayList<Error>();

        errors.addAll(invalidPathErrors(sanitizedBuilder));

        errors.addAll(duplicatePathErrors(sanitizedBuilder));

        errors.addAll(invalidDomainErrors(input.environmentId, sanitizedBuilder));

        errors.addAll(unavailablePathErrors(input, sanitizedBuilder));

        var sanitized = sanitizedBuilder.stream().map(Path.PathBuilder::build).toList();

        return Result.ofBoth(new Input(input.environmentId, input.apiId, sanitized), errors);
    }

    private List<Error> invalidDomainErrors(String environmentId, List<Path.PathBuilder> sanitizedBuilder) throws InvalidPathsException {
        var errors = new ArrayList<Error>();

        var restrictedDomains = installationAccessQueryService.getGatewayRestrictedDomains(environmentId);

        if (CollectionUtils.isNotEmpty(restrictedDomains)) {
            for (var builder : sanitizedBuilder) {
                if (builder.build().hasHost()) {
                    invalidDomainError(builder.build(), restrictedDomains).ifPresent(errors::add);
                }
            }
            if (sanitizedBuilder.stream().noneMatch(builder -> builder.build().isOverrideAccess())) {
                sanitizedBuilder
                    .stream()
                    .findFirst()
                    .ifPresent(builder -> builder.overrideAccess(true));
            }
        }

        return errors;
    }

    private Optional<Validator.Error> invalidDomainError(Path path, List<RestrictedDomain> restrictedDomains) {
        var hostWithoutPort = extractHost(path.getHost());
        var hostPort = extractPort(path.getHost());

        var restrictedDomainsWithoutPort = restrictedDomains
            .stream()
            .map(restrictedDomainEntity -> extractHost(restrictedDomainEntity.getDomain()))
            .toList();

        var isValidDomain = apiHostValidatorDomainService.isValidDomainOrSubDomain(hostWithoutPort, restrictedDomainsWithoutPort);

        var isValidPort = isValidPort(hostPort, restrictedDomains);

        if (!isValidDomain || !isValidPort) {
            return Optional.of(Validator.Error.severe("Domain [%s] is invalid", path.getHost()));
        }
        return Optional.empty();
    }

    private List<Error> unavailablePathErrors(Input input, List<Path.PathBuilder> sanitizedBuilder) {
        var candidatePaths = sanitizedBuilder.stream().map(Path.PathBuilder::build).toList();
        var env = input.environmentId;
        var excludeApiId = input.apiId;

        var snapshot = apiPathIndex.snapshotOf(env, () -> searchPathBearing(env, null));

        // Always supplement the snapshot with rows updated since the snapshot's watermark minus a safety margin
        // (covers inter-pod clock skew + cron batch processing duration). On a fresh seed of a quiet env, this
        // returns 0 rows. On a busy env or in the first seconds after a remote write lands in Mongo but before our
        // broadcast cron pulls it, this catches the unseen rows.
        var since = snapshot.refreshedAt().minus(SAFETY_MARGIN);
        var overridePaths = new HashMap<String, List<Path>>();
        try (var recent = searchPathBearing(env, since)) {
            recent.forEach(api -> overridePaths.put(api.getId(), ApiPathExtractor.extractPaths(api)));
        }

        // Merge: supplementary wins. Empty paths means a path-removing update or non-HTTP API — drop from view.
        var merged = new HashMap<>(snapshot.pathsByApiId());
        overridePaths.forEach((apiId, paths) -> {
            if (paths.isEmpty()) {
                merged.remove(apiId);
            } else {
                merged.put(apiId, paths);
            }
        });

        var conflicts = ApiPathIndex.scanPaths(merged, excludeApiId, candidatePaths);
        if (conflicts.isEmpty()) {
            return List.of();
        }

        // Partition: conflicts against rows we just read from Mongo (override) are trusted; conflicts against
        // snapshot-only rows need a per-apiId recheck to catch hard deletes (which leave no row for the
        // supplementary query to find) and any update that escaped the watermark window.
        var trustedErrors = new ArrayList<Error>();
        var needsRecheck = new ArrayList<String>();
        var seenRecheck = new HashSet<String>();
        for (var conflict : conflicts) {
            if (overridePaths.containsKey(conflict.apiId())) {
                trustedErrors.add(conflict.error());
            } else if (seenRecheck.add(conflict.apiId())) {
                needsRecheck.add(conflict.apiId());
            }
        }

        // Single Mongo round-trip for the entire rechecked cohort (typically N <= a handful).
        // The {_id IN (...)} predicate hits the primary-key index; environmentId is applied as a residual filter
        // server-side (the existing environmentId index isn't useful here because the IDs already narrow the scan).
        // We keep the same pictureExcluded field filter as the supplementary query so the definition is loaded
        // — without it ApiPathExtractor.extractPaths would return [] and we'd drop real conflicts.
        var rechecked = new HashMap<String, List<Path>>();
        if (!needsRecheck.isEmpty()) {
            try (var stream = searchByIds(env, needsRecheck)) {
                stream.forEach(api -> {
                    var paths = ApiPathExtractor.extractPaths(api);
                    if (!paths.isEmpty()) {
                        rechecked.put(api.getId(), paths);
                    }
                });
            }
        }
        var rescannedConflicts = ApiPathIndex.scanPaths(rechecked, excludeApiId, candidatePaths);

        var allErrors = new ArrayList<Error>(trustedErrors);
        rescannedConflicts.forEach(c -> allErrors.add(c.error()));
        return allErrors;
    }

    private java.util.stream.Stream<io.gravitee.apim.core.api.model.Api> searchPathBearing(String envId, java.time.Instant updatedAtFrom) {
        var criteria = ApiSearchCriteria.builder()
            .environmentId(envId)
            .definitionVersion(PATH_BEARING_DEFINITIONS)
            .updatedAtFrom(updatedAtFrom)
            .build();
        return apiSearchService.search(criteria, null, ApiFieldFilter.builder().pictureExcluded(true).build());
    }

    private java.util.stream.Stream<io.gravitee.apim.core.api.model.Api> searchByIds(String envId, List<String> ids) {
        var criteria = ApiSearchCriteria.builder().environmentId(envId).ids(ids).definitionVersion(PATH_BEARING_DEFINITIONS).build();
        return apiSearchService.search(criteria, null, ApiFieldFilter.builder().pictureExcluded(true).build());
    }

    private List<Error> invalidPathErrors(List<Path.PathBuilder> sanitizedBuilder) {
        var errors = new ArrayList<Error>();

        sanitizedBuilder.forEach(builder -> {
            var path = builder.build();
            try {
                var sanitized = path.sanitize();
                builder.path(sanitized.getPath());
            } catch (InvalidPathsException e) {
                errors.add(Error.severe("Path [%s] is invalid", path.getPath()));
            }
        });

        return errors;
    }

    private List<Error> duplicatePathErrors(List<Path.PathBuilder> sanitizedBuilder) {
        var seen = new HashSet<>();
        return sanitizedBuilder
            .stream()
            .map(Path.PathBuilder::build)
            .filter(path -> !seen.add(path))
            .map(duplicate -> Error.severe("Path [%s] is duplicated", duplicate.getPath()))
            .toList();
    }

    private static boolean isValidPort(final String port, final List<RestrictedDomain> restrictedDomainEntities) {
        return restrictedDomainEntities
            .stream()
            .anyMatch(restrictedDomainEntity -> {
                String restrictedDomain = restrictedDomainEntity.getDomain();
                String restrictedPort = extractPort(restrictedDomain);
                return isValidPort(port, restrictedPort);
            });
    }

    private static boolean isValidPort(String port, String restrictedPort) {
        if (port == null && restrictedPort == null) {
            return true;
        }
        if (port == null && isDefaultHttp(restrictedPort)) {
            return true;
        }
        if (restrictedPort == null && isDefaultHttp(port)) {
            return true;
        }
        return Objects.equals(port, restrictedPort);
    }

    private static boolean isDefaultHttp(final String domainRestrictionOnlyPort) {
        return "80".equals(domainRestrictionOnlyPort) || "443".equals(domainRestrictionOnlyPort);
    }

    private static String extractHost(final String hostAndPort) {
        return hostAndPort.split(":")[0];
    }

    private static String extractPort(final String hostAndPort) {
        String[] split = hostAndPort.split(":");
        if (split.length > 1) {
            return split[1];
        }
        return null;
    }
}
