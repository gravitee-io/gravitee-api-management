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
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';

import { ApplicationTabInvitationsComponent } from './application-tab-invitations.component';
import { ApplicationTabInvitationsComponentHarness } from './application-tab-invitations.component.harness';
import { ConfirmDialogComponent } from '../../../../components/confirm-dialog/confirm-dialog.component';
import { ConfirmDialogHarness } from '../../../../components/confirm-dialog/confirm-dialog.harness';
import {
  ApplicationInvitationsResponse,
  ApplicationInvitationsSearchFilters,
} from '../../../../entities/application/application-invitation';
import {
  fakeApplicationInvitation,
  fakeApplicationInvitationsResponse,
} from '../../../../entities/application/application-invitation.fixture';
import { fakeUserApplicationPermissions } from '../../../../entities/permission/permission.fixtures';
import { ConfigService } from '../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';
import { ApplicationInvitationCreateDialogComponent } from '../application-invitation-create-dialog/application-invitation-create-dialog.component';
import {
  ApplicationInvitationEditDialogComponent,
  ApplicationInvitationEditDialogData,
} from '../application-invitation-edit-dialog/application-invitation-edit-dialog.component';

describe('ApplicationTabInvitationsComponent', () => {
  let fixture: ComponentFixture<ApplicationTabInvitationsComponent>;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;
  const applicationId = 'app-1';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationTabInvitationsComponent, ConfirmDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
      ],
    })
      .overrideProvider(InteractivityChecker, { useValue: { isFocusable: () => true, isTabbable: () => true } })
      .compileComponents();

    fixture = TestBed.createComponent(ApplicationTabInvitationsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.componentRef.setInput('applicationId', applicationId);
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R'] }));
  });

  afterEach(() => httpTestingController.verify());

  async function getHarness(): Promise<ApplicationTabInvitationsComponentHarness> {
    return TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationTabInvitationsComponentHarness);
  }

  async function flush(
    response: ApplicationInvitationsResponse,
    expectedFilters: ApplicationInvitationsSearchFilters = {},
  ): Promise<ApplicationTabInvitationsComponentHarness> {
    fixture.detectChanges();
    const request = httpTestingController.expectOne(req => req.url.includes('invitations/_search'));
    expect(request.request.method).toBe('POST');
    expect(request.request.params.get('page')).toBe(`${fixture.componentInstance.currentPage()}`);
    expect(request.request.params.get('size')).toBe(`${fixture.componentInstance.pageSize()}`);
    expect(request.request.body).toEqual({ filters: expectedFilters });
    request.flush(response);
    await fixture.whenStable();
    fixture.detectChanges();
    return getHarness();
  }

  it('should display invitations table', async () => {
    const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));
    expect(await harness.getPaginatedTable()).not.toBeNull();
  });

  it('should show invitation data columns', () => {
    expect(fixture.componentInstance.tableColumns.map(column => column.id)).toEqual(['email', 'role']);
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

  it('should not show invitation count before the first response is loaded', async () => {
    fixture.detectChanges();
    const title = fixture.nativeElement.querySelector('[data-testid="invitations-section-title"]');
    expect(title).not.toBeNull();
    expect(title.textContent?.replace(/\s+/g, ' ').trim()).toBe('Invitations');

    httpTestingController.expectOne(req => req.url.includes('invitations/_search')).flush(fakeApplicationInvitationsResponse([]));
    await fixture.whenStable();
  });

  it('should keep previous invitation count while a reload is pending', async () => {
    await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()], 12));

    fixture.componentInstance.onSearchTermChange('alice@example.com');
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector('[data-testid="invitations-section-title"]');
    expect(title).not.toBeNull();
    expect(title.textContent?.replace(/\s+/g, ' ').trim()).toBe('Invitations (12)');

    httpTestingController
      .expectOne(req => req.url.includes('invitations/_search'))
      .flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()], 3));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(title.textContent?.replace(/\s+/g, ' ').trim()).toBe('Invitations (3)');
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
    expect(await harness.getSearchBar()).toBeNull();
  });

  it('should show create invitation button when user has MEMBER[C] permission', async () => {
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'C'] }));

    const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

    expect(await harness.getCreateInvitationButton()).not.toBeNull();
  });

  it('should hide create invitation button when user lacks MEMBER[C] permission', async () => {
    const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

    expect(await harness.getCreateInvitationButton()).toBeNull();
  });

  it('should open create invitation dialog and reload invitations after successful creation', async () => {
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'C'] }));
    const afterClosed = new Subject<boolean>();
    const openDialogSpy = jest.spyOn(fixture.debugElement.injector.get(MatDialog), 'open').mockReturnValue({
      afterClosed: () => afterClosed.asObservable(),
    } as MatDialogRef<ApplicationInvitationCreateDialogComponent, boolean>);
    const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

    await harness.clickCreateInvitation();
    fixture.detectChanges();

    expect(openDialogSpy).toHaveBeenCalledWith(ApplicationInvitationCreateDialogComponent, {
      data: { applicationId },
      disableClose: true,
    });

    afterClosed.next(true);
    afterClosed.complete();
    fixture.detectChanges();
    httpTestingController
      .expectOne(req => req.url.includes('invitations/_search'))
      .flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()], 2));
    await fixture.whenStable();
  });

  describe('search behavior', () => {
    it('should render search bar when canRead', async () => {
      const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));
      expect(await harness.getSearchBar()).not.toBeNull();
    });

    it('should send email filter when search term is set', async () => {
      await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

      fixture.componentInstance.onSearchTermChange(' alice@example.com ');
      fixture.detectChanges();

      const request = httpTestingController.expectOne(req => req.url.includes('invitations/_search') && req.params.get('page') === '1');
      expect(request.request.body).toEqual({ filters: { email: 'alice@example.com' } });
      request.flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ email: 'alice@example.com' })]));
      await fixture.whenStable();
    });

    it('should reset page to 1 when search term changes', async () => {
      await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()], 25));
      fixture.componentInstance.onPageChange(2);
      fixture.detectChanges();
      httpTestingController
        .expectOne(req => req.url.includes('invitations/_search') && req.params.get('page') === '2')
        .flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ id: 'invitation-p2' })]));
      await fixture.whenStable();

      fixture.componentInstance.onSearchTermChange('alice@example.com');
      fixture.detectChanges();

      const request = httpTestingController.expectOne(req => req.url.includes('invitations/_search'));
      expect(request.request.params.get('page')).toBe('1');
      expect(request.request.body).toEqual({ filters: { email: 'alice@example.com' } });
      request.flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ email: 'alice@example.com' })]));
      await fixture.whenStable();
    });

    it('should show no-match empty state when search is active and returns no invitations', async () => {
      await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

      fixture.componentInstance.onSearchTermChange('missing@example.com');
      fixture.detectChanges();
      httpTestingController.expectOne(req => req.url.includes('invitations/_search')).flush(fakeApplicationInvitationsResponse([]));
      await fixture.whenStable();
      fixture.detectChanges();

      const harness = await getHarness();
      expect(await harness.getSearchNoMatch()).not.toBeNull();
      expect(await harness.getEmptyState()).toBeNull();
      expect(await harness.getPaginatedTable()).toBeNull();
    });

    it('should send no filters after clearing the search term', async () => {
      await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));
      fixture.componentInstance.onSearchTermChange('alice@example.com');
      fixture.detectChanges();
      httpTestingController
        .expectOne(req => req.url.includes('invitations/_search'))
        .flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));
      await fixture.whenStable();

      fixture.componentInstance.onSearchTermChange('');
      fixture.detectChanges();

      const request = httpTestingController.expectOne(req => req.url.includes('invitations/_search'));
      expect(request.request.body).toEqual({ filters: {} });
      request.flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));
      await fixture.whenStable();
    });

    it('should keep active search filter when page size changes', async () => {
      await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()], 25));
      fixture.componentInstance.onSearchTermChange('alice@example.com');
      fixture.detectChanges();
      httpTestingController
        .expectOne(req => req.url.includes('invitations/_search'))
        .flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));
      await fixture.whenStable();

      fixture.componentInstance.onPageSizeChange(25);
      fixture.detectChanges();

      const request = httpTestingController.expectOne(
        req => req.url.includes('invitations/_search') && req.params.get('page') === '1' && req.params.get('size') === '25',
      );
      expect(request.request.body).toEqual({ filters: { email: 'alice@example.com' } });
      request.flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));
      await fixture.whenStable();
    });
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

  describe('edit action', () => {
    it('should not show edit button when user lacks MEMBER[U] permission', async () => {
      const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

      expect(await harness.getEditInvitationButton()).toBeNull();
    });

    it('should show edit button when user has MEMBER[U] permission', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));

      const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

      expect(await harness.getEditInvitationButton()).not.toBeNull();
    });

    it('should open edit dialog and reload invitations after successful update', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));
      const afterClosed = new Subject<boolean>();
      const invitation = fakeApplicationInvitation({ id: 'invitation-1', email: 'alice@example.com', role: 'USER' });
      const openDialogSpy = jest.spyOn(fixture.debugElement.injector.get(MatDialog), 'open').mockReturnValue({
        afterClosed: () => afterClosed.asObservable(),
      } as MatDialogRef<ApplicationInvitationEditDialogComponent, boolean>);
      const harness = await flush(fakeApplicationInvitationsResponse([invitation]));

      await harness.clickEditInvitation();
      fixture.detectChanges();

      expect(openDialogSpy).toHaveBeenCalledWith(ApplicationInvitationEditDialogComponent, {
        data: {
          applicationId,
          invitationId: invitation.id,
          invitationEmail: invitation.email,
          currentRole: invitation.role,
        } satisfies ApplicationInvitationEditDialogData,
      });

      afterClosed.next(true);
      afterClosed.complete();
      fixture.detectChanges();
      httpTestingController
        .expectOne(req => req.url.includes('invitations/_search'))
        .flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ id: invitation.id, role: 'OWNER' })]));
      await fixture.whenStable();
    });
  });

  describe('resend action', () => {
    it('should not show resend button when user lacks MEMBER[U] permission', async () => {
      const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

      expect(await harness.getResendInvitationButton()).toBeNull();
    });

    it('should show resend button when user has MEMBER[U] permission', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));

      const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

      expect(await harness.getResendInvitationButton()).not.toBeNull();
    });

    it('should open confirmation dialog on resend click', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));
      const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ email: 'alice@example.com' })]));

      await harness.clickResendInvitation();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      expect(dialog).not.toBeNull();
      expect(await dialog!.getTitle()).toBe('Resend invitation?');
      expect(await dialog!.getContent()).toContain('alice@example.com');
      expect(await dialog!.getConfirmText()).toBe('Resend');
      expect(await dialog!.getCancelText()).toBe('Cancel');
      await dialog!.cancel();
    });

    it('should not send resend request when dialog is cancelled', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));
      const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

      await harness.clickResendInvitation();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      await dialog!.cancel();
      fixture.detectChanges();

      httpTestingController.expectNone(r => r.url.includes('_resend'));
    });

    it('should send resend request, disable row action, reload list and show success message on confirm', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));
      const invitation = fakeApplicationInvitation({ id: 'invitation-1', email: 'alice@example.com' });
      const harness = await flush(fakeApplicationInvitationsResponse([invitation]));

      await harness.clickResendInvitation();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      await dialog!.confirm();
      fixture.detectChanges();

      const request = httpTestingController.expectOne(
        r => r.url === `${TESTING_BASE_URL}/applications/${applicationId}/invitations/${invitation.id}/_resend` && r.method === 'POST',
      );
      expect(request.request.body).toEqual({
        confirmation_page_url: `${globalThis.location.origin}/user/invitation/confirm`,
      });
      expect(await (await harness.getResendInvitationButton())!.isDisabled()).toBe(true);
      request.flush(null, { status: 204, statusText: 'No Content' });
      fixture.detectChanges();

      httpTestingController.expectOne(r => r.url.includes('invitations/_search')).flush(fakeApplicationInvitationsResponse([invitation]));
      await fixture.whenStable();
      fixture.detectChanges();

      const success = await harness.getActionSuccessMessage();
      expect(success).not.toBeNull();
      expect(await success!.getAttribute('aria-live')).toBe('polite');
      expect(await success!.text()).toContain('Invitation email resent to alice@example.com');
    });

    it('should show inline error when resend fails', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'U'] }));
      const invitation = fakeApplicationInvitation({ id: 'invitation-1' });
      const harness = await flush(fakeApplicationInvitationsResponse([invitation]));

      await harness.clickResendInvitation();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      await dialog!.confirm();
      fixture.detectChanges();

      httpTestingController
        .expectOne(
          r => r.url === `${TESTING_BASE_URL}/applications/${applicationId}/invitations/${invitation.id}/_resend` && r.method === 'POST',
        )
        .flush({ error: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });
      await fixture.whenStable();
      fixture.detectChanges();

      const error = await harness.getActionErrorMessage();
      expect(error).not.toBeNull();
      expect(await error!.getAttribute('role')).toBe('alert');
      expect(await error!.text()).toContain('An error occurred while resending the invitation');
    });
  });

  describe('delete action', () => {
    it('should not show delete button when user lacks MEMBER[D] permission', async () => {
      const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

      expect(await harness.getDeleteInvitationButton()).toBeNull();
    });

    it('should show delete button when user has MEMBER[D] permission', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'D'] }));

      const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

      expect(await harness.getDeleteInvitationButton()).not.toBeNull();
    });

    it('should open confirmation dialog on delete click', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'D'] }));
      const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ email: 'alice@example.com' })]));

      await harness.clickDeleteInvitation();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      expect(dialog).not.toBeNull();
      expect(await dialog!.getTitle()).toBe('Delete invitation?');
      expect(await dialog!.getContent()).toContain('alice@example.com');
      await dialog!.cancel();
    });

    it('should not send DELETE request when dialog is cancelled', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'D'] }));
      const harness = await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation()]));

      await harness.clickDeleteInvitation();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      await dialog!.cancel();
      fixture.detectChanges();

      httpTestingController.expectNone(r => r.method === 'DELETE');
    });

    it('should send DELETE request and reload list on confirm', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'D'] }));
      const invitation = fakeApplicationInvitation({ id: 'invitation-1', email: 'alice@example.com' });
      const harness = await flush(fakeApplicationInvitationsResponse([invitation]));

      await harness.clickDeleteInvitation();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      await dialog!.confirm();
      fixture.detectChanges();

      httpTestingController
        .expectOne(r => r.url === `${TESTING_BASE_URL}/applications/${applicationId}/invitations/${invitation.id}` && r.method === 'DELETE')
        .flush(null, { status: 204, statusText: 'No Content' });
      fixture.detectChanges();

      httpTestingController.expectOne(r => r.url.includes('invitations/_search')).flush(fakeApplicationInvitationsResponse([]));
      await fixture.whenStable();
    });

    it('should move to previous page after deleting the last invitation from the current page', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'D'] }));
      const invitation = fakeApplicationInvitation({ id: 'invitation-last-page' });
      await flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ id: 'invitation-page-1' })], 11));
      fixture.componentInstance.onPageChange(2);
      fixture.detectChanges();
      const pageTwoRequest = httpTestingController.expectOne(r => r.url.includes('invitations/_search') && r.params.get('page') === '2');
      pageTwoRequest.flush(fakeApplicationInvitationsResponse([invitation], 11));
      await fixture.whenStable();
      fixture.detectChanges();
      const harness = await getHarness();

      await harness.clickDeleteInvitation();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      await dialog!.confirm();
      fixture.detectChanges();

      httpTestingController
        .expectOne(r => r.url === `${TESTING_BASE_URL}/applications/${applicationId}/invitations/${invitation.id}` && r.method === 'DELETE')
        .flush(null, { status: 204, statusText: 'No Content' });
      fixture.detectChanges();

      const reloadRequest = httpTestingController.expectOne(r => r.url.includes('invitations/_search') && r.params.get('page') === '1');
      expect(reloadRequest.request.params.get('size')).toBe('10');
      reloadRequest.flush(fakeApplicationInvitationsResponse([fakeApplicationInvitation({ id: 'remaining-invitation' })], 10));
      await fixture.whenStable();
    });

    it('should show inline error when delete fails', async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'D'] }));
      const invitation = fakeApplicationInvitation({ id: 'invitation-1' });
      const harness = await flush(fakeApplicationInvitationsResponse([invitation]));

      await harness.clickDeleteInvitation();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      await dialog!.confirm();
      fixture.detectChanges();

      httpTestingController
        .expectOne(r => r.url === `${TESTING_BASE_URL}/applications/${applicationId}/invitations/${invitation.id}` && r.method === 'DELETE')
        .flush({ error: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });
      await fixture.whenStable();
      fixture.detectChanges();

      const error = await harness.getActionErrorMessage();
      expect(error).not.toBeNull();
      expect(await error!.getAttribute('role')).toBe('alert');
      expect(await error!.text()).toContain('An error occurred while deleting the invitation');
    });
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
