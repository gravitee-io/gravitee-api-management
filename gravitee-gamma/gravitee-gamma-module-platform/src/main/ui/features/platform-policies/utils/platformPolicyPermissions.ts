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

/**
 * `GET /organizations/{orgId}` is annotated `ORGANIZATION_POLICIES` with CREATE, DELETE and UPDATE, and
 * does not accept READ. Gating the page on `-r` would open a studio whose very first request answers 403,
 * so the route mirrors the acls the API actually accepts. Serving read-only users takes a backend change
 * first, tracked separately.
 */
export const ORGANIZATION_POLICIES_ACCESS_PERMISSIONS = [
    'organization-policies-c',
    'organization-policies-d',
    'organization-policies-u',
] as const;

export const ORGANIZATION_POLICIES_UPDATE_PERMISSION = 'organization-policies-u' as const;
