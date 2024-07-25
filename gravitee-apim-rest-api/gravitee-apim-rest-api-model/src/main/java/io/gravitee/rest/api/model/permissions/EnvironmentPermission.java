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
package io.gravitee.rest.api.model.permissions;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Schema(enumAsRef = true)
public enum EnvironmentPermission implements Permission {
    INSTANCE("INSTANCE", 1000),
    GROUP("GROUP", 1200),
    TAG("TAG", 1300),
    TENANT("TENANT", 1400),
    API("API", 1500),
    APPLICATION("APPLICATION", 1700),
    PLATFORM("PLATFORM", 1800),
    AUDIT("AUDIT", 1900),
    NOTIFICATION("NOTIFICATION", 2000),
    MESSAGE("MESSAGE", 2200),
    DICTIONARY("DICTIONARY", 2300),
    ALERT("ALERT", 2400),
    ENTRYPOINT("ENTRYPOINT", 2500),
    SETTINGS("SETTINGS", 2600),
    DASHBOARD("DASHBOARD", 2700),
    QUALITY_RULE("QUALITY_RULE", 2800),
    METADATA("METADATA", 2900),
    DOCUMENTATION("DOCUMENTATION", 3000),
    CATEGORY("CATEGORY", 3100),
    TOP_APIS("TOP_APIS", 3200),
    API_HEADER("API_HEADER", 3300),
    CLIENT_REGISTRATION_PROVIDER("CLIENT_REGISTRATION_PROVIDER", 3500),
    THEME("THEME", 3600),
    IDENTITY_PROVIDER_ACTIVATION("IDENTITY_PROVIDER_ACTIVATION", 3700),
    INTEGRATION("INTEGRATION", 3800),
    SHARED_POLICY_GROUP("SHARED_POLICY_GROUP", 3900);

    String name;
    int mask;

    EnvironmentPermission(String name, int mask) {
        this.name = name;
        this.mask = mask;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMask() {
        return mask;
    }
}
