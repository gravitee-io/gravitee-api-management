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

import jakarta.annotation.Nonnull;
import java.net.URL;

public final class PortalLink extends PageElement<URL> {

    public PortalLink(@Nonnull PageId id, @Nonnull URL url) {
        super(id, url);
    }

    public static PortalLink create(URL url) {
        return new PortalLink(PageId.random(), url);
    }

    @Nonnull
    public URL getUrl() {
        return getContent();
    }

    public void setUrl(@Nonnull URL url) {
        super.setContent(url);
    }
}
