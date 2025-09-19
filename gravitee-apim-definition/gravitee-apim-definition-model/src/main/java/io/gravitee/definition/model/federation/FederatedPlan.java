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
package io.gravitee.definition.model.federation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
public class FederatedPlan implements Serializable {

    @JsonProperty(required = true)
    private String id;

    @JsonProperty(required = true)
    private String providerId;

    @JsonProperty(required = true)
    private PlanSecurity security;

    @Builder.Default
    @JsonProperty(required = true)
    private PlanMode mode = PlanMode.STANDARD;

    @JsonProperty(required = true)
    @Builder.Default
    private PlanStatus status = PlanStatus.PUBLISHED;

    @JsonIgnore
    public final boolean isSubscribable() {
        return (
            (this.getSecurity() != null &&
                this.getSecurity().getType() != null &&
                !"KEY_LESS".equalsIgnoreCase(this.getSecurity().getType())) ||
            usePushMode()
        );
    }

    @JsonIgnore
    public final boolean usePushMode() {
        return this.getMode() != null && this.getMode() == PlanMode.PUSH;
    }

    @JsonIgnore
    public final boolean useStandardMode() {
        return this.getMode() != null && this.getMode() == PlanMode.STANDARD;
    }

    @JsonIgnore
    public final boolean isApiKey() {
        return (
            this.getSecurity() != null &&
            ("API_KEY".equalsIgnoreCase(this.getSecurity().getType()) || "api-key".equalsIgnoreCase(this.getSecurity().getType()))
        );
    }

    @JsonIgnore
    public final boolean isOAuth() {
        return (this.getSecurity() != null && "oauth2".equalsIgnoreCase(this.getSecurity().getType()));
    }

    public FederatedPlan update(FederatedPlan plan) {
        return toBuilder()
            .mode(plan.getMode())
            .status(plan.getStatus())
            .security(security.toBuilder().configuration(plan.getSecurity().getConfiguration()).build())
            .build();
    }
}
