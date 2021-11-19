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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { OrgSettingsIdentityProvidersComponent } from './org-settings-identity-providers.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import {
  fakeIdentityProviderActivation,
  fakeIdentityProviderListItem,
  IdentityProviderActivation,
  IdentityProviderListItem,
} from '../../../entities/identity-provider';
import { CurrentUserService, UIRouterState } from '../../../ajs-upgraded-providers';
import { User } from '../../../entities/user';

describe('OrgSettingsIdentityProvidersComponent', () => {
  const currentUser = new User();
  currentUser.userPermissions = [];
  currentUser.userApiPermissions = [];
  currentUser.userEnvironmentPermissions = [];
  currentUser.userApplicationPermissions = [];

  let fixture: ComponentFixture<OrgSettingsIdentityProvidersComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  let httpTestingController: HttpTestingController;
  const fake$State = {
    go: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, OrganizationSettingsModule, GioHttpTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fake$State },
        {
          provide: CurrentUserService,
          useValue: { currentUser },
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
      },
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrgSettingsIdentityProvidersComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('update console settings with toggling the input', async () => {
    const consoleSettings: ConsoleSettings = {
      authentication: {
        localLogin: { enabled: true },
      },
    };

    flushResponseToInitialRequests(
      consoleSettings,
      [
        fakeIdentityProviderListItem({
          enabled: true,
        }),
      ],
      [fakeIdentityProviderActivation()],
    );

    const activateLoginSlideToggle = await loader.getHarness(
      MatSlideToggleHarness.with({ label: 'Show login form on management console' }),
    );
    await activateLoginSlideToggle.toggle();

    const expectedConsoleSettings = {
      authentication: {
        localLogin: { enabled: false },
      },
    };

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toMatchObject(expectedConsoleSettings);
    req.flush(expectedConsoleSettings);
  });

  describe('onDeleteActionClicked', () => {
    beforeEach(() => {
      currentUser.userPermissions = ['organization-identity_provider-d'];
    });

    it('sends a DELETE request', async () => {
      const consoleSettings: ConsoleSettings = {
        authentication: {
          localLogin: { enabled: true },
        },
      };

      flushResponseToInitialRequests(
        consoleSettings,
        [
          fakeIdentityProviderListItem({
            id: 'gravitee-am',
          }),
        ],
        [
          fakeIdentityProviderActivation({
            identityProvider: 'gravitee-am',
          }),
        ],
      );

      const activateLoginSlideToggle = await getActionButtonWithAriaLabel('Button to delete an identity provider');
      await activateLoginSlideToggle.click();

      // Use rootLoader to find the Delete button inside the dialog
      const confirmDialogButton = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Delete' }));
      await confirmDialogButton.click();

      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities/gravitee-am`).flush(null);

      flushResponseToInitialRequests(consoleSettings, [], []);
    });
  });

  describe('onEditActionClicked', () => {
    it('triggers AngularJS router', async () => {
      const consoleSettings: ConsoleSettings = {
        authentication: {
          localLogin: { enabled: true },
        },
      };

      flushResponseToInitialRequests(
        consoleSettings,
        [
          fakeIdentityProviderListItem({
            id: 'gravitee-am',
          }),
        ],
        [
          fakeIdentityProviderActivation({
            identityProvider: 'gravitee-am',
          }),
        ],
      );

      const activateLoginSlideToggle = await getActionButtonWithAriaLabel('Button to edit an identity provider');
      await activateLoginSlideToggle.click();

      expect(fake$State.go).toHaveBeenCalledWith('organization.settings.ng-identityprovider-edit', { id: 'gravitee-am' });
    });
  });

  describe('onAddIdpClicked', () => {
    it('triggers AngularJS router', async () => {
      const consoleSettings: ConsoleSettings = {
        authentication: {
          localLogin: { enabled: true },
        },
      };

      flushResponseToInitialRequests(
        consoleSettings,
        [
          fakeIdentityProviderListItem({
            id: 'gravitee-am',
          }),
        ],
        [
          fakeIdentityProviderActivation({
            identityProvider: 'gravitee-am',
          }),
        ],
      );

      const activateLoginSlideToggle = await loader.getHarness(MatButtonHarness.with({ text: /Add an identity provider/ }));
      await activateLoginSlideToggle.click();

      expect(fake$State.go).toHaveBeenCalledWith('organization.settings.ng-identityprovider-new');
    });
  });

  describe('onActivationToggleActionClicked', () => {
    beforeEach(() => {
      currentUser.userPermissions = ['organization-identity_provider_activation-u'];
    });

    it('sends a PUT request to activate an identity provider', async () => {
      const consoleSettings: ConsoleSettings = {
        authentication: {
          localLogin: { enabled: true },
        },
      };

      flushResponseToInitialRequests(
        consoleSettings,
        [
          fakeIdentityProviderListItem({
            id: 'gravitee-am',
          }),
        ],
        [],
      );

      const activateLoginSlideToggle = await getActionButtonWithAriaLabel('Switch to toggle an identity provider activation');
      await activateLoginSlideToggle.click();

      const confirmDialogButton = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Ok' }));
      await confirmDialogButton.click();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/identities`);
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual([{ identityProvider: 'gravitee-am' }]);

      req.flush(null);

      flushResponseToInitialRequests(
        consoleSettings,
        [
          fakeIdentityProviderListItem({
            id: 'gravitee-am',
          }),
        ],
        [
          fakeIdentityProviderActivation({
            identityProvider: 'gravitee-am',
          }),
        ],
      );
    });

    it('sends a PUT request to deactivate an identity provider', async () => {
      const consoleSettings: ConsoleSettings = {
        authentication: {
          localLogin: { enabled: true },
        },
      };

      flushResponseToInitialRequests(
        consoleSettings,
        [
          fakeIdentityProviderListItem({
            id: 'gravitee-am',
          }),
        ],
        [fakeIdentityProviderActivation({ identityProvider: 'gravitee-am' })],
      );

      const activateLoginSlideToggle = await getActionButtonWithAriaLabel(`Switch to toggle an identity provider activation`);
      await activateLoginSlideToggle.click();

      const confirmDialogButton = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Ok' }));
      await confirmDialogButton.click();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/identities`);
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual([]);

      req.flush(null);

      flushResponseToInitialRequests(
        consoleSettings,
        [
          fakeIdentityProviderListItem({
            id: 'gravitee-am',
          }),
        ],
        [],
      );
    });
  });

  function getActionButtonWithAriaLabel(actionAriaLabel: string): Promise<MatButtonHarness> {
    return loader.getHarness(MatButtonHarness.with({ selector: `[aria-label="${actionAriaLabel}"]` }));
  }

  function flushResponseToInitialRequests(
    consoleSettings: ConsoleSettings,
    identityProviderListItem: IdentityProviderListItem[],
    activatedIdentityProviders: IdentityProviderActivation[],
  ) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`).flush(consoleSettings);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities`).flush(identityProviderListItem);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/identities`).flush(activatedIdentityProviders);
  }
});
