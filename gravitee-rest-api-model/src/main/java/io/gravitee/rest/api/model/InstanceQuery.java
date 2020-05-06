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
package io.gravitee.rest.api.model;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InstanceQuery {

    private Collection<EventType> types;
    private Map<String, Object> properties;
    private long from, to;
    private int page, size;
    private boolean includeStopped;

    public boolean isIncludeStopped() {
        return includeStopped;
    }

    public void setIncludeStopped(boolean includeStopped) {
        this.includeStopped = includeStopped;
    }

    public Collection<EventType> getTypes() {
        return types;
    }

    public void setTypes(Collection<EventType> types) {
        this.types = types;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstanceQuery that = (InstanceQuery) o;
        return from == that.from &&
                to == that.to &&
                page == that.page &&
                size == that.size &&
                includeStopped == that.includeStopped &&
                Objects.equals(types, that.types) &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(types, properties, from, to, page, size, includeStopped);
    }

    @Override
    public String toString() {
        return "InstanceQuery{" +
                "types=" + types +
                ", properties=" + properties +
                ", from=" + from +
                ", to=" + to +
                ", page=" + page +
                ", size=" + size +
                ", includeStopped=" + includeStopped +
                '}';
    }
}
