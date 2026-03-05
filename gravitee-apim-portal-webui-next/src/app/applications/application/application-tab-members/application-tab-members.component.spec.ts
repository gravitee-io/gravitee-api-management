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
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { ApplicationTabMembersComponent } from './application-tab-members.component';
import { ConfirmDialogHarness } from '../../../../components/confirm-dialog/confirm-dialog.harness';
import { MembersV2Response } from '../../../../entities/application-members/application-members';
import { fakeMember, fakeMembersResponse } from '../../../../entities/application-members/application-members.fixture';
import { fakeUserApplicationPermissions } from '../../../../entities/permission/permission.fixtures';
import { ConfigService } from '../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('ApplicationTabMembersComponent', () => {
  let fixture: ComponentFixture<ApplicationTabMembersComponent>;
  let http: HttpTestingController;
  const applicationId = 'app-123';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationTabMembersComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: { isFocusable: () => true, isTabbable: () => true },
      })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApplicationTabMembersComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('applicationId', applicationId);
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R', 'C', 'U', 'D'] }));
  });

  afterEach(() => http.verify());

  async function setup(response: MembersV2Response = fakeMembersResponse()): Promise<void> {
    fixture.detectChanges();
    http.expectOne(req => req.url.includes(`/applications/${applicationId}/membersV2`)).flush(response);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function flushMembers(response: MembersV2Response = fakeMembersResponse()): void {
    http.match(req => req.url.includes(`/applications/${applicationId}/membersV2`)).forEach(req => req.flush(response));
  }

  describe('with members', () => {
    beforeEach(async () => {
      await setup();
    });

    it('should render members section', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('[data-testid="application-tab-members"]')).toBeTruthy();
    });

    it('should display members title', () => {
      const el: HTMLElement = fixture.nativeElement;
      const header = el.querySelector('.application-tab-members__header');
      expect(header?.textContent).toContain('Members');
    });

    it('should display search input', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('[data-testid="members-search-input"]')).toBeTruthy();
    });

    it('should display Transfer Ownership button', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('[data-testid="transfer-ownership-btn"]')).toBeTruthy();
    });

    it('should display Add Members button when user has CREATE permission', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('[data-testid="add-members-btn"]')).toBeTruthy();
    });

    it('should render paginated table', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('app-paginated-table')).toBeTruthy();
    });

    it('should have correct number of row items', () => {
      expect(fixture.componentInstance.rows().length).toBe(3);
    });
  });

  describe('with empty results', () => {
    beforeEach(async () => {
      await setup({ data: [], metadata: { pagination: { total: 0 } } });
    });

    it('should show empty state', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.application-tab-members__empty-state')).toBeTruthy();
      expect(el.textContent).toContain('No members yet');
    });

    it('should not show paginated table', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('app-paginated-table')).toBeFalsy();
    });
  });

  describe('without CREATE permission', () => {
    beforeEach(async () => {
      fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R'] }));
      await setup();
    });

    it('should not display Add Members button', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('[data-testid="add-members-btn"]')).toBeFalsy();
    });

    it('should still display Transfer Ownership button', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('[data-testid="transfer-ownership-btn"]')).toBeTruthy();
    });
  });

  describe('search', () => {
    beforeEach(async () => {
      await setup();
    });

    it('should trigger refetch on search input', () => {
      const el: HTMLElement = fixture.nativeElement;
      const input = el.querySelector<HTMLInputElement>('[data-testid="members-search-input"]')!;
      input.value = 'admin';
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      const req = http.expectOne(
        r => r.url.includes(`/applications/${applicationId}/membersV2`) && r.params.get('query') === 'admin',
      );
      expect(req.request.method).toEqual('GET');
      req.flush(fakeMembersResponse());
    });
  });

  describe('pagination', () => {
    beforeEach(async () => {
      await setup();
    });

    it('should update page on page change', () => {
      fixture.componentInstance.onPageChange(2);
      expect(fixture.componentInstance.currentPage()).toBe(2);
    });

    it('should reset to page 1 on page size change', () => {
      fixture.componentInstance.onPageChange(3);
      fixture.componentInstance.onPageSizeChange(25);
      expect(fixture.componentInstance.pageSize()).toBe(25);
      expect(fixture.componentInstance.currentPage()).toBe(1);
    });
  });

  describe('delete member', () => {
    let rootLoader: HarnessLoader;

    beforeEach(async () => {
      rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      await setup();
    });

    function clickDeleteButton(): void {
      const el: HTMLElement = fixture.nativeElement;
      const deleteBtn = el.querySelector<HTMLButtonElement>('[data-testid="action-delete"]');
      expect(deleteBtn).toBeTruthy();
      deleteBtn!.click();
      fixture.detectChanges();
    }

    it('should open confirm dialog on delete click', async () => {
      clickDeleteButton();
      const dialog = await rootLoader.getHarnessOrNull(ConfirmDialogHarness);
      expect(dialog).not.toBeNull();
    });

    it('should not call API when cancel is clicked', async () => {
      clickDeleteButton();
      const dialog = await rootLoader.getHarness(ConfirmDialogHarness);
      await dialog.cancel();
      http.expectNone(req => req.method === 'DELETE');
    });

    it('should call DELETE API and reload table on confirm', async () => {
      const deletableMember = fakeMember({ id: 'member-2' });
      clickDeleteButton();
      const dialog = await rootLoader.getHarness(ConfirmDialogHarness);
      await dialog.confirm();

      const deleteReq = http.expectOne(req => req.method === 'DELETE' && req.url.includes(`/membersV2/${deletableMember.id}`));
      deleteReq.flush(null);

      flushMembers();
    });
  });

  describe('primary owner actions', () => {
    beforeEach(async () => {
      const poOnlyResponse = fakeMembersResponse({
        data: [fakeMember({ role: 'PRIMARY_OWNER' })],
        metadata: { pagination: { total: 1, current_page: 1, size: 1, first: 1, last: 1, total_pages: 1 } },
      });
      await setup(poOnlyResponse);
    });

    it('should not show action buttons for PRIMARY_OWNER', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('[data-testid="action-edit"]')).toBeFalsy();
      expect(el.querySelector('[data-testid="action-delete"]')).toBeFalsy();
    });
  });
});
