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
import { throwError } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { InvitationConfirmationComponent } from './invitation-confirmation.component';
import { TokenService } from '../../../services/token.service';
import { UsersService } from '../../../services/users.service';
import { AppTestingModule } from '../../../testing/app-testing.module';

describe('InvitationConfirmationComponent', () => {
  let fixture: ComponentFixture<InvitationConfirmationComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const invitationParsed = { email: 'invited@example.com' };

  const init = async (params?: { token?: string; parsedToken?: { firstname?: string; lastname?: string; email: string } | null }) => {
    const token = params?.token ?? 'token-123';
    const parsedToken = params && 'parsedToken' in params ? params.parsedToken : invitationParsed;

    const tokenServiceMock = {
      parseToken: jest.fn().mockReturnValue(parsedToken),
    };
    const usersServiceMock = {
      finalizeRegistration: jest.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [InvitationConfirmationComponent, AppTestingModule],
      providers: [
        { provide: TokenService, useValue: tokenServiceMock },
        { provide: UsersService, useValue: usersServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InvitationConfirmationComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.componentRef.setInput('token', token);
    fixture.detectChanges();
    fixture.detectChanges();
    return { tokenServiceMock, usersServiceMock };
  };

  afterEach(() => {
    httpTestingController.verify();
    jest.restoreAllMocks();
  });

  it('should display invalid token view when token cannot be parsed', async () => {
    await init({ parsedToken: null });

    const card = await harnessLoader.getHarness(MatCardHarness.with({ selector: 'mat-card.invitation-confirmation__form__container' }));
    expect(await card.getTitleText()).toContain('Invalid token');

    const errorEl = await harnessLoader.getHarness(MatErrorHarness);
    expect(await errorEl.getText()).toContain('Invalid token value');
  });

  it('should display the invitation form with editable, empty name fields and a prefilled disabled email', async () => {
    const { tokenServiceMock } = await init({ token: 'token-abc', parsedToken: invitationParsed });

    expect(tokenServiceMock.parseToken).toHaveBeenCalledWith('token-abc');

    const card = await harnessLoader.getHarness(MatCardHarness.with({ selector: 'mat-card.invitation-confirmation__form__container' }));
    expect(await card.getTitleText()).toContain('Accept your invitation');

    const firstnameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="firstname"]' }));
    const lastnameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="lastname"]' }));
    const emailInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="email"]' }));

    expect(await firstnameInput.isDisabled()).toBe(false);
    expect(await lastnameInput.isDisabled()).toBe(false);
    expect(await firstnameInput.getValue()).toBe('');
    expect(await lastnameInput.getValue()).toBe('');

    expect(await emailInput.isDisabled()).toBe(true);
    expect(await emailInput.getValue()).toBe('invited@example.com');

    const submitBtn = await harnessLoader.getHarness(MatButtonHarness.with({ selector: 'button.invitation-confirmation__form__submit' }));
    expect(await submitBtn.isDisabled()).toBe(true);
  });

  it('should finalize the invitation with the user-provided name and show the success view', async () => {
    const { usersServiceMock } = await init({ token: 'token-123', parsedToken: invitationParsed });

    const firstnameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="firstname"]' }));
    const lastnameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="lastname"]' }));
    const passwordInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    const confirmInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="confirmedPassword"]' }));

    await firstnameInput.setValue('Jane');
    await lastnameInput.setValue('Doe');
    await passwordInput.setValue('P@ssw0rd!');
    await confirmInput.setValue('P@ssw0rd!');

    const submitBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Accept invitation' }));
    await submitBtn.click();
    fixture.detectChanges();

    expect(usersServiceMock.finalizeRegistration).toHaveBeenCalledTimes(1);
    expect(usersServiceMock.finalizeRegistration).toHaveBeenCalledWith({
      firstname: 'Jane',
      lastname: 'Doe',
      token: 'token-123',
      password: 'P@ssw0rd!',
    });

    const card = await harnessLoader.getHarness(MatCardHarness.with({ selector: 'mat-card.invitation-confirmation__form__container' }));
    expect(await card.getTitleText()).toContain('Invitation accepted');
    const cardText = await card.getText();
    expect(cardText).toContain('Your account has been successfully activated.');
    expect(cardText).toContain('Back to login');
  });

  it('should keep the form and show a generic error when finalize fails', async () => {
    const { usersServiceMock } = await init({ token: 'token-123', parsedToken: invitationParsed });
    usersServiceMock.finalizeRegistration.mockReturnValue(throwError(() => new Error('finalize failed')));

    const firstnameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="firstname"]' }));
    const lastnameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="lastname"]' }));
    const passwordInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    const confirmInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="confirmedPassword"]' }));

    await firstnameInput.setValue('Jane');
    await lastnameInput.setValue('Doe');
    await passwordInput.setValue('P@ssw0rd!');
    await confirmInput.setValue('P@ssw0rd!');

    const submitBtn = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Accept invitation' }));
    await submitBtn.click();
    fixture.detectChanges();

    // The form stays visible (no success view) and a generic, non-misleading error is shown.
    const card = await harnessLoader.getHarness(MatCardHarness.with({ selector: 'mat-card.invitation-confirmation__form__container' }));
    expect(await card.getTitleText()).toContain('Accept your invitation');

    const errorEl = await harnessLoader.getHarness(MatErrorHarness);
    expect(await errorEl.getText()).toContain('Unable to accept the invitation');
  });
});
