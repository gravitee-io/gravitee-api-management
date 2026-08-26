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

/** Angular ApimFeature.APIM_CUSTOM_ROLES value (gio-license-data.ts). */
export const CUSTOM_ROLES_LICENSE_FEATURE = 'apim-custom-roles';

export const CUSTOM_ROLES_UPGRADE = {
    title: 'Custom Roles',
    description: 'Create custom roles with fine-grained CRUD permissions instead of relying on the built-in system roles.',
    features: ['Define roles per scope with tailored Create/Read/Update/Delete permissions', 'Assign a default role for new members'],
} as const;
