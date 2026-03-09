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
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatChipGridHarness } from '@angular/material/chips/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatRadioButtonHarness } from '@angular/material/radio/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { Router } from '@angular/router';

import { CreateApplicationComponent } from './create-application.component';
import { FormKeyValuePairsHarness } from '../../../components/form-key-value-pairs/form-key-value-pairs.harness';
import {
  fakeApplication,
  fakeSimpleApplicationType,
  fakeNativeApplicationType,
  fakeBackendToBackendApplicationType,
  fakeBrowserApplicationType,
} from '../../../entities/application/application.fixture';
import { ObservabilityBreakpointService } from '../../../services/observability-breakpoint.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('CreateApplicationComponent', () => {
  let fixture: ComponentFixture<CreateApplicationComponent>;
  let component: CreateApplicationComponent;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let router: Router;
  let routerNavigateSpy: jest.SpyInstance;
  let isMobileSignal: ReturnType<typeof signal<boolean>>;

  const mockApplicationTypes = [
    fakeSimpleApplicationType(),
    fakeNativeApplicationType(),
    fakeBackendToBackendApplicationType(),
    fakeBrowserApplicationType(),
  ];

  beforeEach(async () => {
    isMobileSignal = signal(false);

    const mockObservabilityBreakpointService = {
      isMobile: isMobileSignal.asReadonly(),
    };

    await TestBed.configureTestingModule({
      imports: [CreateApplicationComponent, AppTestingModule],
      providers: [
        {
          provide: ObservabilityBreakpointService,
          useValue: mockObservabilityBreakpointService,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateApplicationComponent);
    component = fixture.componentInstance;
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  describe('Component initialization', () => {
    it('should show loader while loading application types', () => {
      const loader = fixture.nativeElement.querySelector('app-loader');
      expect(loader).toBeTruthy();

      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });
    });

    it('should load application types from API', () => {
      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`);
      expect(req.request.method).toBe('GET');
      req.flush({ data: mockApplicationTypes });
    });

    it('should automatically select first type (simple) as default when types are loaded', async () => {
      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });

      await fixture.whenStable();
      fixture.detectChanges();

      expect(component.typeIdControl.value).toBe('simple');
      expect(component.selectedType()?.id).toBe('simple');
    });

    it('should display form after types are loaded', async () => {
      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });

      await fixture.whenStable();
      fixture.detectChanges();

      const loader = fixture.nativeElement.querySelector('app-loader');
      expect(loader).toBeFalsy();

      const form = fixture.nativeElement.querySelector('form');
      expect(form).toBeTruthy();
    });
  });

  describe('Application type selection', () => {
    beforeEach(async () => {
      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it('should display all available application types', async () => {
      const radioButtons = await harnessLoader.getAllHarnesses(MatRadioButtonHarness);
      expect(radioButtons.length).toBe(mockApplicationTypes.length);
    });

    it('should change selected type when clicking on radio button', async () => {
      const radioButtons = await harnessLoader.getAllHarnesses(MatRadioButtonHarness);
      await radioButtons[1].check();

      expect(component.typeIdControl.value).toBe('native');
      expect(component.selectedType()?.id).toBe('native');
    });

    it('should display simple type fields (Type, Client ID) when simple type is selected', async () => {
      const typeInput = await harnessLoader.getHarnessOrNull(MatInputHarness.with({ selector: '[formControlName="appType"]' }));
      const clientIdInput = await harnessLoader.getHarnessOrNull(MatInputHarness.with({ selector: '[formControlName="appClientId"]' }));

      expect(typeInput).toBeTruthy();
      expect(clientIdInput).toBeTruthy();
    });

    it('should display OAuth fields (grant types, redirect URIs) when OAuth type is selected', async () => {
      const radioButtons = await harnessLoader.getAllHarnesses(MatRadioButtonHarness);
      await radioButtons[1].check();
      fixture.detectChanges();
      await fixture.whenStable();

      const redirectUrisControl = component.redirectUrisControl;
      const grantTypesControl = component.grantTypesControl;

      expect(redirectUrisControl).toBeTruthy();
      expect(grantTypesControl).toBeTruthy();
    });

    it('should hide simple type fields when non-simple type is selected', async () => {
      const radioButtons = await harnessLoader.getAllHarnesses(MatRadioButtonHarness);
      await radioButtons[1].check();
      fixture.detectChanges();
      await fixture.whenStable();

      const typeInput = await harnessLoader.getHarnessOrNull(MatInputHarness.with({ selector: '[formControlName="appType"]' }));
      const clientIdInput = await harnessLoader.getHarnessOrNull(MatInputHarness.with({ selector: '[formControlName="appClientId"]' }));

      expect(typeInput).toBeFalsy();
      expect(clientIdInput).toBeFalsy();
    });
  });

  describe('Form validation', () => {
    beforeEach(async () => {
      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it('should mark form as invalid when name is missing', () => {
      expect(component.form.invalid).toBe(true);
      expect(component.form.controls.name.hasError('required')).toBe(true);
    });

    it('should mark form as valid when all required fields are filled', async () => {
      const nameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('Test Application');

      expect(component.form.valid).toBe(true);
    });

    it('should disable Create button when form is invalid', async () => {
      const createButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Create/ }));
      expect(await createButton.isDisabled()).toBe(true);
    });

    it('should enable Create button when form is valid', async () => {
      const nameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('Test Application');

      const createButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Create/ }));
      expect(await createButton.isDisabled()).toBe(false);
    });

    it('should show validation error when name field is touched and empty', async () => {
      const nameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.focus();
      await nameInput.blur();
      fixture.detectChanges();

      const error = fixture.nativeElement.querySelector('mat-error');
      expect(error).toBeTruthy();
    });
  });

  describe('Dynamic form fields - Redirect URIs', () => {
    beforeEach(async () => {
      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });
      await fixture.whenStable();
      fixture.detectChanges();

      const radioButtons = await harnessLoader.getAllHarnesses(MatRadioButtonHarness);
      await radioButtons[1].check();
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it('should display redirect URIs field for types that require it', () => {
      const redirectUrisControl = component.redirectUrisControl;
      expect(redirectUrisControl).toBeTruthy();
    });

    it('should require at least one redirect URI', () => {
      const redirectUrisControl = component.redirectUrisControl;
      expect(redirectUrisControl?.hasError('required')).toBe(true);
    });

    it('should validate redirect URIs when at least one is provided', () => {
      const redirectUrisControl = component.redirectUrisControl;
      redirectUrisControl?.setValue(['https://example.com/callback']);
      expect(redirectUrisControl?.valid).toBe(true);
    });

    it('should add redirect URI when entered and blurred', async () => {
      const chipGrid = await harnessLoader.getHarness(MatChipGridHarness);
      const input = await chipGrid.getInput();
      expect(input).toBeTruthy();
      if (input) {
        await input.setValue('https://example.com/callback');
        await input.blur();
        fixture.detectChanges();

        const redirectUrisControl = component.redirectUrisControl;
        expect(redirectUrisControl?.value).toContain('https://example.com/callback');
      }
    });

    it('should remove redirect URI when remove button is clicked', async () => {
      const redirectUrisControl = component.redirectUrisControl;
      redirectUrisControl?.setValue(['https://example.com/callback']);
      fixture.detectChanges();
      await fixture.whenStable();

      const chipGrid = await harnessLoader.getHarness(MatChipGridHarness);
      const rows = await chipGrid.getRows();
      expect(rows.length).toBe(1);

      await rows[0].remove();
      fixture.detectChanges();

      expect(redirectUrisControl?.value).not.toContain('https://example.com/callback');
      expect(redirectUrisControl?.value.length).toBe(0);
    });

    it('should prevent adding duplicate redirect URI', async () => {
      const redirectUrisControl = component.redirectUrisControl;
      redirectUrisControl?.setValue(['https://example.com/callback']);
      fixture.detectChanges();
      await fixture.whenStable();

      const chipGrid = await harnessLoader.getHarness(MatChipGridHarness);
      const input = await chipGrid.getInput();
      expect(input).toBeTruthy();
      if (input) {
        await input.setValue('https://example.com/callback');
        await input.blur();
        fixture.detectChanges();

        expect(redirectUrisControl?.value).toEqual(['https://example.com/callback']);
        expect(redirectUrisControl?.value.length).toBe(1);
      }
    });
  });

  describe('Dynamic form fields - Grant Types', () => {
    beforeEach(async () => {
      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });
      await fixture.whenStable();
      fixture.detectChanges();

      const radioButtons = await harnessLoader.getAllHarnesses(MatRadioButtonHarness);
      await radioButtons[1].check();
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it('should display available grant types for selected type', () => {
      const grantTypesList = component.grantTypesList();
      expect(grantTypesList.length).toBeGreaterThan(0);
    });

    it('should mark mandatory grant types as disabled and checked', () => {
      const grantTypesList = component.grantTypesList();
      const mandatoryGrantType = grantTypesList.find(gt => gt.isDisabled);
      expect(mandatoryGrantType).toBeTruthy();
    });

    it('should add grant type when toggled on', () => {
      const grantTypesControl = component.grantTypesControl;
      const initialValue = grantTypesControl?.value ?? [];
      const grantTypeToAdd = 'refresh_token';

      component.toggleGrantType(grantTypeToAdd, true);

      expect(grantTypesControl?.value).toContain(grantTypeToAdd);
      expect(grantTypesControl?.value.length).toBe(initialValue.length + 1);
    });

    it('should remove grant type when toggled off (if not mandatory)', () => {
      const grantTypesControl = component.grantTypesControl;
      const grantTypeToRemove = 'refresh_token';

      component.toggleGrantType(grantTypeToRemove, true);
      const valueAfterAdd = grantTypesControl?.value ?? [];

      component.toggleGrantType(grantTypeToRemove, false);

      expect(grantTypesControl?.value).not.toContain(grantTypeToRemove);
      expect(grantTypesControl?.value.length).toBe(valueAfterAdd.length - 1);
    });

    it('should show error when mandatory grant types are missing', () => {
      const grantTypesControl = component.grantTypesControl;
      grantTypesControl?.setValue([]);
      grantTypesControl?.markAsTouched();
      fixture.detectChanges();

      expect(grantTypesControl?.hasError('mandatoryMissing')).toBe(true);
    });

    it('should toggle grant type using slide toggle', async () => {
      const grantTypesControl = component.grantTypesControl;
      const grantTypeToToggle = 'refresh_token';

      const slideToggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      expect(slideToggles.length).toBeGreaterThan(0);

      for (const toggle of slideToggles) {
        const label = await toggle.getLabelText();
        if (label.includes('Refresh Token')) {
          const isChecked = await toggle.isChecked();
          if (!isChecked) {
            await toggle.toggle();
            fixture.detectChanges();
            expect(grantTypesControl?.value).toContain(grantTypeToToggle);
            break;
          }
        }
      }
    });

    it('should not allow unchecking mandatory grant types', async () => {
      const grantTypesList = component.grantTypesList();
      const mandatoryGrantType = grantTypesList.find(gt => gt.isDisabled);

      expect(mandatoryGrantType).toBeTruthy();

      if (mandatoryGrantType) {
        const slideToggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
        for (const toggle of slideToggles) {
          const label = await toggle.getLabelText();
          if (label.includes(mandatoryGrantType.name)) {
            const isDisabled = await toggle.isDisabled();
            expect(isDisabled).toBe(true);
            break;
          }
        }
      }
    });
  });

  describe('Metadata (Additional Client Metadata)', () => {
    beforeEach(async () => {
      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });
      await fixture.whenStable();
      fixture.detectChanges();

      const radioButtons = await harnessLoader.getAllHarnesses(MatRadioButtonHarness);
      await radioButtons[1].check();
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it('should display metadata field for OAuth application types', async () => {
      const metadataHarness = await harnessLoader.getHarnessOrNull(FormKeyValuePairsHarness);
      expect(metadataHarness).toBeTruthy();
    });

    it('should not display metadata field for simple application type', async () => {
      const radioButtons = await harnessLoader.getAllHarnesses(MatRadioButtonHarness);
      await radioButtons[0].check();
      await fixture.whenStable();
      fixture.detectChanges();

      const metadataHarness = await harnessLoader.getHarnessOrNull(FormKeyValuePairsHarness);
      expect(metadataHarness).toBeFalsy();
    });

    it('should include metadata in API request when creating OAuth application', async () => {
      const nameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('Test App with Metadata');

      const metadataHarness = await harnessLoader.getHarness(FormKeyValuePairsHarness);
      await metadataHarness.setKeyValuePair(0, 'environment', 'production');
      await fixture.whenStable();
      await fixture.whenStable();

      await metadataHarness.setKeyValuePair(1, 'version', '1.0.0');
      await fixture.whenStable();

      const redirectUrisControl = component.redirectUrisControl;
      redirectUrisControl?.setValue(['https://example.com/callback']);

      const grantTypesControl = component.grantTypesControl;
      grantTypesControl?.setValue(['authorization_code']);

      const createButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Create/ }));
      await createButton.click();

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications`);
      expect(req.request.body.settings.oauth.additional_client_metadata).toEqual({
        environment: 'production',
        version: '1.0.0',
      });

      req.flush(fakeApplication({ id: 'new-app-id' }));
    });

    it('should not include metadata in API request when metadata is empty', async () => {
      const nameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('Test App without Metadata');

      const redirectUrisControl = component.redirectUrisControl;
      redirectUrisControl?.setValue(['https://example.com/callback']);

      const grantTypesControl = component.grantTypesControl;
      grantTypesControl?.setValue(['authorization_code']);

      const createButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Create/ }));
      await createButton.click();

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications`);
      expect(req.request.body.settings.oauth.additional_client_metadata).toBeUndefined();

      req.flush(fakeApplication({ id: 'new-app-id' }));
    });
  });

  describe('Create application - success', () => {
    beforeEach(async () => {
      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it('should send correct data to API for simple type', async () => {
      const nameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('Test Simple App');

      const appTypeInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="appType"]' }));
      await appTypeInput.setValue('mobile');

      const clientIdInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="appClientId"]' }));
      await clientIdInput.setValue('test-client-id');

      const createButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Create/ }));
      await createButton.click();

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        name: 'Test Simple App',
        description: undefined,
        settings: {
          app: {
            type: 'mobile',
            client_id: 'test-client-id',
          },
        },
      });

      const createdApplication = fakeApplication({ id: 'new-app-id', name: 'Test Simple App' });
      req.flush(createdApplication);

      expect(routerNavigateSpy).toHaveBeenCalledWith(['/applications', 'new-app-id']);
    });

    it('should send correct data to API for OAuth type', async () => {
      const radioButtons = await harnessLoader.getAllHarnesses(MatRadioButtonHarness);
      await radioButtons[1].check(); // Native type
      await fixture.whenStable();
      fixture.detectChanges();

      const nameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('Test OAuth App');

      const redirectUrisControl = component.redirectUrisControl;
      redirectUrisControl?.setValue(['https://example.com/callback']);

      const grantTypesControl = component.grantTypesControl;
      grantTypesControl?.setValue(['authorization_code', 'refresh_token']);

      const createButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Create/ }));
      await createButton.click();

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.settings.oauth).toBeDefined();
      expect(req.request.body.settings.oauth.redirect_uris).toEqual(['https://example.com/callback']);
      expect(req.request.body.settings.oauth.grant_types).toEqual(['authorization_code', 'refresh_token']);

      const createdApplication = fakeApplication({ id: 'new-oauth-app-id', name: 'Test OAuth App' });
      req.flush(createdApplication);

      expect(routerNavigateSpy).toHaveBeenCalledWith(['/applications', 'new-oauth-app-id']);
    });

    it('should send correct data to API for backend to backend type', async () => {
      const radioButtons = await harnessLoader.getAllHarnesses(MatRadioButtonHarness);
      await radioButtons[2].check(); // Backend to backend type
      await fixture.whenStable();
      fixture.detectChanges();

      const nameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('Test OAuth App');

      const createButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Create/ }));
      await createButton.click();

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.settings.oauth).toBeDefined();
      expect(req.request.body.settings.oauth.application_type).toEqual('backend_to_backend');
      expect(req.request.body.settings.oauth.redirect_uris).toEqual([]);
      expect(req.request.body.settings.oauth.grant_types).toEqual(['client_credentials']);

      const createdApplication = fakeApplication({ id: 'new-oauth-app-id', name: 'Test OAuth App' });
      req.flush(createdApplication);

      expect(routerNavigateSpy).toHaveBeenCalledWith(['/applications', 'new-oauth-app-id']);
    });

    it('should include TLS settings when client certificate is provided', async () => {
      const nameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('Test App with TLS');

      const clientCertTextarea = fixture.nativeElement.querySelector('textarea[formControlName="clientCertificate"]');
      clientCertTextarea.value = '-----BEGIN CERTIFICATE-----\nMOCK_CERT\n-----END CERTIFICATE-----';
      clientCertTextarea.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      const createButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Create/ }));
      await createButton.click();

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications`);
      expect(req.request.body.settings.tls).toBeDefined();
      expect(req.request.body.settings.tls.client_certificate).toContain('BEGIN CERTIFICATE');

      req.flush(fakeApplication({ id: 'new-app-id' }));
    });
  });

  describe('Create application - error', () => {
    beforeEach(async () => {
      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it('should display error message when API call fails', async () => {
      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

      const nameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('Test App');

      const createButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Create/ }));
      await createButton.click();

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications`);
      req.flush({ error: 'Server Error' }, { status: 500, statusText: 'Internal Server Error' });

      fixture.detectChanges();

      expect(component.hasApplicationError).toBe(true);
      expect(consoleErrorSpy).toHaveBeenCalled();

      const errorMessage = fixture.nativeElement.querySelector('.error-message');
      expect(errorMessage).toBeTruthy();

      consoleErrorSpy.mockRestore();
    });

    it('should not navigate when API call fails', async () => {
      const nameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('Test App');

      const createButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Create/ }));
      await createButton.click();

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications`);
      req.flush({ error: 'Server Error' }, { status: 500, statusText: 'Internal Server Error' });

      expect(routerNavigateSpy).not.toHaveBeenCalled();
    });
  });

  describe('Navigation', () => {
    beforeEach(async () => {
      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it('should navigate to /applications when Cancel is clicked', async () => {
      const cancelButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Cancel/ }));
      await cancelButton.click();

      expect(routerNavigateSpy).toHaveBeenCalledWith(['/applications']);
    });
  });

  describe('Responsive design', () => {
    beforeEach(async () => {
      httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/types`).flush({ data: mockApplicationTypes });
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it('should display dropdown on mobile', () => {
      isMobileSignal.set(true);
      fixture.detectChanges();

      const select = fixture.nativeElement.querySelector('mat-select');
      const radioGroup = fixture.nativeElement.querySelector('mat-radio-group');

      expect(select).toBeTruthy();
      expect(radioGroup).toBeFalsy();
    });

    it('should display radio buttons on desktop', () => {
      isMobileSignal.set(false);
      fixture.detectChanges();

      const select = fixture.nativeElement.querySelector('mat-select');
      const radioGroup = fixture.nativeElement.querySelector('mat-radio-group');

      expect(select).toBeFalsy();
      expect(radioGroup).toBeTruthy();
    });
  });
});
