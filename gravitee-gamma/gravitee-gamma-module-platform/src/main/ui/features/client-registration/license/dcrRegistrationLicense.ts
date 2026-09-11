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

/** Classic ApimFeature.APIM_DCR_REGISTRATION (gio-license-data). */
export const DCR_REGISTRATION_LICENSE_FEATURE = 'apim-dcr-registration';

export const DCR_REGISTRATION_UPGRADE = {
    title: 'Dynamic Client Registration',
    description:
        'Dynamic Client Registration is part of Gravitee Enterprise. A DCR provider lets APIM register an OAuth client when someone creates a Browser, Web, Native, or Backend-to-Backend application.',
    features: [
        'Point APIM at any OpenID Connect Dynamic Client Registration authorization server',
        'Register OAuth clients automatically when applications are created',
        'Apply grant types that match the application type you choose',
    ],
} as const;
