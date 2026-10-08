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

import { ModuleUpdating } from './ModuleUpdating';

describe('ModuleUpdating', () => {
    it('should name the app and say it will open by itself', () => {
        render(<ModuleUpdating moduleName="Agent Management" />);

        const message = screen.getByRole('status');
        expect(message.textContent).toContain("Agent Management isn't ready yet");
        expect(message.textContent).toContain('will open it here as soon as');
    });

    it('should title the page, as the module has not rendered its own heading', () => {
        render(<ModuleUpdating moduleName="Agent Management" />);

        expect(screen.getByRole('heading', { level: 1, name: "Agent Management isn't ready yet" })).toBeTruthy();
    });
});
