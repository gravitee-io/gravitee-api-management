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
 * Environment notification settings reuse the application-scoped notification types
 * (`ApplicationNotificationSettings`, `ApplicationNotifier`, `ApplicationNotificationHook`,
 * `ApplicationNotificationHookCategory`, `ApplicationNotificationRow`, `UpdateApplicationNotification`)
 * from `features/applications/types/applicationNotification.ts` as-is: the backend entities
 * (`GenericNotificationConfigEntity` / `PortalNotificationConfigEntity`) are identical in shape
 * regardless of `referenceType`. Only the create payload needs an environment-specific
 * `referenceType` literal, hence this dedicated type.
 */
export interface CreateEnvironmentNotification {
    readonly name: string;
    readonly notifier: string;
    readonly referenceType: 'ENVIRONMENT';
    readonly referenceId: string;
    readonly config_type: 'GENERIC';
    readonly hooks: string[];
}
