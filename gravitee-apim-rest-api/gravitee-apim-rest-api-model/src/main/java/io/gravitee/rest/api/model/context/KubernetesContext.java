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
package io.gravitee.rest.api.model.context;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * This context tells that the API has been created by the Kubernetes Operator.
 */
@Builder
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class KubernetesContext extends AbstractContext {

    private final Mode mode;
    private final String syncFrom;

    public KubernetesContext(Mode mode) {
        this(mode, Origin.KUBERNETES.name());
    }

    public KubernetesContext(Mode mode, String syncFrom) {
        super(Origin.KUBERNETES);
        this.mode = mode;
        this.syncFrom = syncFrom;
    }

    public enum Mode {
        /** Mode indicating the api is fully managed by the origin and so, only the origin should be able to manage the api. */
        FULLY_MANAGED,
    }

    public String getSyncFrom() {
        return syncFrom.toUpperCase();
    }
}
