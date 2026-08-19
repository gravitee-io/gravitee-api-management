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

import { formatAuditTargetText } from './auditListFormat';
import { downloadTextFile } from '../../../shared/browser';
import type { AuditExportFormat, AuditLogRow } from '../types/auditLog';

export function buildAuditExportFileName(format: AuditExportFormat, now = new Date()): string {
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `audit-logs-${year}-${month}-${day}.${format}`;
}

// Audit rows carry operator-controlled text (API/application/user names, JSON patches). A cell starting
// with one of these is executed as a formula by Excel/Sheets, so prefix it with a quote to neutralise it.
const CSV_FORMULA_PREFIXES = ['=', '+', '-', '@', '\t', '\r'];

export function csvEscape(value: string | null | undefined): string {
    const text = value ?? '';
    const safe = CSV_FORMULA_PREFIXES.some(prefix => text.startsWith(prefix)) ? `'${text}` : text;
    if (/[",\n\r]/.test(safe)) {
        return `"${safe.replace(/"/g, '""')}"`;
    }
    return safe;
}

function exportColumns(row: AuditLogRow): string[] {
    return [
        new Date(row.createdAt).toISOString(),
        row.user,
        row.referenceType,
        row.reference,
        row.event,
        formatAuditTargetText(row.targets),
        row.patch,
    ];
}

export function auditLogsToCsv(rows: readonly AuditLogRow[]): string {
    const header = ['Date', 'User', 'Type', 'Reference', 'Event', 'Target', 'Patch'];
    const lines = [header.map(csvEscape).join(',')];
    for (const row of rows) {
        lines.push(exportColumns(row).map(csvEscape).join(','));
    }
    return `${lines.join('\n')}\n`;
}

export function auditLogsToJson(rows: readonly AuditLogRow[]): string {
    return JSON.stringify(
        rows.map(row => ({
            date: new Date(row.createdAt).toISOString(),
            user: row.user,
            type: row.referenceType,
            reference: row.reference,
            event: row.event,
            target: formatAuditTargetText(row.targets),
            patch: row.patch,
        })),
        null,
        2,
    );
}

export function downloadAuditExport(content: string, fileName: string, mimeType: string): void {
    downloadTextFile(content, fileName, mimeType);
}
