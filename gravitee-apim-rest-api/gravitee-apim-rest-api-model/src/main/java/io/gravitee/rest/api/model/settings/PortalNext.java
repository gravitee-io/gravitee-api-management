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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;
import lombok.Data;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalNext {

    @ParameterKey(Key.PORTAL_NEXT_SITE_TITLE)
    private String siteTitle;

    @ParameterKey(Key.PORTAL_NEXT_HOMEPAGE_BANNER_TITLE)
    private String bannerTitle;

    @ParameterKey(Key.PORTAL_NEXT_HOMEPAGE_BANNER_SUBTITLE)
    private String bannerSubtitle;

    @ParameterKey(Key.PORTAL_NEXT_ACCESS_ENABLED)
    private Enabled access;
}
