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
package io.gravitee.gateway.dictionary;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.gateway.dictionary.model.Dictionary;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class MultiEnvironmentDictionaryManager implements DictionaryManager {

    /**
     * Mutated by the sync deployer threads (in parallel) and read by request threads through the EL template
     * provider: writes are serialized on this lock and each change publishes an immutable snapshot of the
     * environment's values, so readers never see a map while it is being modified.
     */
    private final Object lock = new Object();
    private final Map<String, Map<String, Dictionary>> dictionaries = new HashMap<>();
    private final Map<String, Map<String, Map<String, String>>> values = new HashMap<>();
    private final Map<String, Map<String, Map<String, String>>> publishedValues = new ConcurrentHashMap<>();
    private final DataEncryptor dataEncryptor;

    public MultiEnvironmentDictionaryManager(DataEncryptor dataEncryptor) {
        this.dataEncryptor = dataEncryptor;
    }

    @Override
    public void deploy(Dictionary dictionary) {
        String environmentId = dictionary.getEnvironmentId();
        String key = runtimeKey(dictionary);

        synchronized (lock) {
            Map<String, Dictionary> envDictionaries = dictionaries.computeIfAbsent(environmentId, env -> new HashMap<>());
            Dictionary existing = envDictionaries.get(key);
            if (existing != null && !Objects.equals(existing.getId(), dictionary.getId())) {
                log.error(
                    "Dictionary {} (name={}) collides on runtime key '{}' in environment {} with existing dictionary {} (name={}). Incoming deploy is rejected; occupant is kept",
                    dictionary.getId(),
                    dictionary.getName(),
                    key,
                    environmentId,
                    existing.getId(),
                    existing.getName()
                );
                return;
            }
            if (existing == null || isNewer(dictionary, existing)) {
                if (dictionary.getProperties() == null) {
                    dictionary.setProperties(Collections.emptyMap());
                }

                int propertyCount = dictionary.getProperties().size();
                log.info("Dictionary {} has been deployed with {} properties", dictionary, propertyCount);
                envDictionaries.put(key, dictionary);
                Map<String, String> flattenedProperties = new HashMap<>((propertyCount * 4) / 3 + 1);
                for (Map.Entry<String, DictionaryProperty> entry : dictionary.getProperties().entrySet()) {
                    DictionaryProperty property = entry.getValue();
                    if (property != null && property.value() != null) {
                        flattenedProperties.put(entry.getKey(), decryptOrKeep(dictionary, entry.getKey(), property));
                    }
                }
                values.computeIfAbsent(environmentId, env -> new HashMap<>()).put(key, Collections.unmodifiableMap(flattenedProperties));
                publish(environmentId);
            }
        }
    }

    /**
     * A missing deployment date on either side cannot prove the incoming dictionary is older, so it is deployed:
     * skipping it would keep stale values forever.
     */
    private static boolean isNewer(Dictionary incoming, Dictionary existing) {
        if (incoming.getDeployedAt() == null || existing.getDeployedAt() == null) {
            return true;
        }
        return incoming.getDeployedAt().after(existing.getDeployedAt());
    }

    /**
     * Decrypts at deploy and refresh so no crypto runs per request. An undecryptable value is logged and left
     * as stored, so EL resolves it as ciphertext, as AbstractApiDeployer.decryptProperties does.
     */
    private String decryptOrKeep(Dictionary dictionary, String key, DictionaryProperty property) {
        if (!property.encrypted()) {
            return property.value();
        }
        try {
            return dataEncryptor.decrypt(property.value());
        } catch (GeneralSecurityException | RuntimeException e) {
            log.error("Error decrypting dictionary property value for key {} on dictionary {}", key, dictionary.getId(), e);
            return property.value();
        }
    }

    @Override
    public void undeploy(Dictionary dictionary) {
        String environmentId = dictionary.getEnvironmentId();
        String key = runtimeKey(dictionary);
        synchronized (lock) {
            Map<String, Dictionary> envDictionaries = dictionaries.get(environmentId);
            if (envDictionaries == null) {
                return;
            }
            Dictionary occupant = envDictionaries.get(key);
            if (occupant == null) {
                return;
            }
            if (!Objects.equals(occupant.getId(), dictionary.getId())) {
                log.debug(
                    "Dictionary {} was requested for undeploy but runtime key '{}' is occupied by dictionary {} — ignoring",
                    dictionary.getId(),
                    key,
                    occupant.getId()
                );
                return;
            }

            envDictionaries.remove(key);
            if (envDictionaries.isEmpty()) {
                dictionaries.remove(environmentId);
            }

            Map<String, Map<String, String>> envValues = values.get(environmentId);
            if (envValues != null) {
                envValues.remove(key);
                if (envValues.isEmpty()) {
                    values.remove(environmentId);
                }
            }
            publish(environmentId);

            log.info("A dictionary has been undeployed: {}", dictionary);
        }
    }

    private void publish(String environmentId) {
        Map<String, Map<String, String>> envValues = values.get(environmentId);
        if (envValues == null) {
            publishedValues.remove(environmentId);
        } else {
            publishedValues.put(environmentId, Collections.unmodifiableMap(new HashMap<>(envValues)));
        }
    }

    /**
     * Runtime map key: prefer the dictionary {@code key} when present, otherwise fall back to {@code id}
     * for legacy events that predate the key field.
     */
    private static String runtimeKey(Dictionary dictionary) {
        String key = dictionary.getKey();
        return key == null || key.isBlank() ? dictionary.getId() : key;
    }

    @Override
    public EnvironmentDictionaryTemplateVariableProvider createTemplateVariableProvider(String environmentId) {
        return new EnvironmentDictionaryTemplateVariableProvider(environmentId, this);
    }

    @Override
    public Map<String, Map<String, String>> getDictionaries(String environmentId) {
        return publishedValues.get(environmentId);
    }
}
