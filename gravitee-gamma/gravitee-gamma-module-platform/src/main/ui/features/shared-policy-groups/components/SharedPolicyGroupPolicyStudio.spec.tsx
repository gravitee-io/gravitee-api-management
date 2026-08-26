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
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ComponentProps, ReactNode } from 'react';

import { SharedPolicyGroupPolicyStudio } from './SharedPolicyGroupPolicyStudio';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

// The step list reorders via @dnd-kit drag-and-drop (see SharedPolicyGroupPolicySteps). Simulating a real
// pointer drag in JSDOM is unreliable, so DndContext is stubbed to capture its onDragEnd handler, which
// tests invoke directly with a synthetic { active, over } pair — the same shape a real drag produces.
let capturedOnDragEnd: ((event: { active: { id: string }; over: { id: string } | null }) => void) | undefined;
jest.mock('@dnd-kit/core', () => {
    const actual = jest.requireActual('@dnd-kit/core');
    return {
        ...actual,
        DndContext: ({
            children,
            onDragEnd,
        }: {
            children: ReactNode;
            onDragEnd: (event: { active: { id: string }; over: { id: string } | null }) => void;
        }) => {
            capturedOnDragEnd = onDragEnd;
            return children;
        },
    };
});

// Only the panes are stubbed — step/policy resolution comes from the real package so the test exercises
// the same matching rules the studio uses in production.
jest.mock('@gravitee/graphene-policy-studio', () => {
    return {
        ...jest.requireActual('@gravitee/graphene-policy-studio'),
        PolicyStudioProvider: ({ children }: { children: ReactNode }) => children,
        PolicyCatalogPane: ({
            policies,
            initialCategories,
            onAddPolicy,
        }: {
            policies: Array<{ id: string; name: string }>;
            initialCategories?: string[];
            onAddPolicy: (policy: { id: string; name: string }) => void;
        }) => (
            <div>
                <span>Catalog categories: {initialCategories?.join(', ') || 'all'}</span>
                {policies.map(policy => (
                    <button key={policy.id} type="button" onClick={() => onAddPolicy(policy)}>
                        Add {policy.name}
                    </button>
                ))}
            </div>
        ),
        PolicyQuickInsert: ({
            onBrowseCatalog,
            onAddPolicy,
            policies,
            children,
        }: {
            onBrowseCatalog: () => void;
            onAddPolicy: (policy: { id: string; name: string }) => void;
            policies: Array<{ id: string; name: string }>;
            children: ReactNode;
        }) => (
            <div>
                {children}
                <button type="button" onClick={onBrowseCatalog}>
                    Browse all policies
                </button>
                <button type="button" onClick={() => onAddPolicy(policies[0])}>
                    Quick add {policies[0]?.name}
                </button>
            </div>
        ),
        PolicyConfigPanel: ({
            step,
            docsLayout,
            showBackButton,
            onClose,
            onStepChange,
            onToggleEnabled,
        }: {
            step: { name?: string };
            docsLayout?: string;
            showBackButton?: boolean;
            onClose: () => void;
            onStepChange?: (patch: { description: string }) => void;
            onToggleEnabled?: (enabled: boolean) => void;
        }) => (
            <div>
                <span>Configure {step.name}</span>
                <span>Documentation layout: {docsLayout}</span>
                <span>{showBackButton ? 'Configuration back button' : 'No configuration back button'}</span>
                <button type="button" onClick={onClose}>
                    Back to flow
                </button>
                <button type="button" onClick={() => onStepChange?.({ description: 'Updated description' })}>
                    Update configuration
                </button>
                <button type="button" onClick={() => onToggleEnabled?.(false)}>
                    Disable policy
                </button>
            </div>
        ),
    };
});

const SHARED_POLICY_GROUP: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    apiType: 'PROXY',
    phase: 'REQUEST',
    steps: [
        { policy: 'jwt', name: 'JWT', enabled: true, configuration: {} },
        { policy: 'rate-limit', name: 'Rate Limit', enabled: true, configuration: {} },
    ],
};

const POLICIES = [
    {
        id: 'jwt',
        name: 'JWT',
        category: 'security',
        flowPhaseCompatibility: { HTTP_PROXY: ['REQUEST'] as const },
    },
    {
        id: 'rate-limit',
        name: 'Rate Limit',
        category: 'performance',
        flowPhaseCompatibility: { HTTP_PROXY: ['REQUEST', 'RESPONSE'] as const },
    },
    {
        id: 'transform-headers',
        name: 'Transform Headers',
        category: 'transformation',
        flowPhaseCompatibility: { HTTP_PROXY: ['REQUEST', 'RESPONSE'] as const },
    },
];

const FETCH_SCHEMA = jest.fn();
const FETCH_DOCUMENTATION = jest.fn();

function renderStudio(overrides: Partial<ComponentProps<typeof SharedPolicyGroupPolicyStudio>> = {}) {
    const onSave = jest.fn().mockResolvedValue(SHARED_POLICY_GROUP);
    const onDeploy = jest.fn().mockResolvedValue(undefined);
    const props: ComponentProps<typeof SharedPolicyGroupPolicyStudio> = {
        sharedPolicyGroup: SHARED_POLICY_GROUP,
        policies: POLICIES,
        readOnly: false,
        onSave,
        onDeploy,
        onFetchPolicySchema: FETCH_SCHEMA,
        onFetchPolicyDocumentation: FETCH_DOCUMENTATION,
        ...overrides,
    };
    const view = render(<SharedPolicyGroupPolicyStudio {...props} />);
    return {
        onSave,
        onDeploy,
        rerenderStudio: (nextOverrides: Partial<ComponentProps<typeof SharedPolicyGroupPolicyStudio>>) =>
            view.rerender(<SharedPolicyGroupPolicyStudio {...props} {...nextOverrides} onSave={onSave} onDeploy={onDeploy} />),
    };
}

describe('SharedPolicyGroupPolicyStudio', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    it('reminds the user to update the prerequisite message before saving policies', async () => {
        const user = userEvent.setup();
        renderStudio();

        await user.hover(screen.getByRole('button', { name: 'Save policies' }));

        expect(
            await screen.findByRole('tooltip', {
                name: "Don't forget to ensure that the Prerequisite message is updated with the latest changes.",
            }),
        ).not.toBeNull();
    });

    it('uses the API Policy Studio phase language, actors, coach line, and category shortcuts', () => {
        renderStudio({ sharedPolicyGroup: { ...SHARED_POLICY_GROUP, steps: [] } });

        expect(screen.getByRole('heading', { name: 'Request Phase' })).not.toBeNull();
        expect(screen.getByText('Policies applied to the incoming client request before it reaches the backend.')).not.toBeNull();
        expect(screen.getByText('Client')).not.toBeNull();
        expect(screen.getByText('Backend')).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Add policy' })).not.toBeNull();
        expect(screen.getByText('Requests reach your backend unprotected')).not.toBeNull();
        expect(screen.getByRole('button', { name: '+ Security' })).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Browse all...' })).not.toBeNull();
    });

    it('adds and saves a policy using the catalog', async () => {
        const { onSave } = renderStudio({ sharedPolicyGroup: { ...SHARED_POLICY_GROUP, steps: [] } });

        fireEvent.click(screen.getByRole('button', { name: 'Add policy' }));
        fireEvent.click(screen.getByRole('button', { name: 'Browse all policies' }));
        fireEvent.click(screen.getByRole('button', { name: 'Add Transform Headers' }));
        expect(screen.getByText('Documentation layout: side-by-side')).not.toBeNull();
        expect(screen.getByText('Configuration back button')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'Save policies' }));

        await waitFor(() =>
            expect(onSave).toHaveBeenCalledWith([
                {
                    policy: 'transform-headers',
                    name: 'Transform Headers',
                    enabled: true,
                    configuration: {},
                },
            ]),
        );
    });

    it('opens the catalog filtered by a phase category shortcut', () => {
        renderStudio({ sharedPolicyGroup: { ...SHARED_POLICY_GROUP, steps: [] } });

        fireEvent.click(screen.getByRole('button', { name: '+ Security' }));

        expect(screen.getByText('Catalog categories: security')).not.toBeNull();
    });

    it('offers the API Policy Studio transformation shortcut for the empty response phase', () => {
        renderStudio({
            sharedPolicyGroup: { ...SHARED_POLICY_GROUP, phase: 'RESPONSE', steps: [] },
        });

        expect(screen.getByRole('heading', { name: 'Response Phase' })).not.toBeNull();
        expect(screen.getByText('Responses pass through untransformed')).not.toBeNull();
        expect(screen.getByRole('button', { name: '+ Transformation' })).not.toBeNull();
    });

    it('suggests only the gap the phase still has once policies are added', () => {
        renderStudio({
            sharedPolicyGroup: {
                ...SHARED_POLICY_GROUP,
                phase: 'RESPONSE',
                steps: [{ policy: 'transform-headers', name: 'Transform Headers', enabled: true, configuration: {} }],
            },
        });

        expect(screen.queryByRole('button', { name: '+ Transformation' })).toBeNull();
        expect(screen.getByRole('button', { name: '+ Performance' })).not.toBeNull();
    });

    it('reorders, removes, and configures policy steps', async () => {
        const user = userEvent.setup();
        const { onSave } = renderStudio();

        act(() => capturedOnDragEnd?.({ active: { id: 'step-0' }, over: { id: 'step-1' } }));
        fireEvent.click(screen.getByRole('button', { name: '2 JWT' }));
        fireEvent.click(screen.getByRole('button', { name: 'Update configuration' }));
        fireEvent.click(screen.getByRole('button', { name: 'Back to flow' }));
        await user.click(screen.getByRole('button', { name: 'Rate Limit actions' }));
        await user.click(await screen.findByRole('menuitem', { name: 'Remove' }));
        fireEvent.click(screen.getByRole('button', { name: 'Save policies' }));

        await waitFor(() =>
            expect(onSave).toHaveBeenCalledWith([
                expect.objectContaining({ policy: 'jwt', name: 'JWT', description: 'Updated description' }),
            ]),
        );
    });

    it('duplicates and disables a policy step', async () => {
        const user = userEvent.setup();
        const { onSave } = renderStudio();

        await user.click(screen.getByRole('button', { name: 'JWT actions' }));
        await user.click(await screen.findByRole('menuitem', { name: 'Duplicate' }));
        expect(screen.getByText('Configure JWT copy')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'Disable policy' }));
        fireEvent.click(screen.getByRole('button', { name: 'Save policies' }));

        await waitFor(() =>
            expect(onSave).toHaveBeenCalledWith([
                expect.objectContaining({ policy: 'jwt', name: 'JWT', enabled: true }),
                expect.objectContaining({ policy: 'jwt', name: 'JWT copy', enabled: false }),
                expect.objectContaining({ policy: 'rate-limit' }),
            ]),
        );
    });

    it('marks a disabled step on the canvas and re-enables it from the step menu', async () => {
        const user = userEvent.setup();
        const { onSave } = renderStudio({
            sharedPolicyGroup: {
                ...SHARED_POLICY_GROUP,
                steps: [{ policy: 'jwt', name: 'JWT', enabled: false, condition: '{#request.headers.debug != null}', configuration: {} }],
            },
        });

        expect(screen.getByText('Disabled')).not.toBeNull();
        expect(screen.getByText(/Condition: \{#request\.headers\.debug != null\}/)).not.toBeNull();

        await user.click(screen.getByRole('button', { name: 'JWT actions' }));
        await user.click(await screen.findByRole('menuitem', { name: 'Enable' }));
        fireEvent.click(screen.getByRole('button', { name: 'Save policies' }));

        await waitFor(() => expect(onSave).toHaveBeenCalledWith([expect.objectContaining({ policy: 'jwt', enabled: true })]));
    });

    it('adopts refreshed server steps while the editor is clean', () => {
        const { rerenderStudio } = renderStudio({
            sharedPolicyGroup: { ...SHARED_POLICY_GROUP, updatedAt: '2026-08-25T08:00:00.000Z' },
        });

        rerenderStudio({
            sharedPolicyGroup: {
                ...SHARED_POLICY_GROUP,
                updatedAt: '2026-08-25T08:01:00.000Z',
                steps: [{ policy: 'transform-headers', name: 'Transform Headers', enabled: true, configuration: {} }],
            },
        });

        expect(screen.getByRole('button', { name: '1 Transform Headers' })).not.toBeNull();
        expect(screen.queryByRole('button', { name: '1 JWT' })).toBeNull();
    });

    it('preserves unsaved steps when a lifecycle refetch updates the server entity', async () => {
        const { rerenderStudio } = renderStudio({
            sharedPolicyGroup: { ...SHARED_POLICY_GROUP, updatedAt: '2026-08-25T08:00:00.000Z' },
        });
        capturedOnDragEnd?.({ active: { id: 'step-0' }, over: { id: 'step-1' } });

        rerenderStudio({
            sharedPolicyGroup: {
                ...SHARED_POLICY_GROUP,
                lifecycleState: 'UNDEPLOYED',
                updatedAt: '2026-08-25T08:01:00.000Z',
                steps: [{ policy: 'transform-headers', name: 'Transform Headers', enabled: true, configuration: {} }],
            },
        });

        expect(screen.getByRole('button', { name: '1 Rate Limit' })).not.toBeNull();
        expect(screen.getByRole('button', { name: '2 JWT' })).not.toBeNull();
        expect(screen.queryByRole('button', { name: '1 Transform Headers' })).toBeNull();
    });

    it('is read-only without update permission or for a Kubernetes-origin group', () => {
        renderStudio({ readOnly: true, onSave: undefined, onDeploy: undefined });

        expect(screen.queryByRole('button', { name: 'Add policy' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Save policies' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Deploy' })).toBeNull();
    });

    it('requires pending edits to be saved before deployment', async () => {
        const { onSave, onDeploy } = renderStudio({
            sharedPolicyGroup: { ...SHARED_POLICY_GROUP, lifecycleState: 'PENDING' },
        });

        expect(screen.getByRole('button', { name: 'Deploy' })).toHaveProperty('disabled', false);
        act(() => capturedOnDragEnd?.({ active: { id: 'step-0' }, over: { id: 'step-1' } }));
        expect(screen.getByRole('button', { name: 'Deploy' })).toHaveProperty('disabled', true);

        fireEvent.click(screen.getByRole('button', { name: 'Save policies' }));
        await waitFor(() => expect(onSave).toHaveBeenCalled());

        fireEvent.click(screen.getByRole('button', { name: 'Deploy' }));
        await waitFor(() => expect(onDeploy).toHaveBeenCalled());
    });

    it('enables deployment after saving a deployed group when the update returns pending', async () => {
        const onSave = jest.fn().mockResolvedValue({ ...SHARED_POLICY_GROUP, lifecycleState: 'PENDING' });
        renderStudio({
            sharedPolicyGroup: { ...SHARED_POLICY_GROUP, lifecycleState: 'DEPLOYED' },
            onSave,
        });

        act(() => capturedOnDragEnd?.({ active: { id: 'step-0' }, over: { id: 'step-1' } }));
        expect(screen.getByRole('button', { name: 'Deploy' })).toHaveProperty('disabled', true);

        fireEvent.click(screen.getByRole('button', { name: 'Save policies' }));

        await waitFor(() => expect(onSave).toHaveBeenCalled());
        expect(screen.getByRole('button', { name: 'Deploy' })).toHaveProperty('disabled', false);
    });

    it('disables deploy for an already deployed group', () => {
        renderStudio({ sharedPolicyGroup: { ...SHARED_POLICY_GROUP, lifecycleState: 'DEPLOYED' } });

        expect(screen.getByRole('button', { name: 'Deploy' })).toHaveProperty('disabled', true);
    });

    describe('a step whose policy is not installed in this environment', () => {
        const WITH_MISSING_POLICY: SharedPolicyGroup = {
            ...SHARED_POLICY_GROUP,
            steps: [{ policy: 'not-installed', name: 'Legacy policy', enabled: true, configuration: {} }],
        };

        it('cannot be configured, because there is no schema to render', () => {
            renderStudio({ sharedPolicyGroup: WITH_MISSING_POLICY });

            expect(screen.getByRole('button', { name: /^1 Legacy policy/ })).toHaveProperty('disabled', true);
            expect(screen.queryByText('Configure Legacy policy')).toBeNull();
        });

        it('can still be removed so the group is not stuck', async () => {
            const user = userEvent.setup();
            const { onSave } = renderStudio({ sharedPolicyGroup: WITH_MISSING_POLICY });

            await user.click(screen.getByRole('button', { name: 'Legacy policy actions' }));
            await user.click(await screen.findByRole('menuitem', { name: 'Remove' }));
            fireEvent.click(screen.getByRole('button', { name: 'Save policies' }));

            await waitFor(() => expect(onSave).toHaveBeenCalledWith([]));
        });
    });
});
