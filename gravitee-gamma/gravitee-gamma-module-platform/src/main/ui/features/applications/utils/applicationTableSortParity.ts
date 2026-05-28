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

/** Console `gio-metadata` sortable columns for application metadata (client-side on full list). */
export const APPLICATION_METADATA_SORTABLE_IDS = new Set(['key', 'name', 'format', 'value']);

/** Console `subscription-api-keys` mat-sort-header columns (client-side on full list). */
export const SUBSCRIPTION_API_KEY_SORTABLE_IDS = new Set(['isValid', 'createdAt', 'endDate']);
