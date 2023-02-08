/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model.v4.plan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.Date;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GenericPlanEntity {
    String getId();

    String getName();

    String getApiId();

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
    PlanValidationType getPlanValidation();

    Date getNeedRedeployAt();

    List<String> getCharacteristics();

    String getCommentMessage();

    String getDescription();

    int getOrder();
}
