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
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class ApiProductSortByParam {

    public enum ApiProductSortByEnum {
        NAME_ASC("name", "name", true),
        NAME_DESC("-name", "name", false);

        private final String paramValue;
        private final String fieldName;
        private final boolean ascOrder;

        ApiProductSortByEnum(String paramValue, String fieldName, boolean ascOrder) {
            this.paramValue = paramValue;
            this.fieldName = fieldName;
            this.ascOrder = ascOrder;
        }

        public String getParamValue() {
            return paramValue;
        }

        public String getFieldName() {
            return fieldName;
        }

        public boolean isAscOrder() {
            return ascOrder;
        }

        public static ApiProductSortByEnum getByValue(String value) {
            if (value == null) return null;
            for (ApiProductSortByEnum e : values()) {
                if (e.getParamValue().equals(value)) {
                    return e;
                }
            }
            return null;
        }

        public static String allowedValues() {
            return Arrays.stream(values()).map(ApiProductSortByEnum::getParamValue).collect(Collectors.joining(", "));
        }
    }

    @QueryParam("sortBy")
    String sortBy;

    public void validate() {
        if (Objects.nonNull(sortBy) && Objects.isNull(ApiProductSortByEnum.getByValue(sortBy))) {
            throw new BadRequestException("Invalid sortBy parameter: " + sortBy + ". Allowed: " + ApiProductSortByEnum.allowedValues());
        }
    }

    public Sortable toSortable() {
        if (Objects.isNull(sortBy)) {
            return new SortableImpl(ApiProductSortByEnum.NAME_ASC.getFieldName(), ApiProductSortByEnum.NAME_ASC.isAscOrder());
        }
        ApiProductSortByEnum match = ApiProductSortByEnum.getByValue(sortBy);
        return new SortableImpl(match.getFieldName(), match.isAscOrder());
    }
}
