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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { ApiProductMembersComponent } from './api-product-members.component';
import { ApiProductMembersComponentHarness } from './api-product-members.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { fakeMember } from '../../../entities/management-api-v2/member/member.fixture';

describe('ApiProductMembersComponent', () => {
  const API_PRODUCT_ID = 'test-api-product-id';
  const MEMBERS_URL = `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/members`;
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
      imports: [ApiProductMembersComponent, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
      providers: [
        { provide: GioTestingPermissionProvider, useValue: permissions },
        { provide: SnackBarService, useValue: snackBarService },
        {
          provide: InteractivityChecker,
          useValue: { isFocusable: () => true },
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

  function expectMembersRequest(members = [fakeMember()], totalCount = members.length, page = 1, perPage = 10) {
    const req = httpTestingController.expectOne(r => r.url === MEMBERS_URL);
    expect(req.request.params.get('page')).toEqual(page.toString());
    expect(req.request.params.get('perPage')).toEqual(perPage.toString());
    req.flush({ data: members, pagination: { totalCount, page, perPage } });
  }

  function expectRolesRequest() {
    const req = httpTestingController.expectOne(ROLES_URL);
    req.flush([
      { id: 'PRIMARY_OWNER', name: 'PRIMARY_OWNER', default: false, scope: 'API_PRODUCT' },
      { id: 'USER', name: 'USER', default: true, scope: 'API_PRODUCT' },
      { id: 'OWNER', name: 'OWNER', default: false, scope: 'API_PRODUCT' },
    ]);
  }

  it('should render the Members section with title and description', fakeAsync(async () => {
    await init();
    expectMembersRequest([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(await harness.getTitle()).toBe('Members');
    expect(await harness.getDescription()).toContain('Manage who can interact with your API Product');
  }));

  it('should render picture, Name and Role column headers', fakeAsync(async () => {
    await init();
    expectMembersRequest([]);
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
    expectMembersRequest([]);
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
    expectMembersRequest([member]);
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
    expectMembersRequest([owner]);
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
    expectMembersRequest([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(await harness.getEmptyMessage()).toBe('No members found');
  }));

  it('should show "Add members" button when user has create permission', fakeAsync(async () => {
    await init(['api_product-member-r', 'api_product-member-c']);
    expectMembersRequest([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(await harness.getAddMemberButton()).not.toBeNull();
  }));

  it('should hide "Add members" button when user lacks create permission', fakeAsync(async () => {
    await init(['api_product-member-r']);
    expectMembersRequest([]);
    expectRolesRequest();
    tick();
    fixture.detectChanges();

    expect(await harness.getAddMemberButton()).toBeNull();
  }));

  it('should update URL query params when pagination changes', fakeAsync(async () => {
    await init();
    const router = TestBed.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigate');

    expectMembersRequest([fakeMember({ id: 'm1', roles: [{ name: 'USER' }] })], 50, 1, 10);
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
    expectMembersRequest([onlyMember], 11, 1, 10);
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

    httpTestingController.expectOne(r => r.url === `${MEMBERS_URL}/last-member` && r.method === 'DELETE').flush({});
    tick();
    fixture.detectChanges();

    expectMembersRequest([], 10, 1, 10);
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
    expectMembersRequest([onlyMember], 21, 3, 10);
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

    httpTestingController.expectOne(r => r.url === `${MEMBERS_URL}/page3-member` && r.method === 'DELETE').flush({});
    tick();
    fixture.detectChanges();

    httpTestingController.expectNone(r => r.url === MEMBERS_URL && r.method === 'GET');
    expect(navigateSpy).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { page: 2 }, queryParamsHandling: 'merge' }));
  }));
});
