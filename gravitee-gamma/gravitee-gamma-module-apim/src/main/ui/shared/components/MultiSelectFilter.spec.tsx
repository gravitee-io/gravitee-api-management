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

import { MultiSelectFilter } from './MultiSelectFilter';

describe('MultiSelectFilter', () => {
    it('keeps a host-safe scrollable max-height on the options list', async () => {
        const user = userEvent.setup();
        render(
            <MultiSelectFilter
                placeholder="Select"
                ariaLabel="Tags"
                options={[
                    { value: 'a', label: 'Alpha' },
                    { value: 'b', label: 'Beta' },
                ]}
                selectedValues={[]}
                onSelectedValuesChange={() => {}}
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Tags' }));

        const optionsList = screen.getByText('Alpha').closest('div');
        expect(optionsList?.className).toContain('max-h-48');
        expect(optionsList?.className).toContain('overflow-y-auto');
        expect(optionsList?.className).not.toContain('max-h-[200px]');
    });

    it('stays open so multiple values can be toggled without reopening', async () => {
        const user = userEvent.setup();
        const onSelectedValuesChange = jest.fn();
        const { rerender } = render(
            <MultiSelectFilter
                placeholder="Select"
                ariaLabel="Status"
                options={[
                    { value: 'a', label: 'Accepted' },
                    { value: 'b', label: 'Pending' },
                    { value: 'c', label: 'Closed' },
                ]}
                selectedValues={[]}
                onSelectedValuesChange={onSelectedValuesChange}
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Status' }));
        await user.click(screen.getByText('Accepted'));
        expect(onSelectedValuesChange).toHaveBeenCalledWith(['a']);

        rerender(
            <MultiSelectFilter
                placeholder="Select"
                ariaLabel="Status"
                options={[
                    { value: 'a', label: 'Accepted' },
                    { value: 'b', label: 'Pending' },
                    { value: 'c', label: 'Closed' },
                ]}
                selectedValues={['a']}
                onSelectedValuesChange={onSelectedValuesChange}
            />,
        );

        expect(screen.getByText('Pending')).toBeInTheDocument();
        await user.click(screen.getByText('Pending'));
        expect(onSelectedValuesChange).toHaveBeenLastCalledWith(['a', 'b']);
        expect(screen.getByText('Closed')).toBeInTheDocument();
    });
});
