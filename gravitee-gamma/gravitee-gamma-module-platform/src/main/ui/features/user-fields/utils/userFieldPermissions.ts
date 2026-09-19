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

/** Gates the nav item and the list route — mirrors settings-navigation.service.ts's `custom-user-fields` entry. */
export const ORGANIZATION_CUSTOM_USER_FIELD_READ_PERMISSION = 'organization-custom_user_fields-r' as const;

/** Gates the "Add custom field" action — mirrors the `*gioPermission` on the Classic list page's add button. */
export const ORGANIZATION_CUSTOM_USER_FIELD_CREATE_PERMISSION = 'organization-custom_user_fields-c' as const;

/** Gates the edit action — mirrors the `*gioPermission` on the Classic list page's edit button. */
export const ORGANIZATION_CUSTOM_USER_FIELD_UPDATE_PERMISSION = 'organization-custom_user_fields-u' as const;

/** Gates the delete action — mirrors the `*gioPermission` on the Classic list page's delete button. */
export const ORGANIZATION_CUSTOM_USER_FIELD_DELETE_PERMISSION = 'organization-custom_user_fields-d' as const;
