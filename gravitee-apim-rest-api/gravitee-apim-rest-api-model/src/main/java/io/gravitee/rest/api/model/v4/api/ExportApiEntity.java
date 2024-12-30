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
package io.gravitee.rest.api.model.v4.api;

import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ExportApiEntity {

    private GenericApiEntity apiEntity;

    private Set<MemberEntity> members;

    private Set<ApiMetadataEntity> metadata;

    private List<PageEntity> pages;

    private Set<? extends GenericPlanEntity> plans;

    private List<MediaEntity> apiMedia;

    public void excludeGroups() {
        if (apiEntity instanceof ApiEntity) {
            ((ApiEntity) apiEntity).setGroups(null);
        }
        if (apiEntity instanceof NativeApiEntity) {
            ((NativeApiEntity) apiEntity).setGroups(null);
        }
    }
}
