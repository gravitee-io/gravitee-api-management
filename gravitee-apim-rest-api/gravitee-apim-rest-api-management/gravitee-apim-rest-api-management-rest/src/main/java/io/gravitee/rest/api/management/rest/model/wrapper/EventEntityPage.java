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
package io.gravitee.rest.api.management.rest.model.wrapper;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.EventEntity;
import java.util.List;

public class EventEntityPage extends Page<EventEntity> {

    public EventEntityPage(Page<EventEntity> events) {
        super(events.getContent(), events.getPageNumber(), (int) events.getPageElements(), events.getTotalElements());
    }

    public EventEntityPage(List<EventEntity> content, int page, int size, long total) {
        super(content, page, size, total);
    }
}
