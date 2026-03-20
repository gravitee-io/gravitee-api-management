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

import { AddCertificateDialogComponent, AddCertificateDialogData } from './add-certificate-dialog.component';

import { ApplicationGeneralModule } from '../application-general.module';

describe('AddCertificateDialogComponent', () => {
  let fixture: ComponentFixture<AddCertificateDialogComponent>;
  let loader: HarnessLoader;
  let dialogRef: { close: jest.Mock };

  const createComponent = (data: AddCertificateDialogData) => {
    dialogRef = { close: jest.fn() };

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApplicationGeneralModule, MatIconTestingModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    });

    fixture = TestBed.createComponent(AddCertificateDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  };

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

    it('should_not_show_grace_period_field', async () => {
      const gracePeriodInputs = await loader.getAllHarnesses(MatInputHarness.with({ selector: '[data-testid="grace-period-input"]' }));
      expect(gracePeriodInputs.length).toBe(0);
    });

    it('should_disable_submit_when_form_is_empty', async () => {
      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="add-certificate-submit"]' }));
      expect(await submitButton.isDisabled()).toBe(true);
    });

    it('should_set_minDate_to_today', () => {
      const today = new Date();
      const minDate = fixture.componentInstance.minDate;
      expect(minDate.getFullYear()).toBe(today.getFullYear());
      expect(minDate.getMonth()).toBe(today.getMonth());
      expect(minDate.getDate()).toBe(today.getDate());
    });

    it('should_reject_past_expiration_date', () => {
      const pastDate = new Date();
      pastDate.setDate(pastDate.getDate() - 5);
      fixture.componentInstance.form.get('endsAt').setValue(pastDate);
      fixture.detectChanges();

      expect(fixture.componentInstance.form.get('endsAt').hasError('matDatepickerMin')).toBe(true);
    });

    it('should_submit_with_name_and_certificate', async () => {
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="certificate-name-input"]' }));
      await nameInput.setValue('my-cert');

      const certInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="certificate-pem-input"]' }));
      await certInput.setValue('-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----');

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="add-certificate-submit"]' }));
      expect(await submitButton.isDisabled()).toBe(false);
      await submitButton.click();

      expect(dialogRef.close).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'my-cert',
          certificate: '-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----',
          gracePeriodEnd: undefined,
          activeCertificateId: undefined,
        }),
      );
    });
<<<<<<< HEAD
=======

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
>>>>>>> 023a93a4e9 (test: use data test id consistently and improve coverage)
  });

  describe('rotation (with active certificates)', () => {
    const ACTIVE_CERT_ID = 'cert-active-1';

    beforeEach(() => {
      createComponent({
        hasActiveCertificates: true,
        activeCertificateId: ACTIVE_CERT_ID,
      });
    });

    it('should_show_grace_period_field', async () => {
      const gracePeriodInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="grace-period-input"]' }));
      expect(gracePeriodInput).toBeTruthy();
    });

    it('should_disable_submit_when_grace_period_is_not_set', async () => {
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="certificate-name-input"]' }));
      await nameInput.setValue('rotation-cert');

      const certInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="certificate-pem-input"]' }));
      await certInput.setValue('-----BEGIN CERTIFICATE-----\nnew\n-----END CERTIFICATE-----');

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="add-certificate-submit"]' }));
      expect(await submitButton.isDisabled()).toBe(true);
    });

    it('should_mark_form_invalid_when_grace_period_is_not_set', () => {
      const gracePeriodControl = fixture.componentInstance.form.get('gracePeriodEnd');
      expect(gracePeriodControl).toBeTruthy();
      expect(gracePeriodControl.hasError('required')).toBe(true);
      expect(fixture.componentInstance.form.invalid).toBe(true);
    });

    it('should_reject_past_grace_period_date', () => {
      const pastDate = new Date();
      pastDate.setDate(pastDate.getDate() - 3);
      fixture.componentInstance.form.get('gracePeriodEnd').setValue(pastDate);
      fixture.detectChanges();

      expect(fixture.componentInstance.form.get('gracePeriodEnd').hasError('matDatepickerMin')).toBe(true);
    });

    it('should_include_active_certificate_id_in_result', async () => {
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="certificate-name-input"]' }));
      await nameInput.setValue('rotation-cert');

      const certInput = await loader.getHarness(MatInputHarness.with({ selector: '[data-testid="certificate-pem-input"]' }));
      await certInput.setValue('-----BEGIN CERTIFICATE-----\nnew\n-----END CERTIFICATE-----');

      const gracePeriodDate = new Date();
      gracePeriodDate.setDate(gracePeriodDate.getDate() + 7);
      fixture.componentInstance.form.get('gracePeriodEnd').setValue(gracePeriodDate);
      fixture.detectChanges();

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="add-certificate-submit"]' }));
      expect(await submitButton.isDisabled()).toBe(false);
      await submitButton.click();

      expect(dialogRef.close).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'rotation-cert',
          certificate: '-----BEGIN CERTIFICATE-----\nnew\n-----END CERTIFICATE-----',
          activeCertificateId: ACTIVE_CERT_ID,
        }),
      );
    });
  });
});
