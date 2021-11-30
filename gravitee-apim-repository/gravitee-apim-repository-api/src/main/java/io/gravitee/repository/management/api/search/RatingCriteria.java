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
package io.gravitee.repository.management.api.search;

import static java.util.Arrays.asList;

import io.gravitee.repository.management.model.RatingReferenceType;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RatingCriteria {

    private final Collection<String> referenceIds;

    private RatingReferenceType referenceType;

    private final int gt;

    RatingCriteria(RatingCriteria.Builder builder) {
        this.referenceIds = builder.referenceIds;
        this.referenceType = builder.referenceType;
        this.gt = builder.gt;
    }

    public Collection<String> getReferenceIds() {
        return referenceIds;
    }

    public RatingReferenceType getReferenceType() {
        return referenceType;
    }

    public int getGt() {
        return gt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RatingCriteria)) return false;
        RatingCriteria that = (RatingCriteria) o;
        return (
            Objects.equals(referenceIds, that.referenceIds) &&
            Objects.equals(referenceType, that.referenceType) &&
            Objects.equals(gt, that.gt)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceIds, referenceType, gt);
    }

    public static class Builder {

        private Collection<String> referenceIds;
        private RatingReferenceType referenceType = RatingReferenceType.API;
        private int gt = 0;

        public RatingCriteria.Builder referenceIds(final String... referenceIds) {
            this.referenceIds = asList(referenceIds);
            return this;
        }

        public RatingCriteria.Builder referenceIds(Collection<String> referenceIds) {
            this.referenceIds = referenceIds;
            return this;
        }

        public RatingCriteria.Builder referenceType(final RatingReferenceType referenceType) {
            this.referenceType = referenceType;
            return this;
        }

        public RatingCriteria.Builder gt(final int minRate) {
            this.gt = minRate > 0 ? minRate : 0;
            return this;
        }

        public RatingCriteria build() {
            return new RatingCriteria(this);
        }
    }
}
