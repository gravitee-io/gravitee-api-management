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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.management.v2.rest.model.DuplicateApiOptions;
import io.gravitee.rest.api.model.api.DuplicateApiEntity;
import io.gravitee.rest.api.model.v4.api.DuplicateOptions;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class DuplicateApiMapperTest {

    private final DuplicateApiMapper mapper = Mappers.getMapper(DuplicateApiMapper.class);

    @Test
    void should_map_to_DuplicateApiEntity() {
        var duplicateApi = DuplicateApiOptions.builder()
            .contextPath("/context-path")
            .version("1.0")
            .filteredFields(
                Set.of(
                    DuplicateApiOptions.FilteredFieldsEnum.GROUPS,
                    DuplicateApiOptions.FilteredFieldsEnum.PAGES,
                    DuplicateApiOptions.FilteredFieldsEnum.PLANS,
                    DuplicateApiOptions.FilteredFieldsEnum.MEMBERS
                )
            )
            .build();

        DuplicateApiEntity result = mapper.mapToV2(duplicateApi);

        assertThat(result.getContextPath()).isEqualTo("/context-path");
        assertThat(result.getVersion()).isEqualTo("1.0");
        assertThat(result.getFilteredFields()).hasSize(4).contains("groups", "plans", "members", "pages");
    }

    @Test
    void should_map_to_DuplicateOptions() {
        var duplicateApi = DuplicateApiOptions.builder()
            .contextPath("/context-path")
            .version("1.0")
            .filteredFields(
                Set.of(
                    DuplicateApiOptions.FilteredFieldsEnum.GROUPS,
                    DuplicateApiOptions.FilteredFieldsEnum.PAGES,
                    DuplicateApiOptions.FilteredFieldsEnum.PLANS,
                    DuplicateApiOptions.FilteredFieldsEnum.MEMBERS
                )
            )
            .build();

        DuplicateOptions result = mapper.map(duplicateApi);

        assertThat(result.getContextPath()).isEqualTo("/context-path");
        assertThat(result.getVersion()).isEqualTo("1.0");
        assertThat(result.getFilteredFields())
            .hasSize(4)
            .contains(
                DuplicateOptions.FilteredFieldsEnum.MEMBERS,
                DuplicateOptions.FilteredFieldsEnum.PLANS,
                DuplicateOptions.FilteredFieldsEnum.GROUPS,
                DuplicateOptions.FilteredFieldsEnum.PAGES
            );
    }

    @Test
    void should_map_to_DuplicateOptions_with_host() {
        var duplicateApi = DuplicateApiOptions.builder()
            .host("host.gravitee.io")
            .version("1.0")
            .filteredFields(
                Set.of(
                    DuplicateApiOptions.FilteredFieldsEnum.GROUPS,
                    DuplicateApiOptions.FilteredFieldsEnum.PAGES,
                    DuplicateApiOptions.FilteredFieldsEnum.PLANS,
                    DuplicateApiOptions.FilteredFieldsEnum.MEMBERS
                )
            )
            .build();

        DuplicateOptions result = mapper.map(duplicateApi);

        assertThat(result.getHost()).isEqualTo("host.gravitee.io");
        assertThat(result.getVersion()).isEqualTo("1.0");
        assertThat(result.getFilteredFields())
            .hasSize(4)
            .contains(
                DuplicateOptions.FilteredFieldsEnum.MEMBERS,
                DuplicateOptions.FilteredFieldsEnum.PLANS,
                DuplicateOptions.FilteredFieldsEnum.GROUPS,
                DuplicateOptions.FilteredFieldsEnum.PAGES
            );
    }
}
