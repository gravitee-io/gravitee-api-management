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
package io.gravitee.apim.core.portal_page.service_provider;

import jakarta.annotation.Nullable;
import java.util.List;

/** Parses the Gravitee descriptor ({@code .gravitee.json}) of a remote documentation repository. */
public interface PortalNavigationManifestParser {
    String manifestFileName();

    /**
     * @throws io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemSourceException when the manifest cannot be read
     */
    List<ManifestPage> parse(String manifestContent);

    /**
     * One documentation page declared by the manifest. The descriptor's {@code homepage} flag is
     * ignored for portal navigation imports.
     *
     * @param src  path of the file in the repository
     * @param name display name; falls back to the file base name when blank
     * @param dest target folder path; falls back to the file's parent path when blank
     */
    record ManifestPage(String src, @Nullable String name, @Nullable String dest) {}
}
