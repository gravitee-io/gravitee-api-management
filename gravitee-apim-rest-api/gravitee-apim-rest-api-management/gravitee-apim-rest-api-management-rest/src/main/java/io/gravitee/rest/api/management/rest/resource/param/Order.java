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
package io.gravitee.rest.api.management.rest.resource.param;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Order {

    private String field;
    private boolean order;
    private String type;

    public static Order parse(String param) {
        try {
            if (param != null) {
                String[] parts = param.split(":");
                Order order = new Order();
                order.setOrder(!parts[0].startsWith("-"));

                if (parts.length == 2) {
                    order.setType(order.isOrder() ? parts[0] : parts[0].substring(1));
                    order.setField(parts[1]);
                } else {
                    order.setField(order.isOrder() ? parts[0] : parts[0].substring(1));
                }

                return order;
            }
        } catch (IllegalArgumentException iae) {
            // Ignore in case of error
        }
        return null;
    }
}
