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
import { useState } from 'react';

import { CorsSection, type CorsFormState } from './CorsSection';

const INITIAL: CorsFormState = {
    allowOrigin: ['https://console.example.com'],
    allowMethods: ['GET', 'POST'],
    allowHeaders: ['Authorization'],
    exposedHeaders: ['ETag'],
    maxAge: '1728000',
};

function Harness({ initial = INITIAL }: { initial?: CorsFormState }) {
    const [value, setValue] = useState(initial);
    return <CorsSection value={value} disabled={false} onChange={setValue} />;
}

describe('CorsSection', () => {
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
