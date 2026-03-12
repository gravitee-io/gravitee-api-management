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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.services.basicauth.BasicAuthCredential;
import io.gravitee.repository.management.model.BasicAuthCredentials;
import java.util.Optional;

public class BasicAuthCredentialsMapper {

    public BasicAuthCredential to(BasicAuthCredentials model, Subscription subscription) {
        BasicAuthCredential.BasicAuthCredentialBuilder builder = BasicAuthCredential.builder()
            .id(model.getId())
            .username(model.getUsername())
            .password(model.getPassword())
            .application(model.getApplication())
            .expireAt(model.getExpireAt())
            .revoked(model.isRevoked());

        if (subscription != null) {
            builder.api(subscription.getApi()).plan(subscription.getPlan()).subscription(subscription.getId());
        }
        return builder.build();
    }

    public BasicAuthCredential to(BasicAuthCredentials model, Optional<Subscription> optionalSubscription) {
        return to(model, optionalSubscription.orElse(null));
    }
}
