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
package io.gravitee.rest.api.model.api;

import lombok.Getter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
public class DeploymentStatus {

    public static final DeploymentStatus OK = new DeploymentStatus(true);
    public static final DeploymentStatus KO = new DeploymentStatus(false);

    private final DeploymentState state;

    private final String reason;

    public DeploymentStatus(DeploymentState state, String reason) {
        this.state = state;
        this.reason = reason;
    }

    public DeploymentStatus(DeploymentState state) {
        this(state, null);
    }

    public DeploymentStatus(Boolean isSynchronized) {
        this(isSynchronized == null || isSynchronized, null);
    }

    public DeploymentStatus(boolean isSynchronized) {
        this(isSynchronized, null);
    }

    public DeploymentStatus(Boolean isSynchronized, String reason) {
        this(isSynchronized == null || isSynchronized, reason);
    }


    public DeploymentStatus(boolean isSynchronized, String reason) {
        this(isSynchronized ? DeploymentState.DEPLOYED : DeploymentState.NEED_REDEPLOY, reason);
    }

    public DeploymentState getState() {
        return state;
    }

    public String getReason() {
        return reason;
    }

    public boolean isSynchronized() {
        return state == DeploymentState.DEPLOYED;
    }

    public enum DeploymentState {
        NEED_REDEPLOY,
        DEPLOYED,
    }
}
