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
package io.gravitee.apim.infra.adapter.repository;

import io.gravitee.repository.management.model.ExpandsViewContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExpandsViewContextAdapter {

    private ExpandsViewContextAdapter() {}

    public static List<ExpandsViewContext> adapt(List<io.gravitee.apim.core.portal_page.model.ExpandsViewContext> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExpandsViewContext> result = new ArrayList<>();
        for (io.gravitee.apim.core.portal_page.model.ExpandsViewContext value : values) {
            ExpandsViewContext mapped = ExpandsViewContext.fromValue(value.getValue());
            result.add(mapped);
        }
        return result;
    }
}
