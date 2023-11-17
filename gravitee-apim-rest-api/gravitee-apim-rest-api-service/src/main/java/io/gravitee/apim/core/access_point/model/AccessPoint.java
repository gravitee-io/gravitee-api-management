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
package io.gravitee.apim.core.access_point.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
public class AccessPoint {

    private String id;
    private ReferenceType referenceType;
    private String referenceId;
    private Target target;
    private String host;
    private boolean secured;
    private boolean overriding;

    public String buildInstallationAccess() {
        StringBuilder consoleUrl = new StringBuilder();
        if (secured) {
            consoleUrl.append("https");
        } else {
            consoleUrl.append("http");
        }
        consoleUrl.append("://").append(host);
        return consoleUrl.toString();
    }

    public enum ReferenceType {
        ENVIRONMENT,
        ORGANIZATION,
    }

    public enum Target {
        CONSOLE,
        CONSOLE_API,
        PORTAL,
        PORTAL_API,
        GATEWAY,
    }
}
