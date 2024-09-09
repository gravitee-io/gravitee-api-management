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
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatErrorHarness } from '@angular/material/form-field/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { GioFormSelectionInlineCardHarness, GioFormSelectionInlineHarness } from '@gravitee/ui-particles-angular';

import { CreateIntegrationComponent } from './create-integration.component';
import { CreateIntegrationHarness } from './create-integration.harness';

import { IntegrationsModule } from '../integrations.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { CreateIntegrationPayload } from '../integrations.model';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('CreateIntegrationComponent', () => {
  let fixture: ComponentFixture<CreateIntegrationComponent>;
  let componentHarness: CreateIntegrationHarness;
  let httpTestingController: HttpTestingController;

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CreateIntegrationComponent],
      imports: [GioTestingModule, IntegrationsModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: SnackBarService,
          useValue: fakeSnackBarService,
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-integration-u', 'environment-integration-d', 'environment-integration-c', 'environment-integration-r'],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();
  });

  beforeEach(async () => {
    fixture = TestBed.createComponent(CreateIntegrationComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CreateIntegrationHarness);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('choose provider', () => {
    it('should set solace value', async (): Promise<void> => {
      fixture.componentInstance.integrationProviders = {
        active: [
          { icon: 'aws-api-gateway', value: 'aws-api-gateway' },
          { icon: 'solace', value: 'solace' },
        ],
      };

      const radioButtonsGroup: GioFormSelectionInlineHarness = await componentHarness.getRadioButtonsGroup();
      expect(await radioButtonsGroup.getSelectedValue()).toBeUndefined();
      await radioButtonsGroup.select('solace');
      fixture.detectChanges();

      expect(await radioButtonsGroup.getSelectedValue()).toStrictEqual('solace');
    });

    it('should set correct value', async (): Promise<void> => {
      fixture.componentInstance.integrationProviders = {
        active: [{ icon: 'aws.svg', value: 'test_my_value' }],
      };

      const radioButtonsGroup: GioFormSelectionInlineHarness = await componentHarness.getRadioButtonsGroup();
      expect(await radioButtonsGroup.getSelectedValue()).toBeUndefined();

      await radioButtonsGroup.select('test_my_value');
      expect(await radioButtonsGroup.getSelectedValue()).toStrictEqual('test_my_value');
    });

    it('should have correct numbers of radio buttons', async (): Promise<void> => {
      fixture.componentInstance.integrationProviders = {
        active: [
          { icon: 'aws.svg', value: 'aws-api-gateway' },
          { icon: 'solace.svg', value: 'solace' },
          { icon: 'apigee.svg', value: 'apigee' },
          { icon: 'azure.svg', value: 'azure-api-management' },
          { icon: 'confluent.svg', value: 'confluent-platform' },
        ],
        comingSoon: [{ icon: 'kong.svg', value: 'kong' }],
      };
      const radioCards: GioFormSelectionInlineCardHarness[] = await componentHarness.getRadioCards();
      expect(radioCards.length).toEqual(6);
    });

    it('should disable button when no radio button selected', async (): Promise<void> => {
      const submitFirstStepButton: MatButtonHarness = await componentHarness.getSubmitStepFirstButton();
      expect(await submitFirstStepButton.isDisabled()).toBe(true);
    });

    it('should show active button when Choose Provider form is valid', async (): Promise<void> => {
      const submitFirstStepButton: MatButtonHarness = await componentHarness.getSubmitStepFirstButton();
      expect(await submitFirstStepButton.isDisabled()).toBe(true);

      fixture.componentInstance.chooseProviderForm.controls.provider.setValue('test-value');
      fixture.detectChanges();

      expect(await submitFirstStepButton.isDisabled()).toBe(false);
    });
  });

  describe('details form', (): void => {
    it('should not submit integration with too short name', async (): Promise<void> => {
      await componentHarness.setName('');
      await componentHarness.setDescription('Some description');
      fixture.detectChanges();

      const error: MatErrorHarness = await componentHarness.matErrorMessage();
      expect(await error.getText()).toEqual('Integration name is required.');

      await componentHarness.clickOnSubmit();
      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations`);
    });

    it('should not submit integration with too long name', async () => {
      await componentHarness.setName('test too long name 01234567890123456789012345678901234567890123456789');
      await componentHarness.setDescription('Test Description');
      fixture.detectChanges();

      const error: MatErrorHarness = await componentHarness.matErrorMessage();
      expect(await error.getText()).toEqual('Integration name can not exceed 50 characters.');

      await componentHarness.clickOnSubmit();
      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations`);
    });

    it('should not submit integration with too long description', async (): Promise<void> => {
      await componentHarness.setName('test0');
      await componentHarness.setDescription(
        'TOO long description: loa hdvoiah dfopivioa fdo[ivu[au f[09vu a[09eu v9[ua09efu 0v9u e09fv u09qw uef09v uq0w9duf v0 qu0efdu 0vwu df09vu 0wduf09v wu0dfu v0 wud0fv uqw0 uf90v uw9efuv9wu efvu wqpefuvqwu e0fu v0wu ef0vu w0euf 0vqwu 0efu v0qwuef uvqw uefvru wfeuvwufvu w0  ufev',
      );
      await componentHarness.clickOnSubmit();
      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations`);
    });

    it('should create integration with valid name', async (): Promise<void> => {
      const expectedPayload: CreateIntegrationPayload = {
        name: 'TEST123',
        description: '',
        provider: '',
      };
      await componentHarness.setName('TEST123');
      await componentHarness.clickOnSubmit();
      expectIntegrationPostRequest(expectedPayload);
    });

    it('should create integration with description', async (): Promise<void> => {
      const expectedPayload: CreateIntegrationPayload = {
        name: 'TEST123',
        description: 'Test Description',
        provider: '',
      };
      await componentHarness.setName('TEST123');
      await componentHarness.setDescription('Test Description');
      await componentHarness.clickOnSubmit();

      expectIntegrationPostRequest(expectedPayload);
    });

    it('should handle error with message', async (): Promise<void> => {
      await componentHarness.setName('TEST123');
      await componentHarness.clickOnSubmit();

      expectIntegrationWithError();

      fixture.detectChanges();

      expect(fakeSnackBarService.error).toHaveBeenCalledWith('An error occurred. Integration not created');
    });
  });

  function expectIntegrationPostRequest(payload: CreateIntegrationPayload): void {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations`);
    req.flush([]);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual(payload);
  }

  function expectIntegrationWithError(): void {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations`);
    expect(req.request.method).toEqual('POST');
    req.flush({}, { status: 400, statusText: 'Bad Request' });
  }
});
