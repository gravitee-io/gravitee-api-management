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

import static io.gravitee.apim.core.audit.domain_service.SearchAuditDomainService.nameMetadataKey;

import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.rest.api.management.v2.rest.model.Audit;
import io.gravitee.rest.api.management.v2.rest.model.AuditPropertiesInner;
import io.gravitee.rest.api.management.v2.rest.model.AuditReference;
import io.gravitee.rest.api.management.v2.rest.model.BaseUser;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { ApplicationMapper.class, DateMapper.class, PlanMapper.class })
public interface ApiAuditMapper {
    Logger logger = LoggerFactory.getLogger(ApiAuditMapper.class);
    ApiAuditMapper INSTANCE = Mappers.getMapper(ApiAuditMapper.class);

    @Named("mapToAudit")
    default Audit map(AuditEntity source, Map<String, String> metadata) {
        return new Audit()
            .id(source.getId())
            .organizationId(source.getOrganizationId())
            .environmentId(source.getEnvironmentId())
            .reference(
                new AuditReference()
                    .id(source.getReferenceId())
                    .type(AuditReference.TypeEnum.valueOf(source.getReferenceType().name()))
                    .name(metadata.get(nameMetadataKey(source.getReferenceType().name(), source.getReferenceId())))
            )
            .user(new BaseUser().id(source.getUser()).displayName(metadata.get(nameMetadataKey("USER", source.getUser()))))
            .properties(adaptProperties(source, metadata))
            .event(source.getEvent())
            .createdAt(source.getCreatedAt().toOffsetDateTime())
            .patch(source.getPatch());
    }

    default List<Audit> map(List<AuditEntity> source, Map<String, String> metadata) {
        return source
            .stream()
            .map(audit -> map(audit, metadata))
            .toList();
    }

    @Named("adaptProperties")
    default List<AuditPropertiesInner> adaptProperties(AuditEntity source, Map<String, String> metadata) {
        if (source.getProperties() == null) {
            return List.of();
        }

        return source
            .getProperties()
            .entrySet()
            .stream()
            .map(entry ->
                new AuditPropertiesInner()
                    .key(entry.getKey())
                    .value(entry.getValue())
                    .name(metadata.get(nameMetadataKey(entry.getKey(), entry.getValue())))
            )
            .toList();
    }
}
