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
package io.gravitee.apim.gateway.tests.sdk.runner;

import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.genericevent.GenericEventManager;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * The generic events a gateway test deploys: at class level, or at test level, from an annotation or from the test itself.
 * The sync is not run in a test, so they are handed to the manager the way the sync does: upserted, then committed once per deployment.
 *
 * @author GraviteeSource Team
 */
class GenericEventDeployments {

    static final String GENERIC_EVENT_ALREADY_DEPLOYED_MESSAGE = "A generic event has already been deployed with kind {%s} and id {%s}";
    static final String CANNOT_UNDEPLOY_CLASS_GENERIC_EVENT_MESSAGE =
        "A generic event deployed at class level cannot be undeployed (kind {%s} and id {%s})";
    static final String GENERIC_EVENT_NOT_NEWER_MESSAGE =
        "A generic event with kind {%s} and id {%s} must be updated after the one it replaces to be delivered (updatedAt %s, deployed %s)";
    private static final String AT_CLASS_LEVEL = " at class level";

    private record Key(String environmentId, String kind, String id) {
        static Key of(GenericEvent genericEvent) {
            return new Key(genericEvent.environmentId(), genericEvent.kind(), genericEvent.id());
        }
    }

    private final Supplier<GenericEventManager> genericEventManager;
    private final Map<Key, GenericEvent> deployedForTestClass = new HashMap<>();
    private final Map<Key, GenericEvent> deployedForTest = new HashMap<>();

    /**
     * @param genericEventManager supplies the manager of the running gateway, which only exists once the gateway is started.
     */
    GenericEventDeployments(Supplier<GenericEventManager> genericEventManager) {
        this.genericEventManager = genericEventManager;
    }

    void deployForClass(List<GenericEvent> genericEvents) {
        deployedForTestClass.putAll(checkedBatch(genericEvents));
        deploy(genericEvents);
    }

    void deployForTest(List<GenericEvent> genericEvents) {
        deployedForTest.putAll(checkedBatch(genericEvents));
        deploy(genericEvents);
    }

    /**
     * Deploys a generic event from a test, replacing the one the test already deployed. Throws if the event is deployed at class level,
     * or if it is not newer than the one it replaces, which the manager would ignore.
     */
    void deployFromTest(GenericEvent genericEvent) {
        Key key = Key.of(genericEvent);
        if (deployedForTestClass.containsKey(key)) {
            throw alreadyDeployed(genericEvent, AT_CLASS_LEVEL);
        }
        GenericEvent deployed = deployedForTest.get(key);
        if (deployed != null && !isNewer(genericEvent, deployed)) {
            throw new PreconditionViolationException(
                String.format(
                    GENERIC_EVENT_NOT_NEWER_MESSAGE,
                    genericEvent.kind(),
                    genericEvent.id(),
                    genericEvent.updatedAt(),
                    deployed.updatedAt()
                )
            );
        }
        deployedForTest.put(key, genericEvent);
        deploy(List.of(genericEvent));
    }

    /**
     * Undeploys a generic event from a test. Throws if the event is deployed at class level.
     */
    void undeployFromTest(GenericEvent genericEvent) {
        Key key = Key.of(genericEvent);
        if (deployedForTestClass.containsKey(key)) {
            throw new PreconditionViolationException(
                String.format(CANNOT_UNDEPLOY_CLASS_GENERIC_EVENT_MESSAGE, genericEvent.kind(), genericEvent.id())
            );
        }
        GenericEvent deployed = deployedForTest.remove(key);
        if (deployed != null) {
            undeploy(List.of(deployed));
        }
    }

    void undeployForClass() {
        undeploy(List.copyOf(deployedForTestClass.values()));
        deployedForTestClass.clear();
    }

    void undeployForTest() {
        undeploy(List.copyOf(deployedForTest.values()));
        deployedForTest.clear();
    }

    /**
     * The events of one deployment by key, checked before any is recorded: none is deployed when one is already deployed or
     * appears twice.
     */
    private Map<Key, GenericEvent> checkedBatch(List<GenericEvent> genericEvents) {
        Map<Key, GenericEvent> batch = new LinkedHashMap<>();
        for (GenericEvent genericEvent : genericEvents) {
            Key key = Key.of(genericEvent);
            if (deployedForTestClass.containsKey(key) || deployedForTest.containsKey(key) || batch.putIfAbsent(key, genericEvent) != null) {
                throw alreadyDeployed(genericEvent, "");
            }
        }
        return batch;
    }

    /** As the manager compares them: a copy without an update date always replaces the one held. */
    private static boolean isNewer(GenericEvent incoming, GenericEvent existing) {
        return incoming.updatedAt() == null || existing.updatedAt() == null || incoming.updatedAt().isAfter(existing.updatedAt());
    }

    private static PreconditionViolationException alreadyDeployed(GenericEvent genericEvent, String suffix) {
        return new PreconditionViolationException(
            String.format(GENERIC_EVENT_ALREADY_DEPLOYED_MESSAGE + suffix, genericEvent.kind(), genericEvent.id())
        );
    }

    private void deploy(List<GenericEvent> genericEvents) {
        GenericEventManager manager = genericEventManager.get();
        genericEvents.forEach(manager::upsert);
        manager.commit();
    }

    private void undeploy(List<GenericEvent> genericEvents) {
        if (genericEvents.isEmpty()) {
            return;
        }
        GenericEventManager manager = genericEventManager.get();
        genericEvents.forEach(genericEvent -> manager.remove(genericEvent.environmentId(), genericEvent.kind(), genericEvent.id()));
        manager.commit();
    }
}
