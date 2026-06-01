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

import { AdditionalClientMetadataField } from './AdditionalClientMetadataField';

describe('AdditionalClientMetadataField', () => {
    beforeEach(() => {
        let id = 0;
        Object.defineProperty(globalThis, 'crypto', {
            value: { randomUUID: () => `row-${++id}` },
            configurable: true,
        });
    });
    it('highlights duplicate keys and still syncs draft values to the parent', () => {
        const onChange = jest.fn();
        const onDuplicateKeysChange = jest.fn();

        render(<AdditionalClientMetadataField value={null} onChange={onChange} onDuplicateKeysChange={onDuplicateKeysChange} />);

        fireEvent.change(screen.getByLabelText('Metadata key 1'), { target: { value: 'scope' } });
        fireEvent.change(screen.getByLabelText('Metadata value 1'), { target: { value: 'openid' } });
        onChange.mockClear();

        fireEvent.change(screen.getByLabelText('Metadata key 2'), { target: { value: 'scope' } });
        fireEvent.change(screen.getByLabelText('Metadata value 2'), { target: { value: 'profile' } });
        const duplicateKeyInputs = screen.getAllByLabelText(/Metadata key/i);

        expect(screen.getByText('Keys must be unique')).not.toBeNull();
        expect(onDuplicateKeysChange).toHaveBeenLastCalledWith(true);
        expect(onChange).toHaveBeenLastCalledWith({ scope: 'profile' });
        expect(duplicateKeyInputs[0]?.getAttribute('aria-invalid')).toBe('true');
        expect(duplicateKeyInputs[1]?.getAttribute('aria-invalid')).toBe('true');
    });

    it('syncs a unique metadata record after duplicates are resolved', () => {
        const onChange = jest.fn();

        render(<AdditionalClientMetadataField value={null} onChange={onChange} />);

        fireEvent.change(screen.getByLabelText('Metadata key 1'), { target: { value: 'scope' } });
        fireEvent.change(screen.getByLabelText('Metadata value 1'), { target: { value: 'openid' } });
        fireEvent.change(screen.getByLabelText('Metadata key 2'), { target: { value: 'scope' } });
        fireEvent.change(screen.getByLabelText('Metadata value 2'), { target: { value: 'profile' } });

        onChange.mockClear();
        fireEvent.change(screen.getByLabelText('Metadata key 2'), { target: { value: 'audience' } });
        fireEvent.change(screen.getByLabelText('Metadata value 2'), { target: { value: 'api' } });

        expect(onChange).toHaveBeenLastCalledWith({ scope: 'openid', audience: 'api' });
        expect(screen.queryByText('Keys must be unique')).toBeNull();
    });

    it('syncs rows when the value prop changes without remounting', () => {
        const onChange = jest.fn();

        const { rerender } = render(<AdditionalClientMetadataField value={{ scope: 'openid' }} onChange={onChange} />);

        expect(screen.getByDisplayValue('scope')).not.toBeNull();
        expect(screen.getByDisplayValue('openid')).not.toBeNull();

        rerender(<AdditionalClientMetadataField value={{ audience: 'api' }} onChange={onChange} />);

        expect(screen.getByDisplayValue('audience')).not.toBeNull();
        expect(screen.getByDisplayValue('api')).not.toBeNull();
        expect(screen.queryByDisplayValue('scope')).toBeNull();
    });
});
