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

export const OPENID_CONNECT_SSO_LICENSE_FEATURE = 'apim-openid-connect-sso';

export const OPENID_CONNECT_SSO_UPGRADE = {
    title: 'OpenID Connect SSO',
    description:
        'OpenID Connect is part of Gravitee Enterprise. The OpenID Connect provider lets users authenticate with third-party providers such as Okta, Keycloak, and Ping.',
    features: [
        'Authenticate console and portal users with any OpenID Connect server',
        'Map IdP claims onto Gravitee user profile fields',
        'Reuse the same identity provider your organization already runs',
    ],
} as const;
