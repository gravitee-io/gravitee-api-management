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
package io.gravitee.apim.infra.domain_service.portal_page;

import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemSourceException;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationManifestParser;
import io.gravitee.rest.api.service.GraviteeDescriptorService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PortalNavigationManifestParserImpl implements PortalNavigationManifestParser {

    private final GraviteeDescriptorService graviteeDescriptorService;

    @Override
    public String manifestFileName() {
        return graviteeDescriptorService.descriptorName();
    }

    @Override
    public List<ManifestPage> parse(String manifestContent) {
        try {
            var descriptor = graviteeDescriptorService.read(manifestContent);
            if (descriptor.getDocumentation() == null || descriptor.getDocumentation().getPages() == null) {
                return List.of();
            }
            return descriptor
                .getDocumentation()
                .getPages()
                .stream()
                .map(page -> new ManifestPage(page.getSrc(), page.getName(), page.getDest()))
                .toList();
        } catch (Exception e) {
            throw InvalidPortalNavigationItemSourceException.invalidManifest(e);
        }
    }
}
