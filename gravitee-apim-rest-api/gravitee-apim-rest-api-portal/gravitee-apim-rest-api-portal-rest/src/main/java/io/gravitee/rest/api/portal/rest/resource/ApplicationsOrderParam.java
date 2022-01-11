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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.portal.rest.resource.param.AbstractParam;
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

    /**
     * Generate sortable only if order can use by repository
     * @return sortable
     */
    protected Sortable toSortable() {
        ApplicationsOrder order = this.getValue();
        if (order != null && order.canUseByRepository) {
            return new SortableImpl(order.field, order.isAsc);
        }
        return null;
    }

    public enum ApplicationsOrder {
        NAME("name", true, true),
        NAME_DESC("name", false, true),
        NB_SUBSCRIPTIONS("nbSubscriptions", true, false),
        NB_SUBSCRIPTIONS_DESC("nbSubscriptions", false, false);

        public static ApplicationsOrder forValue(String order) {
            boolean isAsc = !order.startsWith("-");
            String field = order.replace("-", "");
            return Arrays
                .stream(ApplicationsOrder.values())
                .filter(applicationsOrder -> applicationsOrder.field.equalsIgnoreCase(field) && applicationsOrder.isAsc == isAsc)
                .findFirst()
                .orElse(null);
        }

        public final String field;
        public final boolean isAsc;
        public final boolean canUseByRepository;

        ApplicationsOrder(String field, boolean isAsc, boolean canUseByRepository) {
            this.field = field;
            this.isAsc = isAsc;
            this.canUseByRepository = canUseByRepository;
        }

        @Override
        public String toString() {
            return isAsc ? field : "-" + field;
        }
    }
}
