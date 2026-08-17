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
import type { ComponentProps, ReactNode } from 'react';

import { SharedPolicyGroupPolicyStudio } from './SharedPolicyGroupPolicyStudio';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

// Only the panes are stubbed — step/policy resolution comes from the real package so the test exercises
// the same matching rules the studio uses in production.
jest.mock('@gravitee/graphene-policy-studio', () => ({
    ...jest.requireActual('@gravitee/graphene-policy-studio'),
    PolicyStudioProvider: ({ children }: { children: ReactNode }) => children,
    PolicyCatalogPane: ({
        policies,
        onAddPolicy,
    }: {
        policies: Array<{ id: string; name: string }>;
        onAddPolicy: (policy: { id: string; name: string }) => void;
    }) => (
        <div>
            {policies.map(policy => (
                <button key={policy.id} type="button" onClick={() => onAddPolicy(policy)}>
                    Add {policy.name}
                </button>
            ))}
        </div>
    ),
    PolicyConfigPanel: ({ step, onStepChange }: { step: { name?: string }; onStepChange?: (patch: { description: string }) => void }) => (
        <div>
            <span>Configure {step.name}</span>
            <button type="button" onClick={() => onStepChange?.({ description: 'Updated description' })}>
                Update configuration
            </button>
        </div>
    ),
}));

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
    { id: 'jwt', name: 'JWT' },
    { id: 'rate-limit', name: 'Rate Limit' },
    { id: 'transform-headers', name: 'Transform Headers' },
];

const FETCH_SCHEMA = jest.fn();
const FETCH_DOCUMENTATION = jest.fn();

function renderStudio(overrides: Partial<ComponentProps<typeof SharedPolicyGroupPolicyStudio>> = {}) {
    const onSave = jest.fn().mockResolvedValue(undefined);
    render(
        <SharedPolicyGroupPolicyStudio
            sharedPolicyGroup={SHARED_POLICY_GROUP}
            policies={POLICIES}
            readOnly={false}
            onSave={onSave}
            onFetchPolicySchema={FETCH_SCHEMA}
            onFetchPolicyDocumentation={FETCH_DOCUMENTATION}
            {...overrides}
        />,
    );
    return { onSave };
}

describe('SharedPolicyGroupPolicyStudio', () => {
    it('adds and saves a policy using the catalog', async () => {
        const { onSave } = renderStudio({ sharedPolicyGroup: { ...SHARED_POLICY_GROUP, steps: [] } });

        fireEvent.click(screen.getByRole('button', { name: 'Add policy' }));
        fireEvent.click(screen.getByRole('button', { name: 'Add Transform Headers' }));
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

    it('reorders, removes, and configures policy steps', async () => {
        const { onSave } = renderStudio();

        fireEvent.click(screen.getByRole('button', { name: 'Move JWT down' }));
        fireEvent.click(screen.getByRole('button', { name: '2 JWT' }));
        fireEvent.click(screen.getByRole('button', { name: 'Update configuration' }));
        fireEvent.click(screen.getByRole('button', { name: 'Remove Rate Limit' }));
        fireEvent.click(screen.getByRole('button', { name: 'Save policies' }));

        await waitFor(() =>
            expect(onSave).toHaveBeenCalledWith([
                expect.objectContaining({ policy: 'jwt', name: 'JWT', description: 'Updated description' }),
            ]),
        );
    });

    it('is read-only without update permission or for a Kubernetes-origin group', () => {
        renderStudio({ readOnly: true });

        expect(screen.queryByRole('button', { name: 'Add policy' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Save policies' })).toBeNull();
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
            const { onSave } = renderStudio({ sharedPolicyGroup: WITH_MISSING_POLICY });

            fireEvent.click(screen.getByRole('button', { name: 'Remove Legacy policy' }));
            fireEvent.click(screen.getByRole('button', { name: 'Save policies' }));

            await waitFor(() => expect(onSave).toHaveBeenCalledWith([]));
        });
    });
});
