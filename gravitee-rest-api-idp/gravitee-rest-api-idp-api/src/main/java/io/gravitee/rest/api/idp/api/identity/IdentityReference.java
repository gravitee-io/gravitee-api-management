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
package io.gravitee.rest.api.idp.api.identity;

import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityReference {

    private final String source;
    private final String reference;

    public IdentityReference(String source, String reference) {
        this.source = source;
        this.reference = reference;
    }

    public String getSource() {
        return source;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityReference)) return false;
        IdentityReference that = (IdentityReference) o;
        return Objects.equals(source, that.source) &&
                Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, reference);
    }

    @Override
    public String toString() {
        return "IdentityReference{" +
                "source='" + source + '\'' +
                ", reference='" + reference + '\'' +
                '}';
    }
}
