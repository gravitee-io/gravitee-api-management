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
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService.ApiMetadataDecodeContext;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiCategoryQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.PrimaryOwnerApiTemplateData;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.exception.ApiPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.search.model.IndexableApi;
import java.util.Date;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@DomainService
@Slf4j
public class ApiIndexerDomainService {

    private final ApiMetadataDecoderDomainService apiMetadataDecoderDomainService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final ApiCategoryQueryService apiCategoryQueryService;
    private final Indexer indexer;

    public ApiIndexerDomainService(
        ApiMetadataDecoderDomainService apiMetadataDecoderDomainService,
        ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService,
        ApiCategoryQueryService apiCategoryQueryService,
        Indexer indexer
    ) {
        this.apiMetadataDecoderDomainService = apiMetadataDecoderDomainService;
        this.apiPrimaryOwnerDomainService = apiPrimaryOwnerDomainService;
        this.apiCategoryQueryService = apiCategoryQueryService;
        this.indexer = indexer;
    }

    public void index(Context context, Api apiToIndex, PrimaryOwnerEntity primaryOwner) {
        indexer.index(context.toIndexationContext(), toIndexableApi(apiToIndex, primaryOwner));
    }

    public void delete(Context context, Api apiToDelete) {
        Indexer.IndexationContext ctx = context.toIndexationContext();
        indexer.delete(ctx, toIndexableApi(ctx, apiToDelete));
    }

    private void commit() {
        indexer.commit();
    }

    /**
     * Build an {@link IndexableApi} from an {@link Api}.
     * @param context The indexation context.
     * @param apiToIndex The API to index
     * @return The {@link IndexableApi} to index.
     */
    public IndexableApi toIndexableApi(Indexer.IndexationContext context, Api apiToIndex) {
        try {
            var primaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(context.organizationId(), apiToIndex.getId());
            return toIndexableApi(apiToIndex, primaryOwner);
        } catch (ApiPrimaryOwnerNotFoundException e) {
            log.warn("Failed to retrieve API primary owner, API will we indexed without his primary owner", e);
            return toIndexableApi(apiToIndex, null);
        }
    }

    /**
     * Build an {@link IndexableApi} from an {@link Api}.
     * @param apiToIndex The API to index
     * @param primaryOwner The primary owner of the API.
     * @return The {@link IndexableApi} to index.
     */
    public IndexableApi toIndexableApi(Api apiToIndex, PrimaryOwnerEntity primaryOwner) {
        var metadata = apiMetadataDecoderDomainService.decodeMetadata(
            apiToIndex.getEnvironmentId(),
            apiToIndex.getId(),
            ApiMetadataDecodeContext
                .builder()
                .name(apiToIndex.getName())
                .description(apiToIndex.getDescription())
                .createdAt(Date.from(apiToIndex.getCreatedAt().toInstant()))
                .updatedAt(Date.from(apiToIndex.getUpdatedAt().toInstant()))
                .primaryOwner(
                    primaryOwner != null
                        ? new PrimaryOwnerApiTemplateData(
                            primaryOwner.id(),
                            primaryOwner.displayName(),
                            primaryOwner.email(),
                            primaryOwner.type().name()
                        )
                        : null
                )
                .build()
        );
        var categoryKeys = apiCategoryQueryService.findApiCategoryKeys(apiToIndex);
        return new IndexableApi(apiToIndex, primaryOwner, metadata, categoryKeys);
    }

    public static Context oneShotIndexation(AuditInfo auditInfo) {
        return new Context(auditInfo, false);
    }

    public Bulk bulk(AuditInfo auditInfo) {
        return new Bulk(this, auditInfo);
    }

    public record Bulk(ApiIndexerDomainService indexer, AuditInfo auditInfo) implements AutoCloseable, Supplier<Context> {
        @Override
        public void close() throws Exception {
            indexer.commit();
        }

        @Override
        public Context get() {
            return new Context(auditInfo, true);
        }
    }

    public static class Context {

        private final Indexer.IndexationContext context;

        public Context(AuditInfo auditInfo, boolean bulk) {
            this.context = new Indexer.IndexationContext(auditInfo.organizationId(), auditInfo.environmentId(), !bulk);
        }

        Indexer.IndexationContext toIndexationContext() {
            return context;
        }
    }
}
