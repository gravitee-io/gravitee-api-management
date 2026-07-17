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
import type { PageContent } from '../types';
import { PAGE_CONTENTS_STORE_NAME, runTransaction } from './db';

export const STORE_NAME = PAGE_CONTENTS_STORE_NAME;

export async function getPageContent(navigationItemId: string): Promise<PageContent | undefined> {
    return runTransaction(PAGE_CONTENTS_STORE_NAME, 'readonly', store =>
        store.index('navigationItemId').get(navigationItemId),
    );
}

export async function savePageContent(content: PageContent): Promise<void> {
    await runTransaction(PAGE_CONTENTS_STORE_NAME, 'readwrite', store => store.put(content));
}

export async function getPageContentsForPortal(portalId: string): Promise<PageContent[]> {
    return runTransaction<PageContent[]>(PAGE_CONTENTS_STORE_NAME, 'readonly', store =>
        store.index('portalId').getAll(portalId),
    );
}

export async function deletePageContent(id: string): Promise<void> {
    await runTransaction(PAGE_CONTENTS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function deletePageContentsForPortal(portalId: string): Promise<void> {
    const contents = await runTransaction<PageContent[]>(PAGE_CONTENTS_STORE_NAME, 'readonly', store =>
        store.index('portalId').getAll(portalId),
    );
    await Promise.all(contents.map(content => deletePageContent(content.id)));
}
