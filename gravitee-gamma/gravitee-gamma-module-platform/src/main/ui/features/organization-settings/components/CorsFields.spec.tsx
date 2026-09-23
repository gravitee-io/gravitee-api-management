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

import { Card, CardContent } from '@gravitee/graphene-core';
import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';

import { CorsFields } from './CorsFields';
import type { CorsFormState } from './CorsSection';

const EMPTY_CORS: CorsFormState = {
    allowOrigin: [],
    allowMethods: ['GET'],
    allowHeaders: [],
    exposedHeaders: [],
    maxAge: '0',
};

function Harness({ initial = EMPTY_CORS }: { initial?: CorsFormState }) {
    const [value, setValue] = useState(initial);
    return (
        <Card className="h-24 overflow-hidden">
            <CardContent>
                <CorsFields value={value} disabled={false} onChange={setValue} allowOriginId="cors-origins" />
            </CardContent>
        </Card>
    );
}

describe('CorsFields', () => {
    it('renders allow-headers suggestions in a portal outside clipped cards', () => {
        render(<Harness />);

        const input = document.getElementById('cors-allow-headers')!;
        fireEvent.focus(input as HTMLElement);

        const listbox = screen.getByRole('listbox');
        expect(listbox.parentElement).toBe(document.body);
        expect(listbox.className).toContain('fixed');
        expect(listbox.className).toContain('z-[100]');
    });

    it('keeps the allow-headers list open while picking multiple suggestions', () => {
        render(<Harness />);

        const input = document.getElementById('cors-allow-headers') as HTMLInputElement;
        fireEvent.focus(input);
        fireEvent.click(screen.getByRole('option', { name: 'Authorization' }));
        expect(screen.getByText('Authorization')).not.toBeNull();
        expect(screen.getByRole('listbox')).not.toBeNull();

        fireEvent.click(screen.getByRole('option', { name: 'Content-Type' }));
        expect(screen.getByText('Content-Type')).not.toBeNull();
    });

    it('renders exposed-headers suggestions in a portal outside clipped cards', () => {
        render(<Harness />);

        const input = document.getElementById('cors-exposed-headers') as HTMLInputElement;
        fireEvent.focus(input);

        const listbox = screen.getByRole('listbox');
        expect(listbox.parentElement).toBe(document.body);
        expect(listbox.className).toContain('fixed');
    });
});
