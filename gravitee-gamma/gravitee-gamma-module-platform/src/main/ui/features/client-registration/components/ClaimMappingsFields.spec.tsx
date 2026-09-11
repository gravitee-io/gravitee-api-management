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

import { ClaimMappingsFields } from './ClaimMappingsFields';
import type { ClaimMappingRow } from '../utils/validateProviderForm';

describe('ClaimMappingsFields', () => {
    beforeEach(() => {
        let id = 0;
        Object.defineProperty(globalThis, 'crypto', {
            value: { randomUUID: () => `row-${++id}` },
            configurable: true,
        });
    });

    it('keeps a surviving row focused when an earlier row is removed', () => {
        const rows: ClaimMappingRow[] = [
            { id: 'row-a', key: 'first', value: 'x' },
            { id: 'row-b', key: 'second', value: 'y' },
        ];
        const { rerender } = render(<ClaimMappingsFields rows={rows} onChange={jest.fn()} disabled={false} showErrors={false} />);

        const secondInput = screen.getByLabelText('Claim name 2');
        secondInput.focus();
        expect(document.activeElement).toBe(secondInput);

        // Parent removes the first row (row-a), leaving only row-b.
        rerender(<ClaimMappingsFields rows={[rows[1]!]} onChange={jest.fn()} disabled={false} showErrors={false} />);

        const remainingInput = screen.getByLabelText('Claim name 1');
        expect(remainingInput).toHaveValue('second');
        expect(document.activeElement).toBe(remainingInput);
    });
});
