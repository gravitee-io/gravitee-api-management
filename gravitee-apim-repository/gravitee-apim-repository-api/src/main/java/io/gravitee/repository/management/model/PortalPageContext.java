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
package io.gravitee.repository.management.model;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PortalPageContext {

    private String pageId;
    private String context;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalPageContext that = (PortalPageContext) o;
        return Objects.equals(pageId, that.pageId) && Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, context);
    }
}
