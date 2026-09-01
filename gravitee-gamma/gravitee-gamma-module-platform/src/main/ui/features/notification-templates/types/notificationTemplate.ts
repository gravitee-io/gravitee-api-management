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

export type NotificationTemplateType = 'EMAIL' | 'PORTAL';

export type NotificationTemplateScope =
    | 'API'
    | 'API_PRODUCT'
    | 'APPLICATION'
    | 'PORTAL'
    | 'TEMPLATES_FOR_ACTION'
    | 'TEMPLATES_FOR_ALERT'
    | 'TEMPLATES_TO_INCLUDE';

export interface NotificationTemplate {
    readonly id?: string;
    readonly hook?: string;
    readonly scope: string;
    readonly name: string;
    readonly description?: string;
    readonly title?: string;
    readonly content: string;
    readonly type: NotificationTemplateType;
    readonly created_at?: number;
    readonly updated_at?: number;
    readonly enabled?: boolean;
}

export interface NotificationTemplateDraft {
    readonly enabled: boolean;
    readonly title: string;
    readonly content: string;
}

export interface NotificationTemplateListRow {
    readonly scope: string;
    readonly name: string;
    readonly hook: string;
    readonly description: string;
    readonly overridden: boolean;
    readonly templateSegment: string;
}

export interface NotificationTemplateCategory {
    readonly scope: string;
    readonly label: string;
    readonly description: string;
    readonly rows: readonly NotificationTemplateListRow[];
    readonly customCount: number;
}
