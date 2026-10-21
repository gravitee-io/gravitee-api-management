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
package io.gravitee.rest.api.model.configuration.identity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author GraviteeSource Team
 */
public final class IdentityProviderClientSecret {

    public static final String CLIENT_SECRET = "clientSecret";

    private IdentityProviderClientSecret() {}

    /**
     * A new map without {@code clientSecret}. The input is left unchanged because login and the
     * repository still read that key from the stored configuration. A null configuration stays null.
     */
    public static Map<String, Object> withoutClientSecret(Map<String, Object> configuration) {
        if (configuration == null) {
            return null;
        }
        Map<String, Object> copy = new HashMap<>(configuration);
        copy.remove(CLIENT_SECRET);
        return copy;
    }

    /**
     * Drops {@code clientSecret} from the entity returned by the Management API. The previous
     * configuration map is not modified.
     */
    public static IdentityProviderEntity omitClientSecret(IdentityProviderEntity entity) {
        if (entity == null) {
            return null;
        }
        entity.setConfiguration(withoutClientSecret(entity.getConfiguration()));
        return entity;
    }
}
