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

import io.gravitee.repository.management.model.ApiProduct;
import java.time.ZonedDateTime;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApiProductAdapter {
    ApiProductAdapter INSTANCE = Mappers.getMapper(ApiProductAdapter.class);

    @Mapping(target = "createdAt", qualifiedByName = "dateToZonedDateTime")
    @Mapping(target = "updatedAt", qualifiedByName = "dateToZonedDateTime")
    io.gravitee.apim.core.api_product.model.ApiProduct toModel(ApiProduct repositoryApiProduct);

    @Mapping(target = "createdAt", qualifiedByName = "zonedDateTimeToDate")
    @Mapping(target = "updatedAt", qualifiedByName = "zonedDateTimeToDate")
    io.gravitee.repository.management.model.ApiProduct toRepository(io.gravitee.apim.core.api_product.model.ApiProduct domainApiProduct);

    @Named("dateToZonedDateTime")
    default ZonedDateTime dateToZonedDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(java.time.ZoneId.systemDefault());
    }

    @Named("zonedDateTimeToDate")
    default Date zonedDateTimeToDate(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return Date.from(zonedDateTime.toInstant());
    }
}
