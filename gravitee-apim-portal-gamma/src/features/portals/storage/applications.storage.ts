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
import type { Application } from '../../editor/entities/application';
import { APPLICATIONS_STORE_NAME, runTransaction } from './db';

export const STORE_NAME = APPLICATIONS_STORE_NAME;

export async function getAllApplications(): Promise<Application[]> {
    return runTransaction<Application[]>(APPLICATIONS_STORE_NAME, 'readonly', store => store.getAll());
}

export async function getApplication(id: string): Promise<Application | undefined> {
    return runTransaction<Application | undefined>(APPLICATIONS_STORE_NAME, 'readonly', store => store.get(id));
}

export async function saveApplication(application: Application): Promise<void> {
    await runTransaction(APPLICATIONS_STORE_NAME, 'readwrite', store => store.put(application));
}

export async function deleteApplication(id: string): Promise<void> {
    await runTransaction(APPLICATIONS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function saveApplications(applications: readonly Application[]): Promise<void> {
    await Promise.all(applications.map(application => saveApplication(application)));
}
