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
package io.gravitee.apim.infra.adapter;

import static com.google.common.base.Functions.identity;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PageCRD;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.node.logging.NodeLoggerFactory;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface ApiCRDAdapter {
    ApiCRDAdapter INSTANCE = Mappers.getMapper(ApiCRDAdapter.class);
    Logger log = NodeLoggerFactory.getLogger(ApiCRDAdapter.class);

    /**
     * Plan keys become plan HRIDs once the manifest is applied, so they must match the
     * HRID pattern of the Automation API.
     */
    Pattern HRID_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_.-]+[a-zA-Z0-9]$");

    @Mapping(target = "version", source = "apiEntity.apiVersion")
    @Mapping(target = "metadata", source = "exportEntity.metadata")
    @Mapping(target = "definitionContext", ignore = true)
    @Mapping(target = "plans", expression = "java(mapPlans(exportEntity))")
    @Mapping(target = "pages", expression = "java(mapPages(exportEntity))")
    @Mapping(target = "members", expression = "java(mapMembers(exportEntity))")
    @Mapping(target = "notifyMembers", expression = "java(!exportEntity.getApiEntity().isDisableMembershipNotifications())")
    @Mapping(target = "consoleNotificationConfiguration", source = "portalNotificationConfig")
    @Mapping(target = "groups", source = "apiEntity.groups")
    @Mapping(target = "allowedInApiProducts", source = "apiEntity.allowedInApiProducts")
    @Mapping(target = "allowMultiJwtOauth2Subscriptions", source = "apiEntity.allowMultiJwtOauth2Subscriptions")
    ApiCRDSpec toCRDSpec(ExportApiEntity exportEntity, ApiEntity apiEntity, PortalNotificationConfigEntity portalNotificationConfig);

    @Mapping(target = "version", source = "apiEntity.apiVersion")
    @Mapping(target = "metadata", source = "exportEntity.metadata")
    @Mapping(target = "definitionContext", ignore = true)
    @Mapping(target = "plans", expression = "java(mapPlans(exportEntity))")
    @Mapping(target = "pages", expression = "java(mapPages(exportEntity))")
    @Mapping(target = "members", expression = "java(mapMembers(exportEntity))")
    @Mapping(target = "notifyMembers", expression = "java(!exportEntity.getApiEntity().isDisableMembershipNotifications())")
    @Mapping(target = "consoleNotificationConfiguration", source = "portalNotificationConfig")
    @Mapping(target = "groups", source = "apiEntity.groups")
    @Mapping(target = "allowMultiJwtOauth2Subscriptions", source = "apiEntity.allowMultiJwtOauth2Subscriptions")
    @Mapping(target = "nativeAnalytics", source = "apiEntity.analytics")
    ApiCRDSpec toCRDSpec(ExportApiEntity exportEntity, NativeApiEntity apiEntity, PortalNotificationConfigEntity portalNotificationConfig);

    default ApiCRDSpec toCRDSpec(
        ExportApiEntity exportEntity,
        GenericApiEntity apiEntity,
        PortalNotificationConfigEntity portalNotificationConfig
    ) {
        if (apiEntity instanceof ApiEntity entity) {
            return toCRDSpec(exportEntity, entity, portalNotificationConfig);
        } else if (apiEntity instanceof NativeApiEntity nativeApiEntity) {
            return toCRDSpec(exportEntity, nativeApiEntity, portalNotificationConfig);
        }
        return null;
    }

    PlanCRD toCRDPlan(PlanEntity planEntity);
    PlanCRD toCRDPlan(NativePlanEntity planEntity);

    default PlanCRD toCRDPlan(GenericPlanEntity genericPlanEntity) {
        if (genericPlanEntity instanceof PlanEntity planEntity) {
            return toCRDPlan(planEntity);
        } else if (genericPlanEntity instanceof NativePlanEntity nativePlanEntity) {
            return toCRDPlan(nativePlanEntity);
        }
        return null;
    }

    default Map<String, PlanCRD> mapPlans(ExportApiEntity definition) {
        var plansMap = new HashMap<String, PlanCRD>();
        // Plans keyed by their HRID claim their keys before any name can, and a tie goes to the same plan on every export.
        var nonClosedPlans = definition
            .getPlans()
            .stream()
            .filter(plan -> !plan.isClosed())
            .sorted(comparing((GenericPlanEntity plan) -> chosenHrid(plan) == null).thenComparing(GenericPlanEntity::getId))
            .toList();
        for (var plan : nonClosedPlans) {
            var key = planKey(plan);
            if (plansMap.containsKey(key)) {
                key = key + "-" + stableSuffix(plan);
            }
            plansMap.put(key, toCRDPlan(plan));
        }
        return plansMap;
    }

    @Mapping(target = "source.configurationMap", source = "source.configuration", qualifiedByName = "deserializeConfig")
    PageCRD toCRDPage(PageEntity pageEntity);

    default Map<String, PageCRD> mapPages(ExportApiEntity definition) {
        return definition.getPages() != null
            ? definition
                .getPages()
                .stream()
                .map(this::clearAutoFetchedContent)
                .filter(Objects::nonNull)
                .map(this::toCRDPage)
                .collect(toMap(this::pageKey, identity(), (a, b) -> a, LinkedHashMap::new))
            : null;
    }

    private PageEntity clearAutoFetchedContent(PageEntity page) {
        PageSourceEntity source = page.getSource();
        if (source == null) {
            return page;
        }

        String pageType = source.getType();
        boolean isMarkdownOrSwagger = PageType.SWAGGER.name().equals(page.getType()) || PageType.MARKDOWN.name().equals(page.getType());
        boolean isAutoFetch =
            "github-fetcher".equals(pageType) ||
            "gitlab-fetcher".equals(pageType) ||
            "git-fetcher".equals(pageType) ||
            "http-fetcher".equals(pageType) ||
            "bitbucket-fetcher".equals(pageType);

        if (isMarkdownOrSwagger && isAutoFetch) {
            // Remove auto-fetched pages that was generated from ROOT github source
            if (page.getMetadata() != null && "auto_fetched".equals(page.getMetadata().get("graviteeio/fetcher_type"))) {
                return null;
            } else {
                page.setContent(null);
                page.setMetadata(null);
            }
        } else if (PageType.FOLDER.name().equals(page.getType()) && isAutoFetch) {
            // Remove auto-generated folders generated from ROOT github fetcher
            return null;
        }

        return page;
    }

    default String pageKey(PageCRD page) {
        if (page.getHrid() == null) {
            return page.getName() == null ? page.getId() : page.getName();
        } else {
            return page.getHrid();
        }
    }

    default SequencedSet<MemberCRD> mapMembers(ExportApiEntity definition) {
        return definition.getMembers() != null
            ? definition
                .getMembers()
                .stream()
                .map(me -> new MemberCRD(me.getId(), null, null, me.getRoles().getFirst().getName()))
                .collect(Collectors.toCollection(LinkedHashSet::new))
            : null;
    }

    @Named("deserializeConfig")
    default Map<String, Object> deserializeConfig(String configuration) {
        if (Objects.isNull(configuration)) {
            return Map.of();
        }

        ObjectMapper mapper = new GraviteeMapper();
        try {
            return mapper.readValue(configuration, LinkedHashMap.class);
        } catch (JsonProcessingException jse) {
            log.debug("Cannot parse configuration as LinkedHashMap: {}", configuration);
        }

        return Map.of();
    }

    /**
     * GKO identifies a plan by its key, so exporting the same plan twice must give the same key: the plan HRID
     * first, then the plan name with its case kept, as exports did before keys had to match the HRID pattern,
     * then the slugified name.
     */
    private static String planKey(GenericPlanEntity plan) {
        return Stream.of(chosenHrid(plan), plan.getName(), StringUtils.slugify(plan.getName()))
            .filter(Objects::nonNull)
            .map(ApiCRDAdapter::asGkoHrid)
            .filter(key -> HRID_PATTERN.matcher(key).matches())
            .findFirst()
            .orElseGet(() -> "plan-" + stableSuffix(plan));
    }

    /**
     * The plan HRID upgrader set every plan's HRID to its id, a placeholder nobody applied as a key. An API
     * duplication copies it to a plan with a new id, where it is kept: dropping every UUID would also drop the
     * keys GKO applied from exports keyed by plan id.
     */
    private static String chosenHrid(GenericPlanEntity plan) {
        var hrid = plan.getHrid();
        return hrid == null || hrid.equals(plan.getId()) ? null : hrid;
    }

    /**
     * GKO builds a plan HRID from its key by turning spaces and dots into dashes, so a key built the same way
     * comes back unchanged once GKO has applied it.
     */
    private static String asGkoHrid(String key) {
        return key.trim().replace(' ', '-').replace('.', '-');
    }

    /**
     * Derived from the plan id rather than random, so the plan keeps its key from one export to the next.
     */
    private static String stableSuffix(GenericPlanEntity plan) {
        return String.format("%08d", Math.floorMod(plan.getId().hashCode(), 100_000_000));
    }
}
