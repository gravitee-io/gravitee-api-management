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
package io.gravitee.apim.core.promotion.domain_service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.promotion.crud_service.PromotionCrudService;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.promotion.query_service.PromotionQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.CustomLog;

@CustomLog
@DomainService
public class PromotionContextDomainService {

    private final PromotionCrudService promotionCrudService;
    private final PromotionQueryService promotionQueryService;
    private final ApiQueryService apiQueryService;
    private final ApiCrudService apiCrudService;
    private final EnvironmentCrudService environmentCrudService;
    private final JsonMapper jsonMapper;

    public PromotionContextDomainService(
        PromotionCrudService promotionCrudService,
        PromotionQueryService promotionQueryService,
        ApiQueryService apiQueryService,
        ApiCrudService apiCrudService,
        EnvironmentCrudService environmentCrudService,
        JsonMapper jsonMapper
    ) {
        this.promotionCrudService = promotionCrudService;
        this.promotionQueryService = promotionQueryService;
        this.apiQueryService = apiQueryService;
        this.apiCrudService = apiCrudService;
        this.environmentCrudService = environmentCrudService;
        this.jsonMapper = jsonMapper;
    }

    public record PromotionContext(
        Promotion promotion,
        DefinitionVersion expectedDefinitionVersion,
        Api existingPromotedApi,
        String targetEnvId
    ) {}

    /*
     * Validates the promotion before applying it.
     *
     * This validation is required because V2 APIs can be migrated to V4 APIs using the dedicated feature.
     * We assume that the target API and its expected definition versions must match.
     *
     * Once all V2 APIs are migrated and V2 no longer supported, this validation step can be removed,
     * and the promotion can be applied directly through the corresponding UseCase.
     */
    public PromotionContext getPromotionContext(String promotionId, boolean isAccepted) {
        var promotion = promotionCrudService.getById(promotionId);
        var crossId = extractCrossIdFromDefinition(promotion);
        var environment = environmentCrudService.getByCockpitId(promotion.getTargetEnvCockpitId());
        var targetEnvId = environment.getId();
        var expectedDefinitionVersion = getPromotionDefinitionVersion(promotion);
        var resolved = resolveTargetApi(promotion, crossId, targetEnvId, expectedDefinitionVersion, isAccepted);

        if (isAccepted && resolved.isPresent()) {
            var targetApi = resolved.get().api();
            if (targetApi.getDefinitionVersion() != expectedDefinitionVersion) {
                throw new IllegalStateException(resolved.get().strategy().definitionVersionMismatch(targetApi));
            }
        }

        return new PromotionContext(promotion, expectedDefinitionVersion, resolved.map(ResolvedApi::api).orElse(null), targetEnvId);
    }

    /** How the API already living in the target environment was identified. */
    private enum Resolution {
        CROSS_ID("An API with the same crossId already exists with a different definition version (API %s)"),
        PROMOTION_HISTORY("The API previously promoted to this environment (%s) has a different definition version"),
        CONTEXT_PATH("The API already serving this context path (%s) has a different definition version");

        private final String mismatchMessage;

        Resolution(String mismatchMessage) {
            this.mismatchMessage = mismatchMessage;
        }

        String definitionVersionMismatch(Api targetApi) {
            return mismatchMessage.formatted(targetApi.getId());
        }
    }

    private record ResolvedApi(Api api, Resolution strategy) {}

    /**
     * Identifies the API in the target environment that this promotion must update, rather than duplicate.
     *
     * <p>crossId is the intended identity but it is not guaranteed to stay aligned across environments: it can be
     * changed on either side, and before the promotion pipeline persisted {@code targetApiId} nothing repaired the
     * link. When it diverges, the promotion has to fall back on evidence that the two APIs are the same one:
     * the {@code targetApiId} recorded by a previous accepted promotion, then the context path, which the gateway
     * already guarantees to be unique within an environment.
     *
     * <p>Scoped to V4: V2 promotions resolve their own target inside {@code PromotionServiceImpl} and only use this
     * result for the definition version guard, so widening it there would reject promotions that used to succeed.
     */
    private Optional<ResolvedApi> resolveTargetApi(
        Promotion promotion,
        String crossId,
        String targetEnvId,
        DefinitionVersion expectedDefinitionVersion,
        boolean isAccepted
    ) {
        var byCrossId = apiQueryService.findByEnvironmentIdAndCrossId(targetEnvId, crossId);
        if (byCrossId.isPresent()) {
            return byCrossId.map(api -> new ResolvedApi(api, Resolution.CROSS_ID));
        }

        if (expectedDefinitionVersion != DefinitionVersion.V4) {
            return Optional.empty();
        }

        var previousPromotions = findPreviousAcceptedPromotions(promotion);
        if (previousPromotions.isEmpty()) {
            // This source API was never promoted into this environment, so nothing in it belongs to this promotion.
            return Optional.empty();
        }

        return findApiPromotedInPreviousPromotion(previousPromotions, targetEnvId)
            .map(api -> new ResolvedApi(api, Resolution.PROMOTION_HISTORY))
            // Context-path scan walks every V2/V4 API in the environment. Reject does not use the resolved
            // API, so skip it — every upgrading environment is in this state until each API is re-promoted.
            .or(() ->
                isAccepted
                    ? findApiOwningTheContextPath(promotion, targetEnvId).map(api -> new ResolvedApi(api, Resolution.CONTEXT_PATH))
                    : Optional.empty()
            )
            .map(resolvedApi -> {
                log.info(
                    "Promotion [{}]: no API with crossId [{}] in environment [{}], updating API [{}] (crossId [{}]) resolved by {}",
                    promotion.getId(),
                    crossId,
                    targetEnvId,
                    resolvedApi.api().getId(),
                    resolvedApi.api().getCrossId(),
                    resolvedApi.strategy()
                );
                return resolvedApi;
            });
    }

    /** Accepted promotions of the same source API into the same target environment, excluding the one being processed. */
    private List<Promotion> findPreviousAcceptedPromotions(Promotion promotion) {
        if (promotion.getApiId() == null || promotion.getTargetEnvCockpitId() == null) {
            return List.of();
        }

        var previousPromotions = promotionQueryService
            .search(
                new PromotionQueryService.PromotionQuery(
                    promotion.getApiId(),
                    Set.of(promotion.getTargetEnvCockpitId()),
                    Set.of(PromotionStatus.ACCEPTED),
                    // Include accepted rows with a null target_api_id — those are the upgrade-path history.
                    null
                )
            )
            .getContent();

        return previousPromotions
            .stream()
            // The query cannot exclude the promotion being processed.
            .filter(previous -> !Objects.equals(previous.getId(), promotion.getId()))
            .toList();
    }

    private Optional<Api> findApiPromotedInPreviousPromotion(List<Promotion> previousPromotions, String targetEnvId) {
        return previousPromotions
            .stream()
            .filter(previous -> previous.getTargetApiId() != null && !previous.getTargetApiId().isBlank())
            .max(Comparator.comparing(Promotion::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
            .map(Promotion::getTargetApiId)
            .flatMap(apiCrudService::findById)
            // A targetApiId must never make us update an API we cannot prove lives in the target environment.
            .filter(api -> targetEnvId.equals(api.getEnvironmentId()));
    }

    /**
     * Last resort, for environments promoted before {@code targetApiId} was recorded: the API serving the context
     * path this promotion is about is the one a previous promotion created. Only used once the promotion history has
     * confirmed this source API was already promoted here, so an unrelated API that merely happens to own the path
     * is never overwritten.
     *
     * <p>Overlap (containment) is the ambiguity check — {@code /orders} overlaps {@code /orders/v2} — but not
     * identity. After a unique overlap, the candidate must own every promoted path exactly (host + path). An
     * ambiguous or inexact match is treated as no match — the promotion fails on the path conflict instead of
     * guessing which API to overwrite.
     */
    private Optional<Api> findApiOwningTheContextPath(Promotion promotion, String targetEnvId) {
        var candidatePaths = extractPathsFromDefinition(promotion);
        if (candidatePaths.isEmpty()) {
            return Optional.empty();
        }

        var pathsByApiId = new HashMap<String, List<Path>>();
        searchPathBearingApis(targetEnvId).forEach(api -> {
            var paths = extractPaths(api);
            if (!paths.isEmpty()) {
                pathsByApiId.put(api.getId(), paths);
            }
        });

        var conflictingApiIds = pathsByApiId
            .entrySet()
            .stream()
            .filter(entry -> overlaps(entry.getValue(), candidatePaths))
            .map(java.util.Map.Entry::getKey)
            .distinct()
            .toList();

        if (conflictingApiIds.size() != 1) {
            if (conflictingApiIds.size() > 1) {
                log.warn(
                    "Promotion [{}]: {} APIs of environment [{}] conflict with the promoted context paths {}, not resolving a target",
                    promotion.getId(),
                    conflictingApiIds.size(),
                    targetEnvId,
                    candidatePaths
                );
            }
            return Optional.empty();
        }

        var apiId = conflictingApiIds.getFirst();
        if (!ownsEveryPromotedPathExactly(pathsByApiId.getOrDefault(apiId, List.of()), candidatePaths)) {
            log.warn(
                "Promotion [{}]: API [{}] overlaps the promoted context paths {} but does not own them exactly, not resolving a target",
                promotion.getId(),
                apiId,
                candidatePaths
            );
            return Optional.empty();
        }

        return apiCrudService.findById(apiId);
    }

    /** Same containment rule as {@code VerifyApiPathDomainService}: either path string starts with the other. */
    private static boolean overlaps(List<Path> existingPaths, List<Path> candidatePaths) {
        return candidatePaths
            .stream()
            .anyMatch(candidate ->
                pathsOnTheSameHost(existingPaths, candidate)
                    .stream()
                    .anyMatch(existingPath -> existingPath.startsWith(candidate.getPath()) || candidate.getPath().startsWith(existingPath))
            );
    }

    private static List<String> pathsOnTheSameHost(List<Path> existingPaths, Path candidate) {
        var candidateHasHost = candidate.getHost() != null && !candidate.getHost().isEmpty();
        return existingPaths
            .stream()
            .filter(existing -> {
                var existingHasHost = existing.getHost() != null && !existing.getHost().isEmpty();
                return candidateHasHost ? existingHasHost && Objects.equals(existing.getHost(), candidate.getHost()) : !existingHasHost;
            })
            .map(Path::getPath)
            .toList();
    }

    private static List<Path> extractPaths(Api api) {
        return switch (api.getApiDefinitionValue()) {
            case io.gravitee.definition.model.v4.Api v4Api -> Objects.requireNonNullElse(
                v4Api.getListeners(),
                List.<io.gravitee.definition.model.v4.listener.Listener>of()
            )
                .stream()
                .flatMap(listener -> listener instanceof HttpListener httpListener ? Stream.of(httpListener) : Stream.of())
                .flatMap(httpListener ->
                    Objects.requireNonNullElse(httpListener.getPaths(), List.<io.gravitee.definition.model.v4.listener.http.Path>of())
                        .stream()
                        .map(path ->
                            Path.builder()
                                .host(path.getHost())
                                .path(path.getPath())
                                .overrideAccess(path.isOverrideAccess())
                                .build()
                                .sanitize()
                        )
                )
                .toList();
            case io.gravitee.definition.model.Api v2Api -> v2Api.getProxy() == null || v2Api.getProxy().getVirtualHosts() == null
                ? List.of()
                : v2Api
                    .getProxy()
                    .getVirtualHosts()
                    .stream()
                    .map(virtualHost ->
                        Path.builder()
                            .host(virtualHost.getHost())
                            .path(virtualHost.getPath())
                            .overrideAccess(virtualHost.isOverrideEntrypoint())
                            .build()
                            .sanitize()
                    )
                    .toList();
            case null, default -> List.of();
        };
    }

    /** Host + path only; {@code overrideAccess} is not part of identity. */
    private static boolean ownsEveryPromotedPathExactly(List<Path> existingPaths, List<Path> promotedPaths) {
        return promotedPaths
            .stream()
            .allMatch(promoted ->
                existingPaths
                    .stream()
                    .anyMatch(
                        existing ->
                            Objects.equals(existing.getHost(), promoted.getHost()) && Objects.equals(existing.getPath(), promoted.getPath())
                    )
            );
    }

    private Stream<Api> searchPathBearingApis(String targetEnvId) {
        return apiQueryService.search(
            ApiSearchCriteria.builder()
                .environmentId(targetEnvId)
                .definitionVersion(List.of(DefinitionVersion.V2, DefinitionVersion.V4))
                .build(),
            null,
            ApiFieldFilter.builder().pictureExcluded(true).build()
        );
    }

    /**
     * Reads the HTTP listener paths out of the V4 definition JSON stored on the promotion. A definition we cannot
     * read paths from simply disables the context path resolution.
     */
    private List<Path> extractPathsFromDefinition(Promotion promotion) {
        try {
            var apiNode = jsonMapper.readTree(promotion.getApiDefinition()).get("api");
            if (apiNode == null || apiNode.isNull()) {
                return List.of();
            }

            var paths = new ArrayList<Path>();
            apiNode
                .path("listeners")
                .forEach(listener ->
                    listener
                        .path("paths")
                        .forEach(path -> {
                            var pathValue = path.path("path");
                            if (!pathValue.isMissingNode() && !pathValue.isNull()) {
                                var host = path.path("host");
                                paths.add(
                                    Path.builder()
                                        .host(host.isMissingNode() || host.isNull() ? null : host.asText())
                                        .path(pathValue.asText())
                                        .build()
                                        .sanitize()
                                );
                            }
                        })
                );
            return paths;
        } catch (Exception e) {
            log.warn("Could not extract context paths from promotion [{}] definition", promotion.getId(), e);
            return List.of();
        }
    }

    /**
     * Extracts the crossId from the API definition JSON stored in the promotion.
     */
    private String extractCrossIdFromDefinition(Promotion promotion) {
        try {
            var root = jsonMapper.readTree(promotion.getApiDefinition());

            // Try to get crossId directly (v2 format)
            var crossIdNode = root.get("crossId");
            if (crossIdNode != null && !crossIdNode.isNull()) {
                return crossIdNode.asText();
            }

            // Try to get crossId from api object (v4 format)
            var apiNode = root.get("api");
            if (apiNode != null && !apiNode.isNull()) {
                crossIdNode = apiNode.get("crossId");
                if (crossIdNode != null && !crossIdNode.isNull()) {
                    return crossIdNode.asText();
                }
            }

            throw new IllegalStateException("Could not find crossId in promotion " + promotion.getId() + " API definition");
        } catch (Exception e) {
            throw new TechnicalManagementException("An error occurred while trying to extract crossId from promotion " + promotion.getId());
        }
    }

    private DefinitionVersion getPromotionDefinitionVersion(Promotion promotion) {
        try {
            var root = jsonMapper.readTree(promotion.getApiDefinition());

            var graviteeNode = root.findValue("gravitee");
            if (graviteeNode != null && "2.0.0".equals(graviteeNode.asText())) {
                return DefinitionVersion.V2;
            }

            var definitionVersionNode = root.findValue("definitionVersion");
            if (definitionVersionNode != null && !definitionVersionNode.isNull()) {
                return DefinitionVersion.valueOf(definitionVersionNode.asText());
            }
        } catch (Exception e) {
            throw new TechnicalManagementException(
                "An error occurred while try to parse promotion definition version " + promotion.getId()
            );
        }

        throw new IllegalStateException("Could not determine definition version for promotion " + promotion.getId());
    }
}
