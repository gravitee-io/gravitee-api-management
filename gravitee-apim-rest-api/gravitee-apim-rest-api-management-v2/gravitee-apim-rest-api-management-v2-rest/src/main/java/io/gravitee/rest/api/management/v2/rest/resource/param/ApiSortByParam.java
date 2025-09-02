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
package io.gravitee.rest.api.management.v2.rest.resource.param;

import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.QueryParam;
import java.util.Objects;

public class ApiSortByParam {

    public enum ApiSortByEnum {
        NAME_ASC("name"),
        NAME_DESC("-name"),
        PATHS_ASC("paths"),
        PATHS_DESC("-paths"),
        API_TYPE_ASC("api_type"),
        API_TYPE_DESC("-api_type"),
        STATUS_ASC("status"),
        STATUS_DESC("-status"),
        ACCESS_ASC("access"),
        ACCESS_DESC("-access"),
        TAGS_ASC("tags_asc"),
        TAGS_DESC("-tags_desc"),
        CATEGORIES_ASC("categories_asc"),
        CATEGORIES_DESC("-categories_desc"),
        OWNER_ASC("owner"),
        OWNER_DESC("-owner"),
        PORTAL_STATUS_ASC("portal_status"),
        PORTAL_STATUS_DESC("-portal_status"),
        VISIBILITY_ASC("visibility"),
        VISIBILITY_DESC("-visibility");

        private String value;

        ApiSortByEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

        public static ApiSortByEnum getByValue(String value) {
            for (ApiSortByEnum orderByEnum : ApiSortByEnum.values()) {
                if (orderByEnum.getValue().equals(value)) {
                    return orderByEnum;
                }
            }
            return null;
        }
    }

    @QueryParam("sortBy")
    String sortBy;

    public void validate() {
        if (Objects.nonNull(this.sortBy) && Objects.isNull(ApiSortByEnum.getByValue(this.sortBy))) {
            throw new BadRequestException("Invalid sortBy parameter: " + this.sortBy);
        }
    }

    public Sortable toSortable() {
        if (Objects.isNull(this.sortBy)) {
            return null;
        }
        boolean isAsc = !this.sortBy.startsWith("-");
        String field = this.sortBy.replace("-", "");

        return new SortableImpl(field, isAsc);
    }
}
