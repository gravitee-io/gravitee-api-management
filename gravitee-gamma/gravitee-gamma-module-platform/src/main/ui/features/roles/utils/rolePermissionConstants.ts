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

/** Gates the `roles` list route — mirrors organization-settings-routing.module.ts's `role/:roleScope` guard. */
export const ORGANIZATION_ROLE_READ_PERMISSION = 'organization-role-r' as const;

/** Gates the "Add a role" action — mirrors the `*gioPermission` on the Angular list page's create button. */
export const ORGANIZATION_ROLE_CREATE_PERMISSION = 'organization-role-c' as const;

/**
 * Gates the create/edit/members routes and the members add/delete actions — mirrors
 * organization-settings-routing.module.ts, where all three routes require `organization-role-u`
 * (not `-c`, even for the create route).
 */
export const ORGANIZATION_ROLE_UPDATE_PERMISSION = 'organization-role-u' as const;

/** Gates the delete-role action — mirrors the `*gioPermission` on the Angular list page's delete button. */
export const ORGANIZATION_ROLE_DELETE_PERMISSION = 'organization-role-d' as const;
