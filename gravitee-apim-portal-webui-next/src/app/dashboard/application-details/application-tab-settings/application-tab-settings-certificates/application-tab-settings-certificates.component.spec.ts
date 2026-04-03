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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { ApplicationTabSettingsCertificatesComponent } from './application-tab-settings-certificates.component';
import { ApplicationTabSettingsCertificatesHarness } from './application-tab-settings-certificates.harness';
import { ConfirmDialogHarness } from '../../../../../components/confirm-dialog/confirm-dialog.harness';
import { ClientCertificate } from '../../../../../entities/application/client-certificate';
import { fakeUserApplicationPermissions } from '../../../../../entities/permission/permission.fixtures';
import { ConfigService } from '../../../../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../../testing/app-testing.module';

const fakeCertificate = (overrides: Partial<ClientCertificate> = {}): ClientCertificate => ({
  id: 'cert-1',
  name: 'My Certificate',
  status: 'ACTIVE',
  createdAt: '2026-01-01T00:00:00Z',
  ...overrides,
});

describe('ApplicationTabSettingsCertificatesComponent', () => {
  let fixture: ComponentFixture<ApplicationTabSettingsCertificatesComponent>;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;
  const applicationId = 'app-1';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationTabSettingsCertificatesComponent, AppTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApplicationTabSettingsCertificatesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.componentRef.setInput('applicationId', applicationId);
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ DEFINITION: ['U'] }));
  });

  afterEach(() => httpTestingController.verify());

  async function initWithCertificates(
    data: ClientCertificate[] = [],
    totalElements = data.length,
  ): Promise<ApplicationTabSettingsCertificatesHarness> {
    fixture.detectChanges();
    httpTestingController
      .match(req => req.url.includes(`/applications/${applicationId}/certificates`))
      .forEach(req =>
        req.flush({
          data,
          metadata: { paginateMetaData: { totalElements } },
        }),
      );
    fixture.detectChanges();
    await fixture.whenStable();
    return TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationTabSettingsCertificatesHarness);
  }

  it('should show empty state when no certificates', async () => {
    const harness = await initWithCertificates([]);
    expect(await harness.getEmptyState()).toBeTruthy();
    expect(await harness.getPaginatedTable()).toBeNull();
  });

  it('should show paginated table when certificates exist', async () => {
    const harness = await initWithCertificates([fakeCertificate()]);
    expect(await harness.getEmptyState()).toBeNull();
    expect(await harness.getPaginatedTable()).toBeTruthy();
  });

  it('should set totalElements from metadata', async () => {
    await initWithCertificates([fakeCertificate()], 42);
    expect(fixture.componentInstance.totalElements()).toBe(42);
  });

  it('should show error message when loading fails', async () => {
    fixture.detectChanges();
    httpTestingController
      .match(req => req.url.includes(`/applications/${applicationId}/certificates`))
      .forEach(req => req.flush({ error: 'Server error' }, { status: 500, statusText: 'Internal Server Error' }));
    fixture.detectChanges();
    await fixture.whenStable();

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationTabSettingsCertificatesHarness);
    expect(await harness.getErrorMessage()).toBeTruthy();
  });

  it('should filter active tab correctly', async () => {
    const certs = [
      fakeCertificate({ id: 'c1', status: 'ACTIVE' }),
      fakeCertificate({ id: 'c2', status: 'ACTIVE_WITH_END' }),
      fakeCertificate({ id: 'c3', status: 'REVOKED' }),
    ];
    await initWithCertificates(certs);

    expect(fixture.componentInstance.activeCertificates()).toHaveLength(2);
    expect(fixture.componentInstance.activeCertificates().map(c => c.id)).toEqual(['c1', 'c2']);
  });

  it('should filter history tab correctly', async () => {
    const certs = [fakeCertificate({ id: 'c1', status: 'ACTIVE' }), fakeCertificate({ id: 'c2', status: 'REVOKED' })];
    await initWithCertificates(certs);

    expect(fixture.componentInstance.historyCertificates()).toHaveLength(1);
    expect(fixture.componentInstance.historyCertificates()[0].id).toBe('c2');
  });

  it('should format expired cert as "Expired"', () => {
    const past = new Date(Date.now() - 86400000).toISOString();
    expect(fixture.componentInstance.formatDaysRemaining(past)).toBe('Expired');
  });

  it('should format no-expiry cert as "—"', () => {
    expect(fixture.componentInstance.formatDaysRemaining(undefined)).toBe('—');
  });

  it('should format future expiry as days remaining', () => {
    const future = new Date(Date.now() + 2 * 86400000).toISOString();
    const result = fixture.componentInstance.formatDaysRemaining(future);
    expect(Number(result)).toBeGreaterThan(0);
  });

  it('should reload certificates on page change', async () => {
    await initWithCertificates([fakeCertificate()]);

    fixture.componentInstance.onPageChange(2);

    httpTestingController
      .match(req => req.url.includes(`/applications/${applicationId}/certificates`) && req.url.includes('page=2'))
      .forEach(req => req.flush({ data: [], metadata: { paginateMetaData: { totalElements: 0 } } }));

    expect(fixture.componentInstance.currentPage()).toBe(2);
  });

  it('should show upload button when user has UPDATE permission', async () => {
    const harness = await initWithCertificates([fakeCertificate()]);
    expect(await harness.getUploadButton()).toBeTruthy();
  });

  it('should not show upload button when user lacks UPDATE permission', async () => {
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ DEFINITION: ['R'] }));
    const harness = await initWithCertificates([fakeCertificate()]);
    expect(await harness.getUploadButton()).toBeNull();
  });

  it('should open upload dialog when upload button is clicked', async () => {
    const harness = await initWithCertificates([fakeCertificate()]);
    const dialog = TestBed.inject(MatDialog);
    const openSpy = jest
      .spyOn(dialog, 'open')
      .mockReturnValue({ afterClosed: () => ({ pipe: () => ({ subscribe: () => ({}) }) }) } as never);

    await harness.clickUploadButton();

    expect(openSpy).toHaveBeenCalled();
  });

  it('should keep certificate untouched when first delete confirmation is cancelled', async () => {
    const certificate = fakeCertificate();
    await initWithCertificates([certificate]);

    await clickDeleteButton();

    const confirmDialog = await getConfirmDialog();
    expect(confirmDialog).not.toBeNull();
    expect(await confirmDialog?.getTitle()).toBe('Delete certificate');
    await confirmDialog?.cancel();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(await getConfirmDialog()).toBeNull();
    httpTestingController.expectNone(req => req.method === 'DELETE');
    expect(fixture.componentInstance.activeCertificates().map(cert => cert.id)).toEqual([certificate.id]);
  });

  it('should delete a non-last-active certificate after the initial confirmation', async () => {
    const certificates = [
      fakeCertificate({ id: 'cert-1', name: 'Certificate 1' }),
      fakeCertificate({ id: 'cert-2', name: 'Certificate 2' }),
    ];
    await initWithCertificates(certificates, 2);

    await clickDeleteButton();

    const confirmDialog = await getConfirmDialog();
    await confirmDialog?.confirm();

    expect(await getConfirmDialog()).toBeNull();
    httpTestingController.expectNone(`${TESTING_BASE_URL}/applications/${applicationId}/certificates?page=1&size=${certificates.length}`);

    httpTestingController
      .expectOne({
        url: `${TESTING_BASE_URL}/applications/${applicationId}/certificates/cert-1`,
        method: 'DELETE',
      })
      .flush(null);

    flushCertificatesRequest([certificates[1]], 1);

    expect(fixture.componentInstance.activeCertificates().map(cert => cert.id)).toEqual(['cert-2']);
  });

  it('should require a final warning confirmation before deleting the last active certificate', async () => {
    const certificate = fakeCertificate();
    await initWithCertificates([certificate]);

    await clickDeleteButton();

    const firstConfirmDialog = await getConfirmDialog();
    expect(await firstConfirmDialog?.getTitle()).toBe('Delete certificate');
    await firstConfirmDialog?.confirm();

    httpTestingController.expectNone(req => req.method === 'DELETE');
    flushCertificatesRequest([certificate], 1);

    const warningDialog = await getConfirmDialog();
    expect(warningDialog).not.toBeNull();
    expect(await warningDialog?.getTitle()).toBe('Warning');
    expect(await warningDialog?.getContent()).toContain(
      'There is no active certificate in case you proceed with the deletion. Do you want to proceed?',
    );
    httpTestingController.expectNone(req => req.method === 'DELETE');

    await warningDialog?.confirm();

    httpTestingController
      .expectOne({
        url: `${TESTING_BASE_URL}/applications/${applicationId}/certificates/${certificate.id}`,
        method: 'DELETE',
      })
      .flush(null);

    flushCertificatesRequest([], 0);

    expect(fixture.componentInstance.activeCertificates()).toEqual([]);
  });

  it('should keep the last active certificate untouched when the warning is cancelled', async () => {
    const certificate = fakeCertificate();
    await initWithCertificates([certificate]);

    await clickDeleteButton();

    const firstConfirmDialog = await getConfirmDialog();
    await firstConfirmDialog?.confirm();

    flushCertificatesRequest([certificate], 1);

    const warningDialog = await getConfirmDialog();
    expect(warningDialog).not.toBeNull();
    await warningDialog?.cancel();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(await getConfirmDialog()).toBeNull();
    httpTestingController.expectNone(req => req.method === 'DELETE');
    expect(fixture.componentInstance.activeCertificates().map(cert => cert.id)).toEqual([certificate.id]);
  });

  it('should delete a certificate from history after confirmation', async () => {
    const activeCertificate = fakeCertificate({ id: 'cert-active', status: 'ACTIVE' });
    const historyCertificate = fakeCertificate({ id: 'cert-history', status: 'REVOKED', name: 'Revoked certificate' });
    await initWithCertificates([activeCertificate, historyCertificate], 2);

    await clickHistoryTab();
    await clickHistoryDeleteButton();

    const confirmDialog = await getConfirmDialog();
    expect(confirmDialog).not.toBeNull();
    expect(await confirmDialog?.getTitle()).toBe('Delete certificate');
    await confirmDialog?.confirm();

    expect(await getConfirmDialog()).toBeNull();
    httpTestingController.expectNone(`${TESTING_BASE_URL}/applications/${applicationId}/certificates?page=1&size=2`);

    httpTestingController
      .expectOne({
        url: `${TESTING_BASE_URL}/applications/${applicationId}/certificates/${historyCertificate.id}`,
        method: 'DELETE',
      })
      .flush(null);

    flushCertificatesRequest([activeCertificate], 1);

    expect(fixture.componentInstance.historyCertificates()).toEqual([]);
  });

  it('should show a persistent inline error when certificate deletion fails', async () => {
    const certificates = [fakeCertificate({ id: 'cert-1' }), fakeCertificate({ id: 'cert-2' })];
    await initWithCertificates(certificates, 2);

    await clickDeleteButton();

    const confirmDialog = await getConfirmDialog();
    await confirmDialog?.confirm();
    httpTestingController.expectNone(`${TESTING_BASE_URL}/applications/${applicationId}/certificates?page=1&size=${certificates.length}`);

    httpTestingController
      .expectOne({
        url: `${TESTING_BASE_URL}/applications/${applicationId}/certificates/${certificates[0].id}`,
        method: 'DELETE',
      })
      .flush({ error: 'Delete failed' }, { status: 500, statusText: 'Internal Server Error' });

    fixture.detectChanges();
    await fixture.whenStable();

    expect(getErrorMessage()?.textContent).toContain('An error occurred while deleting the certificate. Please try again');
    expect(fixture.componentInstance.activeCertificates().map(cert => cert.id)).toEqual(['cert-1', 'cert-2']);
  });

  it('should clear delete error when retrying certificate deletion', async () => {
    const certificates = [fakeCertificate({ id: 'cert-1' }), fakeCertificate({ id: 'cert-2' })];
    await initWithCertificates(certificates, 2);

    await failDeleteAttempt(certificates[0]);
    expect(getErrorMessage()?.textContent).toContain('An error occurred while deleting the certificate. Please try again');

    await clickDeleteButton();

    expect(getErrorMessage()).toBeNull();

    const confirmDialog = await getConfirmDialog();
    await confirmDialog?.cancel();
  });

  it('should clear delete error after a successful certificates reload', async () => {
    const certificates = [fakeCertificate({ id: 'cert-1' }), fakeCertificate({ id: 'cert-2' })];
    await initWithCertificates(certificates, 2);

    await failDeleteAttempt(certificates[0]);
    expect(getErrorMessage()?.textContent).toContain('An error occurred while deleting the certificate. Please try again');

    fixture.componentInstance.loadCertificates();
    flushCertificatesRequest(certificates, 2);

    expect(getErrorMessage()).toBeNull();
  });

  function flushCertificatesRequest(data: ClientCertificate[] = [], totalElements = data.length, page = 1, size = 10): void {
    httpTestingController
      .expectOne({
        url: `${TESTING_BASE_URL}/applications/${applicationId}/certificates?page=${page}&size=${size}`,
        method: 'GET',
      })
      .flush({
        data,
        metadata: { paginateMetaData: { totalElements } },
      });
    fixture.detectChanges();
  }

  async function clickDeleteButton(index = 0): Promise<void> {
    const deleteButtons = fixture.nativeElement.querySelectorAll('[data-testid="paginated-table-action-delete-certificate-button"]');
    (deleteButtons[index] as HTMLButtonElement | undefined)?.click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  async function clickHistoryDeleteButton(index = 0): Promise<void> {
    const deleteButtons = fixture.nativeElement.querySelectorAll(
      '[data-testid="paginated-table-action-delete-history-certificate-button"]',
    );
    (deleteButtons[index] as HTMLButtonElement | undefined)?.click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  async function clickHistoryTab(): Promise<void> {
    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationTabSettingsCertificatesHarness);
    await harness.clickTab('history');
    fixture.detectChanges();
    await fixture.whenStable();
    flushCertificatesRequest(fixture.componentInstance.certificates(), fixture.componentInstance.totalElements());
  }

  async function getConfirmDialog(): Promise<ConfirmDialogHarness | null> {
    return rootLoader.getHarnessOrNull(ConfirmDialogHarness);
  }

  async function failDeleteAttempt(certificate: ClientCertificate): Promise<void> {
    await clickDeleteButton();

    const confirmDialog = await getConfirmDialog();
    await confirmDialog?.confirm();

    httpTestingController
      .expectOne({
        url: `${TESTING_BASE_URL}/applications/${applicationId}/certificates/${certificate.id}`,
        method: 'DELETE',
      })
      .flush({ error: 'Delete failed' }, { status: 500, statusText: 'Internal Server Error' });

    fixture.detectChanges();
    await fixture.whenStable();
  }

  function getErrorMessage(): HTMLElement | null {
    return fixture.nativeElement.querySelector('[data-testid="certificate-error"]');
  }
});
