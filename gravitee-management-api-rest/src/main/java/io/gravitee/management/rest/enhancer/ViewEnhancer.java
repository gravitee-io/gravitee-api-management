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
package io.gravitee.management.rest.enhancer;

import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.ViewEntity;
import io.gravitee.repository.management.model.View;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Function;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ViewEnhancer {

    public Function<ViewEntity, ViewEntity> enhance(Set<ApiEntity> apis) {
        return view -> {
            long totalApis = apis.stream()
                                    .filter(api -> View.ALL_ID.equals(view.getId())
                                            || (api.getViews() != null && api.getViews().contains(view.getId())))
                                    .count();
            view.setTotalApis(totalApis);

            return view;
        };
    }
}
