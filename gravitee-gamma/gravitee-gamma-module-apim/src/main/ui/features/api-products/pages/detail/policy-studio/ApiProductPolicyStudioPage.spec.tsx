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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import type { Flow, PolicyStudioProps, SaveOutput } from '@gravitee/graphene-policy-studio';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiProductPolicyStudioPage } from './ApiProductPolicyStudioPage';
import { resetApimClientForTests } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { TEST_CONFIG, TEST_V2_BASE } from '../../../../../testing/factories';
import { settleOutstandingRequests, trackHandler } from '../../../../../testing/helpers';
import { server } from '../../../../../testing/server';
import { apiPlanKeys } from '../../../../apis/utils/queryKeys';
import { apiProductKeys } from '../../../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));

jest.mock('@gravitee/graphene-core', () => ({
    ...jest.requireActual<object>('@gravitee/graphene-core'),
    useLayoutConfig: jest.fn(),
}));

jest.mock('../../../../../shared/notify', () => ({
    ...jest.requireActual<object>('../../../../../shared/notify'),
    notify: { success: jest.fn(), error: jest.fn() },
}));

// The studio itself is graphene's: this suite checks what the page feeds it and what the page does with its output.
let mockStudioProps: PolicyStudioProps | null = null;
jest.mock('@gravitee/graphene-policy-studio', () => ({
    PolicyStudio: (props: PolicyStudioProps) => {
        mockStudioProps = props;
        return <div data-testid="policy-studio" />;
    },
}));

const PRODUCT_PATH = `${TEST_V2_BASE}/api-products/prod-1`;
const ORG_V2_BASE = `${TEST_CONFIG.managementBaseURL}/v2/organizations/${TEST_CONFIG.organizationId}`;

const PRODUCT_FLOW: Flow = {
    name: 'Rate limit all',
    enabled: true,
    selectors: [{ type: 'HTTP', path: '/', pathOperator: 'STARTS_WITH' }],
    request: [{ policy: 'rate-limit', enabled: true, configuration: {} }],
    response: [],
};
const GOLD_FLOW: Flow = {
    name: 'Gold header',
    enabled: true,
    selectors: [{ type: 'CONDITION', condition: "{#plan == 'gold'}" }],
    request: [{ policy: 'transform-headers', enabled: true, configuration: {} }],
};

const PLAN_CONTEXT = { type: 'api-product', entityId: 'prod-1' } as const;

const PLANS = [
    { id: 'gold', name: 'Gold', status: 'PUBLISHED', flows: [GOLD_FLOW] },
    { id: 'silver', name: 'Silver', status: 'DEPRECATED', flows: [] },
];

function givenStudioData({ permissions = { DEFINITION: 'RU', PLAN: 'RU' } }: { permissions?: Record<string, string> } = {}) {
    server.use(
        http.get(`${PRODUCT_PATH}/members/permissions`, () => HttpResponse.json(permissions)),
        http.get(`${PRODUCT_PATH}/flows`, () =>
            HttpResponse.json({ flowExecution: { mode: 'BEST_MATCH', matchRequired: true }, flows: [PRODUCT_FLOW] }),
        ),
        http.get(`${PRODUCT_PATH}/plans`, () => HttpResponse.json({ data: PLANS, pagination: { totalCount: 2 } })),
        http.get(`${ORG_V2_BASE}/plugins/policies`, () =>
            HttpResponse.json([{ id: 'rate-limit', name: 'Rate Limit', flowPhaseCompatibility: { HTTP_PROXY: ['REQUEST'] } }]),
        ),
        http.get(`${TEST_V2_BASE}/shared-policy-groups/policy-plugins`, () =>
            HttpResponse.json([{ id: 'spg-1', name: 'Auth', policyId: 'shared-policy-group-policy', phase: 'REQUEST', apiType: 'PROXY' }]),
        ),
    );
}

function renderPage(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
    render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/api-products/prod-1/policy-studio']}>
                <Routes>
                    <Route path="/api-products/:productId/policy-studio" element={<ApiProductPolicyStudioPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
    return queryClient;
}

async function renderLoadedStudio(queryClient?: QueryClient): Promise<PolicyStudioProps> {
    renderPage(queryClient);
    await screen.findByTestId('policy-studio');
    await waitFor(() => {
        expect(mockStudioProps?.loading).toBe(false);
        expect(mockStudioProps?.plans).toHaveLength(2);
    });
    return mockStudioProps!;
}

async function save(output: SaveOutput) {
    return mockStudioProps!.onSave(output);
}

describe('ApiProductPolicyStudioPage', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockStudioProps = null;
        (useEnvironment as jest.Mock).mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
    });

    afterEach(() => jest.clearAllMocks());

    describe('data', () => {
        it('feeds the studio the product flows, the flow mode and the published and deprecated plans with their flows', async () => {
            givenStudioData();
            const plansRequest = trackHandler('get', `${PRODUCT_PATH}/plans`, { data: PLANS, pagination: { totalCount: 2 } });

            const props = await renderLoadedStudio();

            expect(props.commonFlows).toEqual([PRODUCT_FLOW]);
            expect(props.flowExecution).toEqual({ mode: 'BEST_MATCH', matchRequired: true });
            expect(props.plans).toEqual([
                { id: 'gold', name: 'Gold', flows: [GOLD_FLOW] },
                { id: 'silver', name: 'Silver', flows: [] },
            ]);
            // CLOSED plans are never loaded, so their flows cannot be edited here.
            const statuses = new URL(plansRequest.lastCall!.url).searchParams.get('statuses');
            expect(statuses?.split(',').sort()).toEqual(['DEPRECATED', 'PUBLISHED']);
        });

        // Graphene only lets policies be added to a phase that has a connector on at least one side.
        it('opens an HTTP proxy studio, request and response phases only, with an HTTP proxy on each side', async () => {
            givenStudioData();

            const props = await renderLoadedStudio();

            expect(props.apiType).toBe('PROXY');
            expect(props.entrypointsInfo).toEqual([expect.objectContaining({ type: 'http-proxy', supportedModes: ['REQUEST_RESPONSE'] })]);
            expect(props.endpointsInfo).toEqual([expect.objectContaining({ type: 'http-proxy', supportedModes: ['REQUEST_RESPONSE'] })]);
            expect(props.policies.map(p => p.id)).toEqual(['rate-limit']);
            expect(props.sharedPolicyGroups.map(s => s.id)).toEqual(['spg-1']);
        });

        it('starts a product that never saved a flow mode on DEFAULT with no flow', async () => {
            givenStudioData();
            server.use(http.get(`${PRODUCT_PATH}/flows`, () => HttpResponse.json({ flows: [] })));

            const props = await renderLoadedStudio();

            expect(props.commonFlows).toEqual([]);
            expect(props.flowExecution).toEqual({ mode: 'DEFAULT', matchRequired: false });
        });

        it('reloads the plans when a plan changes on the Plans page', async () => {
            givenStudioData();
            const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
            await renderLoadedStudio(queryClient);
            server.use(http.get(`${PRODUCT_PATH}/plans`, () => HttpResponse.json({ data: [PLANS[0]], pagination: { totalCount: 1 } })));

            // What the Plans page's plan mutations invalidate.
            await queryClient.invalidateQueries({ queryKey: [apiPlanKeys.all[0], PLAN_CONTEXT.type, PLAN_CONTEXT.entityId] });

            await waitFor(() => expect(mockStudioProps?.plans).toEqual([{ id: 'gold', name: 'Gold', flows: [GOLD_FLOW] }]));
        });

        it.each([
            ['the product flows', `${PRODUCT_PATH}/flows`],
            ['the plans', `${PRODUCT_PATH}/plans`],
            ['the policies', `${ORG_V2_BASE}/plugins/policies`],
            ['the shared policy groups', `${TEST_V2_BASE}/shared-policy-groups/policy-plugins`],
        ])('shows an error instead of the studio when %s cannot be loaded', async (_, url) => {
            givenStudioData();
            server.use(http.get(url, () => HttpResponse.json({ message: 'boom' }, { status: 500 })));

            renderPage();

            expect(await screen.findByText(/failed to load policy studio data/i)).toBeInTheDocument();
            expect(screen.queryByTestId('policy-studio')).not.toBeInTheDocument();
        });
    });

    describe('permissions', () => {
        it('shows no studio to a user who cannot read the product definition', async () => {
            givenStudioData({ permissions: { PLAN: 'R' } });

            renderPage();

            expect(await screen.findByText(/you do not have permission to view the policy studio/i)).toBeInTheDocument();
            expect(screen.queryByTestId('policy-studio')).not.toBeInTheDocument();
        });

        it('waits for the permissions before showing the studio or the no-permission message', async () => {
            givenStudioData();
            let releasePermissions: () => void = () => undefined;
            const permissionsReleased = new Promise<void>(resolve => (releasePermissions = resolve));
            server.use(
                http.get(`${PRODUCT_PATH}/members/permissions`, async () => {
                    await permissionsReleased;
                    return HttpResponse.json({ DEFINITION: 'RU', PLAN: 'RU' });
                }),
            );

            renderPage();
            await settleOutstandingRequests();

            expect(screen.queryByText(/you do not have permission/i)).not.toBeInTheDocument();
            expect(screen.queryByTestId('policy-studio')).not.toBeInTheDocument();

            releasePermissions();
            expect(await screen.findByTestId('policy-studio')).toBeInTheDocument();
        });

        it('opens the studio without plans, and never asks for them, for a user who cannot read the plans', async () => {
            givenStudioData({ permissions: { DEFINITION: 'RU' } });
            const plansRequest = trackHandler('get', `${PRODUCT_PATH}/plans`, { data: PLANS, pagination: { totalCount: 2 } });

            renderPage();

            await screen.findByTestId('policy-studio');
            await waitFor(() => expect(mockStudioProps?.loading).toBe(false));
            expect(mockStudioProps?.commonFlows).toEqual([PRODUCT_FLOW]);
            expect(mockStudioProps?.plans).toEqual([]);
            await settleOutstandingRequests();
            expect(plansRequest.callCount).toBe(0);
        });

        it('is read-only for a user who can update neither the definition nor the plans', async () => {
            givenStudioData({ permissions: { DEFINITION: 'R', PLAN: 'R' } });

            const props = await renderLoadedStudio();

            expect(props.readOnly).toBe(true);
        });

        it.each([
            ['the definition', { DEFINITION: 'RU', PLAN: 'R' }],
            ['the plans', { DEFINITION: 'R', PLAN: 'RU' }],
        ])('is editable for a user who can update %s', async (_, permissions) => {
            givenStudioData({ permissions });

            const props = await renderLoadedStudio();

            expect(props.readOnly).toBe(false);
        });

        it('tells a user who can update only the plans, from the start, that product flows and the mode are not theirs to edit', async () => {
            givenStudioData({ permissions: { DEFINITION: 'R', PLAN: 'RU' } });

            await renderLoadedStudio();

            expect(screen.getByRole('status')).toHaveTextContent(
                'You can edit plan flows only. Product flows and flow mode need the API Product definition update permission.',
            );
        });

        it('tells a user who can update only the definition, from the start, that plan flows are not theirs to edit', async () => {
            givenStudioData({ permissions: { DEFINITION: 'RU', PLAN: 'R' } });

            await renderLoadedStudio();

            expect(screen.getByRole('status')).toHaveTextContent(
                'You can edit product flows and flow mode only. Plan flows need the API Product plan update permission.',
            );
        });

        it.each([
            ['can update both', { DEFINITION: 'RU', PLAN: 'RU' }],
            ['can update neither', { DEFINITION: 'R', PLAN: 'R' }],
        ])('shows no permission message to a user who %s', async (_, permissions) => {
            givenStudioData({ permissions });

            await renderLoadedStudio();

            expect(screen.queryByRole('status')).not.toBeInTheDocument();
        });

        it('refuses to save plan flows without the plan update permission, and sends nothing', async () => {
            givenStudioData({ permissions: { DEFINITION: 'RU', PLAN: 'R' } });
            const flowsPut = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
            const planFlowsPut = trackHandler('put', `${PRODUCT_PATH}/plans/_flows`, undefined, 204);
            await renderLoadedStudio();

            await expect(save({ commonFlows: [PRODUCT_FLOW], plansToUpdate: [{ id: 'gold', name: 'Gold', flows: [] }] })).rejects.toThrow(
                /permission to edit plan flows/i,
            );

            await settleOutstandingRequests();
            expect(flowsPut.callCount).toBe(0);
            expect(planFlowsPut.callCount).toBe(0);
        });

        it('refuses to save product flows or the flow mode without the definition update permission, and sends nothing', async () => {
            givenStudioData({ permissions: { DEFINITION: 'R', PLAN: 'RU' } });
            const flowsPut = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
            const planFlowsPut = trackHandler('put', `${PRODUCT_PATH}/plans/_flows`, undefined, 204);
            await renderLoadedStudio();

            await expect(
                save({ flowExecution: { mode: 'DEFAULT' }, plansToUpdate: [{ id: 'gold', name: 'Gold', flows: [] }] }),
            ).rejects.toThrow(/permission to edit the api product flows/i);

            await settleOutstandingRequests();
            expect(flowsPut.callCount).toBe(0);
            expect(planFlowsPut.callCount).toBe(0);
        });
    });

    describe('save', () => {
        it('saves the product flows with the mode, then only the changed plans, and never deploys', async () => {
            givenStudioData();
            const flowsPut = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
            const planFlowsPut = trackHandler('put', `${PRODUCT_PATH}/plans/_flows`, undefined, 204);
            const deploy = trackHandler('post', `${PRODUCT_PATH}/deployments`, {});
            await renderLoadedStudio();
            const newGoldFlows: Flow[] = [{ ...GOLD_FLOW, enabled: false }];

            await save({
                commonFlows: [],
                flowExecution: { mode: 'DEFAULT', matchRequired: false },
                plansToUpdate: [{ id: 'gold', name: 'Gold', flows: newGoldFlows }],
            });

            expect(flowsPut.calls.map(c => c.body)).toEqual([{ flowExecution: { mode: 'DEFAULT', matchRequired: false }, flows: [] }]);
            expect(planFlowsPut.calls.map(c => c.body)).toEqual([[{ planId: 'gold', flows: newGoldFlows }]]);
            expect(deploy.callCount).toBe(0);
        });

        it('keeps the saved flow mode when only the product flows changed', async () => {
            givenStudioData();
            const flowsPut = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
            const planFlowsPut = trackHandler('put', `${PRODUCT_PATH}/plans/_flows`, undefined, 204);
            await renderLoadedStudio();

            await save({ commonFlows: [] });

            expect(flowsPut.lastCall?.body).toEqual({ flowExecution: { mode: 'BEST_MATCH', matchRequired: true }, flows: [] });
            expect(planFlowsPut.callCount).toBe(0);
        });

        it('keeps the saved product flows when only the flow mode changed', async () => {
            givenStudioData();
            const flowsPut = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
            await renderLoadedStudio();

            await save({ flowExecution: { mode: 'DEFAULT', matchRequired: false } });

            expect(flowsPut.lastCall?.body).toEqual({ flowExecution: { mode: 'DEFAULT', matchRequired: false }, flows: [PRODUCT_FLOW] });
        });

        it('saves only the plan flows when the product flows and the mode did not change', async () => {
            givenStudioData();
            const flowsPut = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
            const planFlowsPut = trackHandler('put', `${PRODUCT_PATH}/plans/_flows`, undefined, 204);
            await renderLoadedStudio();

            await save({ plansToUpdate: [{ id: 'silver', name: 'Silver', flows: [PRODUCT_FLOW] }] });

            expect(flowsPut.callCount).toBe(0);
            expect(planFlowsPut.lastCall?.body).toEqual([{ planId: 'silver', flows: [PRODUCT_FLOW] }]);
        });

        it('refreshes the product after saving, so the deploy banner shows', async () => {
            givenStudioData();
            trackHandler('put', `${PRODUCT_PATH}/flows`, {});
            const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
            const detailKey = apiProductKeys.detail('DEFAULT', 'prod-1');
            queryClient.setQueryData(detailKey, { id: 'prod-1', name: 'Payments', version: '1', deploymentState: 'DEPLOYED' });
            await renderLoadedStudio(queryClient);

            await save({ commonFlows: [] });

            expect(queryClient.getQueryState(detailKey)?.isInvalidated).toBe(true);
        });

        it('refreshes the plans the Plans page has loaded after saving plan flows', async () => {
            givenStudioData();
            trackHandler('put', `${PRODUCT_PATH}/plans/_flows`, undefined, 204);
            const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
            const planDetailKey = apiPlanKeys.detail('DEFAULT', PLAN_CONTEXT, 'gold');
            queryClient.setQueryData(planDetailKey, { ...PLANS[0], flows: [GOLD_FLOW] });
            await renderLoadedStudio(queryClient);

            await save({ plansToUpdate: [{ id: 'gold', name: 'Gold', flows: [] }] });

            expect(queryClient.getQueryState(planDetailKey)?.isInvalidated).toBe(true);
        });

        it('reports a failed plan save after saved product flows, and reloads what is saved', async () => {
            givenStudioData();
            const flowsPut = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
            server.use(
                http.put(`${PRODUCT_PATH}/plans/_flows`, () =>
                    HttpResponse.json({ message: "Plan 'gold' is closed; the flows of a closed plan cannot change" }, { status: 400 }),
                ),
            );
            const flowsReload = trackHandler('get', `${PRODUCT_PATH}/flows`, { flowExecution: { mode: 'DEFAULT' }, flows: [] });
            const plansReload = trackHandler('get', `${PRODUCT_PATH}/plans`, { data: PLANS, pagination: { totalCount: 2 } });
            await renderLoadedStudio();
            const flowsLoadsBefore = flowsReload.callCount;
            const plansLoadsBefore = plansReload.callCount;

            await expect(save({ commonFlows: [], plansToUpdate: [{ id: 'gold', name: 'Gold', flows: [] }] })).rejects.toThrow(
                /api product flows were saved, but the plan flows were not: plan 'gold' is closed/i,
            );

            expect(flowsPut.callCount).toBe(1);
            expect(notify.error).toHaveBeenCalledWith(expect.objectContaining({ message: expect.stringMatching(/plan flows were not/i) }));
            await waitFor(() => {
                expect(flowsReload.callCount).toBeGreaterThan(flowsLoadsBefore);
                expect(plansReload.callCount).toBeGreaterThan(plansLoadsBefore);
            });
        });

        it('reports a failed plan save as the save error when no product flow changed', async () => {
            givenStudioData();
            const flowsPut = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
            server.use(
                http.put(`${PRODUCT_PATH}/plans/_flows`, () =>
                    HttpResponse.json({ message: "Plan 'gold' is closed; the flows of a closed plan cannot change" }, { status: 400 }),
                ),
            );
            await renderLoadedStudio();

            await expect(save({ plansToUpdate: [{ id: 'gold', name: 'Gold', flows: [] }] })).rejects.toThrow(/plan 'gold' is closed/i);

            expect(flowsPut.callCount).toBe(0);
            expect(notify.error).not.toHaveBeenCalled();
        });

        it('sends no plan flows when the product flows fail to save', async () => {
            givenStudioData();
            server.use(http.put(`${PRODUCT_PATH}/flows`, () => HttpResponse.json({ message: 'Invalid flow' }, { status: 400 })));
            const planFlowsPut = trackHandler('put', `${PRODUCT_PATH}/plans/_flows`, undefined, 204);
            await renderLoadedStudio();

            await expect(save({ commonFlows: [], plansToUpdate: [{ id: 'gold', name: 'Gold', flows: [] }] })).rejects.toThrow(
                /invalid flow/i,
            );

            await settleOutstandingRequests();
            expect(planFlowsPut.callCount).toBe(0);
        });
    });
});
