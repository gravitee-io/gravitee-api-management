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

import { ClientRegistrationEmptyProviders } from './ClientRegistrationEmptyProviders';

describe('ClientRegistrationEmptyProviders', () => {
    it('explains why a DCR provider is needed', () => {
        render(<ClientRegistrationEmptyProviders />);

        expect(screen.getByText('Why add a DCR provider?')).not.toBeNull();
        expect(screen.getByText('Without a provider')).not.toBeNull();
        expect(screen.getByText('With a provider')).not.toBeNull();
        expect(screen.getByText('Point at the IdP')).not.toBeNull();
        expect(screen.getByText(/Register on create/)).not.toBeNull();
    });
});
