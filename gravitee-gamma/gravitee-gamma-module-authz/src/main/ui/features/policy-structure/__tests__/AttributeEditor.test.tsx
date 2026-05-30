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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { beforeAll, describe, expect, it } from 'vitest';
import { AttributeEditor, type AttributeRow } from '../AttributeEditor';

beforeAll(() => {
    if (!Element.prototype.scrollIntoView) Element.prototype.scrollIntoView = () => undefined;
});

function Harness({ initial = [] as AttributeRow[], readOnly = false }: { initial?: AttributeRow[]; readOnly?: boolean }) {
    const [rows, setRows] = useState<AttributeRow[]>(initial);
    return <AttributeEditor value={rows} onChange={setRows} readOnly={readOnly} keySuggestions={[]} />;
}

const row = (over: Partial<AttributeRow> = {}): AttributeRow => ({ id: 'r1', key: 'dept', type: 'string', raw: 'eng', ...over });

describe('AttributeEditor', () => {
    it('adds a blank row when "Add attribute" is clicked', async () => {
        const user = userEvent.setup();
        render(<Harness />);
        await user.click(screen.getByRole('button', { name: /Add attribute/i }));
        expect(screen.getByLabelText(/Attribute key/i)).toBeInTheDocument();
    });

    it('shows a reserved-key error for an underscore key', async () => {
        const user = userEvent.setup();
        render(<Harness initial={[row({ key: '' })]} />);
        await user.type(screen.getByLabelText(/Attribute key/i), '_secret');
        expect(screen.getByText(/reserved/i)).toBeInTheDocument();
    });

    it('shows an integer error for a non-numeric value', async () => {
        const user = userEvent.setup();
        render(<Harness initial={[row({ key: 'clearance', type: 'integer', raw: '' })]} />);
        await user.type(screen.getByLabelText(/Attribute value/i), 'abc');
        expect(screen.getByText(/whole number/i)).toBeInTheDocument();
    });

    it('shows the policy-function hint for string-backed types', () => {
        render(<Harness initial={[row({ key: 'srcIp', type: 'ip', raw: '10.0.0.1' })]} />);
        expect(screen.getByText(/ip\(\.\.\.\)/)).toBeInTheDocument();
    });

    it('removes a row', async () => {
        const user = userEvent.setup();
        render(<Harness initial={[row()]} />);
        await user.click(screen.getByRole('button', { name: /Remove dept/i }));
        expect(screen.queryByLabelText(/Attribute key/i)).not.toBeInTheDocument();
    });

    it('renders read-only with a catalog badge and no add button', () => {
        render(<Harness initial={[row()]} readOnly />);
        expect(screen.getByText('eng')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Add attribute/i })).not.toBeInTheDocument();
        expect(screen.queryByLabelText(/Attribute value/i)).not.toBeInTheDocument();
    });
});
