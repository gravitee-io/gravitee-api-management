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
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { ApplicationTabMembersComponent } from './application-tab-members.component';
import { MembersV2Response } from '../../../../entities/application-members/application-members';
import { fakeMembersResponse } from '../../../../entities/application-members/application-members.fixture';
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
    }).compileComponents();
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
});
