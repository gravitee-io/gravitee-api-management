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
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { DangerZoneCard } from './DangerZoneCard';

const consoleAuth = { externalAuth: { enabled: false } };

describe('DangerZoneCard', () => {
    it('should keep confirm disabled when the display name is empty and nothing is typed', async () => {
        const user = userEvent.setup();
        render(
            <DangerZoneCard consoleAuth={consoleAuth} primaryOwner={false} displayName="" deleting={false} onDelete={() => undefined} />,
        );

        await user.click(screen.getByRole('button', { name: 'Delete my account' }));
        const dialog = screen.getByRole('dialog');
        expect((within(dialog).getByRole('button', { name: 'Yes, delete my account' }) as HTMLButtonElement).disabled).toBe(true);
    });
});
