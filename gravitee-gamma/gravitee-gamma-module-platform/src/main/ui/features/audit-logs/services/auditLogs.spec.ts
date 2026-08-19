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

import {
    AUDIT_EXPORT_MAX_ROWS,
    AUDIT_EXPORT_PAGE_SIZE,
    AuditExportLimitError,
    buildAuditQuery,
    exportEnvAudits,
    exportOrgAudits,
    listAuditApis,
    listAuditApplications,
    listAuditEnvironments,
    listEnvAuditEvents,
    listOrgAuditApplicationsByEnvironment,
    listOrgAuditEvents,
    searchEnvAudits,
    searchOrgAudits,
} from './auditLogs';
import { apimFetchJsonOrg, apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { AuditMetadataPage } from '../types/auditLog';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
    apimFetchJsonV1Env: jest.fn(),
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);
const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);
const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

const EMPTY_PAGE: AuditMetadataPage = {
    content: [],
    pageNumber: 1,
    pageElements: 0,
    totalElements: 0,
    metadata: {},
};

describe('auditLogs service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonOrg.mockResolvedValue(EMPTY_PAGE);
        mockApimFetchJsonV1Env.mockResolvedValue(EMPTY_PAGE);
        mockApimFetchJsonV2.mockResolvedValue({ data: [], pagination: { page: 1, pageCount: 1 } });
    });

    it('omits empty filters and only sends environment/api/application for the matching type', () => {
        expect(
            buildAuditQuery({
                page: 1,
                size: 10,
                event: 'API_UPDATED',
                type: 'API',
                environment: 'env-1',
                application: 'app-1',
                api: 'api-1',
                from: 100,
                to: 200,
            }),
        ).toBe('page=1&size=10&event=API_UPDATED&type=API&api=api-1&from=100&to=200');

        expect(buildAuditQuery({ page: 2, size: 25, type: 'ENVIRONMENT', environment: 'env-1' })).toBe(
            'page=2&size=25&type=ENVIRONMENT&environment=env-1',
        );
        expect(buildAuditQuery({ page: 1, size: 10, from: 1_700_000_000_000, to: 1_700_086_400_000 })).toBe(
            'page=1&size=10&from=1700000000000&to=1700086400000',
        );
    });

    it('searches organization audits on GET /audit', async () => {
        await searchOrgAudits({ page: 1, size: 10, event: 'USER_CREATED' });
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/audit?page=1&size=10&event=USER_CREATED');
    });

    it('searches environment audits on the env-scoped /audit resource', async () => {
        await searchEnvAudits('env-1', { page: 1, size: 10 });
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/audit?page=1&size=10');
    });

    it('lists event types from /audit/events', async () => {
        mockApimFetchJsonOrg.mockResolvedValue(['API_CREATED']);
        mockApimFetchJsonV1Env.mockResolvedValue(['APPLICATION_UPDATED']);
        await expect(listOrgAuditEvents()).resolves.toEqual(['API_CREATED']);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/audit/events');
        await expect(listEnvAuditEvents('env-1')).resolves.toEqual(['APPLICATION_UPDATED']);
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/audit/events');
    });

    it('lists environments, applications, and paged APIs for type filters', async () => {
        mockApimFetchJsonOrg.mockResolvedValue([{ id: 'env-1', name: 'Production' }]);
        mockApimFetchJsonV1Env.mockResolvedValue([{ id: 'app-1', name: 'Portal' }]);
        mockApimFetchJsonV2
            .mockResolvedValueOnce({
                data: [{ id: 'api-1', name: 'Pets' }],
                pagination: { page: 1, pageCount: 2 },
            })
            .mockResolvedValueOnce({
                data: [{ id: 'api-2', name: 'Orders' }],
                pagination: { page: 2, pageCount: 2 },
            });

        await expect(listAuditEnvironments()).resolves.toEqual([{ id: 'env-1', name: 'Production' }]);
        await expect(listAuditApplications('env-1')).resolves.toEqual([{ id: 'app-1', name: 'Portal' }]);
        await expect(listAuditApis('env-1')).resolves.toEqual([
            { id: 'api-1', name: 'Pets' },
            { id: 'api-2', name: 'Orders' },
        ]);
        expect(mockApimFetchJsonV2).toHaveBeenNthCalledWith(1, 'env-1', '/apis?page=1&perPage=9999');
        expect(mockApimFetchJsonV2).toHaveBeenNthCalledWith(2, 'env-1', '/apis?page=2&perPage=9999');
    });

    it('groups org application lookups by environment name and drops empty environments', async () => {
        mockApimFetchJsonV1Env.mockResolvedValueOnce([{ id: 'app-1', name: 'Portal' }]).mockResolvedValueOnce([]);

        await expect(
            listOrgAuditApplicationsByEnvironment([
                { id: 'env-1', name: 'Production' },
                { id: 'env-2', name: 'Staging' },
            ]),
        ).resolves.toEqual([{ group: 'Production', items: [{ id: 'app-1', name: 'Portal' }] }]);
        // The environments list is supplied by the caller, so no extra `/environments` round-trip.
        expect(mockApimFetchJsonOrg).not.toHaveBeenCalled();
    });

    it('walks pages when exporting and throws when the total exceeds the cap', async () => {
        mockApimFetchJsonOrg
            .mockResolvedValueOnce({
                content: [{ id: 'a-1' }],
                pageNumber: 1,
                pageElements: 1,
                totalElements: AUDIT_EXPORT_PAGE_SIZE + 1,
                metadata: { 'USER:u:name': 'Ada' },
            } as AuditMetadataPage)
            .mockResolvedValueOnce({
                content: [{ id: 'a-2' }],
                pageNumber: 2,
                pageElements: 1,
                totalElements: AUDIT_EXPORT_PAGE_SIZE + 1,
                metadata: { 'API:x:name': 'Pets' },
            } as AuditMetadataPage);

        const exported = await exportOrgAudits({ event: 'API_UPDATED' });
        expect(exported.content.map(item => item.id)).toEqual(['a-1', 'a-2']);
        expect(exported.metadata).toEqual({ 'USER:u:name': 'Ada', 'API:x:name': 'Pets' });
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith(`/audit?page=1&size=${AUDIT_EXPORT_PAGE_SIZE}&event=API_UPDATED`);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith(`/audit?page=2&size=${AUDIT_EXPORT_PAGE_SIZE}&event=API_UPDATED`);

        mockApimFetchJsonV1Env.mockResolvedValueOnce({
            ...EMPTY_PAGE,
            totalElements: AUDIT_EXPORT_MAX_ROWS + 1,
        });
        await expect(exportEnvAudits('env-1', {})).rejects.toBeInstanceOf(AuditExportLimitError);
    });
});
