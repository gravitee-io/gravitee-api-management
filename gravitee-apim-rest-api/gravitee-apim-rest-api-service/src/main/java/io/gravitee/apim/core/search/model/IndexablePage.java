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
package io.gravitee.apim.core.search.model;

import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.rest.api.model.search.Indexable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class IndexablePage implements Indexable {

    private Page page;

    @Override
    public String getId() {
        return page.getId();
    }

    @Override
    public void setId(String id) {
        page.setId(id);
    }

    @Override
    public String getReferenceType() {
        return page.getReferenceType().name();
    }

    @Override
    public void setReferenceType(String referenceType) {
        page.setReferenceType(Page.ReferenceType.valueOf(referenceType));
    }

    @Override
    public String getReferenceId() {
        return page.getReferenceId();
    }

    @Override
    public void setReferenceId(String referenceId) {
        page.setReferenceId(referenceId);
    }
}
