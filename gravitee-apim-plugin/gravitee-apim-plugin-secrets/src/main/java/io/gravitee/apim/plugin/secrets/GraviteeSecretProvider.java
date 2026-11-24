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

import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.api.SecretValueRepository;
import io.gravitee.secrets.api.core.SecretEvent;
import io.gravitee.secrets.api.core.SecretMap;
import io.gravitee.secrets.api.core.SecretURL;
import io.gravitee.secrets.api.errors.SecretManagerException;
import io.gravitee.secrets.api.plugin.SecretProvider;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Collection;
import java.util.Map;
import lombok.CustomLog;

@CustomLog
public class GraviteeSecretProvider implements SecretProvider {

    public static final String ID = "gravitee";
    static final String NO_SECRET_STORE =
        "Secret provider [" + ID + "] cannot read secrets: the management repository does not store secrets";
    private static final String VALUE_KEY = "value";
    private static final String ENV_ID = SecretURL.WellKnownQueryParam.ENV_ID;

    private final SecretValueRepository secretValues;
    private final Runnable onStart;
    private final String refusal;

    private GraviteeSecretProvider(SecretValueRepository secretValues, Runnable onStart, String refusal) {
        this.secretValues = secretValues;
        this.onStart = onStart;
        this.refusal = refusal;
    }

    static GraviteeSecretProvider decrypting(
        String base64Kek,
        SecretRepository secretRepository,
        OrganizationKeyRepository organizationKeyRepository,
        EnvironmentRepository environmentRepository
    ) {
        var secretValues = new DecryptingSecretValueRepository(
            base64Kek,
            secretRepository,
            organizationKeyRepository,
            environmentRepository
        );
        return new GraviteeSecretProvider(secretValues, secretValues::start, null);
    }

    static GraviteeSecretProvider throughBridge(SecretValueRepository bridge) {
        return new GraviteeSecretProvider(bridge, () -> {}, null);
    }

    static GraviteeSecretProvider refusing(String reason) {
        return new GraviteeSecretProvider(null, () -> {}, reason);
    }

    /**
     * Never throws: the secrets service stops, with every other secret provider, when a provider fails to start. A
     * misconfigured provider logs why once and refuses every secret instead.
     */
    @Override
    public SecretProvider start() {
        if (refusal != null) {
            log.error("{}: it refuses every secret until this is fixed", refusal);
            return this;
        }
        onStart.run();
        return this;
    }

    @Override
    public Maybe<SecretMap> resolve(SecretURL secretURL) {
        if (refusal != null) {
            return Maybe.error(new SecretManagerException(refusal));
        }
        return Maybe.fromCallable(() -> {
            String key = secretURL.path();
            if (key.isEmpty() || key.contains("/")) {
                throw new SecretManagerException(
                    "Secret reference [" + key + "] must have the form /" + ID + "/{key}:value?" + ENV_ID + "={envId}"
                );
            }
            Collection<String> environmentIds = secretURL.query().get(ENV_ID);
            if (environmentIds.size() != 1) {
                throw new SecretManagerException("Secret reference [" + key + "] must name its environment with ?" + ENV_ID + "={envId}");
            }
            String environmentId = environmentIds.iterator().next();
            try {
                return secretValues
                    .findValue(environmentId, key)
                    .map(value -> SecretMap.of(Map.of(VALUE_KEY, value)))
                    .orElse(null);
            } catch (SecretManagerException e) {
                throw e;
            } catch (Exception e) {
                throw new SecretManagerException("Failed to resolve secret [" + key + "] of environment [" + environmentId + "]", e);
            }
        });
    }

    @Override
    public Flowable<SecretEvent> watch(SecretURL secretURL) {
        return Flowable.error(new SecretManagerException("The " + ID + " secret provider does not support watching secrets"));
    }
}
