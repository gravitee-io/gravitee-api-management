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
package io.gravitee.rest.api.model.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SortableImpl implements Sortable {

    private final String field;
    private final boolean ascOrder;
    private final List<SortableImpl> nextSorts = new ArrayList<>();

    public SortableImpl(final String field, final boolean ascOrder) {
        this.field = field;
        this.ascOrder = ascOrder;
    }

    @Override
    public String getField() {
        return field;
    }

    @Override
    public boolean isAscOrder() {
        return ascOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortableImpl sortable = (SortableImpl) o;
        return ascOrder == sortable.ascOrder && Objects.equals(field, sortable.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, ascOrder);
    }

    public SortableImpl thenSort(SortableImpl nextSort) {
        nextSorts.add(nextSort);
        return this;
    }
}
