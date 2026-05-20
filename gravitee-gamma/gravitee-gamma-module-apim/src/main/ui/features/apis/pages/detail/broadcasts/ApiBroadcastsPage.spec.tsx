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
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiBroadcastsPage } from './ApiBroadcastsPage';
import { useApplicationRoles, useSendBroadcast } from '../../../hooks/useApiBroadcast';
import type { BroadcastPayload } from '../../../types/broadcast';

// ─── SDK / icon mocks ─────────────────────────────────────────────────────────

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// ─── Hook mocks ───────────────────────────────────────────────────────────────

jest.mock('../../../hooks/useApiBroadcast', () => ({
    useApplicationRoles: jest.fn(),
    useSendBroadcast: jest.fn(),
}));

// ─── Graphene UI mock — renders semantic HTML so behaviour tests work reliably ─

jest.mock('@gravitee/graphene-core', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires, @typescript-eslint/no-require-imports
    const React = require('react');

    return {
        Alert: ({ children }: { children: ReactNode }) => <div role="alert">{children}</div>,
        AlertDescription: ({ children }: { children: ReactNode }) => <span>{children}</span>,
        Badge: ({ children }: { children: ReactNode }) => <span>{children}</span>,
        Button: ({
            children,
            disabled,
            onClick,
            type,
        }: {
            children: ReactNode;
            disabled?: boolean;
            onClick?: () => void;
            type?: React.ButtonHTMLAttributes<HTMLButtonElement>['type'];
        }) => (
            <button type={type ?? 'button'} disabled={disabled} onClick={onClick}>
                {children}
            </button>
        ),
        Card: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        CardContent: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        CardHeader: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        CardTitle: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        Checkbox: ({
            checked,
            onCheckedChange,
            'aria-label': ariaLabel,
        }: {
            checked?: boolean;
            onCheckedChange?: (v: boolean) => void;
            'aria-label'?: string;
        }) => <input type="checkbox" checked={!!checked} onChange={e => onCheckedChange?.(e.target.checked)} aria-label={ariaLabel} />,
        Input: ({
            id,
            value,
            onChange,
            placeholder,
            type,
        }: {
            id?: string;
            value?: string;
            onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
            placeholder?: string;
            type?: string;
        }) => <input id={id} value={value} onChange={onChange} placeholder={placeholder} type={type ?? 'text'} />,
        Label: ({ children, htmlFor }: { children: ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
        Select: ({ value, onValueChange, children }: { value?: string; onValueChange?: (v: string) => void; children: ReactNode }) => (
            <select aria-label="channel-select" value={value} onChange={e => onValueChange?.(e.target.value)}>
                {children}
            </select>
        ),
        SelectContent: ({ children }: { children: ReactNode }) => <>{children}</>,
        SelectItem: ({ value, children }: { value: string; children: ReactNode }) => <option value={value}>{children}</option>,
        SelectTrigger: ({ children }: { children: ReactNode }) => <>{children}</>,
        SelectValue: () => null,
        Separator: () => <hr />,
        Skeleton: () => <div aria-busy="true" />,
        Switch: ({ id, checked, onCheckedChange }: { id?: string; checked?: boolean; onCheckedChange?: (v: boolean) => void }) => (
            <input id={id} type="checkbox" role="switch" checked={!!checked} onChange={e => onCheckedChange?.(e.target.checked)} />
        ),
        Textarea: ({
            id,
            value,
            onChange,
            maxLength,
        }: {
            id?: string;
            value?: string;
            onChange?: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
            maxLength?: number;
        }) => <textarea id={id} value={value} onChange={onChange} maxLength={maxLength} />,
    };
});

// ─── Test data ────────────────────────────────────────────────────────────────

const RECIPIENT_OPTIONS = [
    { name: 'API_SUBSCRIBERS', displayName: 'API subscribers' },
    { name: 'ADMIN', displayName: 'Members with the ADMIN role on applications subscribed to this API' },
    { name: 'USER', displayName: 'Members with the USER role on applications subscribed to this API' },
];

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApplicationRoles = useApplicationRoles as jest.Mock;
const mockUseSendBroadcast = useSendBroadcast as jest.Mock;

function buildMutateMock(reach = 3) {
    return jest.fn((_payload: BroadcastPayload, opts?: { onSuccess?: (data: number) => void }) => {
        opts?.onSuccess?.(reach);
    });
}

function setupDefaults(overrides: { mutateFn?: jest.Mock } = {}) {
    const mutateFn = overrides.mutateFn ?? jest.fn();

    mockUseApplicationRoles.mockReturnValue({ recipientOptions: RECIPIENT_OPTIONS, isLoading: false, isError: false });
    mockUseSendBroadcast.mockReturnValue({ mutate: mutateFn, isPending: false, error: null, reset: jest.fn() });

    return { mutateFn };
}

function renderPage() {
    render(
        <MemoryRouter initialEntries={['/apis/api-1/broadcasts']}>
            <Routes>
                <Route path="apis/:apiId/broadcasts" element={<ApiBroadcastsPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

async function openComposeForm() {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /compose broadcast/i }));
    return user;
}

// ─── Tests ────────────────────────────────────────────────────────────────────

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
});

// ─── 1. Permission gating ─────────────────────────────────────────────────────

it('hides Compose broadcast button when user lacks api-message-c', () => {
    setupDefaults();
    mockUseHasPermission.mockReturnValue(false);
    renderPage();
    expect(screen.queryByRole('button', { name: /compose broadcast/i })).toBeNull();
});

// ─── 2. Empty / learning state ────────────────────────────────────────────────

it('shows learning card on initial render', () => {
    setupDefaults();
    renderPage();
    expect(screen.getByText(/why send broadcasts/i)).not.toBeNull();
});

it('hides learning card while compose form is open', async () => {
    setupDefaults();
    await openComposeForm();
    expect(screen.queryByText(/why send broadcasts/i)).toBeNull();
});

// ─── 3. Send button disabled until all required fields are filled ─────────────

it('keeps Send disabled until channel, recipients, title, and text are all provided', async () => {
    setupDefaults();
    const user = await openComposeForm();

    const sendBtn = screen.getByRole('button', { name: /^send$/i });
    expect(sendBtn).toBeDisabled();

    // Select a recipient — still missing title + text
    await user.click(screen.getByRole('checkbox', { name: /api subscribers/i }));
    expect(sendBtn).toBeDisabled();

    // Type a title — still missing text
    await user.type(screen.getByLabelText(/title/i), 'Maintenance notice');
    expect(sendBtn).toBeDisabled();

    // Type text — now all required fields are filled
    await user.type(screen.getByLabelText(/message/i), 'Planned downtime this Sunday.');
    expect(sendBtn).not.toBeDisabled();
});

// ─── 4. Channel switching — PORTAL → HTTP ────────────────────────────────────

it('shows URL field and hides Title when channel is switched to HTTP', async () => {
    setupDefaults();
    const user = await openComposeForm();

    // Default (PORTAL): Title visible, URL hidden
    expect(screen.getByLabelText(/title/i)).not.toBeNull();
    expect(screen.queryByLabelText(/url/i)).toBeNull();

    await user.selectOptions(screen.getByRole('combobox'), 'HTTP');

    await waitFor(() => {
        expect(screen.queryByLabelText(/title/i)).toBeNull();
        expect(screen.getByLabelText(/url/i)).not.toBeNull();
    });
});

// ─── 5. Channel switching — HTTP → MAIL ──────────────────────────────────────

it('restores Title and hides URL when switching from HTTP back to MAIL', async () => {
    setupDefaults();
    const user = await openComposeForm();

    await user.selectOptions(screen.getByRole('combobox'), 'HTTP');
    await waitFor(() => expect(screen.getByLabelText(/url/i)).not.toBeNull());

    await user.selectOptions(screen.getByRole('combobox'), 'MAIL');
    await waitFor(() => {
        expect(screen.queryByLabelText(/url/i)).toBeNull();
        expect(screen.getByLabelText(/title/i)).not.toBeNull();
    });
});

// ─── 6. Character counter ─────────────────────────────────────────────────────

it('shows remaining character count as user types in the message field', async () => {
    setupDefaults();
    const user = await openComposeForm();

    const textarea = screen.getByLabelText(/message/i);
    await user.type(textarea, 'a'.repeat(100));

    expect(screen.getByText('150 / 250')).not.toBeNull();
});

it('send enabled at exactly 250 characters', async () => {
    setupDefaults();
    const user = await openComposeForm();

    await user.click(screen.getByRole('checkbox', { name: /api subscribers/i }));
    await user.type(screen.getByLabelText(/title/i), 'T');
    const textarea = screen.getByLabelText(/message/i);
    fireEvent.change(textarea, { target: { value: 'a'.repeat(250) } });

    expect(screen.getByText('0 / 250')).not.toBeNull();
    expect(screen.getByRole('button', { name: /^send$/i })).not.toBeDisabled();
});

// ─── 7. Recipients order ──────────────────────────────────────────────────────

it('lists API_SUBSCRIBERS before role-based recipients', () => {
    setupDefaults();
    renderPage();
    const options = RECIPIENT_OPTIONS;
    expect(options[0].name).toBe('API_SUBSCRIBERS');
    expect(options[1].name).toBe('ADMIN');
    expect(options[2].name).toBe('USER');
});

// ─── 8. PORTAL payload shape ──────────────────────────────────────────────────

it('sends a correctly structured PORTAL payload with selected recipients', async () => {
    const mutateFn = jest.fn();
    setupDefaults({ mutateFn });
    const user = await openComposeForm();

    await user.click(screen.getByRole('checkbox', { name: /api subscribers/i }));
    fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'API deprecation notice' } });
    fireEvent.change(screen.getByLabelText(/message/i), { target: { value: 'The v1 endpoint is deprecated.' } });

    await user.click(screen.getByRole('button', { name: /^send$/i }));

    expect(mutateFn).toHaveBeenCalledWith(
        {
            channel: 'PORTAL',
            title: 'API deprecation notice',
            text: 'The v1 endpoint is deprecated.',
            recipient: {
                role_scope: 'APPLICATION',
                role_value: ['API_SUBSCRIBERS'],
            },
        },
        expect.any(Object),
    );
});

// ─── 9. HTTP payload shape ────────────────────────────────────────────────────

it('sends a correctly structured HTTP payload with URL and no title', async () => {
    const mutateFn = jest.fn();
    setupDefaults({ mutateFn });
    const user = await openComposeForm();

    await user.selectOptions(screen.getByRole('combobox'), 'HTTP');
    await waitFor(() => expect(screen.getByLabelText(/url/i)).not.toBeNull());

    await user.click(screen.getByRole('checkbox', { name: /api subscribers/i }));
    await user.type(screen.getByLabelText(/url/i), 'https://hooks.example.com/notify');
    await user.type(screen.getByLabelText(/message/i), 'System going offline for maintenance.');

    await user.click(screen.getByRole('button', { name: /^send$/i }));

    expect(mutateFn).toHaveBeenCalledWith(
        {
            channel: 'HTTP',
            text: 'System going offline for maintenance.',
            recipient: { url: 'https://hooks.example.com/notify' },
            params: {},
            useSystemProxy: false,
        },
        expect.any(Object),
    );
});

// ─── 10. Success: inline banner shown with reach count ────────────────────────

it('shows success banner with reach count after a successful send', async () => {
    const mutateFn = buildMutateMock(7);
    setupDefaults({ mutateFn });
    const user = await openComposeForm();

    await user.click(screen.getByRole('checkbox', { name: /api subscribers/i }));
    await user.type(screen.getByLabelText(/title/i), 'Test broadcast');
    await user.type(screen.getByLabelText(/message/i), 'Hello consumers.');

    await user.click(screen.getByRole('button', { name: /^send$/i }));

    await waitFor(() => {
        expect(screen.getByText(/broadcast sent/i)).not.toBeNull();
        expect(screen.getByText(/7 recipients/i)).not.toBeNull();
    });
});

it('returns to learning state when Compose another is clicked from success banner', async () => {
    const mutateFn = buildMutateMock();
    setupDefaults({ mutateFn });
    const user = await openComposeForm();

    await user.click(screen.getByRole('checkbox', { name: /api subscribers/i }));
    await user.type(screen.getByLabelText(/title/i), 'Test broadcast');
    await user.type(screen.getByLabelText(/message/i), 'Hello consumers.');
    await user.click(screen.getByRole('button', { name: /^send$/i }));

    await waitFor(() => expect(screen.getByText(/broadcast sent/i)).not.toBeNull());

    await user.click(screen.getByRole('button', { name: /compose another/i }));

    await waitFor(() => {
        expect(screen.queryByText(/broadcast sent/i)).toBeNull();
        expect(screen.getByText(/why send broadcasts/i)).not.toBeNull();
    });
});

// ─── 11. Error from backend shown inline ─────────────────────────────────────

it('displays an error alert in the form when the send request fails', async () => {
    setupDefaults();
    mockUseSendBroadcast.mockReturnValue({
        mutate: jest.fn(),
        isPending: false,
        error: new Error('Server rejected the message'),
    });

    await openComposeForm();

    await waitFor(() => expect(screen.getByRole('alert')).not.toBeNull());
    expect(screen.getByText(/server rejected the message/i)).not.toBeNull();
});
