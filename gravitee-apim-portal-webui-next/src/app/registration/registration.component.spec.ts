/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCardHarness } from '@angular/material/card/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Observable } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { RegistrationComponent } from './registration.component';
import { CustomUserField } from '../../entities/user/custom-user-field';
import { User } from '../../entities/user/user';
import { UsersService } from '../../services/users.service';
import { AppTestingModule } from '../../testing/app-testing.module';
import { DivHarness } from '../../testing/div.harness';

describe('RegistrationComponent', () => {
  let fixture: ComponentFixture<RegistrationComponent>;
  let harnessLoader: HarnessLoader;

  let usersServiceMock: {
    listCustomUserFields: jest.Mock;
    registerNewUser: jest.Mock;
  };

  const init = async (opts?: { customUserFields?: CustomUserField[]; register$?: Observable<User> }) => {
    usersServiceMock = {
      listCustomUserFields: jest.fn().mockReturnValue(of(opts?.customUserFields ?? [])),
      registerNewUser: jest.fn().mockReturnValue(opts?.register$ ?? of({})),
    };

    await TestBed.configureTestingModule({
      imports: [RegistrationComponent, AppTestingModule],
      providers: [{ provide: UsersService, useValue: usersServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrationComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();

    fixture.detectChanges();
    return fixture.componentInstance;
  };

  it('should display registration form by default (submitted=false)', async () => {
    await init();

    const card = await harnessLoader.getHarness(MatCardHarness.with({ selector: 'mat-card.registration__form__container' }));

    expect(await card.getTitleText()).toContain('Create account');

    const signUp = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Sign up' }));
    expect(signUp).not.toBeNull();

    const firstnameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="firstname"]' }));
    const lastnameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="lastname"]' }));
    const emailInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="email"]' }));

    expect(await firstnameInput.getValue()).toBe('');
    expect(await lastnameInput.getValue()).toBe('');
    expect(await emailInput.getValue()).toBe('');

    const success = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.registration__success__container' }));
    expect(success).toBeNull();
  });

  it('ngOnInit should call listCustomUserFields and create customFields form group when fields exist', async () => {
    const customFields = [
      { key: 'company', label: 'Company', required: true, values: [] },
      { key: 'role', label: 'Role', required: false, values: ['Admin', 'User'] },
    ];

    const component = await init({ customUserFields: customFields });

    expect(usersServiceMock.listCustomUserFields).toHaveBeenCalledTimes(1);
    expect(component.customUserFields()).toEqual(customFields);

    await fixture.whenStable();
    fixture.detectChanges();

    const companyField = await harnessLoader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Company' }));
    const companyInput = await companyField.getControl(MatInputHarness);
    expect(companyInput).not.toBeNull();
    expect(await companyInput!.getValue()).toBe('');

    const roleField = await harnessLoader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Role' }));
    const roleSelect = await roleField.getControl(MatSelectHarness);
    expect(roleSelect).not.toBeNull();
    expect(await roleSelect!.isDisabled()).toBe(false);
  });

  it('ngOnInit should NOT add customFields form group when listCustomUserFields returns empty array', async () => {
    await init({ customUserFields: [] });

    expect(usersServiceMock.listCustomUserFields).toHaveBeenCalledTimes(1);

    const customFieldsContainer = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '[data-testid="custom-fields"]' }));

    expect(customFieldsContainer).toBeNull();

    const companyField = await harnessLoader.getHarnessOrNull(MatFormFieldHarness.with({ floatingLabelText: 'Company' }));
    expect(companyField).toBeNull();
  });

  it('register() should call UsersService.registerNewUser with confirmation_page_url and on success show success view', async () => {
    window.history.pushState({}, 'Test', '/registration');

    const component = await init({
      customUserFields: [],
      register$: of({}),
    });

    const firstnameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="firstname"]' }));
    const lastnameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="lastname"]' }));
    const emailInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="email"]' }));
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Sign up' }));

    await firstnameInput.setValue('John');
    await lastnameInput.setValue('Doe');
    await emailInput.setValue('john@doe.com');
    await submitButton.click();

    fixture.detectChanges();

    const expectedConfirmationUrl = `${window.location.origin}${window.location.pathname}/confirm`;

    expect(usersServiceMock.registerNewUser).toHaveBeenCalledTimes(1);
    expect(usersServiceMock.registerNewUser).toHaveBeenCalledWith(
      expect.objectContaining({
        firstname: 'John',
        lastname: 'Doe',
        email: 'john@doe.com',
        confirmation_page_url: expectedConfirmationUrl,
      }),
    );

    expect(component.submitted()).toBe(true);
    expect(component.sentToEmail()).toBe('john@doe.com');

    const successContainer = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.registration__success__container' }));
    expect(successContainer).not.toBeNull();

    const successCard = await harnessLoader.getHarness(MatCardHarness.with({ selector: 'mat-card.registration__form__container' }));
    expect(await successCard.getTitleText()).toContain('Registration success');

    const successText = await successCard.getText();
    expect(successText).toContain('An email has been sent to:');
    expect(successText).toContain('john@doe.com');
  });
});
