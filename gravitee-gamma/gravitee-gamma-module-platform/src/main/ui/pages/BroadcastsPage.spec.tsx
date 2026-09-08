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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';

import { BroadcastsPage } from './BroadcastsPage';
import { useEnvironmentRoles, useSendEnvironmentBroadcast } from '../features/broadcasts/hooks/useEnvironmentBroadcast';
import type { BroadcastPayload } from '../features/broadcasts/types';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../features/broadcasts/hooks/useEnvironmentBroadcast', () => ({
    useEnvironmentRoles: jest.fn(),
    useSendEnvironmentBroadcast: jest.fn(),
}));

jest.mock('@gravitee/graphene-core', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires, @typescript-eslint/no-require-imports
    const React = require('react');

    const ComboboxContext = React.createContext({
        value: [] as string[],
        onValueChange: (_value: string[]) => undefined,
    });

    return {
        Alert: ({ children }: { children: ReactNode }) => <div role="alert">{children}</div>,
        AlertDescription: ({ children }: { children: ReactNode }) => <span>{children}</span>,
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
        Combobox: ({
            value,
            onValueChange,
            children,
        }: {
            value?: string[];
            onValueChange?: (value: string[]) => void;
            children: ReactNode;
        }) => (
            <ComboboxContext.Provider value={{ value: value ?? [], onValueChange: next => onValueChange?.(next) }}>
                <div>{children}</div>
            </ComboboxContext.Provider>
        ),
        ComboboxChip: ({ children }: { children: ReactNode }) => <span>{children}</span>,
        ComboboxChips: React.forwardRef(function ComboboxChips({ children }: { children: ReactNode }, ref: React.Ref<HTMLDivElement>) {
            return <div ref={ref}>{children}</div>;
        }),
        ComboboxChipsInput: ({ id, placeholder }: { id?: string; placeholder?: string }) => (
            <input id={id} placeholder={placeholder} readOnly />
        ),
        ComboboxContent: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        ComboboxEmpty: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        ComboboxItem: ({ value, children }: { value: string; children: ReactNode }) => {
            const ctx = React.useContext(ComboboxContext) as {
                value: string[];
                onValueChange: (value: string[]) => void;
            };
            const checked = ctx.value.includes(value);
            const label = typeof children === 'string' ? children : value;
            return (
                <label>
                    <input
                        type="checkbox"
                        aria-label={label}
                        checked={checked}
                        onChange={() => {
                            ctx.onValueChange(checked ? ctx.value.filter(item => item !== value) : [...ctx.value, value]);
                        }}
                    />
                    {children}
                </label>
            );
        },
        ComboboxList: ({ children }: { children: ReactNode }) => <div>{children}</div>,
        Input: ({
            id,
            value,
            onChange,
            placeholder,
            type,
            'aria-label': ariaLabel,
        }: {
            id?: string;
            value?: string;
            onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
            placeholder?: string;
            type?: string;
            'aria-label'?: string;
        }) => <input id={id} value={value} onChange={onChange} placeholder={placeholder} type={type ?? 'text'} aria-label={ariaLabel} />,
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
        useComboboxAnchor: () => React.useRef(null),
    };
});

const RECIPIENT_OPTIONS = [
    { name: 'ADMIN', displayName: 'Members with the ADMIN role on this environment' },
    { name: 'USER', displayName: 'Members with the USER role on this environment' },
];

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseEnvironmentRoles = useEnvironmentRoles as jest.Mock;
const mockUseSendEnvironmentBroadcast = useSendEnvironmentBroadcast as jest.Mock;

function buildMutateMock(reach = 3) {
    return jest.fn((_payload: BroadcastPayload, opts?: { onSuccess?: (data: number) => void }) => {
        opts?.onSuccess?.(reach);
    });
}

function setupDefaults(overrides: { mutateFn?: jest.Mock; isError?: boolean; isPending?: boolean; error?: unknown } = {}) {
    const mutateFn = overrides.mutateFn ?? jest.fn();
    const resetFn = jest.fn();

    mockUseEnvironmentRoles.mockReturnValue({
        recipientOptions: RECIPIENT_OPTIONS,
        isLoading: false,
        isError: overrides.isError ?? false,
    });
    mockUseSendEnvironmentBroadcast.mockReturnValue({
        mutate: mutateFn,
        isPending: overrides.isPending ?? false,
        error: overrides.error ?? null,
        reset: resetFn,
    });

    return { mutateFn, resetFn };
}

function renderPage() {
    render(<BroadcastsPage />);
}

async function openComposeForm() {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /compose broadcast/i }));
    return user;
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
});

it('hides Compose broadcast when the user lacks environment-message-c', () => {
    setupDefaults();
    mockUseHasPermission.mockReturnValue(false);
    renderPage();
    expect(screen.queryByRole('button', { name: /compose broadcast/i })).toBeNull();
});

it('shows the learning card on the idle page', () => {
    setupDefaults();
    renderPage();
    expect(screen.getByText(/why send broadcasts/i)).not.toBeNull();
    expect(screen.getByText(/reach everyone in this environment/i)).not.toBeNull();
    expect(screen.getByText('Members')).not.toBeNull();
});

it('hides the learning card while the compose form is open', async () => {
    setupDefaults();
    await openComposeForm();
    expect(screen.queryByText(/why send broadcasts/i)).toBeNull();
});

it('returns to idle without sending when Cancel is clicked', async () => {
    const { mutateFn, resetFn } = setupDefaults();
    const user = await openComposeForm();
    await user.click(screen.getByRole('button', { name: /cancel/i }));
    expect(mutateFn).not.toHaveBeenCalled();
    expect(resetFn).toHaveBeenCalled();
    expect(screen.getByText(/why send broadcasts/i)).not.toBeNull();
});

it('clears a sticky send error when Cancel returns to idle', async () => {
    const { resetFn } = setupDefaults({ error: new Error('This url is forbidden. Please, contact your administrator') });
    const user = await openComposeForm();
    expect(screen.getByText(/this url is forbidden/i)).not.toBeNull();
    await user.click(screen.getByRole('button', { name: /cancel/i }));
    expect(resetFn).toHaveBeenCalled();
    expect(screen.queryByText(/this url is forbidden/i)).toBeNull();
});

it('keeps Send disabled until recipients, title, and text are provided', async () => {
    setupDefaults();
    const user = await openComposeForm();

    const sendBtn = screen.getByRole('button', { name: /^send$/i });
    expect(sendBtn).toBeDisabled();

    await user.click(screen.getByRole('checkbox', { name: /members with the admin role on this environment/i }));
    expect(sendBtn).toBeDisabled();

    await user.type(screen.getByLabelText(/title/i), 'Maintenance notice');
    expect(sendBtn).toBeDisabled();

    await user.type(screen.getByLabelText(/message/i), 'Planned downtime this Sunday.');
    expect(sendBtn).not.toBeDisabled();
});

it('shows URL, headers, and proxy when the channel is HTTP, and hides title and recipients', async () => {
    setupDefaults();
    const user = await openComposeForm();

    expect(screen.getByLabelText(/title/i)).not.toBeNull();
    expect(screen.getByLabelText(/recipients/i)).not.toBeNull();
    expect(screen.queryByLabelText(/url/i)).toBeNull();

    await user.selectOptions(screen.getByRole('combobox'), 'HTTP');

    await waitFor(() => {
        expect(screen.queryByLabelText(/title/i)).toBeNull();
        expect(screen.queryByLabelText(/recipients/i)).toBeNull();
        expect(screen.getByLabelText(/url/i)).not.toBeNull();
        expect(screen.getByText(/http headers/i)).not.toBeNull();
        expect(screen.getByRole('switch', { name: /use system proxy/i })).not.toBeNull();
    });
});

it('shows remaining character count against a 4000-character limit', async () => {
    setupDefaults();
    await openComposeForm();
    fireEvent.change(screen.getByLabelText(/message/i), { target: { value: 'a'.repeat(100) } });
    expect(screen.getByText('3900 / 4000')).not.toBeNull();
});

it('sends a PORTAL payload with ENVIRONMENT role_scope', async () => {
    const mutateFn = jest.fn();
    setupDefaults({ mutateFn });
    const user = await openComposeForm();

    await user.click(screen.getByRole('checkbox', { name: /members with the admin role on this environment/i }));
    await user.click(screen.getByRole('checkbox', { name: /members with the user role on this environment/i }));
    fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Env maintenance' } });
    fireEvent.change(screen.getByLabelText(/message/i), { target: { value: 'Gateway restart tonight.' } });
    await user.click(screen.getByRole('button', { name: /^send$/i }));

    expect(mutateFn).toHaveBeenCalledWith(
        {
            channel: 'PORTAL',
            title: 'Env maintenance',
            text: 'Gateway restart tonight.',
            recipient: {
                role_scope: 'ENVIRONMENT',
                role_value: ['ADMIN', 'USER'],
            },
        },
        expect.any(Object),
    );
});

it('sends an HTTP payload with headers and proxy, without title or role_value', async () => {
    const mutateFn = jest.fn();
    setupDefaults({ mutateFn });
    const user = await openComposeForm();

    await user.selectOptions(screen.getByRole('combobox'), 'HTTP');
    await waitFor(() => expect(screen.getByLabelText(/url/i)).not.toBeNull());

    fireEvent.change(screen.getByLabelText(/url/i), { target: { value: 'https://hooks.example.com/notify' } });
    fireEvent.change(screen.getByLabelText(/header name/i), { target: { value: 'Accept' } });
    fireEvent.change(screen.getByLabelText(/header value/i), { target: { value: 'application/json' } });
    await user.click(screen.getByRole('switch', { name: /use system proxy/i }));
    fireEvent.change(screen.getByLabelText(/message/i), { target: { value: 'Webhook ping' } });
    await user.click(screen.getByRole('button', { name: /^send$/i }));

    expect(mutateFn).toHaveBeenCalledWith(
        {
            channel: 'HTTP',
            text: 'Webhook ping',
            recipient: { url: 'https://hooks.example.com/notify' },
            params: { Accept: 'application/json' },
            useSystemProxy: true,
        },
        expect.any(Object),
    );
});

it('keeps Send disabled for an invalid HTTP URL', async () => {
    setupDefaults();
    const user = await openComposeForm();
    await user.selectOptions(screen.getByRole('combobox'), 'HTTP');
    await waitFor(() => expect(screen.getByLabelText(/url/i)).not.toBeNull());
    fireEvent.change(screen.getByLabelText(/url/i), { target: { value: 'not-a-url' } });
    fireEvent.change(screen.getByLabelText(/message/i), { target: { value: 'Hello' } });
    expect(screen.getByRole('button', { name: /^send$/i })).toBeDisabled();
});

it('shows a success banner with reach count and returns to idle on Compose another', async () => {
    const mutateFn = buildMutateMock(7);
    setupDefaults({ mutateFn });
    const user = await openComposeForm();

    await user.click(screen.getByRole('checkbox', { name: /members with the admin role on this environment/i }));
    await user.type(screen.getByLabelText(/title/i), 'Test broadcast');
    await user.type(screen.getByLabelText(/message/i), 'Hello members.');
    await user.click(screen.getByRole('button', { name: /^send$/i }));

    await waitFor(() => {
        expect(screen.getByText(/broadcast sent/i)).not.toBeNull();
        expect(screen.getByText(/7 recipients/i)).not.toBeNull();
    });

    await user.click(screen.getByRole('button', { name: /compose another/i }));
    await waitFor(() => {
        expect(screen.queryByText(/broadcast sent/i)).toBeNull();
        expect(screen.getByText(/why send broadcasts/i)).not.toBeNull();
    });
});

it('shows a recipient-load error and disables Compose', () => {
    setupDefaults({ isError: true });
    renderPage();
    expect(screen.getByRole('alert')).not.toBeNull();
    expect(screen.getByText(/failed to load recipient options/i)).not.toBeNull();
    expect(screen.getByRole('button', { name: /compose broadcast/i })).toBeDisabled();
});

it('disables Compose while a send is pending', () => {
    setupDefaults({ isPending: true });
    renderPage();
    expect(screen.getByRole('button', { name: /compose broadcast/i })).toBeDisabled();
});

it('shows an inline send error and stays composing', async () => {
    setupDefaults({ error: new Error('This url is forbidden. Please, contact your administrator') });
    await openComposeForm();
    expect(screen.getByRole('alert')).not.toBeNull();
    expect(screen.getByText(/this url is forbidden/i)).not.toBeNull();
    expect(screen.getByRole('button', { name: /^send$/i })).not.toBeNull();
});

it('shows a fallback send error when the failure is not an Error', async () => {
    setupDefaults({ error: { status: 500 } });
    await openComposeForm();
    expect(screen.getByText('Failed to send broadcast.')).not.toBeNull();
});
