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

export type ApplicationMetadataFormat = 'STRING' | 'NUMERIC' | 'BOOLEAN' | 'DATE' | 'MAIL' | 'URL';

export interface ApplicationMetadata {
    readonly key: string;
    readonly name: string;
    readonly format?: ApplicationMetadataFormat;
    readonly value?: string;
    readonly defaultValue?: string;
}

export interface NewApplicationMetadata {
    readonly name: string;
    readonly format: ApplicationMetadataFormat;
    readonly value: string;
}

export interface UpdateApplicationMetadata {
    readonly key: string;
    readonly name: string;
    readonly format: ApplicationMetadataFormat;
    readonly value: string;
    readonly defaultValue?: string;
}

export interface ApplicationNotifier {
    readonly id?: string;
    readonly type?: string;
    readonly name?: string;
}

export interface ApplicationNotificationHook {
    readonly id: string;
    readonly label: string;
    readonly description: string;
    readonly scope: string;
    readonly category: string;
}

export interface ApplicationNotificationSettings {
    readonly id?: string;
    readonly name: string;
    readonly referenceType: string;
    readonly referenceId: string;
    readonly notifier?: string;
    readonly hooks?: string[];
    readonly groupHooks?: string[];
    readonly useSystemProxy?: boolean;
    readonly config_type: string;
    readonly config?: string;
    readonly groups?: string[];
    readonly origin?: string;
}

export interface CreateApplicationNotification {
    readonly name: string;
    readonly notifier: string;
    readonly referenceType: 'APPLICATION';
    readonly referenceId: string;
    readonly config_type: 'GENERIC';
    readonly hooks: string[];
}

export type UpdateApplicationNotification = ApplicationNotificationSettings;

export interface ApplicationNotificationRow {
    readonly key: string;
    readonly name: string;
    readonly subscribedEvents: number;
    readonly notifierName: string;
    readonly notification: ApplicationNotificationSettings;
    readonly notifier: ApplicationNotifier | undefined;
    readonly isReadonly: boolean;
}

export interface ApplicationNotificationHookCategory {
    readonly name: string;
    readonly hooks: ApplicationNotificationHook[];
}
