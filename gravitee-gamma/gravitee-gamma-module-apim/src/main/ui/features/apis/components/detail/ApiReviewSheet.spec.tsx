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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('@gravitee/graphene-core', () => ({
    Button: ({
        children,
        onClick,
        disabled,
        type,
    }: {
        children?: ReactNode;
        onClick?: () => void;
        disabled?: boolean;
        type?: 'button';
    }) => (
        <button type={type ?? 'button'} onClick={onClick} disabled={disabled}>
            {children}
        </button>
    ),
    Checkbox: ({
        checked,
        onCheckedChange,
        disabled,
        'aria-label': ariaLabel,
    }: {
        checked?: boolean;
        onCheckedChange?: (value: boolean) => void;
        disabled?: boolean;
        'aria-label'?: string;
    }) => (
        <input
            type="checkbox"
            aria-label={ariaLabel}
            checked={checked}
            disabled={disabled}
            onChange={e => onCheckedChange?.(e.target.checked)}
        />
    ),
    Label: ({ children, htmlFor }: { children?: ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
    Sheet: ({ children, open }: { children?: ReactNode; open?: boolean }) => (open ? <div role="dialog">{children}</div> : null),
    SheetContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    SheetFooter: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
    Skeleton: () => <div data-testid="skeleton" />,
    Textarea: ({
        id,
        value,
        onChange,
        disabled,
        maxLength,
    }: {
        id?: string;
        value?: string;
        onChange?: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
        disabled?: boolean;
        maxLength?: number;
    }) => <textarea id={id} value={value} onChange={onChange} disabled={disabled} maxLength={maxLength} />,
}));

jest.mock('../../services/apiReview', () => ({
    listQualityRules: jest.fn(),
    listApiQualityRuleChecks: jest.fn(),
    submitApiReview: jest.fn(),
}));

jest.mock('../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

import { ApiReviewSheet } from './ApiReviewSheet';
import { notify } from '../../../../shared/notify';
import { listApiQualityRuleChecks, listQualityRules, submitApiReview } from '../../services/apiReview';

const mockListQualityRules = jest.mocked(listQualityRules);
const mockListChecks = jest.mocked(listApiQualityRuleChecks);
const mockSubmit = jest.mocked(submitApiReview);

const RULES = [
    { id: 'r-openapi', name: 'OpenAPI specification is complete', description: 'Every operation is documented.' },
    { id: 'r-owner', name: 'Primary owner is a group', description: 'A team owns the API.' },
];

function renderSheet(open = true) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const onOpenChange = jest.fn();
    render(
        <QueryClientProvider client={client}>
            <ApiReviewSheet apiId="api-1" open={open} onOpenChange={onOpenChange} />
        </QueryClientProvider>,
    );
    return { onOpenChange };
}

describe('ApiReviewSheet', () => {
    beforeEach(() => {
        mockListQualityRules.mockResolvedValue(RULES);
        mockListChecks.mockResolvedValue([{ api: 'api-1', quality_rule: 'r-openapi', checked: true }]);
        mockSubmit.mockResolvedValue(undefined);
    });

    afterEach(() => jest.clearAllMocks());

    it('loads nothing while closed', () => {
        renderSheet(false);
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(mockListQualityRules).not.toHaveBeenCalled();
    });

    it('lists the manual rules and pre-ticks the ones already recorded on the API', async () => {
        renderSheet();
        expect(await screen.findByLabelText('OpenAPI specification is complete')).toBeChecked();
        expect(screen.getByLabelText('Primary owner is a group')).not.toBeChecked();
        expect(screen.getByText('Every operation is documented.')).toBeInTheDocument();
    });

    it('accepts with the ticked rules and the comment, updating recorded checks and creating new ones', async () => {
        const { onOpenChange } = renderSheet();
        await screen.findByLabelText('Primary owner is a group');

        fireEvent.click(screen.getByLabelText('Primary owner is a group'));
        fireEvent.change(screen.getByLabelText('Review comments'), { target: { value: '  Looks good  ' } });
        fireEvent.click(screen.getByRole('button', { name: 'Accept' }));

        await waitFor(() =>
            expect(mockSubmit).toHaveBeenCalledWith('DEFAULT', 'api-1', {
                decision: 'accept',
                message: 'Looks good',
                checks: [
                    { qualityRuleId: 'r-openapi', checked: true, exists: true },
                    { qualityRuleId: 'r-owner', checked: true, exists: false },
                ],
            }),
        );
        await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
        expect(notify.success).toHaveBeenCalledWith('API review saved.');
    });

    it('rejects without a message when the comment is empty', async () => {
        renderSheet();
        await screen.findByLabelText('Primary owner is a group');
        fireEvent.click(screen.getByRole('button', { name: 'Reject' }));

        await waitFor(() =>
            expect(mockSubmit).toHaveBeenCalledWith(
                'DEFAULT',
                'api-1',
                expect.objectContaining({ decision: 'reject', message: undefined }),
            ),
        );
    });

    it('caps the comment at 500 characters and shows the counter', async () => {
        renderSheet();
        const comment = await screen.findByLabelText('Review comments');
        expect(comment).toHaveAttribute('maxlength', '500');
        fireEvent.change(comment, { target: { value: 'abc' } });
        expect(screen.getByText('3/500')).toBeInTheDocument();
    });

    it('keeps the sheet open and reports the failure', async () => {
        const error = new Error('Review is still in progress.');
        mockSubmit.mockRejectedValue(error);
        const { onOpenChange } = renderSheet();
        await screen.findByLabelText('Primary owner is a group');
        fireEvent.click(screen.getByRole('button', { name: 'Accept' }));

        await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'An error occurred while saving API review.'));
        expect(onOpenChange).not.toHaveBeenCalled();
    });

    it('still lets the reviewer decide when the rules cannot be loaded', async () => {
        mockListQualityRules.mockRejectedValue(new Error('403'));
        renderSheet();
        expect(await screen.findByText(/could not load the manual rules/i)).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Accept' }));
        await waitFor(() =>
            expect(mockSubmit).toHaveBeenCalledWith('DEFAULT', 'api-1', { decision: 'accept', message: undefined, checks: [] }),
        );
    });

    it('closes from Cancel without deciding', async () => {
        const { onOpenChange } = renderSheet();
        await screen.findByLabelText('Primary owner is a group');
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onOpenChange).toHaveBeenCalledWith(false);
        expect(mockSubmit).not.toHaveBeenCalled();
    });
});
