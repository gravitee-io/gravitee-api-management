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
import { portalApi } from './portal-client';
import type { Api, Page, PagesResponse } from './types';

export function getApi(apiId: string): Promise<Api> {
    return portalApi.get<Api>(`/apis/${encodeURIComponent(apiId)}`);
}

export function listApiPages(apiId: string): Promise<PagesResponse> {
    const params = new URLSearchParams({ homepage: 'false', page: '1', size: '-1' });
    return portalApi.get<PagesResponse>(`/apis/${encodeURIComponent(apiId)}/pages?${params.toString()}`);
}

export function getApiPage(apiId: string, pageId: string): Promise<Page> {
    return portalApi.get<Page>(`/apis/${encodeURIComponent(apiId)}/pages/${encodeURIComponent(pageId)}?include=content`);
}
