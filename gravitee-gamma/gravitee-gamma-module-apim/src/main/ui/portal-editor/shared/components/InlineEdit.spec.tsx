/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { InlineEdit } from './InlineEdit';

describe('InlineEdit', () => {
    it('should render read-only text when not editable', () => {
        render(<InlineEdit value="Developer Portal" editable={false} onChange={jest.fn()} />);

        expect(screen.getByText('Developer Portal')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Portal name' })).not.toBeInTheDocument();
    });

    it('should enter edit mode on click and save on Enter', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        render(<InlineEdit value="Developer Portal" editable onChange={onChange} ariaLabel="Portal name" />);

        await user.click(screen.getByRole('button', { name: 'Portal name' }));
        const input = screen.getByRole('textbox', { name: 'Portal name' });
        await user.clear(input);
        await user.type(input, 'Payments Portal{Enter}');

        expect(onChange).toHaveBeenCalledWith('Payments Portal');
    });

    it('should allow clearing the value to empty', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        render(
            <InlineEdit
                value="Developer Portal"
                editable
                onChange={onChange}
                ariaLabel="Portal name"
                placeholder="Developer Portal"
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Portal name' }));
        const input = screen.getByRole('textbox', { name: 'Portal name' });
        await user.clear(input);
        await user.keyboard('{Enter}');

        expect(onChange).toHaveBeenCalledWith('');
    });

    it('should not show placeholder text in read-only mode when the value is empty', () => {
        render(
            <InlineEdit
                value=""
                editable={false}
                onChange={jest.fn()}
                ariaLabel="Portal name"
                placeholder="Developer Portal"
            />,
        );

        expect(screen.queryByText('Developer Portal')).not.toBeInTheDocument();
    });

    it('should show a clickable placeholder when the value is empty', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        render(
            <InlineEdit
                value=""
                editable
                onChange={onChange}
                ariaLabel="Portal name"
                placeholder="Developer Portal"
            />,
        );

        const trigger = screen.getByRole('button', { name: 'Portal name' });
        expect(trigger).toHaveTextContent('Developer Portal');

        await user.click(trigger);

        expect(screen.getByRole('textbox', { name: 'Portal name' })).toBeInTheDocument();
    });

    it('should enter edit mode on double click when activateOn is doubleClick', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        render(
            <InlineEdit
                value="Home"
                editable
                activateOn="doubleClick"
                onChange={onChange}
                ariaLabel="Edit Home"
            />,
        );

        await user.dblClick(screen.getByLabelText('Edit Home'));
        const input = screen.getByRole('textbox', { name: 'Edit Home' });
        await user.clear(input);
        await user.type(input, 'Welcome{Enter}');

        expect(onChange).toHaveBeenCalledWith('Welcome');
    });

    it('should not enter edit mode on single click when activateOn is doubleClick', async () => {
        const user = userEvent.setup();

        render(
            <InlineEdit
                value="Home"
                editable
                activateOn="doubleClick"
                onChange={jest.fn()}
                ariaLabel="Edit Home"
            />,
        );

        await user.click(screen.getByLabelText('Edit Home'));

        expect(screen.queryByRole('textbox', { name: 'Edit Home' })).not.toBeInTheDocument();
    });

    it('should cancel edits on Escape', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        render(<InlineEdit value="Developer Portal" editable onChange={onChange} ariaLabel="Portal name" />);

        await user.click(screen.getByRole('button', { name: 'Portal name' }));
        const input = screen.getByRole('textbox', { name: 'Portal name' });
        await user.clear(input);
        await user.type(input, 'Changed');
        await user.keyboard('{Escape}');

        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByRole('button', { name: 'Portal name' })).toHaveTextContent('Developer Portal');
    });
});
