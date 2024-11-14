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
package io.gravitee.rest.api.management.rest.model;

import io.gravitee.rest.api.model.common.PageableImpl;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Pageable {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE_NUMBER = 1;

    @QueryParam("size")
    @DefaultValue("20")
    @Min(value = 1, message = "Page size should not be less than 1")
    @Max(value = 200, message = "Page size should not be more than 200")
    private int size = DEFAULT_PAGE_SIZE;

    @QueryParam("page")
    @DefaultValue("1")
    @Min(value = 1, message = "Page number should not be less than 1")
    private int page = DEFAULT_PAGE_NUMBER;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public io.gravitee.rest.api.model.common.Pageable toPageable() {
        return new PageableImpl(this.getPage(), this.getSize());
    }
}
