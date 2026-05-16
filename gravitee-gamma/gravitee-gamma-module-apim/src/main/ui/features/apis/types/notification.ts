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

/** A configured notification subscription on an API. */
export interface NotificationSettings {
    /** Present for GENERIC notifications; absent for PORTAL (default console) notification. */
    id?: string;
    name: string;
    referenceType: string;
    referenceId: string;
    /** Notifier plugin ID (e.g. 'default-email', 'default-webhook'). Absent for PORTAL. */
    notifier?: string;
    hooks: string[];
    /** Hooks inherited from the API's group — displayed but not editable. */
    groupHooks?: string[];
    /** Email address(es) or webhook URL. Only relevant for EMAIL and WEBHOOK channels. */
    config?: string;
    useSystemProxy?: boolean;
    /** 'PORTAL' = default in-app console notification; 'GENERIC' = user-created. */
    config_type: 'PORTAL' | 'GENERIC';
    /** 'MANAGEMENT' (editable) | 'KUBERNETES' (read-only). */
    origin?: string;
}

/** A notifier plugin available for this API (returned by /notifiers endpoint). */
export interface ApiNotifier {
    id: string;
    /** 'EMAIL' | 'WEBHOOK' | 'DEFAULT' (console). */
    type: string;
    name: string;
}

/** A single hookable event returned by /apis/hooks. */
export interface ApiHook {
    id: string;
    label: string;
    description: string;
    scope: string;
    category: string;
}

/** Hooks grouped by their category label. */
export interface HookCategory {
    name: string;
    hooks: ApiHook[];
}

/** Derived channel type used only in the UI for display and filtering. */
export type NotificationChannel = 'CONSOLE' | 'EMAIL' | 'WEBHOOK';

/** Payload sent to POST /notificationsettings to create a notification. */
export interface CreateNotificationPayload {
    name: string;
    /** Notifier plugin ID. */
    notifier: string;
    config_type: 'GENERIC';
    hooks: string[];
    referenceType: 'API';
    referenceId: string;
}

/** Payload sent to PUT /notificationsettings/{id} to update a notification. */
export type UpdateNotificationPayload = NotificationSettings;
