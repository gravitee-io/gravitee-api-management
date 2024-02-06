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
package io.gravitee.apim.core.event.model;

import io.gravitee.apim.core.user.model.BaseUserEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class EventWithInitiator extends Event {

    /** The user who triggered the action */
    private BaseUserEntity initiator;

    public EventWithInitiator(Event b, BaseUserEntity initiator) {
        super(
            b.getId(),
            b.getType(),
            b.getPayload(),
            b.getParentId(),
            b.getProperties(),
            b.getEnvironments(),
            b.getCreatedAt(),
            b.getUpdatedAt()
        );
        this.initiator = initiator;
    }
}
