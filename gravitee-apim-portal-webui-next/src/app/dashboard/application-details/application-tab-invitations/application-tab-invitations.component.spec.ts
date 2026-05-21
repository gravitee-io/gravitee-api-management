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

import { ApplicationTabInvitationsComponent } from './application-tab-invitations.component';
import { ApplicationTabInvitationsComponentHarness } from './application-tab-invitations.component.harness';
import { ApplicationInvitationsResponse } from '../../../../entities/application/application-invitation';
import {
  fakeApplicationInvitation,
  fakeApplicationInvitationsResponse,
} from '../../../../entities/application/application-invitation.fixture';
import { fakeUserApplicationPermissions } from '../../../../entities/permission/permission.fixtures';
import { ConfigService } from '../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('ApplicationTabInvitationsComponent', () => {
  let fixture: ComponentFixture<ApplicationTabInvitationsComponent>;
  let httpTestingController: HttpTestingController;
  const applicationId = 'app-1';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationTabInvitationsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationTabInvitationsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('applicationId', applicationId);
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R'] }));
  });

  afterEach(() => httpTestingController.verify());

  async function getHarness(): Promise<ApplicationTabInvitationsComponentHarness> {
    return TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationTabInvitationsComponentHarness);
  }

  async function flush(response: ApplicationInvitationsResponse): Promise<ApplicationTabInvitationsComponentHarness> {
    fixture.detectChanges();
    const request = httpTestingController.expectOne(req => req.url.includes('invitations/_search'));
    expect(request.request.method).toBe('POST');
    expect(request.request.params.get('page')).toBe(`${fixture.componentInstance.currentPage()}`);
    expect(request.request.params.get('size')).toBe(`${fixture.componentInstance.pageSize()}`);
    expect(request.request.body).toEqual({ filters: {} });
    request.flush(response);
    await fixture.whenStable();
    fixture.detectChanges();
    return getHarness();
  }

  it('should display invitations table', async () => {
    const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));
    expect(await harness.getPaginatedTable()).not.toBeNull();
  });

  it('should show invitation data columns and an empty actions column', () => {
    expect(fixture.componentInstance.tableColumns.map(column => column.id)).toEqual(['email', 'role', 'actions']);
  });

  it('should show section title with total invitation count from paginate metadata', async () => {
    await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()], 42));
    const harness = await getHarness();
    const title = await harness.getSectionTitle();
    expect(title).not.toBeNull();
    expect((await title!.text())?.replace(/\s+/g, ' ').trim()).toBe('Invitations (42)');
  });

  it('should fall back to pagination metadata total', async () => {
    await flush({
      data: [fakeApplicationInvitation()],
      metadata: {
        pagination: {
          total: 7,
        },
      },
    });
    const harness = await getHarness();
    const title = await harness.getSectionTitle();
    expect(title).not.toBeNull();
    expect((await title!.text())?.replace(/\s+/g, ' ').trim()).toBe('Invitations (7)');
  });

  it('should show Invitations (0) in section title when the list is empty', async () => {
    await flush(fakeApplicationInvitationsResponse([]));
    const harness = await getHarness();
    const title = await harness.getSectionTitle();
    expect(title).not.toBeNull();
    expect((await title!.text())?.replace(/\s+/g, ' ').trim()).toBe('Invitations (0)');
  });

  it('should show loader until the first search response', async () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loader')).not.toBeNull();
    const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));
    expect(await harness.getLoader()).toBeNull();
  });

  it('should show an accessible error message when search fails', async () => {
    fixture.detectChanges();
    const request = httpTestingController.expectOne(req => req.url.includes('invitations/_search'));
    request.flush({ error: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    const harness = await getHarness();
    const alert = await harness.getErrorMessage();
    expect(alert).not.toBeNull();
    expect(await alert!.getAttribute('role')).toBe('alert');
  });

  it('should show empty state when no invitations are returned', async () => {
    const harness = await flush(fakeApplicationInvitationsResponse([]));
    expect(await harness.getEmptyState()).not.toBeNull();
    expect(await harness.getPaginatedTable()).toBeNull();
  });

  it('should hide the table and not call service when user lacks MEMBER[R] permission', async () => {
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: [] }));
    fixture.detectChanges();
    httpTestingController.expectNone(req => req.url.includes('invitations/_search'));
    const harness = await getHarness();
    expect(await harness.getPaginatedTable()).toBeNull();
  });

  it('should render invitation email icon, email and role cells', async () => {
    const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ email: 'bob@example.com', role: 'POC' })]));
    const table = await harness.getPaginatedTable();
    const roleCell = await table!.getCellElement(0, 'role');
    const emailIcon = fixture.nativeElement.querySelector('[data-testid="invitation-email-icon"]');
    const email = fixture.nativeElement.querySelector('[data-testid="invitation-email"]');
    expect(emailIcon).not.toBeNull();
    expect(email.textContent.trim()).toBe('bob@example.com');
    expect((await roleCell!.text()).trim()).toBe('POC');
  });

  it('should render empty actions cell', async () => {
    const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));
    const table = await harness.getPaginatedTable();
    const actionsCell = await table!.getCellElement(0, 'actions');
    expect((await actionsCell!.text()).trim()).toBe('');
  });

  it('should POST with page=2 when page changes', async () => {
    await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()], 25));
    fixture.componentInstance.onPageChange(2);
    fixture.detectChanges();
    const request = httpTestingController.expectOne(req => req.url.includes('invitations/_search') && req.params.get('page') === '2');
    expect(request.request.method).toBe('POST');
    request.flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ id: 'invitation-p2' })]));
    await fixture.whenStable();
  });

  it('should reset page to 1 when page size changes', async () => {
    await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()], 25));
    fixture.componentInstance.onPageChange(2);
    fixture.detectChanges();
    httpTestingController
      .expectOne(req => req.url.includes('invitations/_search') && req.params.get('page') === '2')
      .flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ id: 'invitation-p2' })]));
    await fixture.whenStable();

    fixture.componentInstance.onPageSizeChange(25);
    fixture.detectChanges();
    const request = httpTestingController.expectOne(
      req => req.url.includes('invitations/_search') && req.params.get('page') === '1' && req.params.get('size') === '25',
    );
    request.flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ id: 'invitation-size-25' })]));
    await fixture.whenStable();
  });
});
