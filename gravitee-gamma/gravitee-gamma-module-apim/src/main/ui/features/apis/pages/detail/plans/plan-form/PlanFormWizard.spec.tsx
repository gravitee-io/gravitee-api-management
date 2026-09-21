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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../../services/plans', () => ({
    getPlan: jest.fn(),
    updatePlan: jest.fn(),
    createPlan: jest.fn(),
    deletePlan: jest.fn(),
    listPlans: jest.fn(),
    transitionPlan: jest.fn(),
}));

jest.mock('../../../../hooks/useGroups', () => ({ useGroups: jest.fn(() => ({ data: undefined })) }));
jest.mock('../../../../hooks/useOrgTags', () => ({ useOrgTags: jest.fn(() => ({ data: [] })) }));
jest.mock('../../../../hooks/useUserTags', () => ({ useUserTags: jest.fn(() => ({ data: [] })) }));
jest.mock('../../../../hooks/useApiDetail', () => ({ useApiDetail: jest.fn(() => ({ data: undefined })) }));
jest.mock('../../../../hooks/useApiResources', () => ({ useResourcePlugins: jest.fn(() => ({ data: [] })) }));

import { PlanFormWizard } from './PlanFormWizard';
import { createPlan, getPlan, updatePlan } from '../../../../services/plans';
import type { ManagedPlan, PlanContext } from '../../../../types/plan';

const mockCreatePlan = createPlan as jest.Mock;
const mockGetPlan = getPlan as jest.Mock;
const mockUpdatePlan = updatePlan as jest.Mock;

const API_CTX: PlanContext = { type: 'api', entityId: 'api-1' };
const PRODUCT_CTX: PlanContext = { type: 'api-product', entityId: 'product-1' };

const FEDERATED_PLAN: ManagedPlan = {
    id: 'plan-1',
    name: 'Provider Gold',
    description: 'Plan owned by the upstream provider',
    status: 'PUBLISHED',
    security: { type: 'API_KEY' },
    order: 1,
    validation: 'AUTO',
    characteristics: ['gold'],
    definitionVersion: 'FEDERATED',
};

const V4_PLAN: ManagedPlan = {
    ...FEDERATED_PLAN,
    name: 'Gold',
    description: 'Natively managed plan',
    characteristics: undefined,
    definitionVersion: 'V4',
};

function renderEditWizard(plan: ManagedPlan, ctx: PlanContext = API_CTX) {
    mockGetPlan.mockResolvedValue(plan);
    mockUpdatePlan.mockImplementation((_envId, _ctx, _planId, payload) => Promise.resolve({ ...plan, ...payload }));
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>
            <MemoryRouter initialEntries={['/apis/api-1/plans/plan-1']}>
                <PlanFormWizard ctx={ctx} securityType="API_KEY" planId="plan-1" />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

function renderCreateWizard(ctx: PlanContext = API_CTX) {
    mockCreatePlan.mockImplementation((_envId, _ctx, payload) => Promise.resolve({ id: 'plan-new', order: 1, ...payload }));
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>
            <MemoryRouter initialEntries={['/apis/api-1/plans/new/API_KEY']}>
                <PlanFormWizard ctx={ctx} securityType="API_KEY" />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

async function walkToLastStep(user: ReturnType<typeof userEvent.setup>) {
    // An API Product wizard has one step fewer than an API one — walk to the last step whatever it is.
    let next = screen.queryByRole('button', { name: 'Next' });
    while (next) {
        await user.click(next);
        next = screen.queryByRole('button', { name: 'Next' });
    }
}

async function renamePlanAndSave(user: ReturnType<typeof userEvent.setup>, newName: string) {
    const nameInput = await screen.findByLabelText(/^Name/);
    await user.clear(nameInput);
    await user.type(nameInput, newName);
    await walkToLastStep(user);
    await user.click(screen.getByRole('button', { name: /save changes/i }));
}

async function namePlanAndCreate(user: ReturnType<typeof userEvent.setup>, name: string) {
    await user.type(await screen.findByLabelText(/^Name/), name);
    await walkToLastStep(user);
    await user.click(screen.getByRole('button', { name: /create plan/i }));
}

function lastUpdatePayload(): Partial<ManagedPlan> {
    return mockUpdatePlan.mock.calls.at(-1)?.[3] as Partial<ManagedPlan>;
}

function lastCreatePayload(): Partial<ManagedPlan> {
    return mockCreatePlan.mock.calls.at(-1)?.[2] as Partial<ManagedPlan>;
}

describe('PlanFormWizard edit submit', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('saves a federated plan under its own FEDERATED definition version', async () => {
        const user = userEvent.setup();
        renderEditWizard(FEDERATED_PLAN);

        await renamePlanAndSave(user, 'Renamed by operator');

        await waitFor(() => expect(mockUpdatePlan).toHaveBeenCalled());
        expect(lastUpdatePayload()).toMatchObject({ definitionVersion: 'FEDERATED', name: 'Renamed by operator' });
    });

    it('keeps saving a natively managed plan under V4', async () => {
        const user = userEvent.setup();
        renderEditWizard(V4_PLAN);

        await renamePlanAndSave(user, 'Renamed by operator');

        await waitFor(() => expect(mockUpdatePlan).toHaveBeenCalled());
        expect(lastUpdatePayload()).toMatchObject({ definitionVersion: 'V4', name: 'Renamed by operator' });
    });

    it('refuses to save an API plan whose definition version never arrived', async () => {
        const user = userEvent.setup();
        renderEditWizard({ ...FEDERATED_PLAN, definitionVersion: undefined });

        await renamePlanAndSave(user, 'Renamed by operator');

        expect(await screen.findByText(/could not be loaded completely/i)).toBeInTheDocument();
        expect(mockUpdatePlan).not.toHaveBeenCalled();
    });

    it('keeps saving an API Product plan, whose response carries no definition version', async () => {
        const user = userEvent.setup();
        renderEditWizard({ ...FEDERATED_PLAN, definitionVersion: undefined }, PRODUCT_CTX);

        await renamePlanAndSave(user, 'Renamed by operator');

        await waitFor(() => expect(mockUpdatePlan).toHaveBeenCalled());
        expect(lastUpdatePayload()).toMatchObject({ definitionVersion: 'V4', name: 'Renamed by operator' });
    });
});

describe('PlanFormWizard create submit', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('creates a plan under V4, the only definition version a plan can be created as', async () => {
        const user = userEvent.setup();
        renderCreateWizard();

        await namePlanAndCreate(user, 'Gold');

        await waitFor(() => expect(mockCreatePlan).toHaveBeenCalled());
        expect(lastCreatePayload()).toMatchObject({ definitionVersion: 'V4', name: 'Gold' });
    });
});
