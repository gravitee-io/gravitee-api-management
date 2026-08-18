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

import { auditLogsToCsv, auditLogsToJson, buildAuditExportFileName, downloadAuditExport } from './auditExport';
import type { AuditLogRow } from '../types/auditLog';

const ROW: AuditLogRow = {
    id: 'a-1',
    createdAt: Date.parse('2026-08-19T10:00:00.000Z'),
    user: 'Ada Lovelace',
    referenceType: 'API',
    reference: 'Pets',
    event: 'API_UPDATED',
    targets: [{ key: 'API', value: 'Pets' }],
    patch: '[{"op":"replace","path":"/name","value":"Pets"}]',
};

describe('auditExport', () => {
    it('names the file audit-logs-{yyyy-mm-dd}.{format}', () => {
        expect(buildAuditExportFileName('csv', new Date('2026-08-19T15:00:00'))).toBe('audit-logs-2026-08-19.csv');
        expect(buildAuditExportFileName('json', new Date('2026-08-19T15:00:00'))).toBe('audit-logs-2026-08-19.json');
    });

    it('serializes table columns to CSV, quoting commas and quotes', () => {
        const csv = auditLogsToCsv([{ ...ROW, user: 'Ada, "Countess"' }]);
        expect(csv).toContain('Date,User,Type,Reference,Event,Target,Patch');
        expect(csv).toContain('"Ada, ""Countess"""');
        expect(csv).toContain('API_UPDATED');
        expect(csv).toContain('API: Pets');
    });

    it('neutralises spreadsheet formulas in CSV cells', () => {
        const csv = auditLogsToCsv([
            {
                ...ROW,
                user: '=HYPERLINK("http://evil","click")',
                reference: '+1234',
                event: '-cmd',
                targets: [{ key: 'API', value: '@SUM(A1)' }],
            },
        ]);
        // Each dangerous cell is prefixed with a quote so Excel/Sheets treat it as text.
        expect(csv).toContain(`"'=HYPERLINK(""http://evil"",""click"")"`);
        expect(csv).toContain(`'+1234`);
        expect(csv).toContain(`'-cmd`);
        expect(csv).not.toContain(',=HYPERLINK');
        // The guard is per cell: a target renders as `KEY: value`, so a leading `@` in the value is
        // already inert and is left alone.
        expect(csv).toContain('API: @SUM(A1)');
    });

    it('leaves ordinary CSV cells untouched', () => {
        const csv = auditLogsToCsv([ROW]);
        expect(csv).toContain('Ada Lovelace');
        expect(csv).toContain('API_UPDATED');
        expect(csv).not.toContain("'Ada");
    });

    it('serializes table columns to JSON', () => {
        const parsed = JSON.parse(auditLogsToJson([ROW])) as Array<{ event: string; target: string }>;
        expect(parsed).toEqual([
            expect.objectContaining({
                event: 'API_UPDATED',
                target: 'API: Pets',
                type: 'API',
                user: 'Ada Lovelace',
            }),
        ]);
    });

    it('downloads a blob with the given filename', () => {
        const click = jest.fn();
        const revoke = jest.fn();
        jest.spyOn(URL, 'createObjectURL').mockReturnValue('blob:audit');
        jest.spyOn(URL, 'revokeObjectURL').mockImplementation(revoke);
        jest.spyOn(document, 'createElement').mockReturnValue({ click, download: '', href: '' } as unknown as HTMLAnchorElement);

        downloadAuditExport('csv-body', 'audit-logs-2026-08-19.csv', 'text/csv');

        expect(click).toHaveBeenCalled();
        expect(revoke).toHaveBeenCalledWith('blob:audit');
    });
});
