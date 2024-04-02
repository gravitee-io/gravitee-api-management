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
import io.gravitee.apim.core.documentation.model.PrimaryOwnerApiTemplateData;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.search.model.IndexableApi;
import java.util.Date;

@DomainService
public class ApiIndexerDomainService {

    private final ApiMetadataDecoderDomainService apiMetadataDecoderDomainService;
    private final ApiCategoryQueryService apiCategoryQueryService;
    private final Indexer indexer;

    public ApiIndexerDomainService(
        ApiMetadataDecoderDomainService apiMetadataDecoderDomainService,
        ApiCategoryQueryService apiCategoryQueryService,
        Indexer indexer
    ) {
        this.apiMetadataDecoderDomainService = apiMetadataDecoderDomainService;
        this.apiCategoryQueryService = apiCategoryQueryService;
        this.indexer = indexer;
    }

    public void index(Indexer.IndexationContext context, Api apiToIndex, PrimaryOwnerEntity primaryOwner) {
        var metadata = apiMetadataDecoderDomainService.decodeMetadata(
            apiToIndex.getId(),
            ApiMetadataDecodeContext
                .builder()
                .name(apiToIndex.getName())
                .description(apiToIndex.getDescription())
                .createdAt(Date.from(apiToIndex.getCreatedAt().toInstant()))
                .updatedAt(Date.from(apiToIndex.getUpdatedAt().toInstant()))
                .primaryOwner(
                    new PrimaryOwnerApiTemplateData(
                        primaryOwner.id(),
                        primaryOwner.displayName(),
                        primaryOwner.email(),
                        primaryOwner.type().name()
                    )
                )
                .build()
        );
        var categoryKeys = apiCategoryQueryService.findApiCategoryKeys(apiToIndex);

        indexer.index(context, new IndexableApi(apiToIndex, primaryOwner, metadata, categoryKeys));
    }
}
