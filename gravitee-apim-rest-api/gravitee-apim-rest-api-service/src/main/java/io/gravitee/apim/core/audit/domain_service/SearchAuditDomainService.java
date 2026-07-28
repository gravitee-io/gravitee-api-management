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
package io.gravitee.apim.core.audit.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.ApiAuditQueryFilters;
import io.gravitee.apim.core.api_product.model.ApiProductAuditQueryFilters;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.query_service.AuditMetadataQueryService;
import io.gravitee.apim.core.audit.query_service.AuditQueryService;
import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import lombok.CustomLog;

@CustomLog
@DomainService
public class SearchAuditDomainService {

    private final AuditQueryService auditQueryService;
    private final AuditMetadataQueryService auditMetadataQueryService;

    public SearchAuditDomainService(AuditQueryService auditQueryService, AuditMetadataQueryService auditMetadataQueryService) {
        this.auditQueryService = auditQueryService;
        this.auditMetadataQueryService = auditMetadataQueryService;
    }

    public MetadataPage<AuditEntity> searchApiAudit(ApiAuditQueryFilters query, Pageable pageable) {
        var response = auditQueryService.searchApiAudit(query, pageable);

        return new MetadataPage<>(
            response.audits(),
            pageable.getPageNumber(),
            pageable.getPageSize(),
            response.total(),
            buildMetadata(response.audits())
        );
    }

    public MetadataPage<AuditEntity> searchApiProductAudit(ApiProductAuditQueryFilters query, Pageable pageable) {
        var response = auditQueryService.searchApiProductAudit(query, pageable);
        return new MetadataPage<>(
            response.audits(),
            pageable.getPageNumber(),
            pageable.getPageSize(),
            response.total(),
            buildMetadata(response.audits())
        );
    }

    private Map<String, String> buildMetadata(List<AuditEntity> audits) {
        Map<String, String> metadata = new HashMap<>();
        for (var audit : audits) {
            var userMetadataKey = nameMetadataKey("USER", audit.getUser());
            if (!metadata.containsKey(userMetadataKey)) {
                metadata.put(userMetadataKey, auditMetadataQueryService.fetchUserNameMetadata(audit.getUser()));
            }

            var referenceMetadataKey = nameMetadataKey(audit.getReferenceType().name(), audit.getReferenceId());
            if (!metadata.containsKey(referenceMetadataKey)) {
                metadata.put(referenceMetadataKey, fetchReferenceName(audit));
            }

            if (audit.getProperties() != null) {
                for (Map.Entry<String, String> property : audit.getProperties().entrySet()) {
                    var propertyMetadataKey = nameMetadataKey(property.getKey(), property.getValue());
                    if (!metadata.containsKey(propertyMetadataKey)) {
                        metadata.put(
                            propertyMetadataKey,
                            auditMetadataQueryService.fetchPropertyMetadata(audit, property.getKey(), property.getValue())
                        );
                    }
                }
            }
        }

        return metadata;
    }

    /**
     * Consumers look the reference name up under the audit's own reference type, so the key has to be
     * built from it rather than assumed to be an API. Anything else keeps its id as the display value:
     * resolving it would mean a lookup against the wrong repository, which returns the id anyway.
     */
    private String fetchReferenceName(AuditEntity audit) {
        return audit.getReferenceType() == AuditEntity.AuditReferenceType.API
            ? auditMetadataQueryService.fetchApiNameMetadata(audit.getReferenceId())
            : audit.getReferenceId();
    }

    public static String nameMetadataKey(String type, String value) {
        return new StringJoiner(":").add(type).add(value).add("name").toString();
    }
}
