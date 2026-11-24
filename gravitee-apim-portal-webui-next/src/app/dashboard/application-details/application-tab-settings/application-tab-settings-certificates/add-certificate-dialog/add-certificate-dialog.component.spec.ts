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
const CSR_PEM = '-----BEGIN CERTIFICATE REQUEST-----\ncsr-body\n-----END CERTIFICATE REQUEST-----';
const CERT_PEM = '-----BEGIN CERTIFICATE-----\ncert-body\n-----END CERTIFICATE-----';

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
    fixture.componentInstance.uploadForm.setValue({ name: 'My Cert', certificate: CERT_PEM });
    fixture.detectChanges();
    await harness.clickContinueUpload();
    fixture.detectChanges();
    flushValidate();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => httpTestingController.verify());

  it('should show upload step initially in certificate mode', async () => {
    await init();
    expect(fixture.componentInstance.uploadMode()).toBe('certificate');
    expect(await harness.nameInput()).toBeTruthy();
    expect(await harness.pemInput()).toBeTruthy();
    expect(await harness.csrPemInput()).toBeNull();
  });

  it('should switch to CSR mode and show CSR upload fields', async () => {
    await init();
    await harness.selectMode('csr');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.uploadMode()).toBe('csr');
    expect(await harness.nameInput()).toBeNull();
    expect(await harness.pemInput()).toBeNull();
    expect(await harness.csrPemInput()).toBeTruthy();
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

    expect(fixture.componentInstance.stepper()?.selectedIndex).toBe(1);
  });

  it('should pre-fill endsAt from certificate expiration returned by validation', async () => {
    await init();
    await fillAndValidate();

    expect(fixture.componentInstance.configureForm.controls.endsAt.value).toEqual(new Date('2027-06-01T00:00:00.000Z'));
  });

  it('should show validate error when certificate validation returns 400', async () => {
    await init();

    fixture.componentInstance.uploadForm.setValue({ name: 'My Cert', certificate: CERT_PEM });
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
    expect(fixture.componentInstance.stepper()?.selectedIndex).toBe(0);
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
    expect(req.request.body).toMatchObject({ name: 'My Cert', certificate: CERT_PEM });
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

    const file = new File([CERT_PEM], 'my-server.crt', { type: 'application/x-x509-ca-cert' });
    const mockEvent = { target: { files: [file], value: '' } } as unknown as Event;
    await fixture.componentInstance.onFileSelected(mockEvent);
    fixture.detectChanges();

    expect(fixture.componentInstance.uploadForm.controls.name.value).toBe('my-server');
    expect(fixture.componentInstance.fileName()).toBe('my-server.crt');
  });

  it('should not overwrite an existing certificate name when uploading a file', async () => {
    await init();

    fixture.componentInstance.uploadForm.controls.name.setValue('already-set');

    const file = new File([CERT_PEM], 'my-server.crt', { type: 'application/x-x509-ca-cert' });
    const mockEvent = { target: { files: [file], value: '' } } as unknown as Event;
    await fixture.componentInstance.onFileSelected(mockEvent);
    fixture.detectChanges();

    expect(fixture.componentInstance.uploadForm.controls.name.value).toBe('already-set');
  });

  it('should reject unsupported extensions in certificate mode', async () => {
    await init();

    const file = new File([CSR_PEM], 'request.csr', { type: 'application/pkcs10' });
    const mockEvent = { target: { files: [file], value: '' } } as unknown as Event;
    await fixture.componentInstance.onFileSelected(mockEvent);
    fixture.detectChanges();

    expect(await harness.fileErrorText()).toBeTruthy();
    expect(fixture.componentInstance.uploadForm.controls.certificate.value).toBe('');
  });

  it('should advance to Confirm (step 2 of 2) when CSR mode continues with a CSR', async () => {
    await init();
    fixture.componentInstance.setUploadMode('csr');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.uploadForm.controls.certificate.setValue(CSR_PEM);
    fixture.detectChanges();
    await harness.clickContinueUpload();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.uploadMode()).toBe('csr');
    expect(fixture.componentInstance.stepper()?.selectedIndex).toBe(1);
    expect(fixture.componentInstance.stepper()?.steps.length).toBe(2);
  });

  it('should accept .csr file upload in CSR mode and show the file name', async () => {
    await init();
    fixture.componentInstance.setUploadMode('csr');
    fixture.detectChanges();

    const file = new File([CSR_PEM], 'client.csr', { type: 'application/pkcs10' });
    const mockEvent = { target: { files: [file], value: '' } } as unknown as Event;
    await fixture.componentInstance.onFileSelected(mockEvent);
    fixture.detectChanges();

    expect(fixture.componentInstance.uploadForm.controls.certificate.value).toBe(CSR_PEM);
    expect(await harness.csrFileNameText()).toBe('client.csr');
  });

  it('should accept .req file upload in CSR mode', async () => {
    await init();
    fixture.componentInstance.setUploadMode('csr');
    fixture.detectChanges();

    const file = new File([CSR_PEM], 'client.req', { type: 'application/pkcs10' });
    const mockEvent = { target: { files: [file], value: '' } } as unknown as Event;
    await fixture.componentInstance.onFileSelected(mockEvent);
    fixture.detectChanges();

    expect(fixture.componentInstance.fileName()).toBe('client.req');
  });

  it('should submit CSR from Confirm without updating an active certificate grace period', async () => {
    const activeCertificateId = 'old-cert-id';
    await init(makeData({ hasActiveCertificates: true, activeCertificateId, activeCertificateName: 'Old Cert' }));

    fixture.componentInstance.setUploadMode('csr');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    fixture.componentInstance.uploadForm.controls.certificate.setValue(CSR_PEM);
    fixture.detectChanges();
    await harness.clickContinueUpload();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.stepper()?.selectedIndex).toBe(1);

    await harness.clickSubmit();
    fixture.detectChanges();

    httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APPLICATION_ID}`).flush({
      id: APPLICATION_ID,
      name: 'App',
      settings: {},
    });
    fixture.detectChanges();

    const saveReq = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APPLICATION_ID}`);
    expect(saveReq.request.body).toMatchObject({ csr: CSR_PEM });
    saveReq.flush({ id: APPLICATION_ID, name: 'App', settings: {}, csr: CSR_PEM });
    fixture.detectChanges();
    await fixture.whenStable();

    httpTestingController.expectNone(`${CERTIFICATES_URL}/${activeCertificateId}`);
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should show an error when certificate mode receives CSR content', async () => {
    await init();

    fixture.componentInstance.uploadForm.setValue({ name: 'My Cert', certificate: CSR_PEM });
    fixture.detectChanges();
    await harness.clickContinueUpload();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(await harness.validateErrorText()).toBeTruthy();
    expect(fixture.componentInstance.stepper()?.selectedIndex).toBe(0);
    httpTestingController.expectNone(VALIDATE_URL);
  });

  it('should show an error when CSR mode receives certificate content', async () => {
    await init();
    fixture.componentInstance.setUploadMode('csr');
    fixture.detectChanges();

    fixture.componentInstance.uploadForm.controls.certificate.setValue(CERT_PEM);
    fixture.detectChanges();
    await harness.clickContinueUpload();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(await harness.validateErrorText()).toBeTruthy();
    expect(fixture.componentInstance.stepper()?.selectedIndex).toBe(0);
  });

  it('should return to Upload from Confirm with Previous in CSR mode', async () => {
    await init();
    fixture.componentInstance.setUploadMode('csr');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    fixture.componentInstance.uploadForm.controls.certificate.setValue(CSR_PEM);
    fixture.detectChanges();
    await harness.clickContinueUpload();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.stepper()?.selectedIndex).toBe(1);

    fixture.componentInstance.goPrevious();
    fixture.detectChanges();

    expect(fixture.componentInstance.stepper()?.selectedIndex).toBe(0);
  });

  it('should show three steps in certificate mode', async () => {
    await init();
    expect(fixture.componentInstance.stepper()?.steps.length).toBe(3);
  });
});
