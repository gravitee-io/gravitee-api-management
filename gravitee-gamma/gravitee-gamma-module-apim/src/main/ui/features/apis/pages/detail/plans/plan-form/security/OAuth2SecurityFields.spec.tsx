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

const EXPRESSION_HINT = "Expressions aren't supported for API Product plans. Choose a resource from the product's Resources page.";
const DISABLED_HINT =
    "This resource is disabled. Enable it on the product's Resources page. A plan that uses a disabled resource can't be published or saved once published.";

const PRODUCT_RESOURCES: ApiResourceOption[] = [
    { name: 'prod-oauth', type: 'oauth2', enabled: true },
    { name: 'prod-oauth-off', type: 'oauth2', enabled: false },
    { name: 'prod-cache', type: 'cache', enabled: true },
    { name: 'prod-cache-off', type: 'cache', enabled: false },
];

function Harness({
    resourceOptions,
    initial,
    resourceOwner,
    resourcesKnown,
    error,
}: {
    resourceOptions?: ApiResourceOption[];
    initial?: Partial<OAuth2Config>;
    resourceOwner?: 'API' | 'API_PRODUCT';
    resourcesKnown?: boolean;
    error?: string;
}) {
    const [value, setValue] = useState<OAuth2Config>({ ...DEFAULT_OAUTH2_CONFIG, ...initial });
    return (
        <OAuth2SecurityFields
            value={value}
            onChange={setValue}
            resourceOptions={resourceOptions}
            resourceOwner={resourceOwner}
            resourcesKnown={resourcesKnown}
            error={error}
        />
    );
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

    it('still treats a padded API resource name as that resource', () => {
        render(<Harness resourceOptions={RESOURCES} initial={{ oauthResource: 'am-authz ' }} />);

        expect(screen.queryByText(/Resource does not exist/i)).not.toBeInTheDocument();
    });

    it('does not warn for an EL expression', () => {
        render(<Harness resourceOptions={RESOURCES} initial={{ oauthResource: "{#api.properties['authz']}" }} />);

        expect(screen.getByText(/Supports EL/)).toBeInTheDocument();
        expect(screen.queryByText(EXPRESSION_HINT)).not.toBeInTheDocument();
        expect(screen.queryByText(/Resource does not exist/i)).not.toBeInTheDocument();
    });

    it('does not mark an API resource disabled when enabled is omitted', async () => {
        const user = userEvent.setup();
        render(<Harness resourceOptions={RESOURCES} initial={{ oauthResource: 'am-authz' }} />);

        await user.click(screen.getByLabelText(/OAuth2 resource/i));

        expect(within(await screen.findByRole('listbox')).queryByText('Disabled')).not.toBeInTheDocument();
        expect(screen.queryByText(DISABLED_HINT)).not.toBeInTheDocument();
    });

    it('lists only the product OAuth2 resources and only its cache resources', async () => {
        const user = userEvent.setup();
        const productResources: ApiResourceOption[] = [
            { name: 'prod-oauth', type: 'oauth2' },
            { name: 'prod-oauth-off', type: 'oauth2' },
            { name: 'prod-cache', type: 'cache' },
            { name: 'prod-redis', type: 'cache-redis' },
            { name: 'prod-auth', type: 'auth-provider-inline-resource' },
            { name: 'api-only-oauth', type: 'oauth2' },
        ];
        render(
            <Harness
                resourceOptions={productResources.filter(resource => !resource.name.startsWith('api-only'))}
                resourceOwner="API_PRODUCT"
            />,
        );

        await user.click(screen.getByLabelText(/OAuth2 resource/i));
        const oauthList = await screen.findByRole('listbox');
        expect(within(oauthList).getByText('prod-oauth')).toBeInTheDocument();
        expect(within(oauthList).getByText('prod-oauth-off')).toBeInTheDocument();
        expect(within(oauthList).queryByText('prod-cache')).not.toBeInTheDocument();
        expect(within(oauthList).queryByText('prod-auth')).not.toBeInTheDocument();
        expect(within(oauthList).queryByText('api-only-oauth')).not.toBeInTheDocument();

        await user.click(screen.getByLabelText(/Cache resource/i));
        const cacheList = await screen.findByRole('listbox');
        expect(within(cacheList).getByText('prod-cache')).toBeInTheDocument();
        expect(within(cacheList).getByText('prod-redis')).toBeInTheDocument();
        expect(within(cacheList).queryByText('prod-oauth')).not.toBeInTheDocument();
        expect(within(cacheList).queryByText('api-only-oauth')).not.toBeInTheDocument();
    });

    it('tells the user to add an OAuth2 resource on the API Product when the product has none', () => {
        render(<Harness resourceOptions={[{ name: 'prod-cache', type: 'cache' }]} resourceOwner="API_PRODUCT" />);

        expect(screen.getByText("No OAuth2 resource on this API Product. Add one on the product's Resources page.")).toBeInTheDocument();
        expect(screen.queryByText(/Add it to the API/i)).not.toBeInTheDocument();
    });

    it('does not say an API Product OAuth2 resource supports EL', () => {
        render(<Harness resourceOptions={PRODUCT_RESOURCES} resourceOwner="API_PRODUCT" />);

        expect(screen.queryByText(/Supports EL/)).not.toBeInTheDocument();
        expect(screen.getByText(/this API Product's Resources page\./)).toBeInTheDocument();
    });

    it('warns when the product OAuth2 resource is an expression', () => {
        render(<Harness resourceOptions={PRODUCT_RESOURCES} resourceOwner="API_PRODUCT" initial={{ oauthResource: '{#x}' }} />);

        expect(screen.getByText(EXPRESSION_HINT)).toBeInTheDocument();
        expect(screen.getByLabelText(/OAuth2 resource/i)).toHaveAttribute('aria-describedby', 'oauth2-resource-expression');
        expect(screen.queryByText(/Resource does not exist/i)).not.toBeInTheDocument();
    });

    it('warns when the product cache resource is an expression', () => {
        render(<Harness resourceOptions={PRODUCT_RESOURCES} resourceOwner="API_PRODUCT" initial={{ oauthCacheResource: '{ #x}' }} />);

        expect(screen.getByText(EXPRESSION_HINT)).toBeInTheDocument();
        expect(screen.getByLabelText(/Cache resource/i)).toHaveAttribute('aria-describedby', 'oauth2-cache-resource-expression');
    });

    it('marks a disabled product resource and warns when it is selected', async () => {
        const user = userEvent.setup();
        render(<Harness resourceOptions={PRODUCT_RESOURCES} resourceOwner="API_PRODUCT" />);

        await user.click(screen.getByLabelText(/OAuth2 resource/i));
        const oauthList = await screen.findByRole('listbox');
        expect(within(oauthList).getByRole('option', { name: /prod-oauth-off/ })).toHaveTextContent('Disabled');
        const enabledOption = within(oauthList)
            .getAllByRole('option')
            .find(option => option.textContent?.includes('prod-oauth') && !option.textContent?.includes('prod-oauth-off'));
        expect(enabledOption).toBeDefined();
        expect(enabledOption).not.toHaveTextContent('Disabled');

        await user.click(within(oauthList).getByRole('option', { name: /prod-oauth-off/ }));
        expect(screen.getByText(DISABLED_HINT)).toBeInTheDocument();
        expect(screen.getByLabelText(/OAuth2 resource/i)).toHaveAttribute('aria-describedby', 'oauth2-resource-disabled');

        await user.click(screen.getByLabelText(/Cache resource/i));
        const cacheList = await screen.findByRole('listbox');
        expect(within(cacheList).getByRole('option', { name: /prod-cache-off/ })).toHaveTextContent('Disabled');
        await user.click(within(cacheList).getByRole('option', { name: /prod-cache-off/ }));
        expect(screen.getAllByText(DISABLED_HINT)).toHaveLength(2);
        expect(screen.getByLabelText(/Cache resource/i)).toHaveAttribute('aria-describedby', 'oauth2-cache-resource-disabled');
    });

    it('shows no hint for an enabled product resource', () => {
        render(
            <Harness
                resourceOptions={PRODUCT_RESOURCES}
                resourceOwner="API_PRODUCT"
                initial={{ oauthResource: 'prod-oauth', oauthCacheResource: 'prod-cache' }}
            />,
        );

        expect(screen.queryByText(EXPRESSION_HINT)).not.toBeInTheDocument();
        expect(screen.queryByText(DISABLED_HINT)).not.toBeInTheDocument();
        expect(screen.queryByText(/Resource does not exist/i)).not.toBeInTheDocument();
        expect(
            screen.queryByText("No OAuth2 resource on this API Product. Add one on the product's Resources page."),
        ).not.toBeInTheDocument();
    });

    it('treats a padded product resource name as missing, including a padded disabled name', () => {
        render(
            <Harness
                resourceOptions={PRODUCT_RESOURCES}
                resourceOwner="API_PRODUCT"
                initial={{ oauthResource: 'prod-oauth ', oauthCacheResource: 'prod-cache-off ' }}
            />,
        );

        expect(
            screen.getAllByText("Resource does not exist. Add it on the product's Resources page for it to be taken into account."),
        ).toHaveLength(2);
        expect(screen.queryByText(DISABLED_HINT)).not.toBeInTheDocument();
    });

    it('still warns when a product resource name is missing', () => {
        render(<Harness resourceOptions={PRODUCT_RESOURCES} resourceOwner="API_PRODUCT" initial={{ oauthResource: 'missing-one' }} />);

        expect(
            screen.getByText("Resource does not exist. Add it on the product's Resources page for it to be taken into account."),
        ).toBeInTheDocument();
        expect(screen.queryByText(EXPRESSION_HINT)).not.toBeInTheDocument();
        expect(screen.queryByText(DISABLED_HINT)).not.toBeInTheDocument();
    });

    it('prefers the external error over every other hint', () => {
        render(
            <Harness
                resourceOptions={PRODUCT_RESOURCES}
                resourceOwner="API_PRODUCT"
                error="OAuth2 resource is required."
                initial={{ oauthResource: '{#x}' }}
            />,
        );

        expect(screen.getByText('OAuth2 resource is required.')).toBeInTheDocument();
        expect(screen.getByLabelText(/OAuth2 resource/i)).toHaveAttribute('aria-describedby', 'oauth2-resource-error');
        expect(screen.queryByText(EXPRESSION_HINT)).not.toBeInTheDocument();
    });

    it('prefers the empty-product hint over an expression', () => {
        render(
            <Harness
                resourceOptions={[{ name: 'prod-cache', type: 'cache', enabled: true }]}
                resourceOwner="API_PRODUCT"
                initial={{ oauthResource: '{#x}' }}
            />,
        );

        expect(screen.getByText("No OAuth2 resource on this API Product. Add one on the product's Resources page.")).toBeInTheDocument();
        expect(screen.queryByText(EXPRESSION_HINT)).not.toBeInTheDocument();
    });

    it('prefers the expression hint over a disabled resource of the same characters', () => {
        render(
            <Harness
                resourceOptions={[...PRODUCT_RESOURCES, { name: '{#x}', type: 'oauth2', enabled: false }]}
                resourceOwner="API_PRODUCT"
                initial={{ oauthResource: '{#x}', oauthCacheResource: 'missing-cache' }}
            />,
        );

        expect(screen.getByText(EXPRESSION_HINT)).toBeInTheDocument();
        expect(screen.queryByText(DISABLED_HINT)).not.toBeInTheDocument();
        expect(screen.getByLabelText(/OAuth2 resource/i)).toHaveAttribute('aria-describedby', 'oauth2-resource-expression');
        expect(
            screen.getByText("Resource does not exist. Add it on the product's Resources page for it to be taken into account."),
        ).toBeInTheDocument();
        expect(screen.getByLabelText(/Cache resource/i)).toHaveAttribute('aria-describedby', 'oauth2-cache-resource-missing');
    });
});
