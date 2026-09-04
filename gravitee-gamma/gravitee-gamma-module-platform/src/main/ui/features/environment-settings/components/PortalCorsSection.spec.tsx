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

import { PortalCorsSection } from './PortalCorsSection';
import type { CorsFieldReadonly, CorsFormState } from '../../organization-settings/components/CorsSection';

const INITIAL: CorsFormState = {
    allowOrigin: ['https://portal.example.com'],
    allowMethods: ['GET', 'POST'],
    allowHeaders: ['Authorization'],
    exposedHeaders: ['ETag'],
    maxAge: '1728000',
};

function Harness({ initial = INITIAL, readonly = {} }: { initial?: CorsFormState; readonly?: CorsFieldReadonly }) {
    const [value, setValue] = useState(initial);
    return (
        <TooltipProvider>
            <PortalCorsSection value={value} disabled={false} readonly={readonly} onChange={setValue} />
        </TooltipProvider>
    );
}

describe('PortalCorsSection', () => {
    it('renders environment CORS helper copy', () => {
        render(<Harness />);
        expect(screen.getByText(/Exact origins, \* , or a regular expression\. Press Enter to add\./)).not.toBeNull();
        expect(screen.getByText(/Start typing to pick a common header, or press Enter to add your own\./)).not.toBeNull();
        expect(screen.getByText(/Headers the browser is allowed to read from the response\./)).not.toBeNull();
        expect(screen.getByText(/preflight request\./)).not.toBeNull();
        expect(screen.getByLabelText(/Max age \(seconds\)/)).not.toBeNull();
        expect(screen.getByText(/cached by clients$/)).not.toBeNull();
    });

    it('wraps system-readonly fields with a tooltip hint', () => {
        render(<Harness readonly={{ allowOrigin: true }} />);
        expect(screen.getByLabelText('Allow-Origin').closest('[data-system-readonly="true"]')).not.toBeNull();
        expect(screen.getByLabelText(/Max age \(seconds\)/).closest('[data-system-readonly="true"]')).toBeNull();
    });

    it('does not commit Allow-Origin on blur (Classic addOnBlur=false)', () => {
        render(<Harness />);
        fireEvent.change(screen.getByLabelText('Allow-Origin'), { target: { value: 'https://app.example.com' } });
        fireEvent.blur(screen.getByLabelText('Allow-Origin'));
        expect(screen.queryByText('https://app.example.com')).toBeNull();
    });

    it('commits Allow-Origin on Enter', () => {
        render(<Harness />);
        fireEvent.change(screen.getByLabelText('Allow-Origin'), { target: { value: 'https://app.example.com' } });
        fireEvent.keyDown(screen.getByLabelText('Allow-Origin'), { key: 'Enter' });
        expect(screen.getByText('https://app.example.com')).not.toBeNull();
    });

    it('does not commit incomplete CORS headers on blur (Classic addOnBlur=false)', () => {
        render(<Harness />);
        fireEvent.change(screen.getByLabelText('Allow-Headers'), { target: { value: 'Cont' } });
        fireEvent.blur(screen.getByLabelText('Allow-Headers'));
        expect(screen.queryByText('Cont')).toBeNull();
    });

    it('shows invalid Allow-Origin regex errors', () => {
        render(<Harness initial={{ ...INITIAL, allowOrigin: ['[invalid'] }} />);
        expect(screen.getByText(/Regex is invalid/)).not.toBeNull();
    });

    it('offers Classic default HTTP headers as autocomplete options', () => {
        render(<Harness />);
        fireEvent.focus(screen.getByLabelText('Allow-Headers'));
        expect(screen.getByRole('option', { name: 'Content-Type' })).not.toBeNull();
    });
});
