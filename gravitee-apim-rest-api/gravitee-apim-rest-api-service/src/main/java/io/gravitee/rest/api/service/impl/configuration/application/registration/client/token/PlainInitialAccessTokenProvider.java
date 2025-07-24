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
package io.gravitee.rest.api.service.impl.configuration.application.registration.client.token;

import io.gravitee.rest.api.model.configuration.application.registration.KeyStoreEntity;
import io.gravitee.rest.api.model.configuration.application.registration.TrustStoreEntity;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlainInitialAccessTokenProvider implements InitialAccessTokenProvider {

    private final String initialAccessToken;

    public PlainInitialAccessTokenProvider(String initialAccessToken) {
        this.initialAccessToken = initialAccessToken;
    }

    @Override
    public String get(Map<String, String> attributes, TrustStoreEntity trustStore, KeyStoreEntity keyStore) {
        return initialAccessToken;
    }
}
