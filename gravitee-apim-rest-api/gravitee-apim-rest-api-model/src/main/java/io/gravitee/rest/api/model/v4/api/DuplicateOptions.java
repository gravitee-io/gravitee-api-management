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
package io.gravitee.rest.api.model.v4.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@With
public class DuplicateOptions {

    private String contextPath;
    private String version;

    private List<FilteredFieldsEnum> filteredFields;

    public boolean isGroupsFiltered() {
        return filteredFields.contains(FilteredFieldsEnum.GROUPS);
    }

    public boolean isPagesFiltered() {
        return filteredFields.contains(FilteredFieldsEnum.PAGES);
    }

    public boolean isPlansFiltered() {
        return filteredFields.contains(FilteredFieldsEnum.PLANS);
    }

    public boolean isMembersFiltered() {
        return filteredFields.contains(FilteredFieldsEnum.MEMBERS);
    }

    public enum FilteredFieldsEnum {
        GROUPS,
        PLANS,
        MEMBERS,
        PAGES,
    }
}
