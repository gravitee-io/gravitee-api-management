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
import { fireEvent, render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

jest.mock('@gravitee/graphene-core', () => ({
    Alert: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    AlertDescription: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    Button: ({ children, ...props }: { children?: ReactNode }) => <button {...props}>{children}</button>,
    PageFocused: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    Skeleton: () => <div />,
    cn: (...parts: unknown[]) => parts.filter(Boolean).join(' '),
}));
jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// The steps themselves are covered by their own specs; here they only have to let the wizard reach
// its Create button, so General offers the one field the wizard validates.
jest.mock('./PlanGeneralStep', () => ({
    PlanGeneralStep: ({ value, onChange }: { value: GeneralFormData; onChange: (v: GeneralFormData) => void }) => (
        <button type="button" onClick={() => onChange({ ...value, name: 'My plan' })}>
            name the plan
        </button>
    ),
}));
jest.mock('./PlanRestrictionsStep', () => ({ PlanRestrictionsStep: () => <div /> }));
// `buildWizardSteps` decides how many steps there are, so it stays real; only its rendering is stubbed.
jest.mock('./PlanFormStepIndicator', () => ({
    ...jest.requireActual('./PlanFormStepIndicator'),
    PlanFormStepIndicator: () => <div />,
}));
jest.mock('./PlanSecurityStep', () => ({ PlanSecurityStep: () => <div /> }));

const mockCreateMutate = jest.fn();
const mockUpdateMutate = jest.fn();
let mockExistingPlan: Record<string, unknown> | undefined;

jest.mock('../../../../hooks/usePlans', () => ({
    usePlan: () => ({ data: mockExistingPlan, isLoading: false }),
    useCreatePlan: () => ({ mutate: mockCreateMutate, isPending: false }),
    useUpdatePlan: () => ({ mutate: mockUpdateMutate, isPending: false }),
}));

import { PlanFormWizard } from './PlanFormWizard';
import type { GeneralFormData, PlanContext, PlanStatus } from '../../../../types/plan';

const CTX: PlanContext = { type: 'api', entityId: 'API1' };
const CREATE_ROUTE = '/apis/API1/plans/new/KEY_LESS';

/** Reports where a navigation actually landed, and through which route it got there. */
function LocationProbe({ label }: { label: string }) {
    const location = useLocation();
    return <div data-testid="landed">{`${label}|${location.pathname}${location.search}`}</div>;
}

/**
 * The plans routes exactly as `AppRoutes.tsx` declares them. The create route is two URL segments
 * (`new/:securityType`) while the edit route is one (`:planId`), and `:planId` sits right beside
 * `new/…` — so a "back to the list" that resolves one segment short lands on the plan detail route
 * with planId="new" rather than on the list.
 */
function renderCreateForm() {
    return render(
        <MemoryRouter initialEntries={[CREATE_ROUTE]}>
            <Routes>
                <Route path="apis/:apiId/plans">
                    <Route index element={<LocationProbe label="LIST" />} />
                    <Route path="new/:securityType" element={<PlanFormWizard ctx={CTX} securityType="KEY_LESS" />} />
                    <Route path=":planId" element={<LocationProbe label="PLAN_DETAIL" />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('PlanFormWizard — leaving a create form', () => {
    beforeEach(() => {
        mockCreateMutate.mockReset();
        mockUpdateMutate.mockReset();
        mockExistingPlan = undefined;
    });

    it('returns to the plans list when the form is cancelled', () => {
        renderCreateForm();

        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(screen.getByTestId('landed')).toHaveTextContent('LIST|/apis/API1/plans?status=PUBLISHED');
    });

    it('returns to the plans list from the back arrow', () => {
        renderCreateForm();

        fireEvent.click(screen.getByRole('button', { name: 'Back to plans' }));

        expect(screen.getByTestId('landed')).toHaveTextContent('LIST|/apis/API1/plans?status=PUBLISHED');
    });

    /** A plan is always created as STAGING (`planTransformers.ts`), so the list has to open there. */
    it('returns to the staging bucket of the list once the plan is created', () => {
        mockCreateMutate.mockImplementation((_form, options: { onSuccess: () => void }) => options.onSuccess());
        renderCreateForm();

        fireEvent.click(screen.getByRole('button', { name: 'name the plan' }));
        fireEvent.click(screen.getByRole('button', { name: 'Next' }));
        fireEvent.click(screen.getByRole('button', { name: 'Create plan' }));

        expect(screen.getByTestId('landed')).toHaveTextContent('LIST|/apis/API1/plans?status=STAGING');
    });
});

/** The shape `planToFormValue` reads when the wizard seeds itself from a stored plan. */
function planInStatus(status: PlanStatus) {
    return { id: 'PLAN123', name: 'A plan', status, validation: 'MANUAL', security: { type: 'KEY_LESS', configuration: {} }, flows: [] };
}

describe('PlanFormWizard — leaving an edit form', () => {
    beforeEach(() => {
        mockCreateMutate.mockReset();
        mockUpdateMutate.mockReset();
        mockExistingPlan = undefined;
    });

    function renderEditForm() {
        return render(
            <MemoryRouter initialEntries={['/apis/API1/plans/PLAN123']}>
                <Routes>
                    <Route path="apis/:apiId/plans">
                        <Route index element={<LocationProbe label="LIST" />} />
                        <Route path="new/:securityType" element={<div />} />
                        <Route path=":planId" element={<PlanFormWizard ctx={CTX} securityType="KEY_LESS" planId="PLAN123" />} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
    }

    /** The reader came from a bucket; sending them back to Published would hide the plan they just edited. */
    it('returns to the bucket the edited plan is in, not to Published', () => {
        mockExistingPlan = planInStatus('DEPRECATED');
        renderEditForm();

        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(screen.getByTestId('landed')).toHaveTextContent('LIST|/apis/API1/plans?status=DEPRECATED');
    });
});
