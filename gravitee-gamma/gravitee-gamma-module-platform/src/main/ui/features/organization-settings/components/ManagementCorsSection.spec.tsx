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

import { TooltipProvider } from '@gravitee/graphene-core';
import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';

import type { CorsFieldReadonly, CorsFormState } from './CorsSection';
import { ManagementCorsSection } from './ManagementCorsSection';

const INITIAL: CorsFormState = {
    allowOrigin: ['https://console.example.com'],
    allowMethods: ['GET', 'POST'],
    allowHeaders: ['Authorization'],
    exposedHeaders: ['ETag'],
    maxAge: '1728000',
};

function Harness({ initial = INITIAL, readonly = {} }: { initial?: CorsFormState; readonly?: CorsFieldReadonly }) {
    const [value, setValue] = useState(initial);
    return (
        <TooltipProvider>
            <ManagementCorsSection value={value} disabled={false} readonly={readonly} onChange={setValue} />
        </TooltipProvider>
    );
}

describe('ManagementCorsSection', () => {
    it('renders Classic org CORS copy before fields', () => {
        render(<Harness />);
        expect(screen.getByText(/same-origin definition/i)).not.toBeNull();
        expect(screen.getByText(/Regular expressions are also supported\./)).not.toBeNull();
        expect(screen.getAllByText(/preflight request to indicate which HTTP headers/i).length).toBeGreaterThan(0);
    });

    it('renders origin chips, method checkboxes, and max age', () => {
        render(<Harness />);
        expect(screen.getByText('https://console.example.com')).not.toBeNull();
        expect((screen.getByLabelText('GET') as HTMLButtonElement).getAttribute('data-state') ?? '').not.toBe('');
        expect((screen.getByLabelText('Max age') as HTMLInputElement).value).toBe('1728000');
    });

    it('offers Classic default HTTP headers as CORS autocomplete options', () => {
        render(<Harness />);
        fireEvent.focus(screen.getByLabelText('Allow-Headers'));
        expect(screen.getByRole('option', { name: 'Content-Type' })).not.toBeNull();
        fireEvent.click(screen.getByRole('option', { name: 'Content-Type' }));
        expect(screen.getByText('Content-Type')).not.toBeNull();
        expect(screen.queryByRole('option', { name: 'Authorization' })).toBeNull();
    });

    it('does not commit incomplete CORS headers on blur (Classic addOnBlur=false)', () => {
        render(<Harness />);
        fireEvent.change(screen.getByLabelText('Allow-Headers'), { target: { value: 'Cont' } });
        fireEvent.blur(screen.getByLabelText('Allow-Headers'));
        expect(screen.queryByText('Cont')).toBeNull();

        fireEvent.change(screen.getByLabelText('Exposed-Headers'), { target: { value: 'X-Partial' } });
        fireEvent.blur(screen.getByLabelText('Exposed-Headers'));
        expect(screen.queryByText('X-Partial')).toBeNull();
    });

    it('still commits Allow-Origin on blur', () => {
        render(<Harness />);
        fireEvent.change(screen.getByLabelText('Allow-Origin'), { target: { value: 'https://app.example.com' } });
        fireEvent.blur(screen.getByLabelText('Allow-Origin'));
        expect(screen.getByText('https://app.example.com')).not.toBeNull();
    });

    it('wraps system-readonly fields with a tooltip hint', () => {
        render(<Harness readonly={{ allowOrigin: true }} />);
        expect(screen.getByPlaceholderText(/https:\/\/mydomain.com/).closest('[data-system-readonly="true"]')).not.toBeNull();
        expect(screen.getByLabelText('Max age').closest('[data-system-readonly="true"]')).toBeNull();
    });

    it('asks before adding * as an origin', () => {
        render(<Harness />);
        fireEvent.change(screen.getByPlaceholderText(/https:\/\/mydomain.com/), { target: { value: '*' } });
        fireEvent.keyDown(screen.getByPlaceholderText(/https:\/\/mydomain.com/), { key: 'Enter' });
        expect(screen.getByText('Are you sure?')).not.toBeNull();
        expect(screen.queryByText(/exposes this management API/)).toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'Yes, I want to allow all origins.' }));
        expect(screen.getByText(/exposes this management API/)).not.toBeNull();
    });
});
