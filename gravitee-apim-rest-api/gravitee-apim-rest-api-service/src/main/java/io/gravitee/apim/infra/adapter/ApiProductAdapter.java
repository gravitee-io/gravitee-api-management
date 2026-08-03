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

import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.node.logging.NodeLoggerFactory;
import io.gravitee.repository.management.model.ApiProduct;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;

@Mapper
public interface ApiProductAdapter {
    ApiProductAdapter INSTANCE = Mappers.getMapper(ApiProductAdapter.class);

    Logger log = NodeLoggerFactory.getLogger(ApiProductAdapter.class);

    @Mapping(target = "createdAt", qualifiedByName = "dateToZonedDateTime")
    @Mapping(target = "updatedAt", qualifiedByName = "dateToZonedDateTime")
    @Mapping(target = "primaryOwner", ignore = true)
    @Mapping(target = "analytics", qualifiedByName = "deserializeAnalytics")
    io.gravitee.apim.core.api_product.model.ApiProduct toModel(ApiProduct repositoryApiProduct);

    @Mapping(target = "createdAt", qualifiedByName = "zonedDateTimeToDate")
    @Mapping(target = "updatedAt", qualifiedByName = "zonedDateTimeToDate")
    @Mapping(target = "analytics", qualifiedByName = "serializeAnalytics")
    io.gravitee.repository.management.model.ApiProduct toRepository(io.gravitee.apim.core.api_product.model.ApiProduct domainApiProduct);

    @Named("deserializeAnalytics")
    default Analytics deserializeAnalytics(String analytics) {
        if (analytics == null || analytics.isBlank()) {
            return null;
        }
        try {
            return GraviteeJacksonMapper.getInstance().readValue(analytics, Analytics.class);
        } catch (IOException e) {
            log.error("Unexpected error while deserializing API Product analytics", e);
            return null;
        }
    }

    @Named("serializeAnalytics")
    default String serializeAnalytics(Analytics analytics) {
        if (analytics == null) {
            return null;
        }
        try {
            return GraviteeJacksonMapper.getInstance().writeValueAsString(analytics);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize API Product analytics", e);
        }
    }

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
