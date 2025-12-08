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

import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.rest.api.management.v2.rest.mapper.CorsMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.BaseOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV4;
import io.gravitee.rest.api.management.v2.rest.model.Entrypoint;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.Metadata;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.context.OriginContext;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(
    uses = {
        ApiMapper.class, DateMapper.class, MemberMapper.class, MetadataMapper.class, PageMapper.class, PlanMapper.class, FlowMapper.class,
    }
)
public interface ImportExportApiMapper {
    ImportExportApiMapper INSTANCE = Mappers.getMapper(ImportExportApiMapper.class);
    GraviteeMapper JSON_MAPPER = new GraviteeMapper();

    default ExportApiV4 map(GraviteeDefinition src) {
        return switch (src) {
            case null -> null;
            case GraviteeDefinition.V4 v4 -> map(v4);
            case GraviteeDefinition.Native natV4 -> map(natV4);
            // we don't allow to export federated APIs
            case GraviteeDefinition.Federated fed -> throw new IllegalStateException("Unexpected API: " + src);
            // we don't allow to export v2 APIs
            case GraviteeDefinition.V2 v2 -> throw new IllegalStateException("Unexpected API: " + src);
        };
    }

    ExportApiV4 map(GraviteeDefinition.V4 exportApiEntityV4);

    ExportApiV4 map(GraviteeDefinition.Native exportApiEntityV4);

    default ApiV4 map(ApiDescriptor src) {
        return switch (src) {
            case null -> null;
            case ApiDescriptor.ApiDescriptorV4 v4 -> map(v4);
            case ApiDescriptor.Native natV4 -> map(natV4);
            // we don't allow to export federated APIs
            case ApiDescriptor.Federated fed -> throw new IllegalStateException("Unexpected API: " + src);
            // we don't allow to export v2 APIs
            case ApiDescriptor.ApiDescriptorV2 v2 -> throw new IllegalStateException("Unexpected API: " + src);
        };
    }

    @Mapping(target = "metadata", ignore = true)
    ApiV4 map(ApiDescriptor.ApiDescriptorV4 src);

    @Mapping(target = "type", constant = "NATIVE")
    @Mapping(target = "metadata", ignore = true)
    ApiV4 map(ApiDescriptor.Native src);

    @AfterMapping
    default void clearMetadata(@MappingTarget ApiV4 target) {
        // Explicitly set metadata to null to prevent empty object serialization in export
        target.setMetadata(null);
    }

    default io.gravitee.rest.api.management.v2.rest.model.Listener map(Listener src) {
        return switch (src) {
            case null -> null;
            case HttpListener http -> new io.gravitee.rest.api.management.v2.rest.model.Listener(mapHttpListener(http));
            case SubscriptionListener subscription -> new io.gravitee.rest.api.management.v2.rest.model.Listener(
                mapSubscriptionListener(subscription)
            );
            case TcpListener tcp -> new io.gravitee.rest.api.management.v2.rest.model.Listener(mapTcpListener(tcp));
            default -> throw new IllegalStateException("Unexpected value: " + src);
        };
    }

    default BaseOriginContext map(OriginContext src) {
        return switch (src) {
            case null -> null;
            case OriginContext.Management management -> new io.gravitee.rest.api.management.v2.rest.model.ManagementOriginContext().origin(
                BaseOriginContext.OriginEnum.MANAGEMENT
            );
            case OriginContext.Kubernetes k8s -> new io.gravitee.rest.api.management.v2.rest.model.KubernetesOriginContext().origin(
                BaseOriginContext.OriginEnum.KUBERNETES
            );
            case OriginContext.Integration integration -> new io.gravitee.rest.api.management.v2.rest.model.IntegrationOriginContext()
                .integrationId(integration.integrationId())
                .integrationName(integration.integrationName())
                .provider(integration.provider())
                .origin(BaseOriginContext.OriginEnum.INTEGRATION);
        };
    }

    @Mapping(target = "cors", expression = "java(CorsMapper.INSTANCE.map(http.getCors()))")
    io.gravitee.rest.api.management.v2.rest.model.HttpListener mapHttpListener(HttpListener http);

    io.gravitee.rest.api.management.v2.rest.model.SubscriptionListener mapSubscriptionListener(SubscriptionListener subscription);
    io.gravitee.rest.api.management.v2.rest.model.TcpListener mapTcpListener(TcpListener tcp);

    default io.gravitee.rest.api.management.v2.rest.model.Listener map(NativeListener src) {
        return switch (src) {
            case null -> null;
            case KafkaListener kafka -> new io.gravitee.rest.api.management.v2.rest.model.Listener(mapKafkaListener(kafka));
            default -> throw new IllegalStateException("Unexpected value: " + src);
        };
    }

    io.gravitee.rest.api.management.v2.rest.model.KafkaListener mapKafkaListener(KafkaListener kafka);

    @SneakyThrows
    @AfterMapping
    default void mapConfiguration(Object ignored, @MappingTarget Entrypoint target) {
        if (target.getConfiguration() instanceof String conf) {
            target.setConfiguration(JSON_MAPPER.readTree(conf));
        }
    }

    @SneakyThrows
    @AfterMapping
    default void mapConfiguration(Object ignored, @MappingTarget EndpointV4 target) {
        if (target.getConfiguration() instanceof String conf) {
            target.setConfiguration(JSON_MAPPER.readTree(conf));
        }
    }

    default Map<String, Map<String, io.gravitee.rest.api.management.v2.rest.model.ResponseTemplate>> map(
        @Nullable Map<String, Map<String, ResponseTemplate>> value
    ) {
        if (value == null) {
            return Map.of();
        }
        return stream(value.entrySet())
            .map(e ->
                Map.entry(e.getKey(), stream(e.getValue().entrySet()).collect(Collectors.toMap(Map.Entry::getKey, o -> map(o.getValue()))))
            )
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    io.gravitee.rest.api.management.v2.rest.model.ResponseTemplate map(ResponseTemplate value);

    @Mapping(target = "apiExport", expression = "java(buildApiExport(exportApiV4))")
    @Mapping(target = "plans", expression = "java(buildPlans(exportApiV4))")
    ImportDefinition toImportDefinition(ExportApiV4 exportApiV4);

    @Mapping(target = "type", constant = "USER")
    @Mapping(target = "referenceType", constant = "API")
    @Mapping(target = "referenceId", expression = "java(apiId)")
    MemberEntity map(Member member, String apiId);

    default ApiExport buildApiExport(ExportApiV4 exportApiV4) {
        final ApiExport apiExport = ApiMapper.INSTANCE.toApiExport(exportApiV4.getApi());
        apiExport.setPicture(exportApiV4.getApiPicture());
        apiExport.setBackground(exportApiV4.getApiBackground());
        Optional.ofNullable(exportApiV4)
            .map(ExportApiV4::getApi)
            .map(ApiV4::getPrimaryOwner)
            .ifPresent(po -> {
                var type = Optional.ofNullable(po.getType())
                    .map(t -> PrimaryOwnerEntity.Type.valueOf(t.getValue()))
                    .orElse(PrimaryOwnerEntity.Type.USER);

                PrimaryOwnerEntity primaryOwnerEntity = PrimaryOwnerEntity.builder()
                    .id(po.getId())
                    .email(po.getEmail())
                    .displayName(po.getDisplayName())
                    .type(type)
                    .build();
                apiExport.setPrimaryOwner(primaryOwnerEntity);
            });
        return apiExport;
    }

    default Set<PlanWithFlows> buildPlans(ExportApiV4 exportApiV4) {
        if (exportApiV4.getPlans() == null || exportApiV4.getApi() == null) {
            return null;
        }

        return PlanMapper.INSTANCE.map(exportApiV4.getPlans(), exportApiV4.getApi().getType());
    }

    @Mapping(target = "apiId", expression = "java(apiId)")
    ApiMetadataEntity map(Metadata metadata, String apiId);
}
