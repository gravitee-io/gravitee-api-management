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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { NotificationTemplateDetailPage } from './NotificationTemplateDetailPage';
import { useNotificationTemplate, useSaveNotificationTemplates } from '../features/notification-templates/hooks/useNotificationTemplate';
import { useNotificationTemplatePermissions } from '../features/notification-templates/hooks/useNotificationTemplatePermissions';
import type { NotificationTemplate } from '../features/notification-templates/types/notificationTemplate';
import { notify } from '../shared/notify';

jest.mock('@gravitee/graphene-core/code-editor', () => ({
    CodeEditor: ({ value, onChange, disabled }: { value?: string; onChange?: (next: string) => void; disabled?: boolean }) => (
        <textarea value={value} disabled={disabled} onChange={event => onChange?.(event.target.value)} />
    ),
}));

jest.mock('../features/notification-templates/hooks/useNotificationTemplate');
jest.mock('../features/notification-templates/hooks/useNotificationTemplatePermissions');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

const mockUseNotificationTemplate = jest.mocked(useNotificationTemplate);
const mockUseSaveNotificationTemplates = jest.mocked(useSaveNotificationTemplates);
const mockUseNotificationTemplatePermissions = jest.mocked(useNotificationTemplatePermissions);

const EMAIL: NotificationTemplate = {
    name: 'API Started',
    scope: 'API',
    type: 'EMAIL',
    hook: 'API_STARTED',
    description: 'Triggered when an API is started.',
    title: 'API started',
    content: '<p>started</p>',
    enabled: false,
};

const PORTAL: NotificationTemplate = {
    ...EMAIL,
    type: 'PORTAL',
    title: 'API started',
    content: 'API started',
};

const INCLUDE: NotificationTemplate = {
    name: 'header.html',
    scope: 'TEMPLATES_TO_INCLUDE',
    type: 'EMAIL',
    hook: '',
    content: '<header></header>',
    enabled: false,
};

function renderPage(path = '/templates/API/API_STARTED') {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return (
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={[path]}>
                    <Routes>
                        <Route path="/templates/:scope/:hook" element={children} />
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>
        );
    }
    return render(<NotificationTemplateDetailPage />, { wrapper: Wrapper });
}

describe('NotificationTemplateDetailPage', () => {
    const mutateAsync = jest.fn().mockResolvedValue([]);

    beforeEach(() => {
        jest.clearAllMocks();
        mockUseNotificationTemplatePermissions.mockReturnValue({
            canRead: true,
            canCreate: true,
            canUpdate: true,
            canEdit: true,
        });
        mockUseSaveNotificationTemplates.mockReturnValue({
            mutateAsync,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveNotificationTemplates>);
        mockUseNotificationTemplate.mockReturnValue({
            data: [EMAIL, PORTAL],
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useNotificationTemplate>);
    });

    it('renders email and portal channel cards', () => {
        renderPage();
        expect(screen.getByText('API Started')).not.toBeNull();
        expect(screen.getByText('Email notification')).not.toBeNull();
        expect(screen.getByText('Portal notification')).not.toBeNull();
    });

    it('POSTs first overrides for both channels on save', async () => {
        renderPage();
        fireEvent.click(screen.getAllByLabelText('Override default template')[0]!);
        fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

        await waitFor(() => expect(mutateAsync).toHaveBeenCalled());
        const payload = mutateAsync.mock.calls[0]?.[0] as NotificationTemplate[];
        expect(payload).toHaveLength(2);
        expect(payload).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ type: 'EMAIL', enabled: true }),
                expect.objectContaining({ type: 'PORTAL', enabled: false }),
            ]),
        );
        expect(payload.every(template => template.id === undefined)).toBe(true);
        expect(notify.success).toHaveBeenCalledWith('Template has been successfully saved!');
    });

    it('PUTs an existing customization by id', async () => {
        mockUseNotificationTemplate.mockReturnValue({
            data: [{ ...EMAIL, id: 'tmpl-1', enabled: true }],
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useNotificationTemplate>);

        renderPage();
        fireEvent.change(screen.getByLabelText('Title of the notification'), { target: { value: 'API is up' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

        await waitFor(() => expect(mutateAsync).toHaveBeenCalled());
        expect(mutateAsync.mock.calls[0]?.[0]).toEqual([expect.objectContaining({ id: 'tmpl-1', title: 'API is up', enabled: true })]);
    });

    it('shows a read-only banner and hides the save bar without create or update permission', () => {
        mockUseNotificationTemplatePermissions.mockReturnValue({
            canRead: true,
            canCreate: false,
            canUpdate: false,
            canEdit: false,
        });

        renderPage();
        fireEvent.click(screen.getAllByLabelText('Override default template')[0]!);

        expect(screen.getByText('You have read-only access to notification templates, so this template cannot be changed.')).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Save changes' })).toBeNull();
    });

    it('shows the include-fragment hint and hides the title field', () => {
        mockUseNotificationTemplate.mockReturnValue({
            data: [INCLUDE],
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useNotificationTemplate>);

        renderPage('/templates/TEMPLATES_TO_INCLUDE/header.html');

        expect(screen.getByText(/<#include "header.html" \/>/)).not.toBeNull();
        expect(screen.queryByLabelText('Title of the notification')).toBeNull();
        expect(screen.getByText('Templates to include')).not.toBeNull();
        expect(screen.queryByText('TEMPLATES_TO_INCLUDE')).toBeNull();
    });

    it('blocks save until required fields are filled', async () => {
        renderPage();
        fireEvent.click(screen.getAllByLabelText('Override default template')[0]!);
        fireEvent.change(screen.getAllByRole('group', { name: 'Content' })[0]!.querySelector('textarea')!, { target: { value: '' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

        expect(await screen.findByText('Content is required.')).not.toBeNull();
        expect(mutateAsync).not.toHaveBeenCalled();
    });
});
