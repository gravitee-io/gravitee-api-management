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
package io.gravitee.rest.api.model.v4.plan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.Identifiable;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GenericPlanEntity extends Serializable, Identifiable {
    DefinitionVersion getDefinitionVersion();

    String getName();

    String getApiId();

    String getHrid();

    String getEnvironmentId();

    List<String> getExcludedGroups();

    boolean isCommentRequired();

    String getGeneralConditions();

    //Those following methods need to be prefix by `Plan` in order to avoid collision with v2 model
    @JsonIgnore
    PlanType getPlanType();

    @JsonIgnore
    PlanSecurity getPlanSecurity();

    @JsonIgnore
    PlanStatus getPlanStatus();

    @JsonIgnore
    PlanMode getPlanMode();

    @JsonIgnore
    PlanValidationType getPlanValidation();

    Date getNeedRedeployAt();

    List<String> getCharacteristics();

    String getCommentMessage();

    String getDescription();

    int getOrder();

    @JsonIgnore
    default boolean isActive() {
        return !(isClosed() || isStaging());
    }

    @JsonIgnore
    default boolean isClosed() {
        return PlanStatus.CLOSED.equals(getPlanStatus());
    }

    @JsonIgnore
    default boolean isPublished() {
        return PlanStatus.PUBLISHED.equals(getPlanStatus());
    }

    @JsonIgnore
    default boolean isStaging() {
        return PlanStatus.STAGING.equals(getPlanStatus());
    }

    @JsonIgnore
    default boolean isDeprecated() {
        return PlanStatus.DEPRECATED.equals(getPlanStatus());
    }
}
