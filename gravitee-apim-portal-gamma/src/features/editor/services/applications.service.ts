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
import type { Application, ApplicationsResponse } from '../entities/application';
import {
    getAllApplications,
    getApplication,
    saveApplication,
} from '../../portals/storage/applications.storage';

export interface ListApplicationsParams {
    page?: number;
    size?: number;
    q?: string;
}

export async function listApplications({
    page = 1,
    size = 6,
    q = '',
}: ListApplicationsParams = {}): Promise<ApplicationsResponse> {
    const all = await getAllApplications();
    const query = q.trim().toLowerCase();
    const filtered = all.filter(app => {
        if (!query) return true;
        return (
            app.name.toLowerCase().includes(query) ||
            (app.description?.toLowerCase().includes(query) ?? false)
        );
    });

    const start = (page - 1) * size;
    const data = filtered.slice(start, start + size);

    return {
        data,
        metadata: {
            pagination: {
                current_page: page,
                size,
                total: filtered.length,
                total_pages: Math.max(1, Math.ceil(filtered.length / size)),
            },
        },
    };
}

export async function getApplicationById(id: string): Promise<Application | undefined> {
    return getApplication(id);
}

export async function createApplication(application: Application): Promise<Application> {
    await saveApplication(application);
    return application;
}

export async function updateApplication(application: Application): Promise<Application> {
    await saveApplication(application);
    return application;
}
