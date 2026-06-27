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
import { getAllApplications, saveApplications } from './applications.storage';
import { createDummyApplications } from './dummy-applications';
import { createDummySubscriptions } from './dummy-subscriptions';
import { getAllSubscriptions, saveSubscriptions } from './subscriptions.storage';

export async function seedCatalogDataIfEmpty(): Promise<void> {
    const applications = await getAllApplications();
    const subscriptions = await getAllSubscriptions();

    if (applications.length === 0) {
        await saveApplications(createDummyApplications());
    }

    if (subscriptions.length === 0) {
        await saveSubscriptions(createDummySubscriptions());
    }
}
