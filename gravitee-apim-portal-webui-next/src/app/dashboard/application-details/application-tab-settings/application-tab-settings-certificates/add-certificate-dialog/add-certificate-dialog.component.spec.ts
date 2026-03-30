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

  it('should advance to configure step when upload form is valid', async () => {
    await init();

    fixture.componentInstance.uploadForm.setValue({ name: 'My Cert', certificate: '-----BEGIN CERTIFICATE-----' });
    fixture.detectChanges();

    await harness.clickContinueUpload();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.stepper().selectedIndex).toBe(1);
  });

  it('should not show grace period field when hasActiveCertificates is false', async () => {
    await init(makeData({ hasActiveCertificates: false }));

    fixture.componentInstance.uploadForm.setValue({ name: 'My Cert', certificate: '-----BEGIN CERTIFICATE-----' });
    await harness.clickContinueUpload();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.gracePeriodInput()).toBeNull();
  });

  it('should show grace period field when hasActiveCertificates is true', async () => {
    await init(makeData({ hasActiveCertificates: true, activeCertificateId: 'old-cert' }));

    fixture.componentInstance.uploadForm.setValue({ name: 'My Cert', certificate: '-----BEGIN CERTIFICATE-----' });
    await harness.clickContinueUpload();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.gracePeriodInput()).toBeTruthy();
  });

  it('should call create and close dialog on successful submit', async () => {
    await init();

    fixture.componentInstance.uploadForm.setValue({ name: 'My Cert', certificate: '-----BEGIN CERTIFICATE-----' });
    await harness.clickContinueUpload();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await harness.clickContinueConfigure();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await harness.clickSubmit();
    fixture.detectChanges();

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APPLICATION_ID}/certificates`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toMatchObject({ name: 'My Cert', certificate: '-----BEGIN CERTIFICATE-----' });
    req.flush({ id: 'new-cert', name: 'My Cert', status: 'ACTIVE' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should also call update on old cert when gracePeriodEnd is set', async () => {
    const activeCertificateId = 'old-cert-id';
    await init(makeData({ hasActiveCertificates: true, activeCertificateId }));

    fixture.componentInstance.uploadForm.setValue({ name: 'My Cert', certificate: '-----BEGIN CERTIFICATE-----' });
    await harness.clickContinueUpload();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const graceDate = new Date('2026-12-31');
    fixture.componentInstance.configureForm.controls.gracePeriodEnd.setValue(graceDate);
    await harness.clickContinueConfigure();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await harness.clickSubmit();
    fixture.detectChanges();

    const createReq = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APPLICATION_ID}/certificates`);
    createReq.flush({ id: 'new-cert', name: 'My Cert', status: 'ACTIVE' });
    fixture.detectChanges();

    const updateReq = httpTestingController.expectOne(
      `${TESTING_BASE_URL}/applications/${APPLICATION_ID}/certificates/${activeCertificateId}`,
    );
    expect(updateReq.request.method).toBe('PUT');
    expect(updateReq.request.body).toMatchObject({ endsAt: graceDate.toISOString() });
    updateReq.flush({ id: activeCertificateId, name: 'Old Cert', status: 'ACTIVE_WITH_END' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should show validation error on 400 response', async () => {
    await init();

    fixture.componentInstance.uploadForm.setValue({ name: 'My Cert', certificate: 'INVALID PEM' });
    await harness.clickContinueUpload();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await harness.clickContinueConfigure();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    await harness.clickSubmit();
    fixture.detectChanges();

    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/applications/${APPLICATION_ID}/certificates`)
      .flush({ error: 'Invalid PEM' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.submitErrorText()).toBeTruthy();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
