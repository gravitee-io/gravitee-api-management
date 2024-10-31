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
package io.gravitee.rest.api.model.v4.nativeapi;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.rest.api.model.DeploymentRequired;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(name = "NativePlanEntityV4")
@SuperBuilder(toBuilder = true)
public class NativePlanEntity extends BasePlanEntity {

    @DeploymentRequired
    @Builder.Default
    private List<NativeFlow> flows = new ArrayList<>();
}
