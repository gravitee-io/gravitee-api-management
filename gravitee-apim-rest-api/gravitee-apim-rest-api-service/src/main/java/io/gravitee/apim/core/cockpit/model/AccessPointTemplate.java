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
package io.gravitee.apim.core.cockpit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AccessPointTemplate {

    private Target target;
    private String host;
    private boolean secured;

    @RequiredArgsConstructor
    @Getter
    public enum Type {
        ENVIRONMENT("environment"),
        ORGANIZATION("organization");

        private final String label;
    }

    @RequiredArgsConstructor
    @Getter
    public enum Target {
        CONSOLE("console"),
        CONSOLE_API("console-api"),
        PORTAL("portal"),
        PORTAL_API("portal-api"),
        GATEWAY("gateway");

        private final String label;
    }
}
