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
package io.gravitee.gateway.handlers.sharedpolicygroup.registry;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactor;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactorFactory;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactorFactoryManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SharedPolicyGroupRegistry {

    private final SharedPolicyGroupReactorFactoryManager sharedPolicyGroupReactorFactoryManager;

    @VisibleForTesting
    protected final Map<SharedPolicyGroupReactorRegistryKey, SharedPolicyGroupReactor> reactorRegistry = new ConcurrentHashMap<>();

    @VisibleForTesting
    protected final Map<SharedPolicyGroupReactableKey, ReactableSharedPolicyGroup> reactableRegistry = new ConcurrentHashMap<>();

    public SharedPolicyGroupReactor get(String sharedPolicyGroupId, String instanceId, String environmentId) {
        SharedPolicyGroupReactorRegistryKey reactorKey = new SharedPolicyGroupReactorRegistryKey(
            sharedPolicyGroupId,
            environmentId,
            instanceId
        );
        SharedPolicyGroupReactor reactor = reactorRegistry.get(reactorKey);

        if (reactor == null) {
            log.debug("SharedPolicyGroupReactor not found for key {}. Attempting to create it.", reactorKey);

            SharedPolicyGroupReactableKey reactableKey = new SharedPolicyGroupReactableKey(sharedPolicyGroupId, environmentId);
            ReactableSharedPolicyGroup reactable = reactableRegistry.get(reactableKey);

            if (reactable != null) {
                createInternal(reactorKey, reactable);
                reactor = reactorRegistry.get(reactorKey);
            } else {
                log.error(
                    "Cannot create SharedPolicyGroupReactor for {}: ReactableSharedPolicyGroup not found in registry for sharedPolicyGroup {} of environment {}. " +
                    "Ensure ReactableSharedPolicyGroup is registered.",
                    reactorKey,
                    sharedPolicyGroupId,
                    environmentId
                );
                // TODO: Define if we should throw an exception here or return skip
            }
        }
        return reactor;
    }

    public void create(ReactableSharedPolicyGroup reactable) {
        SharedPolicyGroupReactableKey key = new SharedPolicyGroupReactableKey(reactable.getId(), reactable.getEnvironmentId());
        reactableRegistry.put(key, reactable);
        log.debug("Create ReactableSharedPolicyGroup for {}", key);
    }

    private void createInternal(SharedPolicyGroupReactorRegistryKey key, ReactableSharedPolicyGroup reactable) {
        try {
            final SharedPolicyGroupReactor sharedPolicyGroupReactor = sharedPolicyGroupReactorFactoryManager.create(reactable);
            sharedPolicyGroupReactor.start();
            final SharedPolicyGroupReactor previousReactor = reactorRegistry.put(key, sharedPolicyGroupReactor);
            if (previousReactor != null) {
                log.debug(
                    "The ReactableSharedPolicyGroup reactor was already deployed for {}; stopping previous SharedPolicyGroupReactor.",
                    key
                );
                previousReactor.stop();
            }
        } catch (Exception e) {
            log.error("Unable to create and start the new shared policy group reactor for {}", key, e);
        }
    }

    public void update(ReactableSharedPolicyGroup reactable) {
        removeReactors(reactable);

        create(reactable);
    }

    private void removeReactors(ReactableSharedPolicyGroup reactable) {
        reactorRegistry
            .entrySet()
            .removeIf(entry -> {
                SharedPolicyGroupReactorRegistryKey reactorKey = entry.getKey();
                if (
                    reactorKey.sharedPolicyGroupId().equals(reactable.getId()) &&
                    reactorKey.environmentId().equals(reactable.getEnvironmentId())
                ) {
                    try {
                        log.debug("Stopping reactor {}", reactorKey);
                        entry.getValue().stop();
                    } catch (Exception ex) {
                        log.error("Error stopping reactor during global removal for {}", reactorKey, ex);
                    }
                    return true;
                }
                return false;
            });
    }

    public void remove(ReactableSharedPolicyGroup reactable) {
        SharedPolicyGroupReactableKey reactableKey = new SharedPolicyGroupReactableKey(reactable.getId(), reactable.getEnvironmentId());
        log.debug("Removing ReactableSharedPolicyGroup and all associated SharedPolicyGroupReactors for {}", reactableKey);

        reactableRegistry.remove(reactableKey);

        removeReactors(reactable);
    }

    public void clear() {
        reactorRegistry.forEach((key, value) -> {
            try {
                value.stop();
            } catch (Exception ex) {
                log.error("Unable to stop shared policy group reactor for {}", key, ex);
            }
        });
        reactorRegistry.clear();
        reactableRegistry.clear();
    }

    record SharedPolicyGroupReactorRegistryKey(String sharedPolicyGroupId, String environmentId, String instanceId) {}

    record SharedPolicyGroupReactableKey(String sharedPolicyGroupId, String environmentId) {}
}
