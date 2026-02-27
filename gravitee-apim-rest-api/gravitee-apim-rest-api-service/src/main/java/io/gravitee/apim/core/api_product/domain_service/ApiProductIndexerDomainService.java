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
package io.gravitee.apim.core.api_product.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.exception.ApiProductPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.search.model.IndexableApiProduct;
import lombok.CustomLog;

@DomainService
@CustomLog
public class ApiProductIndexerDomainService {

    private final ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService;
    private final Indexer indexer;

    public ApiProductIndexerDomainService(ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService, Indexer indexer) {
        this.apiProductPrimaryOwnerDomainService = apiProductPrimaryOwnerDomainService;
        this.indexer = indexer;
    }

    public void index(Context context, ApiProduct apiProductToIndex, PrimaryOwnerEntity primaryOwner) {
        indexer.index(context.toIndexationContext(), toIndexableApiProduct(apiProductToIndex, primaryOwner));
    }

    public void delete(Context context, ApiProduct apiProductToDelete) {
        Indexer.IndexationContext ctx = context.toIndexationContext();
        indexer.delete(ctx, toIndexableApiProduct(ctx, apiProductToDelete));
    }

    public IndexableApiProduct toIndexableApiProduct(Indexer.IndexationContext context, ApiProduct apiProduct) {
        try {
            var primaryOwner = apiProductPrimaryOwnerDomainService.getApiProductPrimaryOwner(context.organizationId(), apiProduct.getId());
            return toIndexableApiProduct(apiProduct, primaryOwner);
        } catch (ApiProductPrimaryOwnerNotFoundException e) {
            log.warn("Failed to retrieve API Product primary owner, will index without primary owner", e);
            return toIndexableApiProduct(apiProduct, null);
        }
    }

    public IndexableApiProduct toIndexableApiProduct(ApiProduct apiProduct, PrimaryOwnerEntity primaryOwner) {
        return new IndexableApiProduct(apiProduct, primaryOwner);
    }

    public static Context oneShotIndexation(AuditInfo auditInfo) {
        return new Context(auditInfo, false);
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
