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

import { ModulesLoadFailed } from './ModulesLoadFailed';

describe('ModulesLoadFailed', () => {
    it('should say that the applications could not be loaded, and what to do', () => {
        render(<ModulesLoadFailed retrying={false} onRetry={jest.fn()} />);

        const alert = screen.getByRole('alert');
        expect(alert.textContent).toContain("Couldn't load your applications");
        expect(alert.textContent).toContain('The platform may be updating. Try again in a minute.');
    });

    it('should title the page, as no page rendered its own heading', () => {
        render(<ModulesLoadFailed retrying={false} onRetry={jest.fn()} />);

        expect(screen.getByRole('heading', { level: 1, name: "Couldn't load your applications" })).toBeTruthy();
    });

    it('should load the list again on "Retry"', async () => {
        const user = userEvent.setup();
        const onRetry = jest.fn();
        render(<ModulesLoadFailed retrying={false} onRetry={onRetry} />);

        await user.click(screen.getByRole('button', { name: 'Retry' }));

        expect(onRetry).toHaveBeenCalledTimes(1);
    });

    it('should show that a retry is running', () => {
        render(<ModulesLoadFailed retrying onRetry={jest.fn()} />);

        expect(screen.getByRole('button', { name: 'Retrying…' }).getAttribute('aria-disabled')).toBe('true');
    });
});
