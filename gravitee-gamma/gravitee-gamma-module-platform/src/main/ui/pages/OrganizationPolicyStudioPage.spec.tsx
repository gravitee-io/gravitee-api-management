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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import type { Flow, FlowExecution, OrganizationTag, SaveOutput } from '@gravitee/graphene-policy-studio';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { OrganizationPolicyStudioPage } from './OrganizationPolicyStudioPage';
import { listOrgTags } from '../features/entrypoints/services/tags';
import { getOrganization, updateOrganization } from '../features/platform-policies/services/platformPolicies';
import type { Organization } from '../features/platform-policies/types/platformPolicies';
import { listPolicies } from '../shared/services/policyPlugins';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

let capturedLayoutConfig: Record<string, unknown> | undefined;
/** What the studio is told when its save does not go through, as the real one reports it on the Save button. */
let mockSaveRejection: string | undefined;
jest.mock('@gravitee/graphene-core', () => ({
    ...jest.requireActual<object>('@gravitee/graphene-core'),
    useLayoutConfig: (config: Record<string, unknown>) => {
        capturedLayoutConfig = config;
    },
}));

// The studio itself is covered by its own package: the page owns which props it feeds it and what
// happens when it asks to save.
jest.mock('@gravitee/graphene-policy-studio', () => ({
    PolicyStudio: ({
        scope,
        apiType,
        readOnly,
        commonFlows,
        organizationTags,
        flowExecution,
        onSave,
    }: {
        scope: string;
        apiType: string;
        readOnly: boolean;
        commonFlows: readonly Flow[];
        organizationTags: readonly OrganizationTag[];
        flowExecution: FlowExecution;
        onSave: (output: SaveOutput) => Promise<void> | void;
    }) => (
        <div>
            <span>{`scope: ${scope}`}</span>
            <span>{`api type: ${apiType}`}</span>
            <span>{`read only: ${String(readOnly)}`}</span>
            <span>{`flows: ${commonFlows.map(flow => flow.name).join(', ')}`}</span>
            <span>{`tags: ${organizationTags.map(tag => tag.name).join(', ')}`}</span>
            <span>{`flow mode: ${flowExecution.mode}`}</span>
            <button
                type="button"
                onClick={() => {
                    void Promise.resolve(onSave({ commonFlows, flowExecution: { mode: 'BEST_MATCH' } })).catch((error: Error) => {
                        mockSaveRejection = error.message;
                    });
                }}
            >
                Save
            </button>
        </div>
    ),
}));

jest.mock('../features/platform-policies/services/platformPolicies', () => ({
    getOrganization: jest.fn(),
    updateOrganization: jest.fn(),
}));

jest.mock('../shared/services/policyPlugins', () => ({
    listPolicies: jest.fn(),
    getPolicySchema: jest.fn(),
    getPolicyDocumentation: jest.fn(),
}));

jest.mock('../features/entrypoints/services/tags', () => ({
    listOrgTags: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockGetOrganization = jest.mocked(getOrganization);
const mockUpdateOrganization = jest.mocked(updateOrganization);
const mockListPolicies = jest.mocked(listPolicies);
const mockListOrgTags = jest.mocked(listOrgTags);

const ORGANIZATION: Organization = {
    id: 'DEFAULT',
    name: 'Gravitee',
    flowMode: 'DEFAULT',
    flows: [
        {
            id: 'flow-1',
            name: 'Partner traffic',
            enabled: true,
            'path-operator': { path: '/partners', operator: 'STARTS_WITH' },
            consumers: [{ consumerType: 'TAG', consumerId: 'tag-eu' }],
        },
    ],
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return render(<OrganizationPolicyStudioPage />, { wrapper: Wrapper });
}

async function renderLoadedPage() {
    renderPage();
    await screen.findByText('flows: Partner traffic');
}

describe('OrganizationPolicyStudioPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        capturedLayoutConfig = undefined;
        mockSaveRejection = undefined;
        mockUseHasPermission.mockReturnValue(true);
        mockGetOrganization.mockResolvedValue(ORGANIZATION);
        mockUpdateOrganization.mockResolvedValue(undefined);
        mockListPolicies.mockResolvedValue([{ id: 'rate-limit', name: 'Rate Limiting' }]);
        mockListOrgTags.mockResolvedValue([{ id: 'tag-eu', key: 'eu', name: 'Europe' }]);
    });

    it('opens the studio on the organization scope with the stored platform flows', async () => {
        await renderLoadedPage();

        expect(screen.getByText('scope: ORGANIZATION')).toBeTruthy();
        expect(screen.getByText('api type: PROXY')).toBeTruthy();
        expect(screen.getByText('flow mode: DEFAULT')).toBeTruthy();
    });

    it('says which phases the platform flows reach, without shouting it at screen readers', async () => {
        await renderLoadedPage();

        expect(screen.getByRole('region', { name: 'Platform flows scope' })).toBeTruthy();
        expect(screen.getByText(/run on the request and response phases of every API in this organization/, { exact: false })).toBeTruthy();
    });

    it('offers the organization sharding tags to the flow form', async () => {
        await renderLoadedPage();

        expect(screen.getByText('tags: Europe')).toBeTruthy();
    });

    it('gives the studio a full-bleed page', async () => {
        await renderLoadedPage();

        expect(capturedLayoutConfig).toEqual({ contentVariant: 'full-bleed' });
    });

    it('turns the studio read-only without the update permission', async () => {
        mockUseHasPermission.mockReturnValue(false);
        await renderLoadedPage();

        expect(screen.getByText('read only: true')).toBeTruthy();
    });

    it('asks to confirm the gateway deployment before saving', async () => {
        await renderLoadedPage();

        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        expect(await screen.findByText('Deploy the policies?')).toBeTruthy();
        expect(mockUpdateOrganization).not.toHaveBeenCalled();
    });

    it('saves the studio output once the deployment is confirmed', async () => {
        await renderLoadedPage();

        fireEvent.click(screen.getByRole('button', { name: 'Save' }));
        fireEvent.click(await screen.findByRole('button', { name: 'Save and deploy' }));

        await waitFor(() => expect(mockUpdateOrganization).toHaveBeenCalled());
        expect(mockUpdateOrganization.mock.calls[0][0]).toEqual(expect.objectContaining({ id: 'DEFAULT', flowMode: 'BEST_MATCH' }));
    });

    it('writes nothing and keeps the studio unsaved when the deployment is cancelled', async () => {
        await renderLoadedPage();

        fireEvent.click(screen.getByRole('button', { name: 'Save' }));
        fireEvent.click(await screen.findByRole('button', { name: 'Cancel' }));

        await waitFor(() => expect(mockSaveRejection).toBe('Deployment cancelled'));
        expect(screen.queryByText('Deploy the policies?')).toBeNull();
        expect(mockUpdateOrganization).not.toHaveBeenCalled();
    });

    it('hands the failure back to the studio so the flows stay unsaved when the deployment fails', async () => {
        mockUpdateOrganization.mockRejectedValue(new Error('Gateway unreachable'));
        await renderLoadedPage();

        fireEvent.click(screen.getByRole('button', { name: 'Save' }));
        fireEvent.click(await screen.findByRole('button', { name: 'Save and deploy' }));

        await waitFor(() => expect(mockSaveRejection).toBe('Gateway unreachable'));
        expect(screen.queryByText('Deploy the policies?')).toBeNull();
    });

    it('falls back to a generic message when the deployment fails with a non-Error', async () => {
        mockUpdateOrganization.mockRejectedValue('boom');
        await renderLoadedPage();

        fireEvent.click(screen.getByRole('button', { name: 'Save' }));
        fireEvent.click(await screen.findByRole('button', { name: 'Save and deploy' }));

        await waitFor(() => expect(mockSaveRejection).toBe('Failed to deploy the platform policies'));
        expect(screen.queryByText('Deploy the policies?')).toBeNull();
    });

    it('reports a failure to load the platform policies', async () => {
        mockGetOrganization.mockRejectedValue(new Error('boom'));
        renderPage();

        expect(await screen.findByText('Failed to load the platform policies. Please refresh and try again.')).toBeTruthy();
    });
});
