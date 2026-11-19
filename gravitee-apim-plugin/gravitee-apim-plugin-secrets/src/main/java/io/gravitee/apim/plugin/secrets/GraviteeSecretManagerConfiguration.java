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
package io.gravitee.apim.plugin.secrets;

import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.util.ConfigHelper;
import java.util.Map;

public class GraviteeSecretManagerConfiguration implements SecretManagerConfiguration {

    public static final String ENABLED_PROPERTY = "enabled";
    public static final String ENCRYPTION_KEY_PROPERTY = "encryption.key";
    public static final String ENVIRONMENT_PROPERTY = "environment";

    private final boolean enabled;
    private final String encryptionKey;
    private final String environment;

    public GraviteeSecretManagerConfiguration(Map<String, Object> properties) {
        this.enabled = ConfigHelper.getProperty(properties, ENABLED_PROPERTY, Boolean.class, true);
        this.encryptionKey = ConfigHelper.getProperty(properties, ENCRYPTION_KEY_PROPERTY, String.class, null);
        this.environment = ConfigHelper.getProperty(properties, ENVIRONMENT_PROPERTY, String.class, null);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String encryptionKey() {
        return encryptionKey;
    }

    public String environment() {
        return environment;
    }
}
