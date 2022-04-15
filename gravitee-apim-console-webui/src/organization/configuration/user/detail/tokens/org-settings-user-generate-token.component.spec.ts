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
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';

import {
  OrgSettingsUserGenerateTokenComponent,
  OrgSettingsUserGenerateTokenDialogData,
} from './org-settings-user-generate-token.component';

import { OrganizationSettingsModule } from '../../../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { Token } from '../../../../../entities/user/userTokens';
import { fakeUserToken } from '../../../../../entities/user/userToken.fixture';

describe('OrgSettingsUserGenerateTokenComponent', () => {
  let component: OrgSettingsUserGenerateTokenComponent;
  let fixture: ComponentFixture<OrgSettingsUserGenerateTokenComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const fakeConstants = CONSTANTS_TESTING;

  const userId = 'user-id';

  const matDialogRefMock = {
    close: jest.fn(),
  };

  afterEach(() => {
    matDialogRefMock.close.mockClear();
  });
  beforeEach(() => {
    const dialogData: OrgSettingsUserGenerateTokenDialogData = {
      userId: userId,
    };
    TestBed.configureTestingModule({
      imports: [OrganizationSettingsModule, GioHttpTestingModule],
      providers: [
        {
          provide: 'Constants',
          useValue: fakeConstants,
        },
        {
          provide: MAT_DIALOG_DATA,
          useFactory: () => dialogData,
        },
        { provide: MatDialogRef, useValue: matDialogRefMock },
      ],
    });
    fixture = TestBed.createComponent(OrgSettingsUserGenerateTokenComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe('token creation', () => {
    it('should be able to create a token', async () => {
      fixture.detectChanges();

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      expect(await submitButton.isDisabled()).toBeTruthy();

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('External');

      await submitButton.click();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/tokens`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body.name).toEqual('External');

      const tokenResponse: Token = fakeUserToken({ name: 'External' });
      req.flush(tokenResponse);

      expect(component.hasBeenGenerated).toBeTruthy();
      expect(component.token).toEqual(tokenResponse);

      const closeButton = await loader.getHarness(MatButtonHarness.with({ text: /^Close/ }));
      await closeButton.click();

      expect(matDialogRefMock.close).toHaveBeenCalledWith(false);
    });

    it('should not be able to create a token because it already exists', async () => {
      fixture.detectChanges();

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      expect(await submitButton.isDisabled()).toBeTruthy();

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('External');

      await submitButton.click();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/tokens`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body.name).toEqual('External');

      req.flush(
        {
          message: 'A token with the name [External] already exists.',
          technicalCode: 'token.alreadyExists',
          http_status: 400,
        },
        {
          status: 400,
          statusText: 'Bad request',
        },
      );

      expect(component.hasBeenGenerated).toBeFalsy();
    });

    it.each([
      ['a', 'Name has to be greater than 2 characters long.'],
      [
        'A too long name for this token should trigger an error message, because 64 is the limit',
        'Name has to be less than 64 characters long.',
      ],
      [null, 'Name is required.'],
    ])(
      'should not be able to create a token because it does not have a valid format: with "%s"',
      async (input: string, errorMessage: string) => {
        fixture.detectChanges();

        const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
        expect(await submitButton.isDisabled()).toBeTruthy();

        const matFormFieldHarness = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: /Name/ }));
        const nameInput = await matFormFieldHarness.getControl(MatInputHarness);
        await nameInput.setValue(input);
        await nameInput.blur();

        expect(await submitButton.isDisabled()).toBeTruthy();

        expect(await matFormFieldHarness.hasErrors()).toBe(true);
        expect(await matFormFieldHarness.getTextErrors()).toContain(errorMessage);
      },
    );
  });

  describe('getExampleOfUse', () => {
    const token = 'A_TOKEN';

    it('should return a proper curl example with full url', () => {
      expect(component.getExampleOfUse(token)).toEqual(
        `curl -H "Authorization: Bearer A_TOKEN" "https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT"`,
      );
    });

    it('should return a proper curl example with url starting with /', () => {
      fakeConstants.org.baseURL = '/management/organizations/DEFAULT';
      expect(component.getExampleOfUse(token)).toEqual(
        `curl -H "Authorization: Bearer A_TOKEN" "http://localhost/management/organizations/DEFAULT/environments/DEFAULT"`,
      );
    });
  });
});
