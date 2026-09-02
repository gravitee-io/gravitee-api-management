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

import { PlatformNoAccessPage } from './PlatformNoAccessPage';

describe('PlatformNoAccessPage', () => {
    it('tells the user they do not have access and to contact an administrator', () => {
        render(<PlatformNoAccessPage />);

        expect(screen.getByRole('heading', { name: "You don't have access here" })).not.toBeNull();
        expect(screen.getByText(/assign an environment or organization role/i)).not.toBeNull();
    });

    it('explains what environment and organization roles unlock', () => {
        render(<PlatformNoAccessPage />);

        expect(screen.getByText('Why these menus are empty')).not.toBeNull();
        expect(screen.getByText('Without an environment or organization role')).not.toBeNull();
        expect(screen.getByText('With an environment or organization role')).not.toBeNull();
        expect(screen.getByText('No menus')).not.toBeNull();
        expect(screen.getByText('Menus appear')).not.toBeNull();
        expect(screen.getByText('Ask your administrator')).not.toBeNull();
        expect(screen.getByText('Typical USER access')).not.toBeNull();
        expect(screen.getByText('Typical ADMIN access')).not.toBeNull();
    });
});
