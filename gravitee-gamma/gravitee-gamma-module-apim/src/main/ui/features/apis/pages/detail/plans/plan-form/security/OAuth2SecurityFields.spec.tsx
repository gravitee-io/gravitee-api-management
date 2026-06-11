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
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';

import { DEFAULT_OAUTH2_CONFIG, OAuth2SecurityFields } from './OAuth2SecurityFields';
import type { OAuth2Config } from './OAuth2SecurityFields';
import type { ApiResourceOption } from './ResourceSelectInput';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

const RESOURCES: ApiResourceOption[] = [
    { name: 'am-authz', type: 'oauth2-am-resource' },
    { name: 'generic-authz', type: 'oauth2-generic-resource' },
    { name: 'token-cache', type: 'cache' },
];

function Harness({ resourceOptions, initial }: { resourceOptions?: ApiResourceOption[]; initial?: Partial<OAuth2Config> }) {
    const [value, setValue] = useState<OAuth2Config>({ ...DEFAULT_OAUTH2_CONFIG, ...initial });
    return <OAuth2SecurityFields value={value} onChange={setValue} resourceOptions={resourceOptions} />;
}

describe('OAuth2SecurityFields resource pickers', () => {
    it('suggests only oauth2-typed resources for the OAuth2 resource field', async () => {
        const user = userEvent.setup();
        render(<Harness resourceOptions={RESOURCES} />);

        await user.click(screen.getByLabelText(/OAuth2 resource/i));

        const listbox = await screen.findByRole('listbox');
        expect(within(listbox).getByText('am-authz')).toBeInTheDocument();
        expect(within(listbox).getByText('generic-authz')).toBeInTheDocument();
        // Cache resource must not appear in the oauth2 field.
        expect(within(listbox).queryByText('token-cache')).not.toBeInTheDocument();
    });

    it('selects a suggested resource by name', async () => {
        const user = userEvent.setup();
        render(<Harness resourceOptions={RESOURCES} />);

        const input = screen.getByLabelText(/OAuth2 resource/i);
        await user.click(input);
        await user.click(await screen.findByText('am-authz'));

        expect(input).toHaveValue('am-authz');
    });

    it('warns when the typed resource is not configured on the API', async () => {
        const user = userEvent.setup();
        render(<Harness resourceOptions={RESOURCES} />);

        await user.type(screen.getByLabelText(/OAuth2 resource/i), 'missing-one');

        expect(screen.getByText(/Resource does not exist/i)).toBeInTheDocument();
    });

    it('does not warn for an EL expression', () => {
        render(<Harness resourceOptions={RESOURCES} initial={{ oauthResource: "{#api.properties['authz']}" }} />);

        expect(screen.queryByText(/Resource does not exist/i)).not.toBeInTheDocument();
    });
});
