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

import { GroupTooManyUsersDialog } from './GroupTooManyUsersDialog';

describe('GroupTooManyUsersDialog', () => {
    it('explains the ambiguous email match', () => {
        render(<GroupTooManyUsersDialog open email="user@example.com" onClose={jest.fn()} onContinue={jest.fn()} />);

        expect(screen.getByText(/user@example\.com/)).not.toBeNull();
        expect(screen.getByText(/select and add a specific user/)).not.toBeNull();
    });

    it('continues to user search', () => {
        const onContinue = jest.fn();
        render(<GroupTooManyUsersDialog open email="user@example.com" onClose={jest.fn()} onContinue={onContinue} />);

        fireEvent.click(screen.getByRole('button', { name: 'Continue' }));

        expect(onContinue).toHaveBeenCalledTimes(1);
    });

    it('closes without continuing', () => {
        const onClose = jest.fn();
        const onContinue = jest.fn();
        render(<GroupTooManyUsersDialog open email="user@example.com" onClose={onClose} onContinue={onContinue} />);

        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(onClose).toHaveBeenCalledTimes(1);
        expect(onContinue).not.toHaveBeenCalled();
    });
});
