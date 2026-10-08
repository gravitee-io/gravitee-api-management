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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: () => ({ id: 'DEFAULT' }) }));
jest.mock('@tanstack/react-query', () => ({ useQueryClient: () => ({ invalidateQueries: jest.fn() }) }));
jest.mock('@gravitee/graphene-core', () => ({
    Badge: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
    Button: ({ children, onClick }: { children?: ReactNode; onClick?: () => void }) => (
        <button type="button" onClick={onClick}>
            {children}
        </button>
    ),
    Card: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    Checkbox: () => <input type="checkbox" />,
    DataTable: ({
        columns,
        data,
    }: {
        columns?: { cell?: (ctx: { row: { index: number; original: unknown } }) => ReactNode }[];
        data?: unknown[];
    }) => (
        <div>
            {data?.map((row, index) =>
                columns?.map((col, i) => <div key={`${index}-${i}`}>{col.cell?.({ row: { index, original: row } })}</div>),
            )}
        </div>
    ),
    DataTableEmptyState: () => <div />,
    DateCell: () => <span />,
    DropdownMenu: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DropdownMenuContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    DropdownMenuItem: ({ children, onSelect }: { children?: ReactNode; onSelect?: () => void }) => (
        <button type="button" onClick={onSelect}>
            {children}
        </button>
    ),
    DropdownMenuTrigger: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
}));
jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));
jest.mock('react-router-dom', () => ({ useParams: () => ({ apiId: 'api-1' }) }));
jest.mock('./DiffDialog', () => ({ DiffDialog: () => null }));
jest.mock('./SingleEventDialog', () => ({ SingleEventDialog: () => null }));
jest.mock('../../../hooks/useApiEvents', () => ({
    useApiEvents: () => ({
        data: {
            data: [
                {
                    id: 'live',
                    createdAt: '2026-02-01T00:00:00Z',
                    payload: '{}',
                    initiator: { id: 'u', displayName: 'Admin' },
                    properties: { DEPLOYMENT_NUMBER: '2' },
                },
                {
                    id: 'old',
                    createdAt: '2026-01-01T00:00:00Z',
                    payload: '{}',
                    initiator: { id: 'u', displayName: 'Admin' },
                    properties: { DEPLOYMENT_NUMBER: '1' },
                },
            ],
            pagination: { totalCount: 2 },
        },
        isLoading: false,
    }),
    useLiveDeploymentEvent: () => ({
        id: 'live',
        createdAt: '2026-02-01T00:00:00Z',
        payload: '{}',
        initiator: { id: 'u', displayName: 'Admin' },
        properties: { DEPLOYMENT_NUMBER: '2' },
    }),
}));
jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: () => ({ api: { deploymentState: 'NEED_REDEPLOY' } }),
}));
jest.mock('../../../services/apis', () => ({
    getCurrentDeployment: jest.fn().mockResolvedValue({ name: 'pending' }),
    rollbackApi: jest.fn(),
}));

import { DeploymentHistoryPage } from './DeploymentHistoryPage';
import { getCurrentDeployment } from '../../../services/apis';

describe('DeploymentHistoryPage', () => {
    it('loads the current deployment when comparing a version with pending changes', async () => {
        render(<DeploymentHistoryPage />);
        fireEvent.click(screen.getAllByRole('button', { name: /compare with pending changes/i })[0]);
        await waitFor(() => expect(getCurrentDeployment).toHaveBeenCalledWith('DEFAULT', 'api-1'));
    });
});
