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
package io.gravitee.gateway.handlers.api.manager;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import java.time.Duration;

/**
 * The approval requests this gateway knows of, synced from the control plane, so that a held call is answered from
 * this gateway's memory, never by asking the control plane. A state past its {@code keepUntil} is never returned.
 */
public interface ApprovalStateManager {
    /** Deploy a state the sync received; the calls waiting for it, by its id or an alias, are woken to look again. */
    void deploy(DeployedApprovalState state);

    /** Forget a state; the calls waiting for it, by its id or an alias, are woken to look again. */
    void undeploy(String approvalRequestId);

    /**
     * The request with this id, or of which this id is an alias. Lookups are read when subscribed to.
     */
    Maybe<DeployedApprovalState> byId(String approvalRequestId);

    /**
     * The request for the same call, whichever gateway opened it, until it is released: pending, decided, running, or
     * concluded but not yet told to the client. A client calling again presents no id, and finds its request this way.
     */
    Maybe<DeployedApprovalState> byOpenKey(String openKey);

    /**
     * An approved time-based grant that still covers this caller's call to this rule of this API with these arguments.
     * A grant never covers a call to another API, even when the same rule covers both.
     */
    Maybe<DeployedApprovalState> activeGrant(
        String environmentId,
        String apiId,
        String ruleId,
        String applicationId,
        String subject,
        String argumentsDigest
    );

    /**
     * Completes when a newer state of this request arrives, or once {@code atMost} has passed, whichever comes first.
     * Waiting holds no thread: the wait is a timer on {@code scheduler}, and it is forgotten once complete or disposed.
     */
    Completable awaitChange(String approvalRequestId, Duration atMost, Scheduler scheduler);

    /** Whether this gateway receives approval states for the organization: without them no held call can be answered. */
    boolean receivesStates(String organizationId);
}
