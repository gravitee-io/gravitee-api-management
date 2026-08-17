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

import { TenantsEmptyState } from './TenantsEmptyState';

describe('TenantsEmptyState', () => {
    it('explains why to create a tenant and compares with/without', () => {
        render(<TenantsEmptyState canCreate />);

        expect(screen.getByText('Why create a tenant?')).not.toBeNull();
        expect(screen.getByText('Without tenants')).not.toBeNull();
        expect(screen.getByText('With tenants')).not.toBeNull();
        expect(screen.getByText('Every gateway proxies every endpoint on the API')).not.toBeNull();
        expect(screen.getByText('Tag a gateway with one tenant in gravitee.yml')).not.toBeNull();
        expect(screen.getByText('Tag the gateway')).not.toBeNull();
        expect(screen.getByText('Pin an endpoint')).not.toBeNull();
        expect(screen.getByText('Keep traffic local')).not.toBeNull();
        expect(screen.getByText('Create the first tenant, then paste its key into gravitee.yml.')).not.toBeNull();
    });

    it('does not include an Add a tenant button; the page header owns that CTA', () => {
        render(<TenantsEmptyState canCreate />);
        expect(screen.queryByRole('button', { name: /Add a tenant/i })).toBeNull();
    });

    it('drops the create prompt for a user who cannot create tenants', () => {
        render(<TenantsEmptyState />);

        expect(screen.getByText('Why create a tenant?')).not.toBeNull();
        expect(screen.queryByText('Create the first tenant, then paste its key into gravitee.yml.')).toBeNull();
    });
});
