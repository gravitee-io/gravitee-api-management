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

import { AddButton } from './AddButton';

describe('AddButton', () => {
    it('should render with md hit area by default', () => {
        render(<AddButton aria-label="Add item" />);

        const button = screen.getByRole('button', { name: 'Add item' });
        expect(button).toHaveClass('md');
    });

    it('should render sm size when requested', () => {
        render(<AddButton aria-label="Add item" size="sm" />);

        expect(screen.getByRole('button', { name: 'Add item' })).toHaveClass('sm');
    });

    it('should call onClick when clicked', async () => {
        const user = userEvent.setup();
        const onClick = jest.fn();

        render(<AddButton aria-label="Add item" onClick={onClick} />);

        await user.click(screen.getByRole('button', { name: 'Add item' }));
        expect(onClick).toHaveBeenCalled();
    });
});
