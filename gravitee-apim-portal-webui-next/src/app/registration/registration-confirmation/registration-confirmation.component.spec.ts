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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCardHarness } from '@angular/material/card/testing';
import { of } from 'rxjs/internal/observable/of';

import { RegistrationConfirmationComponent } from './registration-confirmation.component';
import { TokenService } from '../../../services/token.service';
import { UsersService } from '../../../services/users.service';
import { AppTestingModule } from '../../../testing/app-testing.module';

describe('RegistrationConfirmationComponent', () => {
  let fixture: ComponentFixture<RegistrationConfirmationComponent>;
  let harnessLoader: HarnessLoader;
  const defaultParsed = { firstname: 'John', lastname: 'Doe', email: 'john@doe.com' };

  let httpTestingController: HttpTestingController;

  const init = async (params?: { token?: string; parsedToken?: { firstname: string; lastname: string; email: string } | null }) => {
    const token = params?.token ?? 'token-123';

    const defaultParsed = { firstname: 'John', lastname: 'Doe', email: 'john@doe.com' };
    const parsedToken = params && 'parsedToken' in params ? params.parsedToken : defaultParsed;

    const tokenServiceMock = {
      parseToken: jest.fn().mockReturnValue(parsedToken),
    };

    const usersServiceMock = {
      finalizeRegistration: jest.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [RegistrationConfirmationComponent, AppTestingModule],
      providers: [
        { provide: TokenService, useValue: tokenServiceMock },
        { provide: UsersService, useValue: usersServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrationConfirmationComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.componentRef.setInput('token', token);
    fixture.detectChanges();
    fixture.detectChanges();
    return {
      component: fixture.componentInstance,
      tokenServiceMock,
      usersServiceMock,
    };
  };

  afterEach(() => {
    httpTestingController.verify();
    jest.restoreAllMocks();
  });

  it('should display invalid token view when token cannot be parsed', async () => {
    await init({ parsedToken: null });

    const titleEl: HTMLElement | null = fixture.nativeElement.querySelector('.registration-confirmation__form__title');
    expect(titleEl).not.toBeNull();
    expect(titleEl!.textContent).toContain('Invalid token');

    const errorEl: HTMLElement | null = fixture.nativeElement.querySelector('mat-error');
    expect(errorEl).not.toBeNull();
    expect(errorEl!.textContent).toContain('Invalid token value');
  });

  it('should display confirm registration form when token is valid and prefill disabled fields', async () => {
    const { component, tokenServiceMock } = await init({ token: 'token-abc', parsedToken: defaultParsed });

    expect(tokenServiceMock.parseToken).toHaveBeenCalledWith('token-abc');
    expect(component.error()).toBe(200);
    expect(component.submitted()).toBe(false);

    const card = await harnessLoader.getHarness(MatCardHarness.with({ selector: 'mat-card.registration-confirmation__form__container' }));
    expect(await card.getTitleText()).toContain('Confirm registration');

    const formEl: HTMLFormElement | null = fixture.nativeElement.querySelector('form');
    expect(formEl).not.toBeNull();

    expect(component.registrationConfirmationForm).toBeTruthy();
    expect(component.registrationConfirmationForm!.controls.firstname.disabled).toBe(true);
    expect(component.registrationConfirmationForm!.controls.lastname.disabled).toBe(true);
    expect(component.registrationConfirmationForm!.controls.email.disabled).toBe(true);

    expect(component.registrationConfirmationForm!.controls.firstname.value).toBe('John');
    expect(component.registrationConfirmationForm!.controls.lastname.value).toBe('Doe');
    expect(component.registrationConfirmationForm!.controls.email.value).toBe('john@doe.com');

    const submitBtn = await harnessLoader.getHarness(MatButtonHarness.with({ selector: 'button.registration-confirmation__form__submit' }));
    expect(await submitBtn.isDisabled()).toBe(true);
  });

  it('confirmRegistration should call finalizeRegistration and show success view on success', async () => {
    const { component, usersServiceMock } = await init({
      token: 'token-123',
      parsedToken: defaultParsed,
    });

    component.registrationConfirmationForm!.controls.password.setValue('P@ssw0rd!');
    component.registrationConfirmationForm!.controls.confirmedPassword.setValue('P@ssw0rd!');
    component.registrationConfirmationForm!.updateValueAndValidity();

    component.confirmRegistration();
    fixture.detectChanges();

    expect(usersServiceMock.finalizeRegistration).toHaveBeenCalledTimes(1);
    expect(usersServiceMock.finalizeRegistration).toHaveBeenCalledWith({
      firstname: 'John',
      lastname: 'Doe',
      token: 'token-123',
      password: 'P@ssw0rd!',
    });

    expect(component.submitted()).toBe(true);

    const card = await harnessLoader.getHarness(MatCardHarness.with({ selector: 'mat-card.registration-confirmation__form__container' }));
    expect(await card.getTitleText()).toContain('Account created');

    const successContent = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-btn"]' }));
    expect(successContent).not.toBeNull();
  });
});
