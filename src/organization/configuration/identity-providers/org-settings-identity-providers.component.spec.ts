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

import { OrgSettingsIdentityProvidersComponent } from './org-settings-identity-providers.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import { fakeIdentityProviderListItem } from '../../../entities/identity-provider/identityProviderListItem.fixture';
import { fakeIdentityProviderActivation } from '../../../entities/identity-provider';

describe('OrgSettingsIdentityProvidersComponent', () => {
  let fixture: ComponentFixture<OrgSettingsIdentityProvidersComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, OrganizationSettingsModule, GioHttpTestingModule],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
      },
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrgSettingsIdentityProvidersComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should createComponent', () => {
    const consoleSettings: ConsoleSettings = {
      authentication: {
        localLogin: { enabled: true },
      },
    };
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`).flush(consoleSettings);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/identities`).flush([fakeIdentityProviderActivation()]);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities`).flush([]);

    expect(fixture.componentInstance).toBeDefined();
  });

  it('update console settings with toggling the input', async () => {
    const consoleSettings: ConsoleSettings = {
      authentication: {
        localLogin: { enabled: true },
      },
    };
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`).flush(consoleSettings);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/identities`).flush([fakeIdentityProviderActivation()]);
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities`).flush([
      fakeIdentityProviderListItem({
        enabled: true,
      }),
    ]);

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
});
