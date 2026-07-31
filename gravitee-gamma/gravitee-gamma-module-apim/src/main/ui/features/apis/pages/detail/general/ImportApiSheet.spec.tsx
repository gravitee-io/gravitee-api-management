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
        type?: 'button' | 'submit' | 'reset';
    }) => (
        <button type={type ?? 'button'} onClick={onClick} disabled={disabled}>
            {children}
        </button>
    ),
    Sheet: ({ children, open }: { children?: ReactNode; open?: boolean }) => (open ? <div role="dialog">{children}</div> : null),
    SheetContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    SheetFooter: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
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
    Label: ({ children, htmlFor }: { children?: ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
    Switch: ({ checked, onCheckedChange, disabled }: { checked?: boolean; onCheckedChange?: (v: boolean) => void; disabled?: boolean }) => (
        <input type="checkbox" role="switch" checked={checked} onChange={e => onCheckedChange?.(e.target.checked)} disabled={disabled} />
    ),
    cn: (...args: string[]) => args.filter(Boolean).join(' '),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../services/policyStudioService', () => ({
    listPolicies: jest.fn(() => Promise.resolve([])),
}));

import { ImportApiSheet } from './ImportApiSheet';
import { listPolicies } from '../../../services/policyStudioService';

const mockListPolicies = jest.mocked(listPolicies);

// jsdom's File/Blob implementation doesn't provide `.text()`, which ImportApiSheet relies on to read the picked file.
function fileWithText(name: string, content: string, type: string): File {
    const file = new File([content], name, { type });
    Object.defineProperty(file, 'text', { value: () => Promise.resolve(content) });
    return file;
}

function jsonFile(name: string, content: unknown): File {
    return fileWithText(name, JSON.stringify(content), 'application/json');
}

function textFile(name: string, content: string, type = 'application/x-yaml'): File {
    return fileWithText(name, content, type);
}

function renderSheet(overrides: Partial<React.ComponentProps<typeof ImportApiSheet>> = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const onImport = jest.fn();
    const onOpenChange = jest.fn();

    const utils = render(
        <QueryClientProvider client={queryClient}>
            <ImportApiSheet open onOpenChange={onOpenChange} onImport={onImport} isImporting={false} {...overrides} />
        </QueryClientProvider>,
    );

    const fileInput = utils.container.querySelector('input[type="file"]') as HTMLInputElement;

    return { ...utils, onImport, onOpenChange, fileInput };
}

describe('ImportApiSheet', () => {
    // `.mockResolvedValue` overrides survive `clearAllMocks` (which only clears call history), so
    // reset the default here — tests that need specific policies set their own value in the test body.
    beforeEach(() => mockListPolicies.mockResolvedValue([]));
    afterEach(() => jest.clearAllMocks());

    it('defaults to Gravitee definition + local file, with Import disabled until a valid file is picked', () => {
        renderSheet();

        expect(screen.getByRole('tab', { name: 'Gravitee definition', selected: true })).toBeTruthy();
        expect(screen.getByRole('button', { name: /^import$/i })).toBeDisabled();
    });

    it('enables Import and submits the parsed definition once a valid Gravitee JSON file is picked', async () => {
        const { fileInput, onImport } = renderSheet();
        const definition = { api: { name: 'My API' } };

        fireEvent.change(fileInput, { target: { files: [jsonFile('api.json', definition)] } });

        await waitFor(() => expect(screen.getByRole('button', { name: /^import$/i })).not.toBeDisabled());
        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));

        expect(onImport).toHaveBeenCalledWith({ format: 'gravitee', source: 'local', definition });
    });

    it('shows a parse error and keeps Import disabled for invalid JSON', async () => {
        const { fileInput } = renderSheet();

        fireEvent.change(fileInput, { target: { files: [textFile('api.json', '{ not valid json', 'application/json')] } });

        await waitFor(() => expect(screen.getByText(/invalid json/i)).toBeInTheDocument());
        expect(screen.getByRole('button', { name: /^import$/i })).toBeDisabled();
    });

    it('switches to remote URL source, validates http(s) URLs, and submits the trimmed URL', async () => {
        const { onImport } = renderSheet();

        fireEvent.click(screen.getByRole('radio', { name: 'Remote URL' }));
        const urlInput = screen.getByLabelText('Definition URL');

        fireEvent.change(urlInput, { target: { value: 'not-a-url' } });
        expect(screen.getByText(/enter a valid http\(s\) url/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /^import$/i })).toBeDisabled();

        fireEvent.change(urlInput, { target: { value: '  https://example.com/api.json  ' } });
        expect(screen.getByRole('button', { name: /^import$/i })).not.toBeDisabled();

        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));
        expect(onImport).toHaveBeenCalledWith({ format: 'gravitee', source: 'remote', url: 'https://example.com/api.json' });
    });

    it('switching to OpenAPI keeps both source cards and shows Options with documentation on by default', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('tab', { name: 'OpenAPI specification' }));

        expect(screen.getByRole('radio', { name: 'Remote URL' })).toBeInTheDocument();
        expect(screen.getByText(/create documentation page from spec/i)).toBeInTheDocument();
        expect(screen.queryByText(/add openapi specification validation/i)).toBeNull();
    });

    it('submits an OpenAPI descriptor with the URL as payload when importing from a remote source', async () => {
        const { onImport } = renderSheet();

        fireEvent.click(screen.getByRole('tab', { name: 'OpenAPI specification' }));
        fireEvent.click(screen.getByRole('radio', { name: 'Remote URL' }));
        fireEvent.change(screen.getByLabelText('Specification URL'), { target: { value: 'https://example.com/openapi.yaml' } });

        await waitFor(() => expect(screen.getByRole('button', { name: /^import$/i })).not.toBeDisabled());
        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));

        expect(onImport).toHaveBeenCalledWith({
            format: 'openapi',
            descriptor: { payload: 'https://example.com/openapi.yaml', withDocumentation: true },
        });
    });

    it('submits an OpenAPI descriptor with the raw file text as payload once a spec file is picked', async () => {
        const { fileInput, onImport } = renderSheet();
        const yaml = 'openapi: 3.0.0\ninfo:\n  title: My API';

        fireEvent.click(screen.getByRole('tab', { name: 'OpenAPI specification' }));
        fireEvent.change(fileInput, { target: { files: [textFile('api.yaml', yaml)] } });

        await waitFor(() => expect(screen.getByRole('button', { name: /^import$/i })).not.toBeDisabled());
        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));

        expect(onImport).toHaveBeenCalledWith({
            format: 'openapi',
            descriptor: { payload: yaml, withDocumentation: true },
        });
    });

    it('reveals the OAS validation toggle (default on) only when the oas-validation policy is installed', async () => {
        mockListPolicies.mockResolvedValue([{ id: 'oas-validation', name: 'OAS Validation' }]);
        const { fileInput, onImport } = renderSheet();

        fireEvent.click(screen.getByRole('tab', { name: 'OpenAPI specification' }));
        await waitFor(() => expect(screen.getByText(/add openapi specification validation/i)).toBeInTheDocument());

        fireEvent.change(fileInput, { target: { files: [textFile('api.json', '{"openapi":"3.0.0"}', 'application/json')] } });
        await waitFor(() => expect(screen.getByRole('button', { name: /^import$/i })).not.toBeDisabled());
        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));

        expect(onImport).toHaveBeenCalledWith({
            format: 'openapi',
            descriptor: { payload: '{"openapi":"3.0.0"}', withDocumentation: true, withOASValidationPolicy: true },
        });
    });

    it('switching to WSDL hides REST-to-SOAP and OAS validation until their policies are installed', async () => {
        renderSheet();

        fireEvent.click(screen.getByRole('tab', { name: 'WSDL' }));

        await waitFor(() => expect(screen.getByText(/create documentation page from spec/i)).toBeInTheDocument());
        expect(screen.queryByText(/apply rest to soap transformer policy/i)).toBeNull();
        expect(screen.queryByText(/add openapi specification validation/i)).toBeNull();
    });

    it('submits a WSDL descriptor with type INLINE once a local WSDL file is picked', async () => {
        const { fileInput, onImport } = renderSheet();
        const wsdl = '<?xml version="1.0"?><definitions></definitions>';

        fireEvent.click(screen.getByRole('tab', { name: 'WSDL' }));
        fireEvent.change(fileInput, { target: { files: [textFile('service.wsdl', wsdl, 'application/xml')] } });

        await waitFor(() => expect(screen.getByRole('button', { name: /^import$/i })).not.toBeDisabled());
        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));

        expect(onImport).toHaveBeenCalledWith({
            format: 'wsdl',
            descriptor: { payload: wsdl, type: 'INLINE', withDocumentation: false, withPolicies: [] },
        });
    });

    it('submits a WSDL descriptor with type URL when importing from a remote source', async () => {
        const { onImport } = renderSheet();

        fireEvent.click(screen.getByRole('tab', { name: 'WSDL' }));
        fireEvent.click(screen.getByRole('radio', { name: 'Remote URL' }));
        fireEvent.change(screen.getByLabelText('WSDL URL'), { target: { value: 'https://example.com/service.wsdl' } });

        await waitFor(() => expect(screen.getByRole('button', { name: /^import$/i })).not.toBeDisabled());
        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));

        expect(onImport).toHaveBeenCalledWith({
            format: 'wsdl',
            descriptor: { payload: 'https://example.com/service.wsdl', type: 'URL', withDocumentation: false, withPolicies: [] },
        });
    });

    it('defaults REST-to-SOAP on and auto-enables documentation + OAS validation when both policies are installed', async () => {
        mockListPolicies.mockResolvedValue([
            { id: 'rest-to-soap', name: 'REST to SOAP' },
            { id: 'oas-validation', name: 'OAS Validation' },
        ]);
        const { fileInput, onImport } = renderSheet();

        fireEvent.click(screen.getByRole('tab', { name: 'WSDL' }));
        await waitFor(() => expect(screen.getByText(/apply rest to soap transformer policy/i)).toBeInTheDocument());

        fireEvent.change(fileInput, { target: { files: [textFile('service.wsdl', '<xml/>', 'application/xml')] } });
        await waitFor(() => expect(screen.getByRole('button', { name: /^import$/i })).not.toBeDisabled());
        fireEvent.click(screen.getByRole('button', { name: /^import$/i }));

        expect(onImport).toHaveBeenCalledWith({
            format: 'wsdl',
            descriptor: {
                payload: '<xml/>',
                type: 'INLINE',
                withDocumentation: true,
                withOASValidationPolicy: true,
                withPolicies: ['rest-to-soap'],
            },
        });
    });

    it('documentation and OAS validation are disabled for WSDL until REST-to-SOAP is on', async () => {
        mockListPolicies.mockResolvedValue([
            { id: 'rest-to-soap', name: 'REST to SOAP' },
            { id: 'oas-validation', name: 'OAS Validation' },
        ]);
        renderSheet();

        fireEvent.click(screen.getByRole('tab', { name: 'WSDL' }));
        await waitFor(() => expect(screen.getByText(/apply rest to soap transformer policy/i)).toBeInTheDocument());

        // DOM order: REST-to-SOAP, Documentation, OAS validation (all default on from the policy seed).
        const [restToSoapSwitch, documentationSwitch, oasSwitch] = screen.getAllByRole('switch');
        expect(documentationSwitch).not.toBeDisabled();
        expect(oasSwitch).not.toBeDisabled();

        fireEvent.click(restToSoapSwitch); // turn off
        expect(documentationSwitch).toBeDisabled();
        expect(oasSwitch).toBeDisabled();
        expect(documentationSwitch).not.toBeChecked();
        expect(oasSwitch).not.toBeChecked();

        fireEvent.click(restToSoapSwitch); // back on — re-enables and re-checks both
        expect(documentationSwitch).not.toBeDisabled();
        expect(oasSwitch).not.toBeDisabled();
        expect(documentationSwitch).toBeChecked();
        expect(oasSwitch).toBeChecked();
    });

    it('surfaces a message when the policies fetch fails, instead of silently hiding the options', async () => {
        mockListPolicies.mockRejectedValue(new Error('network error'));
        renderSheet();

        fireEvent.click(screen.getByRole('tab', { name: 'OpenAPI specification' }));

        await waitFor(() => expect(screen.getByText(/could not check which policies are installed/i)).toBeInTheDocument());
    });

    it('shows the Importing… state and disables Cancel/Import while a mutation is in flight', () => {
        renderSheet({ isImporting: true });

        expect(screen.getByRole('button', { name: /importing/i })).toBeDisabled();
        expect(screen.getByRole('button', { name: /cancel/i })).toBeDisabled();
    });

    it('surfaces the error message passed in from the mutation', () => {
        renderSheet({ error: 'Failed to import API definition.' });

        expect(screen.getByText('Failed to import API definition.')).toBeInTheDocument();
    });

    it('calls onOpenChange(false) when Cancel is clicked', () => {
        const { onOpenChange } = renderSheet();

        fireEvent.click(screen.getByRole('button', { name: /cancel/i }));

        expect(onOpenChange).toHaveBeenCalledWith(false);
    });
});
