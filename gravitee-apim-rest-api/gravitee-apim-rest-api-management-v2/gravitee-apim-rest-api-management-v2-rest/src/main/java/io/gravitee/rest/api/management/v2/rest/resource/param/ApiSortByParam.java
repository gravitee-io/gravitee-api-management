/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Objects;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.QueryParam;

public class ApiSortByParam {

    public enum ApiSortByEnum {
        NAME("name"),
        _NAME("-name"),
        PATHS("paths"),
        _PATHS("-paths");

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
