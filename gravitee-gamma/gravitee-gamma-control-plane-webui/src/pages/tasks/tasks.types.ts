/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
export type TaskType = 'SUBSCRIPTION_APPROVAL' | 'IN_REVIEW' | 'REQUEST_FOR_CHANGES' | 'USER_REGISTRATION_APPROVAL' | 'PROMOTION_APPROVAL';

export interface TaskEntity {
    readonly type: TaskType;
    readonly data: Record<string, unknown>;
    readonly created_at: number;
}

export type TaskMetadata = Record<string, Record<string, unknown> | undefined>;

export interface TasksResponse {
    readonly data: TaskEntity[];
    readonly metadata: TaskMetadata;
    readonly page?: { readonly total_elements?: number };
}

export type TaskCategory = 'SUBSCRIPTION' | 'API_REVIEW' | 'CHANGES_REQUESTED' | 'USER_REGISTRATION' | 'API_PROMOTION';

export type TaskAreaKey = 'apim' | 'mcp' | 'ai' | 'kafka' | 'users';

export interface TaskArea {
    readonly key: TaskAreaKey;
    readonly label: string;
}

export type TaskIconKey = 'subscription' | 'review' | 'changes' | 'registration' | 'promotion';

export interface TaskView {
    readonly id: string;
    readonly type: TaskType;
    readonly category: TaskCategory;
    readonly categoryLabel: string;
    readonly actionLabel: string;
    readonly iconKey: TaskIconKey;
    readonly area: TaskArea;
    readonly title: string;
    readonly subtitle: string;
    readonly comment?: string;
    readonly createdAt: number;
    readonly to: string | null;
    readonly toModuleId: string | null;
    readonly entity: TaskEntity;
}
