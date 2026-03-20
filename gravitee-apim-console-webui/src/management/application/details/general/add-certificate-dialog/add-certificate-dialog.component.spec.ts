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
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NewFile } from '@gravitee/ui-particles-angular';

import { AddCertificateDialogComponent, AddCertificateDialogData } from './add-certificate-dialog.component';

import { ApplicationGeneralModule } from '../application-general.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { ValidateCertificateResponse } from '../../../../../entities/application/ClientCertificate';

describe('AddCertificateDialogComponent', () => {
  const APPLICATION_ID = 'app-test-id';
  const VALID_PEM = '-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----';
  const MOCK_VALIDATION_RESPONSE: ValidateCertificateResponse = {
    certificateExpiration: '2027-06-15T10:00:00Z',
    subject: 'CN=test-subject',
    issuer: 'CN=test-issuer',
  };

  let fixture: ComponentFixture<AddCertificateDialogComponent>;
  let loader: HarnessLoader;
  let dialogRef: { close: jest.Mock };
  let httpTestingController: HttpTestingController;

  const createComponent = (data: Partial<AddCertificateDialogData> & Pick<AddCertificateDialogData, 'hasActiveCertificates'>) => {
    const fullData: AddCertificateDialogData = { applicationId: APPLICATION_ID, ...data };
    dialogRef = { close: jest.fn() };

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApplicationGeneralModule, MatIconTestingModule, GioTestingModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: fullData },
      ],
    });

    fixture = TestBed.createComponent(AddCertificateDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  async function fillUploadForm(name: string = 'my-cert', certificate: string = VALID_PEM): Promise<void> {
    const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="certificate-name-input"]' }));
    await nameInput.setValue(name);
    const certInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="certificate-pem-input"]' }));
    await certInput.setValue(certificate);
  }

  async function clickValidate(): Promise<void> {
    const validateButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="certificate-validate-button"]' }));
    await validateButton.click();
  }

  function flushValidation(response: ValidateCertificateResponse = MOCK_VALIDATION_RESPONSE): void {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/certificates/_validate`,
      method: 'POST',
    });
    req.flush(response);
  }

  function failValidation(): void {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/certificates/_validate`,
      method: 'POST',
    });
    req.flush({ message: 'Invalid certificate' }, { status: 400, statusText: 'Bad Request' });
  }

  async function advanceToConfigureStep(name: string = 'my-cert', certificate: string = VALID_PEM): Promise<void> {
    await fillUploadForm(name, certificate);
    await clickValidate();
    flushValidation();
    fixture.detectChanges();
  }

  async function clickContinueToConfirm(): Promise<void> {
    const continueButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="certificate-continue-button"]' }));
    await continueButton.click();
    fixture.detectChanges();
  }

  async function advanceToConfirmStep(name: string = 'my-cert', certificate: string = VALID_PEM): Promise<void> {
    await advanceToConfigureStep(name, certificate);
    await clickContinueToConfirm();
  }

  describe('first certificate (no active certificates)', () => {
    beforeEach(() => {
      createComponent({ hasActiveCertificates: false });
    });

    it('should_have_name_and_certificate_fields', async () => {
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="certificate-name-input"]' }));
      expect(nameInput).toBeTruthy();

      const certInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="certificate-pem-input"]' }));
      expect(certInput).toBeTruthy();
    });

    it('should_not_have_grace_period_control', () => {
      expect(fixture.componentInstance.configureForm.controls.gracePeriodEnd).toBeUndefined();
    });

    it('should_disable_validate_button_when_form_is_empty', async () => {
      const validateButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="certificate-validate-button"]' }));
      expect(await validateButton.isDisabled()).toBe(true);
    });

    it('should_set_minDate_to_today', () => {
      const today = new Date();
      const minDate = fixture.componentInstance.minDate;
      expect(minDate.getFullYear()).toBe(today.getFullYear());
      expect(minDate.getMonth()).toBe(today.getMonth());
      expect(minDate.getDate()).toBe(today.getDate());
    });

    it('should_submit_with_name_and_certificate', async () => {
      await advanceToConfirmStep('my-cert', VALID_PEM);

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="add-certificate-submit"]' }));
      expect(await submitButton.isDisabled()).toBe(false);
      await submitButton.click();

      expect(dialogRef.close).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'my-cert',
          certificate: VALID_PEM,
          activeCertificateId: undefined,
        }),
      );
    });

    it('should_populate_certificate_field_when_file_is_uploaded', async () => {
      const pemContent = '-----BEGIN CERTIFICATE-----\nfiletest\n-----END CERTIFICATE-----';
      const file = new File([pemContent], 'cert.pem', { type: 'application/x-pem-file' });
      const newFile = new NewFile('cert.pem', '', file);

      await fixture.componentInstance.onFileSelected([newFile]);

      expect(fixture.componentInstance.uploadForm.controls.certificate.value).toBe(pemContent);
      expect(fixture.componentInstance.filePickerValue).toEqual([]);
    });
  });

  describe('stepper flow', () => {
    beforeEach(() => {
      createComponent({ hasActiveCertificates: false });
    });

    it('should_show_validation_error_banner_when_validation_fails', async () => {
      await fillUploadForm();
      await clickValidate();
      failValidation();
      fixture.detectChanges();

      expect(fixture.componentInstance.validationError).toBe('Invalid certificate format');
      const errorBanner = fixture.nativeElement.querySelector('[data-testid="validation-error-banner"]');
      expect(errorBanner).toBeTruthy();
      expect(errorBanner.textContent).toContain('Invalid certificate format');
      expect(fixture.componentInstance.currentStep).toBe(0);
    });

    it('should_advance_to_configure_step_on_successful_validation', async () => {
      await advanceToConfigureStep();

      expect(fixture.componentInstance.currentStep).toBe(1);
      expect(fixture.componentInstance.validationResponse).toEqual(MOCK_VALIDATION_RESPONSE);

      const successBanner = fixture.nativeElement.querySelector('[data-testid="validation-success-banner"]');
      expect(successBanner).toBeTruthy();
      expect(successBanner.textContent).toContain('Certificate validated successfully');
    });

    it('should_advance_to_confirm_step_from_configure', async () => {
      await advanceToConfigureStep();
      await clickContinueToConfirm();

      expect(fixture.componentInstance.currentStep).toBe(2);
    });

    it('should_show_summary_table_on_confirm_step', async () => {
      await advanceToConfirmStep();

      const summary = fixture.nativeElement.querySelector('[data-testid="certificate-summary"]');
      expect(summary).toBeTruthy();

      const nameValue = fixture.nativeElement.querySelector('[data-testid="certificate-summary-name-value"]');
      expect(nameValue.textContent.trim()).toBe('my-cert');

      const activeUntilValue = fixture.nativeElement.querySelector('[data-testid="certificate-summary-active-until-value"]');
      expect(activeUntilValue.textContent.trim()).toBeTruthy();
    });

    it('should_go_back_to_upload_step_when_clicking_previous_on_configure_step', async () => {
      await advanceToConfigureStep();
      expect(fixture.componentInstance.currentStep).toBe(1);

      const previousButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="certificate-previous-button"]' }));
      await previousButton.click();

      expect(fixture.componentInstance.currentStep).toBe(0);
    });

    it('should_go_back_to_configure_step_when_clicking_previous_on_confirm_step', async () => {
      await advanceToConfirmStep();
      expect(fixture.componentInstance.currentStep).toBe(2);

      const previousButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="certificate-previous-button"]' }));
      await previousButton.click();

      expect(fixture.componentInstance.currentStep).toBe(1);
    });

    it('should_populate_ends_at_from_validation_response', async () => {
      await advanceToConfigureStep();

      const endsAt = fixture.componentInstance.configureForm.controls.endsAt.value;
      expect(new Date(endsAt).toISOString()).toBe('2027-06-15T10:00:00.000Z');
    });

    it('should_reject_past_expiration_date', async () => {
      await advanceToConfigureStep();

      const pastDate = new Date();
      pastDate.setDate(pastDate.getDate() - 3);
      fixture.componentInstance.configureForm.controls.endsAt.setValue(pastDate);
      fixture.detectChanges();

      expect(fixture.componentInstance.configureForm.controls.endsAt.hasError('owlDateTimeMin')).toBe(true);
    });
  });

  describe('rotation (with active certificates)', () => {
    const ACTIVE_CERT_ID = 'cert-active-1';

    beforeEach(() => {
      createComponent({
        hasActiveCertificates: true,
        activeCertificateId: ACTIVE_CERT_ID,
      });
    });

    it('should_have_grace_period_control', () => {
      expect(fixture.componentInstance.configureForm.controls.gracePeriodEnd).toBeTruthy();
    });

    it('should_show_grace_period_field_on_configure_step', async () => {
      await advanceToConfigureStep();

      const gracePeriodInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="grace-period-input"]' }));
      expect(gracePeriodInput).toBeTruthy();
    });

    it('should_disable_continue_when_grace_period_is_not_set', async () => {
      await advanceToConfigureStep();

      fixture.componentInstance.configureForm.controls.gracePeriodEnd.setValue(null);
      fixture.detectChanges();

      const continueButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="certificate-continue-button"]' }));
      expect(await continueButton.isDisabled()).toBe(true);
    });

    it('should_mark_form_invalid_when_grace_period_is_not_set', () => {
      fixture.componentInstance.configureForm.controls.gracePeriodEnd.setValue(null);
      fixture.detectChanges();

      expect(fixture.componentInstance.configureForm.controls.gracePeriodEnd.hasError('required')).toBe(true);
      expect(fixture.componentInstance.configureForm.invalid).toBe(true);
    });

    it('should_reject_past_grace_period_date', async () => {
      await advanceToConfigureStep();

      const pastDate = new Date();
      pastDate.setDate(pastDate.getDate() - 3);
      fixture.componentInstance.configureForm.controls.gracePeriodEnd.setValue(pastDate);
      fixture.detectChanges();

      expect(fixture.componentInstance.configureForm.controls.gracePeriodEnd.hasError('owlDateTimeMin')).toBe(true);
    });

    it('should_include_active_certificate_id_in_result', async () => {
      await advanceToConfigureStep('rotation-cert', VALID_PEM);

      const gracePeriodDate = new Date();
      gracePeriodDate.setDate(gracePeriodDate.getDate() + 7);
      fixture.componentInstance.configureForm.controls.gracePeriodEnd.setValue(gracePeriodDate);
      fixture.detectChanges();

      await clickContinueToConfirm();

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="add-certificate-submit"]' }));
      await submitButton.click();

      expect(dialogRef.close).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'rotation-cert',
          certificate: VALID_PEM,
          activeCertificateId: ACTIVE_CERT_ID,
        }),
      );
    });
  });
});
