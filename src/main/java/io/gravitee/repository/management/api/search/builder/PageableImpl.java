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
package io.gravitee.repository.management.api.search.builder;

import io.gravitee.repository.management.api.search.Pageable;

/**
 * @author David BRASSELY (david at graviteesource.com)
 * @author GraviteeSource Team
 */
class PageableImpl implements Pageable {

    private int pageSize;
    private int pageNumber;

    public PageableImpl(int pageSize, int pageNumber) {
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
    }

    @Override
    public int pageSize() {
        return pageSize;
    }

    @Override
    public int pageNumber() {
        return pageNumber;
    }

    @Override
    public int from() {
        return (pageNumber) * pageSize;
    }

    @Override
    public int to() {
        return ((pageNumber + 1) * pageSize) - 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PageableImpl pageable = (PageableImpl) o;

        if (pageSize != pageable.pageSize) return false;
        return pageNumber == pageable.pageNumber;

    }

    @Override
    public int hashCode() {
        int result = pageSize;
        result = 31 * result + pageNumber;
        return result;
    }
}
