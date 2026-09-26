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
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

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
import type { ManagedPlan, PlanContext, PlanSecurityType } from '../../../../types/plan';

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

const FEDERATED_OAUTH2_PLAN: ManagedPlan = { ...FEDERATED_PLAN, security: { type: 'OAUTH2' } };

const V4_OAUTH2_PLAN: ManagedPlan = { ...V4_PLAN, security: { type: 'OAUTH2' } };

const SECURED_PLAN_TYPES: PlanSecurityType[] = ['API_KEY', 'JWT', 'OAUTH2', 'MTLS'];

function renderEditWizard(plan: ManagedPlan, ctx: PlanContext = API_CTX, readOnly = false) {
    mockGetPlan.mockResolvedValue(plan);
    mockUpdatePlan.mockImplementation((_envId, _ctx, _planId, payload) => Promise.resolve({ ...plan, ...payload }));
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>
            <MemoryRouter initialEntries={['/apis/api-1/plans/plan-1']}>
                <PlanFormWizard ctx={ctx} securityType={plan.security.type} planId="plan-1" readOnly={readOnly} />
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

function renderEditWizardUnderPlansList(plan: ManagedPlan) {
    mockGetPlan.mockResolvedValue(plan);
    mockUpdatePlan.mockImplementation((_envId, _ctx, _planId, payload) => Promise.resolve({ ...plan, ...payload }));
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>
            <MemoryRouter initialEntries={['/apis/api-1/plans/plan-1']}>
                <Routes>
                    <Route path="/apis/api-1/plans">
                        <Route index element={<p>Plans list</p>} />
                        <Route
                            path=":planId"
                            element={<PlanFormWizard ctx={API_CTX} securityType={plan.security.type} planId="plan-1" />}
                        />
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

async function walkToLastStep(user: ReturnType<typeof userEvent.setup>) {
    // An API Product wizard has one step fewer than an API one — walk to the last step whatever it is.
    // Bounded so a step that blocks Next fails the test instead of hanging it.
    const maxClicks = 5;
    let next = screen.queryByRole('button', { name: 'Next' });
    for (let clicks = 0; next && clicks < maxClicks; clicks++) {
        await user.click(next);
        next = screen.queryByRole('button', { name: 'Next' });
    }
}

function stepLabels(): string[] {
    const stepIndicator = screen.getByRole('navigation', { name: 'Plan creation steps' });
    return Array.from(stepIndicator.children).map(step => within(step as HTMLElement).getByText(/^[A-Za-z]/).textContent ?? '');
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

    it('saves a federated OAuth2 plan without asking for an OAuth2 resource', async () => {
        const user = userEvent.setup();
        renderEditWizard(FEDERATED_OAUTH2_PLAN);

        await renamePlanAndSave(user, 'Renamed by operator');

        await waitFor(() => expect(mockUpdatePlan).toHaveBeenCalled());
        expect(lastUpdatePayload()).toMatchObject({
            definitionVersion: 'FEDERATED',
            name: 'Renamed by operator',
            security: { type: 'OAUTH2', configuration: {} },
        });
        expect(screen.queryByText('OAuth2 resource is required.')).not.toBeInTheDocument();
    });

    it.each(SECURED_PLAN_TYPES)('leaves the Security step out of a federated %s plan', async type => {
        renderEditWizard({ ...FEDERATED_PLAN, security: { type } });

        await screen.findByLabelText(/^Name/);

        expect(stepLabels()).toEqual(['General', 'Restrictions']);
    });

    it.each(SECURED_PLAN_TYPES)('shows the Security step for a V4 %s plan', async type => {
        renderEditWizard({ ...V4_PLAN, security: { type } });

        await screen.findByLabelText(/^Name/);

        expect(stepLabels()).toEqual(['General', 'Security', 'Restrictions']);
    });

    it('leaves the Security step out of a V4 keyless plan', async () => {
        renderEditWizard({ ...V4_PLAN, security: { type: 'KEY_LESS' } });

        await screen.findByLabelText(/^Name/);

        expect(stepLabels()).toEqual(['General', 'Restrictions']);
    });

    it('still requires an OAuth2 resource on a natively managed OAuth2 plan', async () => {
        const user = userEvent.setup();
        renderEditWizard(V4_OAUTH2_PLAN);

        await screen.findByLabelText(/^Name/);
        await user.click(screen.getByRole('button', { name: 'Next' }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        expect(await screen.findByText('OAuth2 resource is required.')).toBeInTheDocument();
        expect(mockUpdatePlan).not.toHaveBeenCalled();
    });

    it('clears the OAuth2 resource error once the OAuth2 resource is edited', async () => {
        const user = userEvent.setup();
        renderEditWizard(V4_OAUTH2_PLAN);

        await screen.findByLabelText(/^Name/);
        await user.click(screen.getByRole('button', { name: 'Next' }));
        await user.click(screen.getByRole('button', { name: 'Next' }));
        expect(await screen.findByText('OAuth2 resource is required.')).toBeInTheDocument();

        await user.type(screen.getByRole('combobox', { name: /^OAuth2 resource/ }), 'oauth2-am');

        expect(screen.queryByText('OAuth2 resource is required.')).not.toBeInTheDocument();
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

    it('keeps a federated plan at its current position when saving an edit', async () => {
        const user = userEvent.setup();
        renderEditWizard({ ...FEDERATED_PLAN, order: 3 });

        await renamePlanAndSave(user, 'Renamed by operator');

        await waitFor(() => expect(mockUpdatePlan).toHaveBeenCalled());
        expect(lastUpdatePayload()).toMatchObject({ definitionVersion: 'FEDERATED', order: 3 });
    });

    it('keeps a natively managed plan at its current position when saving an edit', async () => {
        const user = userEvent.setup();
        renderEditWizard({ ...V4_PLAN, order: 3 });

        await renamePlanAndSave(user, 'Renamed by operator');

        await waitFor(() => expect(mockUpdatePlan).toHaveBeenCalled());
        expect(lastUpdatePayload()).toMatchObject({ definitionVersion: 'V4', order: 3 });
    });

    it('keeps an API Product plan at its current position when saving an edit', async () => {
        const user = userEvent.setup();
        renderEditWizard({ ...FEDERATED_PLAN, definitionVersion: undefined, order: 3 }, PRODUCT_CTX);

        await renamePlanAndSave(user, 'Renamed by operator');

        await waitFor(() => expect(mockUpdatePlan).toHaveBeenCalled());
        expect(lastUpdatePayload()).toMatchObject({ order: 3 });
    });

    it('goes back to the plans list once the plan is saved', async () => {
        const user = userEvent.setup();
        renderEditWizardUnderPlansList(V4_PLAN);

        await renamePlanAndSave(user, 'Renamed by operator');

        expect(await screen.findByText('Plans list')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
    });

    it('shows the server error when a plan update is rejected', async () => {
        const user = userEvent.setup();
        renderEditWizard(V4_PLAN);
        mockUpdatePlan.mockRejectedValue(new Error('plan rejected'));

        await renamePlanAndSave(user, 'Renamed by operator');

        expect(await screen.findByText('plan rejected')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Save changes' })).toBeInTheDocument();
    });

    it('refuses to save an API Product OAuth2 plan without an OAuth2 resource', async () => {
        const user = userEvent.setup();
        renderEditWizard(V4_OAUTH2_PLAN, PRODUCT_CTX);

        await screen.findByLabelText(/^Name/);
        await user.click(screen.getByRole('button', { name: 'Next' }));
        await user.click(screen.getByRole('button', { name: 'Save changes' }));

        expect(await screen.findByText('OAuth2 resource is required.')).toBeInTheDocument();
        expect(mockUpdatePlan).not.toHaveBeenCalled();
        expect(screen.getByRole('button', { name: 'Save changes' })).toBeInTheDocument();
    });
});

describe('PlanFormWizard read-only', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('shows a federated plan without letting it be saved', async () => {
        const user = userEvent.setup();
        renderEditWizard(FEDERATED_PLAN, API_CTX, true);

        expect(await screen.findByRole('heading', { name: 'View plan' })).toBeInTheDocument();

        await walkToLastStep(user);

        expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
        expect(mockUpdatePlan).not.toHaveBeenCalled();
    });

    it('walks a natively managed OAuth2 plan without asking for an OAuth2 resource', async () => {
        const user = userEvent.setup();
        renderEditWizard(V4_OAUTH2_PLAN, API_CTX, true);

        expect(await screen.findByRole('heading', { name: 'View plan' })).toBeInTheDocument();

        await walkToLastStep(user);

        expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();
        expect(screen.queryByText('OAuth2 resource is required.')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
        expect(mockUpdatePlan).not.toHaveBeenCalled();
    });
});

describe('PlanFormWizard General step validation', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('stays on the General step when the name is empty', async () => {
        const user = userEvent.setup();
        renderCreateWizard();

        await screen.findByLabelText(/^Name/);
        await user.click(screen.getByRole('button', { name: 'Next' }));

        expect(await screen.findByText('Name is required.')).toBeInTheDocument();
        expect(screen.getByLabelText(/^Name/)).toBeInTheDocument();
        expect(screen.queryByText('API Key authentication configuration')).not.toBeInTheDocument();
        expect(mockCreatePlan).not.toHaveBeenCalled();
    });

    it('stays on the General step when the name is longer than 50 characters', async () => {
        const user = userEvent.setup();
        renderCreateWizard();

        fireEvent.change(await screen.findByLabelText(/^Name/), { target: { value: 'a'.repeat(51) } });
        await user.click(screen.getByRole('button', { name: 'Next' }));

        expect(await screen.findByText('Name must be at most 50 characters.')).toBeInTheDocument();
        expect(screen.getByLabelText(/^Name/)).toBeInTheDocument();
        expect(screen.queryByText('API Key authentication configuration')).not.toBeInTheDocument();
        expect(mockCreatePlan).not.toHaveBeenCalled();
    });

    it('advances to the Security step when the name is exactly 50 characters', async () => {
        const user = userEvent.setup();
        renderCreateWizard();

        await user.type(await screen.findByLabelText(/^Name/), 'a'.repeat(50));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        expect(await screen.findByText('API Key authentication configuration')).toBeInTheDocument();
        expect(screen.queryByLabelText(/^Name/)).not.toBeInTheDocument();
        expect(screen.queryByText('Name must be at most 50 characters.')).not.toBeInTheDocument();
    });
});

describe('PlanFormWizard step navigation', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('returns to the General step when Previous is clicked on the Security step', async () => {
        const user = userEvent.setup();
        renderCreateWizard();

        await user.type(await screen.findByLabelText(/^Name/), 'Gold');
        await user.click(screen.getByRole('button', { name: 'Next' }));
        expect(await screen.findByText('API Key authentication configuration')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Previous' }));

        expect(screen.getByLabelText(/^Name/)).toHaveValue('Gold');
        expect(screen.queryByText('API Key authentication configuration')).not.toBeInTheDocument();
    });
});

describe('PlanFormWizard loading', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('shows a loading placeholder instead of the form while the edited plan is still loading', async () => {
        mockGetPlan.mockReturnValueOnce(new Promise(() => {}));
        const { container } = renderEditWizard(V4_PLAN);

        await waitFor(() => expect(mockGetPlan).toHaveBeenCalled());

        expect(container.querySelectorAll('[data-slot="skeleton"]')).toHaveLength(2);
        expect(screen.queryByLabelText(/^Name/)).not.toBeInTheDocument();
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

describe('PlanFormWizard Restrictions step', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('locks rate limiting, quota and resource filtering when editing a plan', async () => {
        const user = userEvent.setup();
        renderEditWizard(V4_PLAN);

        await screen.findByLabelText(/^Name/);
        await walkToLastStep(user);

        expect(stepLabels()).toEqual(['General', 'Security', 'Restrictions']);
        const switches = screen.getAllByRole('switch');
        expect(switches).toHaveLength(3);
        const [rateLimitSwitch, quotaSwitch, resourceFilteringSwitch] = switches;
        expect(rateLimitSwitch).toBeDisabled();
        expect(quotaSwitch).toBeDisabled();
        expect(resourceFilteringSwitch).toBeDisabled();
    });

    it('lets rate limiting, quota and resource filtering be toggled when creating a plan', async () => {
        const user = userEvent.setup();
        renderCreateWizard();

        await user.type(await screen.findByLabelText(/^Name/), 'Gold');
        await walkToLastStep(user);

        expect(screen.getByRole('button', { name: /create plan/i })).toBeInTheDocument();
        const switches = screen.getAllByRole('switch');
        expect(switches).toHaveLength(3);
        const [rateLimitSwitch, quotaSwitch, resourceFilteringSwitch] = switches;
        expect(rateLimitSwitch).toBeEnabled();
        expect(quotaSwitch).toBeEnabled();
        expect(resourceFilteringSwitch).toBeEnabled();
    });
});
