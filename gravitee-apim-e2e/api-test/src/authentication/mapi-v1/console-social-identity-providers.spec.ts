/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { afterAll, beforeAll, describe, expect, it } from '@jest/globals';
import { faker } from '@faker-js/faker';

import { ANONYMOUS, forManagement, forManagementAsAdminUser } from '@gravitee/utils/configuration';
import { ConfigurationApi } from '../../../../lib/management-webclient-sdk/src/lib/apis/ConfigurationApi';
import { OrganizationApi } from '../../../../lib/management-webclient-sdk/src/lib/apis/OrganizationApi';
import { PortalApi } from '../../../../lib/management-webclient-sdk/src/lib/apis/PortalApi';
import { created, noContent, succeed } from '@lib/jest-utils';

const orgId = 'DEFAULT';

const configurationApiAsAdmin = new ConfigurationApi(forManagementAsAdminUser());
const organizationApiAsAdmin = new OrganizationApi(forManagementAsAdminUser());
const portalApiAsAnonymous = new PortalApi(forManagement(ANONYMOUS));

/**
 * The Console login page reads GET /organizations/{orgId}/social-identities unauthenticated to
 * decide which SSO buttons to render. "enabled" governs Portal visibility only, so an identity
 * provider activated on the organization must appear here even when it is disabled — otherwise an
 * organization whose local login is off is locked out of its own Console.
 */
describe('Console social identity providers', () => {
  let identityProviderId: string;

  beforeAll(async () => {
    const identityProvider = await created(
      configurationApiAsAdmin.createIdentityProviderRaw({
        orgId,
        newIdentityProviderEntity: {
          name: `e2e-console-sso-${faker.string.alphanumeric(8)}`,
          type: 'GOOGLE',
          enabled: false,
          configuration: { clientId: 'a-client-id', clientSecret: 'a-client-secret' },
        },
      }),
    );
    identityProviderId = identityProvider.id;

    await noContent(
      organizationApiAsAdmin.updateOrganizationIdentitiesRaw({
        orgId,
        identityProviderActivationEntity: [{ identityProvider: identityProviderId }],
      }),
    );
  });

  afterAll(async () => {
    await organizationApiAsAdmin.updateOrganizationIdentities({ orgId, identityProviderActivationEntity: [] });
    await configurationApiAsAdmin.deleteIdentityProvider({ orgId, identityProvider: identityProviderId });
  });

  it('should list an identity provider that is disabled but activated on the organization', async () => {
    const socialIdentityProviders = await succeed(portalApiAsAnonymous.getSocialIdentityProviders1Raw({ orgId }));

    expect(socialIdentityProviders.map((idp) => idp.id)).toContain(identityProviderId);
  });

  it('should stop listing it once it is deactivated on the organization', async () => {
    await organizationApiAsAdmin.updateOrganizationIdentities({ orgId, identityProviderActivationEntity: [] });

    const socialIdentityProviders = await succeed(portalApiAsAnonymous.getSocialIdentityProviders1Raw({ orgId }));

    expect(socialIdentityProviders.map((idp) => idp.id)).not.toContain(identityProviderId);
  });
});
