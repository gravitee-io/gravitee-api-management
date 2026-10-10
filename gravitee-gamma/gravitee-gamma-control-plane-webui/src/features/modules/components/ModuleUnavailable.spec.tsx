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

import { ModuleUnavailable } from './ModuleUnavailable';

describe('ModuleUnavailable', () => {
    it('should name the app that is not available', () => {
        render(<ModuleUnavailable moduleName="Agent Management" onReload={jest.fn()} />);

        expect(screen.getByRole('alert').textContent).toContain("Agent Management isn't available right now");
    });

    it('should title the page, as the module did not render its own heading', () => {
        render(<ModuleUnavailable moduleName="Agent Management" onReload={jest.fn()} />);

        expect(screen.getByRole('heading', { level: 1, name: "Agent Management isn't available right now" })).toBeTruthy();
    });

    it('should reload the page from its button', async () => {
        const user = userEvent.setup();
        const onReload = jest.fn();
        render(<ModuleUnavailable moduleName="Agent Management" onReload={onReload} />);

        await user.click(screen.getByRole('button', { name: 'Reload page' }));

        expect(onReload).toHaveBeenCalledTimes(1);
    });
});
