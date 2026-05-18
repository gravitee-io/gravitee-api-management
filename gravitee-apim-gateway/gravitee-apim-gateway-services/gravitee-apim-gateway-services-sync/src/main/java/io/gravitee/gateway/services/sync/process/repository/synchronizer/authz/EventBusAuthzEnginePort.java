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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.CustomLog;

@CustomLog
public class EventBusAuthzEnginePort implements AuthzEnginePort {

    public static final String SYNC_ADDRESS = "service:authz-pdp:sync";

    static final String OP_ADD_OR_UPDATE_ENTITY = "addOrUpdateEntity";
    static final String OP_REMOVE_ENTITY = "removeEntity";
    static final String OP_ADD_OR_UPDATE_POLICY = "addOrUpdatePolicy";
    static final String OP_REMOVE_POLICY = "removePolicy";
    static final String OP_COMMIT = "commit";

    // I4: Vertx default reply timeout is 30s — long enough to wedge a sync cycle when the PDP service
    // is unhealthy. 5s is short enough to fail fast and long enough for normal in-process replies.
    static final long REPLY_TIMEOUT_MS = 5_000L;

    private final Vertx vertx;
    private final DeliveryOptions deliveryOptions = new DeliveryOptions().setSendTimeout(REPLY_TIMEOUT_MS);

    public EventBusAuthzEnginePort(Vertx vertx) {
        this.vertx = Objects.requireNonNull(vertx, "vertx must not be null");
    }

    @Override
    public Completable addOrUpdateEntity(String uid, Map<String, Object> attributes, List<String> parents) {
        JsonObject command = new JsonObject().put("op", OP_ADD_OR_UPDATE_ENTITY).put("uid", uid);
        if (attributes != null && !attributes.isEmpty()) {
            command.put("attributes", new JsonObject(attributes));
        }
        if (parents != null && !parents.isEmpty()) {
            command.put("parents", new JsonArray(parents));
        }
        return send(command);
    }

    @Override
    public Completable removeEntity(String uid) {
        return send(new JsonObject().put("op", OP_REMOVE_ENTITY).put("uid", uid));
    }

    @Override
    public Completable addOrUpdatePolicy(String docId, String name, String policyText) {
        JsonObject command = new JsonObject()
            .put("op", OP_ADD_OR_UPDATE_POLICY)
            .put("docId", docId)
            .put("name", name)
            .put("policyText", policyText);
        return send(command);
    }

    @Override
    public Completable removePolicy(String docId) {
        return send(new JsonObject().put("op", OP_REMOVE_POLICY).put("docId", docId));
    }

    @Override
    public Completable commit() {
        return Completable.create(emitter ->
            vertx
                .eventBus()
                .<JsonObject>request(SYNC_ADDRESS, new JsonObject().put("op", OP_COMMIT), deliveryOptions)
                .onSuccess(reply -> {
                    if (log.isDebugEnabled()) {
                        Long gen = reply.body() != null ? reply.body().getLong("commitGeneration") : null;
                        log.debug("Authz engine commit landed, commitGeneration={}", gen);
                    }
                    emitter.onComplete();
                })
                .onFailure(emitter::onError)
        );
    }

    private Completable send(JsonObject command) {
        return Completable.create(emitter ->
            vertx
                .eventBus()
                .<JsonObject>request(SYNC_ADDRESS, command, deliveryOptions)
                .onSuccess(reply -> emitter.onComplete())
                .onFailure(emitter::onError)
        );
    }
}
