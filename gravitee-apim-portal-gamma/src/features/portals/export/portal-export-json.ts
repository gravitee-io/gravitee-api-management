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
import { aggregatePortalExport } from './aggregate-portal-export';
import { derivePortalHrid } from './derive-portal-hrid';
import { downloadFile } from './download-file';
import type { PortalExportBundle } from './portal-export.types';

export function exportPortalToJson(bundle: PortalExportBundle): string {
    return `${JSON.stringify(bundle, null, 2)}\n`;
}

export async function downloadPortalJson(portalId: string): Promise<void> {
    const bundle = await aggregatePortalExport(portalId);
    const hrid = derivePortalHrid(bundle.portal);
    const json = exportPortalToJson(bundle);
    downloadFile(json, `portal-${hrid}.json`, 'application/json');
}
