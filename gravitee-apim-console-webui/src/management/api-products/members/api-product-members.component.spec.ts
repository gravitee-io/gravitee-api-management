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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { ApiProductMembersComponent, GROUP_LIST_PAGE_SIZE } from './api-product-members.component';
import { ApiProductGroupsDialogHarness } from './api-product-groups/api-product-groups.component.harness';
import { ApiProductMembersComponentHarness } from './api-product-members.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { Member, fakeBaseGroup, fakeGroup } from '../../../entities/management-api-v2';
import { fakeMember } from '../../../entities/management-api-v2/member/member.fixture';

describe('ApiProductMembersComponent', () => {
  const API_PRODUCT_ID = 'test-api-product-id';
  const MEMBERS_URL = `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/members`;
  const GROUPS_LIST_URL = `${CONSTANTS_TESTING.env.v2BaseURL}/groups`;
  const ROLES_URL = `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/API_PRODUCT/roles`;

  let fixture: ComponentFixture<ApiProductMembersComponent>;
  let harness: ApiProductMembersComponentHarness;
  let httpTestingController: HttpTestingController;
  const snackBarService = { error: jest.fn(), success: jest.fn() };

  const queryParams$ = new BehaviorSubject<Record<string, string>>({});

  async function init(
    permissions: string[] = ['api_product-member-r', 'api_product-member-c'],
    initialQueryParams: Record<string, string> = {},
  ) {
    await TestBed.configureTestingModule({
      imports: [ApiProductMembersComponent, NoopAnimationsModule, GioTestingModule, MatIconTestingModule, MatDialogModule],
      providers: [
        { provide: GioTestingPermissionProvider, useValue: permissions },
        { provide: SnackBarService, useValue: snackBarService },
        {
          provide: InteractivityChecker,
          useValue: { isFocusable: () => true, isTabbable: () => true },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: new BehaviorSubject(convertToParamMap({ apiProductId: API_PRODUCT_ID })),
            queryParams: queryParams$,
            snapshot: {
              paramMap: convertToParamMap({ apiProductId: API_PRODUCT_ID }),
              queryParams: initialQueryParams,
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductMembersComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductMembersComponentHarness);
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
    queryParams$.next({});
  });

  function expectMembersPageRequests(
    members = [fakeMember()],
    totalCount = members.length,
    page = 1,
    perPage = 10,
    apiProductExtras: Record<string, unknown> = {},
    groups = [fakeGroup({ id: 'group-1', name: 'Group One' })],
  ) {
    const pending = httpTestingController.match(
      req => req.method === 'GET' && !req.url.includes('/configuration/rolescopes/API_PRODUCT/roles'),
    );
    expect(pending.length).toBe(3);
    for (const req of pending) {
      const url = req.request.url;
      if (url.includes(`/api-products/${API_PRODUCT_ID}/members`)) {
        expect(req.request.params.get('page')).toEqual(page.toString());
        expect(req.request.params.get('perPage')).toEqual(perPage.toString());
        req.flush({ data: members, pagination: { totalCount, page, perPage } });
      } else if (url.includes(`/api-products/${API_PRODUCT_ID}`) && !url.includes('/members')) {
        req.flush({
          id: API_PRODUCT_ID,
          name: 'Test API Product',
          version: '1.0',
          apiIds: [],
          groups: [],
          ...apiProductExtras,
        });
      } else if (url.includes(GROUPS_LIST_URL)) {
        req.flush({
          data: groups,
          pagination: { totalCount: groups.length, page: 1, perPage: GROUP_LIST_PAGE_SIZE },
        });
      } else {
        fail(`Unexpected GET ${url}`);
      }
    }
  }

  function expectMembersPageLoadFailure(errorMessage = 'Could not load members list', page = 1, perPage = 10) {
    const pending = httpTestingController.match(
      req => req.method === 'GET' && !req.url.includes('/configuration/rolescopes/API_PRODUCT/roles'),
    );
    expect(pending.length).toBe(3);
    const membersReq = pending.find(r => r.request.url.includes(`/api-products/${API_PRODUCT_ID}/members`));
    const apiProductReq = pending.find(
      r => r.request.url.includes(`/api-products/${API_PRODUCT_ID}`) && !r.request.url.includes('/members'),
    );
    const groupsReq = pending.find(r => r.request.url.includes(GROUPS_LIST_URL));
    expect(membersReq).toBeTruthy();
    expect(apiProductReq).toBeTruthy();
    expect(groupsReq).toBeTruthy();
    expect(membersReq!.request.params.get('page')).toEqual(page.toString());
    expect(membersReq!.request.params.get('perPage')).toEqual(perPage.toString());

    apiProductReq!.flush({
      id: API_PRODUCT_ID,
      name: 'Test API Product',
      version: '1.0',
      apiIds: [],
      groups: [],
    });
    groupsReq!.flush({
      data: [],
      pagination: { totalCount: 0, page: 1, perPage: GROUP_LIST_PAGE_SIZE },
    });
    membersReq!.flush({ message: errorMessage }, { status: 500, statusText: 'Internal Server Error' });
  }

  function expectRolesRequest() {
    const req = httpTestingController.expectOne(ROLES_URL);
    req.flush([
      { id: 'PRIMARY_OWNER', name: 'PRIMARY_OWNER', default: false, scope: 'API_PRODUCT' },
      { id: 'USER', name: 'USER', default: true, scope: 'API_PRODUCT' },
      { id: 'OWNER', name: 'OWNER', default: false, scope: 'API_PRODUCT' },
    ]);
  }

  function flushGroupInheritedMemberRequests(groupIds: string[]) {
    for (const groupId of groupIds) {
      const requests = httpTestingController.match(r => r.method === 'GET' && r.url.includes(`/groups/${groupId}/members`));
      expect(requests.length).toBeGreaterThan(0);
      for (const req of requests) {
        req.flush({ data: [], metadata: { groupName: groupId }, pagination: { totalCount: 0 } });
      }
    }
  }

  function flushGroupInheritedMemberForbidden(groupIds: string[]) {
    for (const groupId of groupIds) {
      const requests = httpTestingController.match(r => r.method === 'GET' && r.url.includes(`/groups/${groupId}/members`));
      expect(requests.length).toBeGreaterThan(0);
      for (const req of requests) {
        req.flush(
          { httpStatus: 403, message: 'You do not have the permissions to access this resource' },
          { status: 403, statusText: 'Forbidden' },
        );
      }
    }
  }

  function flushGroupInheritedMemberRequestsWithMembers(groupId: string, members: Member[]) {
    const requests = httpTestingController.match(r => r.method === 'GET' && r.url.includes(`/groups/${groupId}/members`));
    expect(requests.length).toBeGreaterThan(0);
    const totalCount = members.length;
    for (const req of requests) {
      req.flush({ data: members, metadata: { groupName: groupId }, pagination: { totalCount, page: 1, perPage: 10 } });
    }
  }

  it('should render the Members section with title and description', fakeAsync(async () => {
    await init();
    expectMembersPageRequests([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(await harness.getTitle()).toBe('Members');
    expect(await harness.getDescription()).toContain('Manage who can interact with your API Product');
  }));

  it('should disable notification toggle when read-only', fakeAsync(async () => {
    await init(['api_product-member-r']);
    expectMembersPageRequests([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(await harness.isMembersNotificationToggleDisabled()).toBe(true);
  }));

  it('should render picture, Name and Role column headers', fakeAsync(async () => {
    await init();
    expectMembersPageRequests([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    const table = await harness.getMembersTable();
    const headerRows = await table.getHeaderRows();
    const cells = await headerRows[0].getCells();
    const headerTexts = await Promise.all(cells.map(c => c.getText()));
    expect(headerTexts).toEqual(['', 'Name', 'Role']);
  }));

  it('should render delete column when user has delete permission', fakeAsync(async () => {
    await init(['api_product-member-r', 'api_product-member-c', 'api_product-member-d', 'api_product-member-u']);
    expectMembersPageRequests([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    const table = await harness.getMembersTable();
    const headerRows = await table.getHeaderRows();
    const cells = await headerRows[0].getCells();
    const headerTexts = await Promise.all(cells.map(c => c.getText()));
    expect(headerTexts).toEqual(['', 'Name', 'Role', '']);
  }));

  it('should display existing members in the table', fakeAsync(async () => {
    await init();
    const member = fakeMember({ id: 'user-1', displayName: 'Alice', roles: [{ name: 'USER' }] });
    expectMembersPageRequests([member]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    const table = await harness.getMembersTable();
    const rows = await table.getRows();
    expect(rows.length).toBe(1);
    const cells = await rows[0].getCells();
    const texts = await Promise.all(cells.map(c => c.getText()));
    expect(texts[1]).toBe('Alice');
    expect(texts[2]).toContain('USER');
  }));

  it('should show Primary Owner role in the Role column', fakeAsync(async () => {
    await init();
    const owner = fakeMember({ id: 'owner-1', displayName: 'Admin', roles: [{ name: 'PRIMARY_OWNER' }] });
    expectMembersPageRequests([owner]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    const table = await harness.getMembersTable();
    const rows = await table.getRows();
    const cells = await rows[0].getCells();
    expect(await cells[2].getText()).toContain('PRIMARY_OWNER');
  }));

  it('should show "No members found" when there are no members', fakeAsync(async () => {
    await init();
    expectMembersPageRequests([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(await harness.getEmptyMessage()).toBe('No members found');
  }));

  it('should surface load failure with message and retry (not empty state)', fakeAsync(async () => {
    await init();
    expectMembersPageLoadFailure('Members endpoint unavailable');
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(snackBarService.error).not.toHaveBeenCalled();
    expect(await harness.getLoadErrorMessage()).toBe('Members endpoint unavailable');
    expect(await harness.getEmptyMessageOrNull()).toBeNull();

    await harness.clickRetryMembersLoad();
    tick();
    fixture.detectChanges();

    expectMembersPageRequests([]);
    tick();
    fixture.detectChanges();

    expect(await harness.getLoadErrorMessage()).toBeNull();
    expect(await harness.getEmptyMessage()).toBe('No members found');
  }));

  it('should show "Add members" button when user has create permission', fakeAsync(async () => {
    await init(['api_product-member-r', 'api_product-member-c']);
    expectMembersPageRequests([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(await harness.getAddMemberButton()).not.toBeNull();
  }));

  it('should expose Manage groups when permitted and groups exist', fakeAsync(async () => {
    await init(['api_product-member-r', 'api_product-member-u']);
    expectMembersPageRequests([], 0, 1, 10, { groups: ['group-1'] }, [fakeGroup({ id: 'group-1', name: 'Group One' })]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();
    flushGroupInheritedMemberRequests(['group-1']);
    tick();
    fixture.detectChanges();

    expect(await harness.getManageGroupsButton()).not.toBeNull();
  }));

  it('should open Manage groups dialog with editable groups select and save when user has api_product-member-u', fakeAsync(async () => {
    await init(['api_product-member-r', 'api_product-member-u']);
    expectMembersPageRequests([], 0, 1, 10, { groups: ['group-1'] }, [fakeGroup({ id: 'group-1', name: 'Group One' })]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();
    flushGroupInheritedMemberRequests(['group-1']);
    tick();
    fixture.detectChanges();

    await harness.clickManageGroups();
    tick();
    fixture.detectChanges();

    const rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    const dialogBody = await rootLoader.getHarness(ApiProductGroupsDialogHarness);
    expect(await dialogBody.getHeadingTitle()).toBe('Manage groups');
    expect(await dialogBody.getGroupsSelect()).not.toBeNull();
    expect(await dialogBody.getSaveButton()).not.toBeNull();
  }));

  it('should PUT API Product with selected groups when Manage groups save is confirmed', fakeAsync(async () => {
    await init(['api_product-member-r', 'api_product-member-u']);
    const g1 = fakeGroup({ id: 'group-1', name: 'Group One' });
    const g2 = fakeGroup({ id: 'group-2', name: 'Group Two' });
    expectMembersPageRequests([], 0, 1, 10, { groups: ['group-1'] }, [g1, g2]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();
    flushGroupInheritedMemberRequestsWithMembers('group-1', [
      fakeMember({ id: 'gm-1', displayName: 'Inherited User', roles: [{ name: 'USER', scope: 'API_PRODUCT' }] }),
    ]);
    tick();
    fixture.detectChanges();

    await harness.clickManageGroups();
    tick();
    fixture.detectChanges();

    const rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    const dialogBody = await rootLoader.getHarness(ApiProductGroupsDialogHarness);
    expect(await dialogBody.getHeadingTitle()).toBe('Manage groups');
    expect(await dialogBody.getGroupsSelect()).not.toBeNull();
    expect(await dialogBody.getSaveButton()).not.toBeNull();

    await dialogBody.clickSave();
    tick();
    fixture.detectChanges();

    const putReq = httpTestingController.expectOne(
      req => req.method === 'PUT' && req.url === `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`,
    );
    expect(putReq.request.body.groups).toEqual(['group-1']);
    putReq.flush({
      id: API_PRODUCT_ID,
      name: 'Test API Product',
      version: '1.0',
      apiIds: [],
      groups: ['group-1'],
    });
    tick();
    fixture.detectChanges();

    expectMembersPageRequests([], 0, 1, 10, { groups: ['group-1'] }, [g1, g2]);
    tick();
    fixture.detectChanges();
  }));

  it('should show permission message on inherited group card when group members API returns 403', fakeAsync(async () => {
    await init(['api_product-member-r', 'api_product-member-u']);
    expectMembersPageRequests([], 0, 1, 10, { groups: ['group-1'] }, [fakeGroup({ id: 'group-1', name: 'Group One' })]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();
    flushGroupInheritedMemberForbidden(['group-1']);
    tick();
    fixture.detectChanges();

    const cards = await harness.getInheritedGroupMemberCards();
    expect(cards.length).toBe(1);
    expect(await cards[0].getPermissionDeniedMessage()).toContain('appropriate permissions');
    expect(await cards[0].getInheritedMembersTable()).toBeNull();
  }));

  it('should render inherited group member rows on group card when API returns members', fakeAsync(async () => {
    await init(['api_product-member-r', 'api_product-member-u']);
    expectMembersPageRequests([], 0, 1, 10, { groups: ['group-1'] }, [fakeGroup({ id: 'group-1', name: 'Group One' })]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();
    flushGroupInheritedMemberRequestsWithMembers('group-1', [
      fakeMember({ id: 'gm-1', displayName: 'Inherited User', roles: [{ name: 'USER', scope: 'API_PRODUCT' }] }),
    ]);
    tick();
    fixture.detectChanges();

    const cards = await harness.getInheritedGroupMemberCards();
    expect(cards.length).toBe(1);
    expect(await cards[0].getCardHeading()).toContain('Group One');
    expect(await cards[0].getCardHeading()).toContain('inherited members');
    const table = await cards[0].getInheritedMembersTable();
    expect(table).not.toBeNull();
    const rows = await table!.getRows();
    expect(rows.length).toBe(1);
    const cells = await rows[0].getCells();
    const cellTexts = await Promise.all(cells.map(c => c.getText()));
    expect(cellTexts[1]).toContain('Inherited User');
    expect(cellTexts[2]).toContain('USER');
  }));

  it('should hide "Add members" button when user lacks create permission', fakeAsync(async () => {
    await init(['api_product-member-r']);
    expectMembersPageRequests([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(await harness.getAddMemberButton()).toBeNull();
  }));

  it('should update URL query params when pagination changes', fakeAsync(async () => {
    await init();
    const router = TestBed.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigate');

    expectMembersPageRequests([fakeMember({ id: 'm1', roles: [{ name: 'USER' }] })], 50, 1, 10);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    const tableWrapper = await harness.getTableWrapper();
    const paginator = await tableWrapper.getPaginator('header');
    await paginator!.setPageSize(20);
    await paginator!.goToNextPage();
    tick(300);
    fixture.detectChanges();

    expect(navigateSpy).toHaveBeenCalledWith(
      [],
      expect.objectContaining({ queryParams: { page: 2, size: 20 }, queryParamsHandling: 'merge' }),
    );
  }));

  it('should remove member and reload from server', fakeAsync(async () => {
    await init(['api_product-member-r', 'api_product-member-c', 'api_product-member-d']);

    const onlyMember = fakeMember({ id: 'last-member', displayName: 'Last Member', roles: [{ name: 'USER' }] });
    expectMembersPageRequests([onlyMember], 11, 1, 10);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    await (await harness.getDeleteMemberButton()).click();
    tick();
    fixture.detectChanges();

    const confirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
    await confirmDialog.confirm();
    tick();
    fixture.detectChanges();

    httpTestingController.expectOne(req => req.url === `${MEMBERS_URL}/last-member` && req.method === 'DELETE').flush({});
    tick();
    fixture.detectChanges();

    expectMembersPageRequests([], 10, 1, 10);
    tick();
    fixture.detectChanges();

    expect(await (await harness.getMembersTable()).getRows()).toHaveLength(0);
  }));

  it('should navigate to the last valid page (not page 1) when deleting the only member on a deeper page', fakeAsync(async () => {
    queryParams$.next({ page: '3', size: '10' });

    await init(['api_product-member-r', 'api_product-member-c', 'api_product-member-d'], { page: '3', size: '10' });
    const router = TestBed.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigate');

    const onlyMember = fakeMember({ id: 'page3-member', displayName: 'Page 3 Member', roles: [{ name: 'USER' }] });
    expectMembersPageRequests([onlyMember], 21, 3, 10);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    await (await harness.getDeleteMemberButton()).click();
    tick();
    fixture.detectChanges();

    const confirmDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
    await confirmDialog.confirm();
    tick();
    fixture.detectChanges();

    httpTestingController.expectOne(req => req.url === `${MEMBERS_URL}/page3-member` && req.method === 'DELETE').flush({});
    tick();
    fixture.detectChanges();

    httpTestingController.expectNone(req => req.url === MEMBERS_URL && req.method === 'GET');
    expect(navigateSpy).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { page: 2 }, queryParamsHandling: 'merge' }));
  }));

  it('should show transfer ownership button when user has update permission', fakeAsync(async () => {
    await init(['api_product-member-r', 'api_product-member-c', 'api_product-member-u']);
    expectMembersPageRequests([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(await harness.isTransferOwnershipVisible()).toBeTruthy();
  }));

  it('should hide transfer ownership button when user lacks update permission', fakeAsync(async () => {
    await init(['api_product-member-r']);
    expectMembersPageRequests([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(await harness.isTransferOwnershipVisible()).toBeFalsy();
  }));

  it('should display inherited group members section when API Product has groups', fakeAsync(async () => {
    await init();

    const poMember = fakeMember({ id: 'owner-1', displayName: 'Admin', roles: [{ name: 'PRIMARY_OWNER' }] });
    const group = fakeBaseGroup({ id: 'group-1', name: 'PO Group' });
    expectMembersPageRequests(
      [poMember],
      1,
      1,
      10,
      {
        groups: ['group-1'],
        primaryOwner: { displayName: 'PO Group', id: 'group-1', type: 'GROUP' },
      },
      [group],
    );
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    flushGroupInheritedMemberRequestsWithMembers('group-1', [
      fakeMember({ id: 'member-1', displayName: 'Group Member', roles: [{ name: 'USER', scope: 'API_PRODUCT' }] }),
    ]);
    tick();
    fixture.detectChanges();

    const cards = await harness.getInheritedGroupMemberCards();
    expect(cards.length).toBe(1);
    expect(await cards[0].getCardHeading()).toContain('PO Group');
    expect(await cards[0].getCardHeading()).toContain('inherited members');
    const table = await cards[0].getInheritedMembersTable();
    expect(table).not.toBeNull();
    const rows = await table!.getRows();
    expect(rows.length).toBe(1);
  }));

  it('should not display inherited group cards when API Product has no groups', fakeAsync(async () => {
    await init();
    expectMembersPageRequests([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect((await harness.getInheritedGroupMemberCards()).length).toBe(0);
  }));

  describe('notification toggle', () => {
    it('should reflect disableMembershipNotifications=true as unchecked toggle', fakeAsync(async () => {
      await init(['api_product-member-r', 'api_product-member-u']);
      expectMembersPageRequests([], 0, 1, 10, { disableMembershipNotifications: true });
      expectRolesRequest();
      tick();
      fixture.detectChanges();

      expect(await harness.isMembersNotificationToggleChecked()).toBe(false);
    }));

    it('should reflect disableMembershipNotifications=false as checked toggle', fakeAsync(async () => {
      await init(['api_product-member-r', 'api_product-member-u']);
      expectMembersPageRequests([], 0, 1, 10, { disableMembershipNotifications: false });
      expectRolesRequest();
      tick();
      fixture.detectChanges();

      expect(await harness.isMembersNotificationToggleChecked()).toBe(true);
    }));

    it('should PUT API Product with disableMembershipNotifications=true when toggle is unchecked and saved', fakeAsync(async () => {
      await init(['api_product-member-r', 'api_product-member-u']);
      expectMembersPageRequests([], 0, 1, 10, { disableMembershipNotifications: false });
      expectRolesRequest();
      tick();
      fixture.detectChanges();

      await harness.toggleMembersNotification();
      fixture.detectChanges();

      await harness.clickSave();
      tick();
      fixture.detectChanges();

      const putReq = httpTestingController.expectOne(
        req => req.method === 'PUT' && req.url === `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`,
      );
      expect(putReq.request.body.disableMembershipNotifications).toEqual(true);
      putReq.flush({ id: API_PRODUCT_ID, name: 'Test API Product', version: '1.0', apiIds: [], groups: [] });
      tick();
      fixture.detectChanges();

      expectMembersPageRequests([]);
    }));
  });
});
