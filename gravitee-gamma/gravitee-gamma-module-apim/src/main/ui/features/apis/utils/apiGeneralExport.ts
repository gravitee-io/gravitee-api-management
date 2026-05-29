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
import type { ApiDetailDto } from '../types';

export const EXPORT_INCLUDE_OPTIONS = [
    { id: 'groups', label: 'Groups' },
    { id: 'members', label: 'Members' },
    { id: 'pages', label: 'Pages' },
    { id: 'plans', label: 'Plans' },
    { id: 'metadata', label: 'Metadata' },
] as const;

export type ExportIncludeKey = (typeof EXPORT_INCLUDE_OPTIONS)[number]['id'];

export function buildExportFileName(api: ApiDetailDto | null, suffix?: string): string {
    const base = `${api?.name ?? 'api'}-${api?.apiVersion ?? 'export'}`.replace(/[\s]/gi, '-').replace(/[^\w-]/gi, '-');
    return suffix ? `${base}${suffix}` : base;
}

export function buildExcludeAdditionalData(include: Record<ExportIncludeKey, boolean>): string[] {
    return EXPORT_INCLUDE_OPTIONS.filter(option => !include[option.id]).map(option => option.id);
}

export function downloadBlob(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.download = fileName;
    anchor.href = url;
    anchor.click();
    URL.revokeObjectURL(url);
}
