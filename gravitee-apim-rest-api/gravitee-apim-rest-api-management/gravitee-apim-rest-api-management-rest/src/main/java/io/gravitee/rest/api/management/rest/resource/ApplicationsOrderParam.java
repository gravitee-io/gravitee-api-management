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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.rest.api.management.rest.resource.param.AbstractParam;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import java.util.Arrays;

public class ApplicationsOrderParam extends AbstractParam<ApplicationsOrderParam.ApplicationsOrder> {

    public ApplicationsOrderParam(String param) {
        super(param);
    }

    @Override
    protected ApplicationsOrder parse(String param) {
        if (param != null) {
            return ApplicationsOrder.forValue(param.toLowerCase());
        }
        return null;
    }

    protected Sortable toSortable() {
        ApplicationsOrder order = this.getValue();
        if (order != null) {
            return new SortableImpl(order.field, order.isAsc);
        }
        return null;
    }

    public enum ApplicationsOrder {
        NAME("name", true),
        NAME_DESC("name", false),
        UPDATED_AT("updated_at", true),
        UPDATED_AT_DESC("updated_at", false);

        public static ApplicationsOrder forValue(String order) {
            boolean isAsc = !order.startsWith("-");
            String field = order.replace("-", "");
            return Arrays.stream(ApplicationsOrder.values())
                .filter(o -> o.field.equalsIgnoreCase(field) && o.isAsc == isAsc)
                .findFirst()
                .orElse(null);
        }

        public final String field;
        public final boolean isAsc;

        ApplicationsOrder(String field, boolean isAsc) {
            this.field = field;
            this.isAsc = isAsc;
        }

        @Override
        public String toString() {
            return isAsc ? field : "-" + field;
        }
    }
}
