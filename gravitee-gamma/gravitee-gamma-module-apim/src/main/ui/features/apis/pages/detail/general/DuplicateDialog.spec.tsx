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
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('@gravitee/graphene-core', () => ({
    Button: ({ children, onClick, disabled }: { children?: React.ReactNode; onClick?: () => void; disabled?: boolean }) => (
        <button type="button" onClick={onClick} disabled={disabled}>
            {children}
        </button>
    ),
    Dialog: ({ children, open }: { children?: React.ReactNode; open?: boolean }) => (open ? <div role="dialog">{children}</div> : null),
    DialogClose: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
    DialogContent: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    DialogFooter: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    DialogHeader: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    DialogTitle: ({ children }: { children?: React.ReactNode }) => <h2>{children}</h2>,
    Input: ({
        id,
        value,
        onChange,
        placeholder,
    }: {
        id?: string;
        value?: string;
        onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
        placeholder?: string;
    }) => <input id={id} value={value} onChange={onChange} placeholder={placeholder} />,
    Label: ({ children, htmlFor }: { children?: React.ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('./DialogCheckboxOptions', () => ({
    DialogCheckboxOptions: () => null,
}));

jest.mock('../../../services/apiProxy', () => ({
    verifyContextPath: jest.fn(() => Promise.resolve({ ok: true })),
    verifyApiHosts: jest.fn(() => Promise.resolve({ ok: true })),
}));

import { DuplicateDialog } from './DuplicateDialog';
import { verifyContextPath } from '../../../services/apiProxy';

const mockVerifyContextPath = jest.mocked(verifyContextPath);

describe('DuplicateDialog', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockVerifyContextPath.mockResolvedValue({ ok: true });
    });

    it('disables duplicate when verify rejects the context path (e.g. same as source API)', async () => {
        mockVerifyContextPath.mockResolvedValue({
            ok: false,
            reason: 'This context path is already in use by another API.',
        });

        const onDuplicate = jest.fn();
        render(
            <DuplicateDialog
                open
                onOpenChange={() => {}}
                initialVersion="1.0.0"
                entryMode="contextPath"
                contextPathPlaceholder="/one1/"
                hostPlaceholder=""
                onDuplicate={onDuplicate}
                isLoading={false}
            />,
        );

        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByPlaceholderText('/one1/'), { target: { value: '/one1/' } });
        fireEvent.change(within(dialog).getByPlaceholderText('1.0.0'), { target: { value: '2.0.0' } });

        await waitFor(() => expect(mockVerifyContextPath).toHaveBeenCalledWith('DEFAULT', [{ path: '/one1/' }]));
        expect(mockVerifyContextPath.mock.calls.every(call => call.length === 2)).toBe(true);

        await waitFor(() => expect(screen.getByText('This context path is already in use by another API.')).toBeInTheDocument());
        expect(screen.getByRole('button', { name: /duplicate/i })).toBeDisabled();
        expect(onDuplicate).not.toHaveBeenCalled();
    });

    it('calls verify API without apiId when context path differs from source', async () => {
        render(
            <DuplicateDialog
                open
                onOpenChange={() => {}}
                initialVersion="1.0.0"
                entryMode="contextPath"
                contextPathPlaceholder="/one1/"
                hostPlaceholder=""
                onDuplicate={jest.fn()}
                isLoading={false}
            />,
        );

        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByPlaceholderText('/one1/'), { target: { value: '/another-path/' } });

        await waitFor(() => expect(mockVerifyContextPath).toHaveBeenCalledWith('DEFAULT', [{ path: '/another-path/' }]));
        expect(mockVerifyContextPath.mock.calls.every(call => call.length === 2)).toBe(true);
    });
});
