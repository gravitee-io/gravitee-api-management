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

/** Mirrors classic Console's environment alerts route guard (management-routing.module.ts / gio-side-nav). */
export const ENVIRONMENT_ALERT_READ_PERMISSION = 'environment-alert-r' as const;
export const ENVIRONMENT_ALERT_CREATE_PERMISSION = 'environment-alert-c' as const;
export const ENVIRONMENT_ALERT_UPDATE_PERMISSION = 'environment-alert-u' as const;
export const ENVIRONMENT_ALERT_DELETE_PERMISSION = 'environment-alert-d' as const;
