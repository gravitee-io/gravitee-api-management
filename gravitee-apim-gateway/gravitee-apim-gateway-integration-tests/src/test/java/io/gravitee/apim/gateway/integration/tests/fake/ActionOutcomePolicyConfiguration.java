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
package io.gravitee.apim.gateway.integration.tests.fake;

import io.gravitee.policy.api.PolicyConfiguration;

public class ActionOutcomePolicyConfiguration implements PolicyConfiguration {

    public enum Action {
        ENDPOINT_REQUEST,
        ENTRYPOINT_RESPONSE,
        TERMINATE,
    }

    public enum Outcome {
        INTERRUPT,
        FAIL,
    }

    private Action action;
    private Outcome outcome;

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }
}
