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

import { ApplicationsPageHeader } from './ApplicationsPageHeader';

describe('ApplicationsPageHeader', () => {
    it('shows the Register Application button when canCreate is true', () => {
        render(<ApplicationsPageHeader canCreate onRegisterApplication={jest.fn()} />);
        expect(screen.queryByRole('button', { name: /Register Application/i })).not.toBeNull();
    });

    it('hides the Register Application button when canCreate is false', () => {
        render(<ApplicationsPageHeader canCreate={false} onRegisterApplication={jest.fn()} />);
        expect(screen.queryByRole('button', { name: /Register Application/i })).toBeNull();
    });

    it('calls onRegisterApplication when the create button is clicked', () => {
        const onRegisterApplication = jest.fn();
        render(<ApplicationsPageHeader canCreate onRegisterApplication={onRegisterApplication} />);
        fireEvent.click(screen.getByRole('button', { name: /Register Application/i }));
        expect(onRegisterApplication).toHaveBeenCalled();
    });
});
