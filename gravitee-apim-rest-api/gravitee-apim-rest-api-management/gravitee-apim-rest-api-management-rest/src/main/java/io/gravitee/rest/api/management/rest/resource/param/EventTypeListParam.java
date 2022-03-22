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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.EventType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import java.util.Arrays;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at gravitee.io)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties("empty")
@ArraySchema(uniqueItems = true)
public class EventTypeListParam extends AbstractListParam<EventType> {

    public EventTypeListParam(String param) {
        super(param);
        if (isEmpty()) {
            addAll(Arrays.asList(EventType.values()));
        }
    }

    @Override
    protected EventType parseValue(String param) {
        return EventType.valueOf(param);
    }
}
