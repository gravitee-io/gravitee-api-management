/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { MatErrorHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { of } from 'rxjs/internal/observable/of';

import { RegistrationConfirmationComponent } from './registration-confirmation.component';
import { TokenService } from '../../../services/token.service';
import { UsersService } from '../../../services/users.service';
import { AppTestingModule } from '../../../testing/app-testing.module';
import { DivHarness } from '../../../testing/div.harness';

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

    const card = await harnessLoader.getHarness(MatCardHarness.with({ selector: 'mat-card.registration-confirmation__form__container' }));
    expect(await card.getTitleText()).toContain('Invalid token');

    const errorEl = await harnessLoader.getHarness(MatErrorHarness);
    expect(await errorEl.getText()).toContain('Invalid token value');
  });

  it('should display confirm registration form when token is valid and prefill disabled fields', async () => {
    const { tokenServiceMock } = await init({ token: 'token-abc', parsedToken: defaultParsed });

    expect(tokenServiceMock.parseToken).toHaveBeenCalledWith('token-abc');

    const card = await harnessLoader.getHarness(MatCardHarness.with({ selector: 'mat-card.registration-confirmation__form__container' }));
    expect(await card.getTitleText()).toContain('Confirm your registration');

    const detailEls = await harnessLoader.getAllHarnesses(DivHarness.with({ selector: '.registration-confirmation__form__detail' }));
    expect(detailEls.length).toBe(3);
    const detailTexts = await Promise.all(detailEls.map(detail => detail.getText()));
    expect(detailTexts.join(' ')).toContain('First name:');
    expect(detailTexts.join(' ')).toContain('John');
    expect(detailTexts.join(' ')).toContain('Last name:');
    expect(detailTexts.join(' ')).toContain('Doe');
    expect(detailTexts.join(' ')).toContain('Email:');
    expect(detailTexts.join(' ')).toContain('john@doe.com');

    const submitBtn = await harnessLoader.getHarness(MatButtonHarness.with({ selector: 'button.registration-confirmation__form__submit' }));
    expect(await submitBtn.isDisabled()).toBe(true);
  });

  it('confirmRegistration should call finalizeRegistration and show success view on success', async () => {
    const { usersServiceMock } = await init({
      token: 'token-123',
      parsedToken: defaultParsed,
    });

    const passwordInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    const confirmInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="confirmedPassword"]' }));

    await passwordInput.setValue('P@ssw0rd!');
    await confirmInput.setValue('P@ssw0rd!');

    const submitBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Complete registration' }));
    await submitBtn.click();

    fixture.detectChanges();

    expect(usersServiceMock.finalizeRegistration).toHaveBeenCalledTimes(1);
    expect(usersServiceMock.finalizeRegistration).toHaveBeenCalledWith({
      firstname: 'John',
      lastname: 'Doe',
      token: 'token-123',
      password: 'P@ssw0rd!',
    });

    const card = await harnessLoader.getHarness(MatCardHarness.with({ selector: 'mat-card.registration-confirmation__form__container' }));
    expect(await card.getTitleText()).toContain('Registration confirmed');

    const cardText = await card.getText();
    expect(cardText).toContain('Your account has been successfully activated.');
    expect(cardText).toContain('Back to login');
  });
});
