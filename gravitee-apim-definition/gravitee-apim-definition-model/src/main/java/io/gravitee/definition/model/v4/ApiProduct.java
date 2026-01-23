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
package io.gravitee.definition.model.v4;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.v4.plan.Plan;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class ApiProduct extends AbstractApiProduct {

    @JsonProperty(required = true)
    @NotNull
    private Map<@NotEmpty String, @NotNull Plan> plans;

    public ApiProduct(ApiProduct other) {
        super(
            other.definitionVersion,
            other.id,
            other.name,
            other.getEnvironmentId(),
            other.type,
            other.apiVersion,
            other.apiIds,
            other.getDeployedAt()
        );
        this.apiIds = other.apiIds;
    }

    public Plan getPlan(final String plan) {
        return plans.get(plan);
    }

    @Nullable
    public List<Plan> getPlans() {
        if (plans != null) {
            return new ArrayList<>(this.plans.values());
        }
        return null;
    }

    public ApiProduct plans(List<Plan> plans) {
        setPlans(plans);
        return this;
    }

    public void setPlans(List<Plan> plans) {
        if (plans != null) {
            this.plans = plans.stream().collect(Collectors.toMap(Plan::getId, Function.identity()));
        } else {
            this.plans = new HashMap<>();
        }
    }

    @Override
    public Set<String> getTags() {
        return Set.of();
    }

    @Override
    public void setTags(Set<String> tags) {}
}
