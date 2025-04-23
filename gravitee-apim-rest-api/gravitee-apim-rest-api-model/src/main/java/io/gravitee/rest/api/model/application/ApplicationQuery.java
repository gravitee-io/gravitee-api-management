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
package io.gravitee.rest.api.model.application;

import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationQuery {

    private Set<String> ids;
    private String user;
    private Set<String> groups;
    private String status;
    private String name;
    private String query;

    private List<ApplicationExcludeFilter> excludeFilters;

    public boolean includePicture() {
        return include(ApplicationExcludeFilter.PICTURE);
    }

    public boolean includeOwner() {
        return include(ApplicationExcludeFilter.OWNER);
    }

    private boolean include(ApplicationExcludeFilter applicationExcludeFilter) {
        return excludeFilters == null || !excludeFilters.contains(applicationExcludeFilter);
    }
}
