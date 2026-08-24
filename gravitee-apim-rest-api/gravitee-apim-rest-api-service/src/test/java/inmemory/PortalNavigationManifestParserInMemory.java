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
package inmemory;

import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemSourceException;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationManifestParser;
import java.util.ArrayList;
import java.util.List;

public class PortalNavigationManifestParserInMemory implements PortalNavigationManifestParser {

    public static final String MANIFEST_FILE_NAME = ".gravitee.json";

    private final List<ManifestPage> pages = new ArrayList<>();
    private boolean failOnParse = false;

    public void willParse(List<ManifestPage> manifestPages) {
        pages.clear();
        pages.addAll(manifestPages);
    }

    public void failOnParse() {
        this.failOnParse = true;
    }

    @Override
    public String manifestFileName() {
        return MANIFEST_FILE_NAME;
    }

    @Override
    public List<ManifestPage> parse(String manifestContent) {
        if (failOnParse) {
            throw InvalidPortalNavigationItemSourceException.invalidManifest(new RuntimeException("unreadable manifest"));
        }
        return List.copyOf(pages);
    }
}
