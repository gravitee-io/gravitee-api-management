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
package io.gravitee.rest.api.management.rest.model.wrapper;

import io.gravitee.rest.api.management.rest.model.PagedResult;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import java.util.Collection;

public class ApplicationListItemPagedResult extends PagedResult<ApplicationListItem> {

    public ApplicationListItemPagedResult() {
        super();
    }

    public ApplicationListItemPagedResult(Collection<ApplicationListItem> data, int pageNumber, int perPage, int totalElements) {
        super(data, pageNumber, perPage, totalElements);
    }

    public ApplicationListItemPagedResult(Collection<ApplicationListItem> data) {
        super(data);
    }

    public ApplicationListItemPagedResult(io.gravitee.common.data.domain.Page<ApplicationListItem> page, int perPage) {
        super(page.getContent(), page.getPageNumber() + 1, perPage, (int) page.getTotalElements());
    }
}
