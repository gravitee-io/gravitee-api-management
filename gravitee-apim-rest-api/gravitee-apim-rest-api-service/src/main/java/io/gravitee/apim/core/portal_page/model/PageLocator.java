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
package io.gravitee.apim.core.portal_page.model;

import javax.annotation.Nonnull;

public class PageLocator {

    public static final PageLocator HOMEPAGE = new PageLocator(true);

    @Nonnull
    private final Boolean homepage;

    public PageLocator(boolean homepage) {
        this.homepage = homepage;
    }

    public boolean isHomepage() {
        return homepage;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PageLocator that = (PageLocator) o;
        return homepage.equals(that.homepage);
    }

    @Override
    public int hashCode() {
        return homepage.hashCode();
    }
}
