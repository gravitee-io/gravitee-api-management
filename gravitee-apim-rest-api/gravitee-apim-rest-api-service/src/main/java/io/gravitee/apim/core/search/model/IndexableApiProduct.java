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
package io.gravitee.apim.core.search.model;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.ReferenceContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class IndexableApiProduct implements Indexable {

    @Builder.Default
    private ApiProduct apiProduct = new ApiProduct();

    private PrimaryOwnerEntity primaryOwner;

    @Override
    public String getId() {
        return apiProduct != null ? apiProduct.getId() : null;
    }

    @Override
    public void setId(String id) {
        if (apiProduct != null) {
            apiProduct.setId(id);
        }
    }

    @Override
    public String getReferenceType() {
        return ReferenceContext.Type.ENVIRONMENT.name();
    }

    @Override
    public void setReferenceType(String referenceType) {}

    @Override
    public String getReferenceId() {
        return apiProduct != null ? apiProduct.getEnvironmentId() : null;
    }

    @Override
    public void setReferenceId(String referenceId) {
        if (apiProduct != null) {
            apiProduct.setEnvironmentId(referenceId);
        }
    }
}
