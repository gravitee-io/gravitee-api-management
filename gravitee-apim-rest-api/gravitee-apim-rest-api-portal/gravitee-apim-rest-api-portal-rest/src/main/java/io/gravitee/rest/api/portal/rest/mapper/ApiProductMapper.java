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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.api_product.model.PortalApiProductDetails.ApiSummary;
import io.gravitee.rest.api.portal.rest.model.PortalApiProductApi;
import io.gravitee.rest.api.portal.rest.model.PortalApiProductDetails;
import io.gravitee.rest.api.portal.rest.model.PortalCatalogApiProductSummary;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApiProductMapper {
    ApiProductMapper INSTANCE = Mappers.getMapper(ApiProductMapper.class);

    PortalApiProductDetails map(io.gravitee.apim.core.api_product.model.PortalApiProductDetails apiProduct);

    PortalApiProductApi map(ApiSummary api);

    List<PortalCatalogApiProductSummary> map(List<io.gravitee.apim.core.portal_page.model.PortalCatalogApiProductSummary> apiProducts);

    PortalCatalogApiProductSummary map(io.gravitee.apim.core.portal_page.model.PortalCatalogApiProductSummary apiProduct);

    PortalApiProductApi map(io.gravitee.apim.core.portal_page.model.PortalCatalogApiProductSummary.ApiSummary api);

    default UUID map(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    default PortalApiProductDetails.KindEnum map(ApiProductKind kind) {
        return kind == null ? null : PortalApiProductDetails.KindEnum.valueOf(kind.name());
    }
}
