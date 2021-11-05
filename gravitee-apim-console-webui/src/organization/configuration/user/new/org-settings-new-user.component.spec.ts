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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { OrgSettingsNewUserComponent } from './org-settings-new-user.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { OrganizationSettingsModule } from '../../organization-settings.module';
import { UIRouterState } from '../../../../ajs-upgraded-providers';
import { fakeIdentityProviderListItem } from '../../../../entities/identity-provider';
import { fakeUser } from '../../../../entities/user/user.fixture';

describe('OrgSettingsNewUserComponent', () => {
  let fixture: ComponentFixture<OrgSettingsNewUserComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
      providers: [{ provide: UIRouterState, useValue: { go: jest.fn() } }],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        },
      })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(OrgSettingsNewUserComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should create a user when no identity provider is configured', async () => {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities`).flush([]);
    fixture.detectChanges();

    const firstNameFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /First Name/ }));
    const firstNameInput = await firstNameFormField.getControl(MatInputHarness);
    await firstNameInput?.setValue('Bruce');

    const lastNameFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /Last Name/ }));
    const lastNameInput = await lastNameFormField.getControl(MatInputHarness);
    await lastNameInput?.setValue('Wayne');

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();

    const emailFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /Email/ }));
    const emailInput = await emailFormField.getControl(MatInputHarness);
    await emailInput?.setValue('contact@batman.com');

    expect(await saveBar.isSubmitButtonInvalid()).toBeFalsy();
    await saveBar.clickSubmit();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users`);

    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual({
      firstname: 'Bruce',
      lastname: 'Wayne',
      email: 'contact@batman.com',
    });
    req.flush(fakeUser());
  });

  it('should create a user when multiple identity providers are configured', async () => {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities`).flush([
      fakeIdentityProviderListItem({
        id: 'google',
        type: 'GOOGLE',
        name: 'Google',
      }),
      fakeIdentityProviderListItem({
        id: 'github',
        type: 'GITHUB',
        name: 'GitHub',
      }),
    ]);
    fixture.detectChanges();

    const firstNameFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^First Name/ }));
    const firstNameInput = await firstNameFormField.getControl(MatInputHarness);
    await firstNameInput?.setValue('Bruce');

    const lastNameFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^Last Name/ }));
    const lastNameInput = await lastNameFormField.getControl(MatInputHarness);
    await lastNameInput?.setValue('Wayne');

    const emailFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^Email/ }));
    const emailInput = await emailFormField.getControl(MatInputHarness);
    await emailInput?.setValue('contact@batman.com');

    const idpFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^Name/ }));
    const idpSelect = await idpFormField.getControl(MatSelectHarness);
    await idpSelect.clickOptions({ text: 'GitHub' });

    const idpIdFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /^Identifier/ }));
    const idpIdInput = await idpIdFormField.getControl(MatInputHarness);
    await idpIdInput?.setValue('idp-id');

    const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await gioSaveBar.isSubmitButtonInvalid()).toBeFalsy();
    await gioSaveBar.clickSubmit();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users`);

    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual({
      firstname: 'Bruce',
      lastname: 'Wayne',
      email: 'contact@batman.com',
      source: 'github',
      sourceId: 'idp-id',
    });
    req.flush(fakeUser());
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
