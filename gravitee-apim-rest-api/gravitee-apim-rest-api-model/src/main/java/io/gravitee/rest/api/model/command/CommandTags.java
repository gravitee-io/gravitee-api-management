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
package io.gravitee.rest.api.model.command;

import static io.gravitee.rest.api.model.command.CommandTags.CommandCastMode.MULTICAST;
import static io.gravitee.rest.api.model.command.CommandTags.CommandCastMode.UNICAST;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */

@RequiredArgsConstructor
@Schema(enumAsRef = true)
public enum CommandTags {
    DATA_TO_INDEX(MULTICAST),
    SUBSCRIPTION_FAILURE(UNICAST),
    EMAIL_TEMPLATE_UPDATE(MULTICAST),
    GROUP_DEFAULT_ROLES_UPDATE(MULTICAST);

    private final CommandCastMode castMode;

    public boolean isUnicast() {
        return this.castMode == UNICAST;
    }

    enum CommandCastMode {
        UNICAST,
        MULTICAST,
    }
}
