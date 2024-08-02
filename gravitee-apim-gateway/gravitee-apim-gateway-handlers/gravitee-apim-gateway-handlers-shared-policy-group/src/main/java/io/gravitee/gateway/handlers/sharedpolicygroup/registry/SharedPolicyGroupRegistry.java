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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SharedPolicyGroupRegistry {

    private final SharedPolicyGroupReactorFactoryManager sharedPolicyGroupReactorFactoryManager;

    @VisibleForTesting
    protected final Map<SharedPolicyGroupRegistryKey, SharedPolicyGroupReactor> registry = new HashMap<>();

    public SharedPolicyGroupReactor get(String sharedPolicyGroupId, String environmentId) {
        return registry.get(new SharedPolicyGroupRegistryKey(sharedPolicyGroupId, environmentId));
    }

    public void create(ReactableSharedPolicyGroup reactable) {
        try {
            final SharedPolicyGroupReactor sharedPolicyGroupReactor = sharedPolicyGroupReactorFactoryManager.create(reactable);
            sharedPolicyGroupReactor.start();
            final SharedPolicyGroupReactor previousReactor = registry.put(
                new SharedPolicyGroupRegistryKey(reactable.getId(), reactable.getEnvironmentId()),
                sharedPolicyGroupReactor
            );
            if (previousReactor != null) {
                log.debug(
                    "The ReactableSharedPolicyGroup was already deployed; stopping previous SharedPolicyGroupReactor for shared policy group: {} of environment {}",
                    reactable.getId(),
                    reactable.getEnvironmentId()
                );
                previousReactor.stop();
            }
        } catch (Exception e) {
            log.error(
                "Unable to create and start the new shared policy group '{}' reactor for environment {}",
                reactable.getId(),
                reactable.getEnvironmentId(),
                e
            );
        }
    }

    public void update(ReactableSharedPolicyGroup reactable) {
        remove(reactable);
        create(reactable);
    }

    public void remove(ReactableSharedPolicyGroup reactable) {
        log.debug("Removing an SharedPolicyGroupReactor {} for organization: {}", reactable.getId(), reactable.getEnvironmentId());
        try {
            SharedPolicyGroupReactor sharedPolicyGroupReactor = registry.remove(
                new SharedPolicyGroupRegistryKey(reactable.getId(), reactable.getEnvironmentId())
            );
            if (sharedPolicyGroupReactor != null) {
                sharedPolicyGroupReactor.stop();
            }
        } catch (Exception ex) {
            log.error(
                "Unable to remove and stop the shared policy group reactor '{}' for environment {}",
                reactable.getId(),
                reactable.getEnvironmentId(),
                ex
            );
        }
    }

    public void clear() {
        registry
            .values()
            .forEach(reactor -> {
                try {
                    reactor.stop();
                } catch (Exception ex) {
                    log.error(
                        "Unable to remove and stop shared policy group {} for environment {}",
                        reactor.id(),
                        reactor.reactableSharedPolicyGroup().getEnvironmentId(),
                        ex
                    );
                }
            });
        registry.clear();
    }

    record SharedPolicyGroupRegistryKey(String sharedPolicyGroupId, String environmentId) {}
}
