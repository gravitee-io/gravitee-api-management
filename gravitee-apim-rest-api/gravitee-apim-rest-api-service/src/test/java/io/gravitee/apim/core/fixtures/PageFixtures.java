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
package io.gravitee.apim.core.fixtures;

import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;

public class PageFixtures {

    public static Page aPage(String apiId, String pageId, String name) {
        var page = new Page();
        page.setId(pageId);
        page.setName(name);
        page.setReferenceId(apiId);
        page.setReferenceType(PageReferenceType.API);
        return page;
    }
}
