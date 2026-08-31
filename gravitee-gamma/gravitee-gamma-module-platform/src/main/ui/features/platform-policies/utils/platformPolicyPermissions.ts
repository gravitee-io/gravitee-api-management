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

/** Mirrors the acls `GET /organizations/{orgId}` accepts, so every user the API will serve is offered the page. */
export const ORGANIZATION_POLICIES_ACCESS_PERMISSIONS = [
    'organization-policies-r',
    'organization-policies-c',
    'organization-policies-d',
    'organization-policies-u',
] as const;

export const ORGANIZATION_POLICIES_UPDATE_PERMISSION = 'organization-policies-u' as const;
