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

import io.gravitee.rest.api.management.v2.rest.model.DuplicateApiOptions;
import io.gravitee.rest.api.model.api.DuplicateApiEntity;
import io.gravitee.rest.api.model.v4.api.DuplicateOptions;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper
public interface DuplicateApiMapper {
    DuplicateApiMapper INSTANCE = Mappers.getMapper(DuplicateApiMapper.class);

    default DuplicateApiEntity mapToV2(DuplicateApiOptions duplicateApiOptions) {
        if (duplicateApiOptions == null) {
            return null;
        }

        return new DuplicateApiEntity(
            duplicateApiOptions.getContextPath(),
            duplicateApiOptions
                .getFilteredFields()
                .stream()
                .map(filteredFieldsEnum -> filteredFieldsEnum.getValue().toLowerCase())
                .toList(),
            duplicateApiOptions.getVersion()
        );
    }

    DuplicateOptions map(DuplicateApiOptions duplicateApiOptions);
}
