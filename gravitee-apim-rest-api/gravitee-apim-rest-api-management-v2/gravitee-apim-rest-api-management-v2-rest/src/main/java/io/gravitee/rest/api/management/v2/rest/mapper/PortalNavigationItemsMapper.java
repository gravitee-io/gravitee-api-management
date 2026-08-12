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

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.use_case.FetchPortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.SeedDefaultPagesForPortalNavigationItemsUseCase;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.rest.api.management.v2.rest.model.BaseCreatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.BaseUpdatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationApiProduct;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.FetchPortalNavigationItemResponse;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemApiSummary;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemFetchResult;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemsFetchSummary;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageContentType;
import io.gravitee.rest.api.management.v2.rest.model.SeedDefaultPagesRequest;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationApiProduct;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationPage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class })
public interface PortalNavigationItemsMapper {
    PortalNavigationItemsMapper INSTANCE = Mappers.getMapper(PortalNavigationItemsMapper.class);

    @Mapping(target = "type", constant = "PAGE")
    @Mapping(target = "portalPageContentId", expression = "java(page.getPortalPageContentId().id())")
    @Mapping(target = "rootId", source = "rootId", qualifiedByName = "portalNavigationItemIdToUuid")
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage map(
        io.gravitee.apim.core.portal_page.model.PortalNavigationPage page
    );

    @Mapping(target = "type", constant = "FOLDER")
    @Mapping(target = "rootId", source = "rootId", qualifiedByName = "portalNavigationItemIdToUuid")
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder map(
        io.gravitee.apim.core.portal_page.model.PortalNavigationFolder folder
    );

    @Mapping(target = "type", constant = "LINK")
    @Mapping(target = "rootId", source = "rootId", qualifiedByName = "portalNavigationItemIdToUuid")
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationLink map(
        io.gravitee.apim.core.portal_page.model.PortalNavigationLink link
    );

    @Mapping(target = "type", constant = "API")
    @Mapping(target = "rootId", source = "rootId", qualifiedByName = "portalNavigationItemIdToUuid")
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationApi map(io.gravitee.apim.core.portal_page.model.PortalNavigationApi api);

    @Mapping(target = "type", constant = "API_PRODUCT")
    @Mapping(target = "apiProductId", source = "apiProductId")
    @Mapping(target = "rootId", source = "rootId", qualifiedByName = "portalNavigationItemIdToUuid")
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationApiProduct map(
        io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct apiProduct
    );

    default List<PortalNavigationItem> map(List<io.gravitee.apim.core.portal_page.model.PortalNavigationItem> items) {
        return items.stream().map(this::map).toList();
    }

    default PortalNavigationItem map(io.gravitee.apim.core.portal_page.model.PortalNavigationItem portalNavigationItem) {
        if (portalNavigationItem == null) {
            return null;
        }
        return switch (portalNavigationItem) {
            case io.gravitee.apim.core.portal_page.model.PortalNavigationFolder folder -> new PortalNavigationItem(map(folder));
            case io.gravitee.apim.core.portal_page.model.PortalNavigationPage page -> new PortalNavigationItem(map(page));
            case io.gravitee.apim.core.portal_page.model.PortalNavigationLink link -> new PortalNavigationItem(map(link));
            case io.gravitee.apim.core.portal_page.model.PortalNavigationApi api -> new PortalNavigationItem(map(api));
            case io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct apiProduct -> new PortalNavigationItem(map(apiProduct));
        };
    }

    @Mapping(
        target = "portalPageContentId",
        expression = "java(page.getPortalPageContentId() == null ? null : io.gravitee.apim.core.portal_page.model.PortalPageContentId.of(page.getPortalPageContentId().toString()))"
    )
    @Mapping(
        target = "contentType",
        expression = "java(page.getContentType() == null ? io.gravitee.apim.core.portal_page.model.PortalPageContentType.GRAVITEE_MARKDOWN : map(page.getContentType()))"
    )
    io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationPage page
    );

    @Mapping(target = "contentType", constant = "GRAVITEE_MARKDOWN")
    io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationFolder folder
    );

    @Mapping(target = "contentType", constant = "GRAVITEE_MARKDOWN")
    io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationLink link
    );

    @Mapping(target = "contentType", constant = "GRAVITEE_MARKDOWN")
    io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationApi api
    );

    @Mapping(target = "contentType", constant = "GRAVITEE_MARKDOWN")
    @Mapping(target = "apiProductId", source = "apiProductId")
    io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem map(CreatePortalNavigationApiProduct apiProduct);

    default io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem map(
        BaseCreatePortalNavigationItem createPortalNavigationItem
    ) {
        return switch (createPortalNavigationItem) {
            case CreatePortalNavigationFolder folder -> map(folder);
            case CreatePortalNavigationPage page -> map(page);
            case CreatePortalNavigationLink link -> map(link);
            case CreatePortalNavigationApi api -> map(api);
            case CreatePortalNavigationApiProduct apiProduct -> map(apiProduct);
            default -> throw new TechnicalDomainException(
                String.format("Unknown PortalNavigationItem class %s", createPortalNavigationItem.getClass().getSimpleName())
            );
        };
    }

    default List<io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem> mapCreatePortalNavigationItems(
        List<io.gravitee.rest.api.management.v2.rest.model.BaseCreatePortalNavigationItem> createPortalNavigationItems
    ) {
        return createPortalNavigationItems.stream().map(this::map).toList();
    }

    default SeedDefaultPagesForPortalNavigationItemsUseCase.Input mapSeedDefaultPagesInput(
        String organizationId,
        String environmentId,
        SeedDefaultPagesRequest request
    ) {
        return new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(
            organizationId,
            environmentId,
            request.getIds().stream().map(this::map).toList()
        );
    }

    PortalNavigationItemFetchResult map(FetchPortalNavigationItemUseCase.PageFetchResult result);

    PortalNavigationItemsFetchSummary map(FetchPortalNavigationItemUseCase.FetchSummary summary);

    FetchPortalNavigationItemResponse map(FetchPortalNavigationItemUseCase.Output output);

    // Hand-built because the fetch state is readOnly in the OpenAPI spec: the generated model only
    // exposes it through its @JsonCreator constructor, which MapStruct cannot target.
    default io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemSource map(
        io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource source
    ) {
        if (source == null) {
            return null;
        }
        return new io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemSource(
            DateMapper.INSTANCE.map(source.getLastFetchedAt()),
            DateMapper.INSTANCE.map(source.getLastFetchAttemptAt()),
            source.getLastFetchError()
        )
            .type(source.getSourceType())
            .configuration(ConfigurationSerializationMapper.INSTANCE.deserializeConfiguration(source.getSourceConfiguration()))
            .useAutoFetch(source.isUseAutoFetch())
            .fetchCron(source.getFetchCron());
    }

    // readOnly in the spec does not stop Jackson from deserializing: fetch state never comes from the client
    @Mapping(target = "sourceType", source = "type")
    @Mapping(target = "sourceConfiguration", source = "configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "lastFetchedAt", ignore = true)
    @Mapping(target = "lastFetchAttemptAt", ignore = true)
    @Mapping(target = "lastFetchError", ignore = true)
    io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource map(
        io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemSource source
    );

    default PortalNavigationItemId map(UUID id) {
        return id == null ? null : PortalNavigationItemId.of(id.toString());
    }

    @Named("portalNavigationItemIdToUuid")
    default UUID mapToUuid(io.gravitee.apim.core.portal_page.model.PortalNavigationItemId id) {
        return id != null ? id.id() : null;
    }

    default String map(io.gravitee.apim.core.portal_page.model.PortalNavigationItemId id) {
        return id != null ? id.json() : null;
    }

    default io.gravitee.apim.core.portal_page.model.PortalPageContentType map(PortalPageContentType type) {
        if (type == null) return null;
        return io.gravitee.apim.core.portal_page.model.PortalPageContentType.valueOf(type.name());
    }

    io.gravitee.apim.core.portal_page.model.PortalNavigationItemType map(
        io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemType type
    );

    default io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem map(
        BaseUpdatePortalNavigationItem updatePortalNavigationItem
    ) {
        return switch (updatePortalNavigationItem) {
            case UpdatePortalNavigationFolder folder -> map(folder);
            case UpdatePortalNavigationPage page -> map(page);
            case UpdatePortalNavigationLink link -> map(link);
            case UpdatePortalNavigationApi api -> map(api);
            case UpdatePortalNavigationApiProduct apiProduct -> map(apiProduct);
            default -> throw new TechnicalDomainException(
                String.format("Unknown PortalNavigationItem class %s", updatePortalNavigationItem.getClass().getSimpleName())
            );
        };
    }

    io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationPage page
    );

    io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationFolder folder
    );

    io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationLink link
    );

    io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationApi api
    );

    io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem map(UpdatePortalNavigationApiProduct apiProduct);

    default PortalCategoryId mapCategoryId(UUID id) {
        if (id == null) {
            return null;
        }
        return new PortalCategoryId(id);
    }

    default UUID mapCategoryId(PortalCategoryId id) {
        if (id == null) {
            return null;
        }
        return id.id();
    }

    /**
     * Maps the domain APIs already resolved by the use case (via {@code ApiPortalSearchQueryService},
     * version-agnostic) to the REST summary DTO, keyed by navigation item id.
     */
    default Map<String, PortalNavigationItemApiSummary> mapApisMetadata(
        Map<PortalNavigationItemId, io.gravitee.apim.core.api.model.Api> apisByNavigationItemId
    ) {
        Map<String, PortalNavigationItemApiSummary> summariesByNavigationItemId = new HashMap<>();
        apisByNavigationItemId.forEach((navigationItemId, api) ->
            summariesByNavigationItemId.put(navigationItemId.json(), mapApiSummary(api))
        );
        return summariesByNavigationItemId;
    }

    default PortalNavigationItemApiSummary mapApiSummary(io.gravitee.apim.core.api.model.Api api) {
        return new PortalNavigationItemApiSummary()
            .id(api.getId())
            .name(api.getName())
            .apiVersion(api.getVersion())
            .contextPath(resolveApiContextPath(api));
    }

    /**
     * Mirrors the frontend's {@code getApiAccess}/{@code getApiContextPath} utilities
     * (gravitee-apim-console-webui/src/shared/utils/api-access.util.ts): returns the API's primary
     * access path for display, or {@code null} for API versions with no gateway-managed endpoint
     * (federated, federated agent).
     */
    private String resolveApiContextPath(io.gravitee.apim.core.api.model.Api api) {
        var v2Definition = api.getApiDefinition();
        if (v2Definition != null) {
            var virtualHosts = v2Definition.getProxy() != null ? v2Definition.getProxy().getVirtualHosts() : null;
            if (virtualHosts == null || virtualHosts.isEmpty()) {
                return null;
            }
            VirtualHost virtualHost = virtualHosts.get(0);
            return (virtualHost.getHost() != null ? virtualHost.getHost() : "") + virtualHost.getPath();
        }

        var listeners = api.getApiListeners();

        String tcpHost = listeners
            .stream()
            .filter(listener -> listener.getType() == ListenerType.TCP)
            .map(TcpListener.class::cast)
            .flatMap(listener -> listener.getHosts().stream())
            .findFirst()
            .orElse(null);
        if (tcpHost != null) {
            return tcpHost;
        }

        String kafkaHost = listeners
            .stream()
            .filter(listener -> listener.getType() == ListenerType.KAFKA)
            .map(KafkaListener.class::cast)
            .map(KafkaListener::getHost)
            .findFirst()
            .orElse(null);
        if (kafkaHost != null) {
            return kafkaHost;
        }

        return listeners
            .stream()
            .filter(listener -> listener.getType() == ListenerType.HTTP)
            .map(HttpListener.class::cast)
            .flatMap(listener -> listener.getPaths().stream())
            .findFirst()
            .map(path -> (path.getHost() != null ? path.getHost() : "") + path.getPath())
            .orElse(null);
    }
}
