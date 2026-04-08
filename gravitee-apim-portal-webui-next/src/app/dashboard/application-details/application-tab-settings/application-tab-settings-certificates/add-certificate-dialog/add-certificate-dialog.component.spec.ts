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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { AddCertificateDialogComponent, AddCertificateDialogData } from './add-certificate-dialog.component';
import { AddCertificateDialogHarness } from './add-certificate-dialog.component.harness';
import { ConfigService } from '../../../../../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../../../testing/app-testing.module';

const APPLICATION_ID = 'app-1';
const VALIDATE_URL = `${TESTING_BASE_URL}/applications/${APPLICATION_ID}/certificates/_validate`;
const CERTIFICATES_URL = `${TESTING_BASE_URL}/applications/${APPLICATION_ID}/certificates`;

function makeData(overrides: Partial<AddCertificateDialogData> = {}): AddCertificateDialogData {
  return {
    applicationId: APPLICATION_ID,
    hasActiveCertificates: false,
    ...overrides,
  };
}

describe('AddCertificateDialogComponent', () => {
  let fixture: ComponentFixture<AddCertificateDialogComponent>;
  let harness: AddCertificateDialogHarness;
  let httpTestingController: HttpTestingController;
  let dialogRef: { close: jest.Mock };

  async function init(data: AddCertificateDialogData = makeData()): Promise<void> {
    dialogRef = { close: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [AddCertificateDialogComponent, AppTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddCertificateDialogComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AddCertificateDialogHarness);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function flushValidate(overrides: object = {}): void {
    const req = httpTestingController.expectOne(VALIDATE_URL);
    expect(req.request.method).toBe('POST');
    req.flush({ certificateExpiration: '2027-06-01T00:00:00.000Z', subject: 'CN=test', issuer: 'CN=ca', ...overrides });
  }

  async function fillAndValidate(): Promise<void> {
    fixture.componentInstance.uploadForm.setValue({ name: 'My Cert', certificate: '-----BEGIN CERTIFICATE-----' });
    fixture.detectChanges();
    await harness.clickContinueUpload();
    fixture.detectChanges();
    flushValidate();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => httpTestingController.verify());

  it('should show upload step initially', async () => {
    await init();
    expect(await harness.nameInput()).toBeTruthy();
    expect(await harness.pemInput()).toBeTruthy();
  });

  it('should show validation errors when continuing with empty upload form', async () => {
    await init();
    await harness.clickContinueUpload();
    fixture.detectChanges();

    expect(fixture.componentInstance.uploadForm.touched).toBe(true);
    expect(fixture.componentInstance.uploadForm.invalid).toBe(true);
  });

  it('should advance to configure step after successful certificate validation', async () => {
    await init();
    await fillAndValidate();

    expect(fixture.componentInstance.stepper().selectedIndex).toBe(1);
  });

  it('should pre-fill endsAt from certificate expiration returned by validation', async () => {
    await init();
    await fillAndValidate();

    expect(fixture.componentInstance.configureForm.controls.endsAt.value).toEqual(new Date('2027-06-01T00:00:00.000Z'));
  });

  it('should show validate error when certificate validation returns 400', async () => {
    await init();

    fixture.componentInstance.uploadForm.setValue({ name: 'My Cert', certificate: 'INVALID PEM' });
    fixture.detectChanges();
    await harness.clickContinueUpload();
    fixture.detectChanges();

    httpTestingController.expectOne(VALIDATE_URL).flush(
      { error: 'Invalid PEM' },
      {
        status: 400,
        statusText: 'Bad Request',
      },
    );
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.validateErrorText()).toBeTruthy();
    expect(fixture.componentInstance.stepper().selectedIndex).toBe(0);
  });

  it('should not show grace period field when hasActiveCertificates is false', async () => {
    await init(makeData({ hasActiveCertificates: false }));
    await fillAndValidate();

    expect(await harness.gracePeriodInput()).toBeNull();
  });

  it('should show grace period field when hasActiveCertificates is true', async () => {
    await init(makeData({ hasActiveCertificates: true, activeCertificateId: 'old-cert' }));
    await fillAndValidate();

    expect(await harness.gracePeriodInput()).toBeTruthy();
  });

  it('should call create and close dialog on successful submit', async () => {
    await init();
    await fillAndValidate();

    await harness.clickContinueConfigure();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await harness.clickSubmit();
    fixture.detectChanges();

    const req = httpTestingController.expectOne(CERTIFICATES_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toMatchObject({ name: 'My Cert', certificate: '-----BEGIN CERTIFICATE-----' });
    req.flush({ id: 'new-cert', name: 'My Cert', status: 'ACTIVE' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should also call update on old cert when gracePeriodEnd is set', async () => {
    const activeCertificateId = 'old-cert-id';
    const activeCertificateName = 'Old Cert Name';
    await init(makeData({ hasActiveCertificates: true, activeCertificateId, activeCertificateName }));
    await fillAndValidate();

    const graceDate = new Date('2026-12-31');
    fixture.componentInstance.configureForm.controls.gracePeriodEnd.setValue(graceDate);
    await harness.clickContinueConfigure();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await harness.clickSubmit();
    fixture.detectChanges();

    const createReq = httpTestingController.expectOne(CERTIFICATES_URL);
    createReq.flush({ id: 'new-cert', name: 'My Cert', status: 'ACTIVE' });
    fixture.detectChanges();

    const updateReq = httpTestingController.expectOne(`${CERTIFICATES_URL}/${activeCertificateId}`);
    expect(updateReq.request.method).toBe('PUT');
    expect(updateReq.request.body).toMatchObject({ name: activeCertificateName, endsAt: graceDate.toISOString() });
    updateReq.flush({ id: activeCertificateId, name: activeCertificateName, status: 'ACTIVE_WITH_END' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should not send name in update when activeCertificateName is missing', async () => {
    const activeCertificateId = 'old-cert-id';
    await init(makeData({ hasActiveCertificates: true, activeCertificateId, activeCertificateName: undefined }));
    await fillAndValidate();

    const graceDate = new Date('2026-12-31');
    fixture.componentInstance.configureForm.controls.gracePeriodEnd.setValue(graceDate);
    await harness.clickContinueConfigure();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await harness.clickSubmit();
    fixture.detectChanges();

    const createReq = httpTestingController.expectOne(CERTIFICATES_URL);
    createReq.flush({ id: 'new-cert', name: 'My Cert', status: 'ACTIVE' });
    fixture.detectChanges();

    const updateReq = httpTestingController.expectOne(`${CERTIFICATES_URL}/${activeCertificateId}`);
    expect(updateReq.request.method).toBe('PUT');
    expect(updateReq.request.body).not.toHaveProperty('name');
    expect(updateReq.request.body).toMatchObject({ endsAt: graceDate.toISOString() });
    updateReq.flush({ id: activeCertificateId, name: 'Old Cert', status: 'ACTIVE_WITH_END' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should show submit error on 400 response from create', async () => {
    await init();
    await fillAndValidate();

    await harness.clickContinueConfigure();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await harness.clickSubmit();
    fixture.detectChanges();

    httpTestingController.expectOne(CERTIFICATES_URL).flush(
      { error: 'Invalid PEM' },
      {
        status: 400,
        statusText: 'Bad Request',
      },
    );
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.submitErrorText()).toBeTruthy();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should fill certificate name from uploaded file name', async () => {
    await init();

    const file = new File(['-----BEGIN CERTIFICATE-----'], 'my-server.crt', { type: 'application/x-x509-ca-cert' });
    const mockEvent = { target: { files: [file], value: '' } } as unknown as Event;
    await fixture.componentInstance.onFileSelected(mockEvent);
    fixture.detectChanges();

    expect(fixture.componentInstance.uploadForm.controls.name.value).toBe('my-server');
  });

  it('should not overwrite an existing certificate name when uploading a file', async () => {
    await init();

    fixture.componentInstance.uploadForm.controls.name.setValue('already-set');

    const file = new File(['-----BEGIN CERTIFICATE-----'], 'my-server.crt', { type: 'application/x-x509-ca-cert' });
    const mockEvent = { target: { files: [file], value: '' } } as unknown as Event;
    await fixture.componentInstance.onFileSelected(mockEvent);
    fixture.detectChanges();

    expect(fixture.componentInstance.uploadForm.controls.name.value).toBe('already-set');
  });
});
