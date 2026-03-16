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
