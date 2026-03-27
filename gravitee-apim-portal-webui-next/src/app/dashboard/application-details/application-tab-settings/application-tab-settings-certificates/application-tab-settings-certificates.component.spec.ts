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
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { ApplicationTabSettingsCertificatesComponent } from './application-tab-settings-certificates.component';
import { ApplicationTabSettingsCertificatesHarness } from './application-tab-settings-certificates.harness';
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
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationTabSettingsCertificatesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
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
});
