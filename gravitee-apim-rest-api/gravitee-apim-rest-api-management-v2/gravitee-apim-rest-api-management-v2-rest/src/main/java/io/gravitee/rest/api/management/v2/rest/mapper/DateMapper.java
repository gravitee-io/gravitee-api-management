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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DateMapper {
    DateMapper INSTANCE = Mappers.getMapper(DateMapper.class);

    default OffsetDateTime map(Date value) {
        return Objects.isNull(value) ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }

    default Date map(OffsetDateTime offsetDateTime) {
        return Objects.isNull(offsetDateTime) ? null : Date.from(offsetDateTime.toInstant());
    }

    default OffsetDateTime map(Instant value) {
        return Objects.isNull(value) ? null : value.atOffset(ZoneOffset.UTC);
    }

    default Instant mapToInstant(OffsetDateTime value) {
        return Objects.isNull(value) ? null : value.toInstant();
    }

    default OffsetDateTime map(ZonedDateTime zonedDateTime) {
        if (Objects.isNull(zonedDateTime)) {
            return null;
        }
        return zonedDateTime.toOffsetDateTime();
    }

    @Named("mapTimestamp")
    default OffsetDateTime mapTimestamp(String timestamp) {
        return OffsetDateTime.parse(timestamp);
    }
}
