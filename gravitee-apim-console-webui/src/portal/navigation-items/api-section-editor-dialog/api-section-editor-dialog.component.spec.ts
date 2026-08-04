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
import { Component, inject } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import {
  ApiSectionEditorDialogComponent,
  ApiSectionEditorDialogData,
  ApiSectionEditorDialogResult,
} from './api-section-editor-dialog.component';
import { ApiSectionEditorDialogHarness } from './api-section-editor-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import {
  fakeApiFederated,
  fakeApiFederatedAgent,
  fakeApiV2,
  fakeApiV4,
  fakePortalNavigationFolder,
} from '../../../entities/management-api-v2';

@Component({
  selector: 'test-host-component',
  template: `<button (click)="clicked()">Click me</button>`,
})
class TestHostComponent {
  dialogValue: ApiSectionEditorDialogResult;
  dialogData: ApiSectionEditorDialogData = { mode: 'create' };
  private matDialog = inject(MatDialog);

  public clicked(dialogData?: Partial<ApiSectionEditorDialogData>): void {
    const data: ApiSectionEditorDialogData = { mode: 'create', ...this.dialogData, ...dialogData };
    this.matDialog
      .open<ApiSectionEditorDialogComponent, ApiSectionEditorDialogData>(ApiSectionEditorDialogComponent, {
        width: '500px',
        data,
      })
      .afterClosed()
      .subscribe({
        next: result => {
          this.dialogValue = result;
        },
      });
  }
}

describe('ApiSectionEditorDialogComponent', () => {
  let component: TestHostComponent;
  let fixture: ComponentFixture<TestHostComponent>;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, GioTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  function expectApiSearchResponse() {
    const req = httpTestingController.expectOne(
      request =>
        request.method === 'POST' &&
        request.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search` &&
        (request.params.get('page') ?? '1') === '1' &&
        (request.params.get('perPage') ?? '10') === '10',
    );

    expect(req.request.body).toEqual({ query: '' });

    req.flush({
      data: [
        fakeApiV2({ id: 'api-v2', name: 'API V2' }),
        fakeApiV4({ id: 'api-v4', name: 'API V4' }),
        fakeApiFederated({ id: 'api-fed', name: 'API Federated', definitionVersion: 'FEDERATED' }),
        fakeApiFederatedAgent({ id: 'api-fed-agent', name: 'API Federated Agent' }),
      ],
      pagination: { totalCount: 4 },
    });
  }

  function expectApiProductApisRequest(apiProductId: string, options: { page?: number; perPage?: number; query?: string } = {}) {
    const { page = 1, perPage = 10, query } = options;
    return httpTestingController.expectOne(request => {
      if (request.method !== 'GET' || request.url !== `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${apiProductId}/apis`) {
        return false;
      }
      if (request.params.get('page') !== page.toString() || request.params.get('perPage') !== perPage.toString()) {
        return false;
      }
      return query === undefined ? !request.params.has('query') : request.params.get('query') === query;
    });
  }

  it('should not allow submit when no api is selected', fakeAsync(() => {
    component.clicked();
    fixture.detectChanges();

    tick(350);
    expectApiSearchResponse();

    let dialog: ApiSectionEditorDialogHarness;
    rootLoader.getHarness(ApiSectionEditorDialogHarness).then(h => (dialog = h));
    tick();

    dialog.isSubmitButtonDisabled().then(disabled => expect(disabled).toEqual(true));
    tick();
  }));

  it('should save selected api ids', fakeAsync(() => {
    component.clicked();
    fixture.detectChanges();

    tick(350);
    expectApiSearchResponse();

    let dialog: ApiSectionEditorDialogHarness;
    rootLoader.getHarness(ApiSectionEditorDialogHarness).then(h => (dialog = h));
    tick();

    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader.getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-picker-checkbox-"]' })).then(c => (checkboxes = c));
    tick();

    expect(checkboxes.length).toBeGreaterThan(1);

    checkboxes[0].check();
    checkboxes[1].check();
    tick();

    dialog.clickSubmitButton();
    tick();

    expect(component.dialogValue?.visibility).toEqual('PUBLIC');
    expect(Array.isArray(component.dialogValue?.apiIds)).toEqual(true);
    expect(component.dialogValue?.apiIds?.length).toBeGreaterThan(1);
    expect(component.dialogValue?.apiId).toEqual(component.dialogValue?.apiIds?.[0]);
  }));

  it('should load APIs from the owning API Product and display the product-specific description', fakeAsync(async () => {
    component.clicked({
      apiProductContext: { navigationItemId: 'api-product-navigation-item', apiProductId: 'api-product-id' },
    });
    fixture.detectChanges();

    tick(350);
    expectApiProductApisRequest('api-product-id').flush({
      data: [fakeApiV2({ id: 'product-api', name: 'Product API' })],
      pagination: { totalCount: 1 },
    });
    fixture.detectChanges();
    tick();

    const dialog = await rootLoader.getHarness(ApiSectionEditorDialogHarness);
    expect(await dialog.getDescription()).toBe('Pick the APIs included in this API Product that you want to add to its navigation.');

    const checkboxes = await rootLoader.getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-picker-checkbox-"]' }));
    expect(checkboxes).toHaveLength(1);
    expect(await (await checkboxes[0].host()).getAttribute('data-testid')).toBe('api-picker-checkbox-product-api');
  }));

  it('should forward product search and pagination to the API Product APIs endpoint', fakeAsync(async () => {
    component.clicked({
      apiProductContext: { navigationItemId: 'api-product-navigation-item', apiProductId: 'api-product-id' },
    });
    fixture.detectChanges();

    tick(350);
    expectApiProductApisRequest('api-product-id').flush({
      data: [fakeApiV2({ id: 'first-api', name: 'First API' })],
      pagination: { totalCount: 20 },
    });
    fixture.detectChanges();
    tick();

    const dialog = await rootLoader.getHarness(ApiSectionEditorDialogHarness);
    await dialog.goToNextPage();
    tick(450);
    expectApiProductApisRequest('api-product-id', { page: 2 }).flush({
      data: [fakeApiV2({ id: 'second-page-api', name: 'Second page API' })],
      pagination: { totalCount: 20 },
    });
    fixture.detectChanges();
    tick();

    await dialog.setSearchValue('Orders');
    tick(450);
    expectApiProductApisRequest('api-product-id', { query: 'Orders' }).flush({
      data: [fakeApiV2({ id: 'orders-api', name: 'Orders API' })],
      pagination: { totalCount: 1 },
    });
    fixture.detectChanges();
    tick();
  }));

  it('should preserve selected product APIs while searching', fakeAsync(async () => {
    component.clicked({
      apiProductContext: { navigationItemId: 'api-product-navigation-item', apiProductId: 'api-product-id' },
    });
    fixture.detectChanges();

    tick(350);
    expectApiProductApisRequest('api-product-id').flush({
      data: [fakeApiV2({ id: 'first-api', name: 'First API' })],
      pagination: { totalCount: 2 },
    });
    fixture.detectChanges();
    tick();

    let checkboxes = await rootLoader.getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-picker-checkbox-"]' }));
    await checkboxes[0].check();

    const dialog = await rootLoader.getHarness(ApiSectionEditorDialogHarness);
    await dialog.setSearchValue('Second');
    tick(450);
    expectApiProductApisRequest('api-product-id', { query: 'Second' }).flush({
      data: [fakeApiV2({ id: 'second-api', name: 'Second API' })],
      pagination: { totalCount: 1 },
    });
    fixture.detectChanges();
    tick();

    checkboxes = await rootLoader.getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-picker-checkbox-"]' }));
    await checkboxes[0].check();
    await dialog.clickSubmitButton();
    tick();

    expect(component.dialogValue?.apiIds).toEqual(['first-api', 'second-api']);
  }));

  it('should not fall back to the environment API search when loading product APIs fails', fakeAsync(() => {
    component.clicked({
      apiProductContext: { navigationItemId: 'api-product-navigation-item', apiProductId: 'api-product-id' },
    });
    fixture.detectChanges();

    tick(350);
    expectApiProductApisRequest('api-product-id').flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    tick();

    httpTestingController.expectNone(request => request.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search`);
  }));

  it('should preserve the standalone dialog description and environment API search', fakeAsync(async () => {
    component.clicked();
    fixture.detectChanges();

    tick(350);
    expectApiSearchResponse();
    fixture.detectChanges();
    tick();

    const dialog = await rootLoader.getHarness(ApiSectionEditorDialogHarness);
    expect(await dialog.getDescription()).toBe('Pick the unpublished APIs you want to add to your navigation menu.');
  }));

  it('should hide checkbox, show "Already added" label and grey out row for already-added API', fakeAsync(async () => {
    component.clicked({ existingApiIds: ['api-v2'] });
    fixture.detectChanges();

    tick(350);
    expectApiSearchResponse();
    tick();

    const dialog = await rootLoader.getHarness(ApiSectionEditorDialogHarness);
    const checkboxes = await rootLoader.getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-picker-checkbox-"]' }));
    expect(checkboxes.length).toBe(3);
    const checkboxIds = await Promise.all(
      checkboxes.map(async checkbox => {
        const element = await checkbox.host();
        const dataTestId = await element.getAttribute('data-testid');
        return dataTestId?.replace('api-picker-checkbox-', '') ?? '';
      }),
    );
    expect(checkboxIds).not.toContain('api-v2');
    expect(checkboxIds).toContain('api-v4');

    const alreadyAddedLabel = await dialog.getAlreadyAddedLabel('api-v2');
    expect(alreadyAddedLabel).toBeTruthy();
    expect(await alreadyAddedLabel.getText()).toBe('Already added');
  }));

  it('should show checkboxes for all APIs and no "Already added" label when existingApiIds is empty', fakeAsync(async () => {
    component.clicked({ existingApiIds: [] });
    fixture.detectChanges();

    tick(350);
    expectApiSearchResponse();
    tick();

    const dialog = await rootLoader.getHarness(ApiSectionEditorDialogHarness);
    const checkboxes = await rootLoader.getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-picker-checkbox-"]' }));
    expect(checkboxes.length).toBe(4);

    const alreadyAddedLabels = await dialog.getAlreadyAddedLabels();
    expect(alreadyAddedLabels.length).toBe(0);
  }));

  it('should allow selecting PRIVATE visibility when parent is public', fakeAsync(() => {
    component.dialogData = { mode: 'create', parentItem: fakePortalNavigationFolder({ visibility: 'PUBLIC' }) };
    component.clicked();
    fixture.detectChanges();

    tick(350);
    expectApiSearchResponse();

    let dialog: ApiSectionEditorDialogHarness;
    rootLoader.getHarness(ApiSectionEditorDialogHarness).then(h => (dialog = h));
    tick();

    dialog.isAuthenticationToggleDisabled().then(disabled => expect(disabled).toEqual(false));
    dialog.isAuthenticationToggleChecked().then(checked => expect(checked).toEqual(false));
    tick();

    dialog.toggleAuthentication();
    tick();
    dialog.isAuthenticationToggleChecked().then(checked => expect(checked).toEqual(true));
    tick();

    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader.getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-picker-checkbox-"]' })).then(c => (checkboxes = c));
    tick();

    checkboxes[0].check();
    tick();

    dialog.clickSubmitButton();
    tick();

    expect(component.dialogValue?.visibility).toEqual('PRIVATE');
  }));

  it('should force PRIVATE visibility when parent is private', fakeAsync(() => {
    component.dialogData = { mode: 'create', parentItem: fakePortalNavigationFolder({ visibility: 'PRIVATE' }) };
    component.clicked();
    fixture.detectChanges();

    tick(350);
    expectApiSearchResponse();

    let dialog: ApiSectionEditorDialogHarness;
    rootLoader.getHarness(ApiSectionEditorDialogHarness).then(h => (dialog = h));
    tick();

    dialog.isAuthenticationToggleDisabled().then(disabled => expect(disabled).toEqual(true));
    dialog.isAuthenticationToggleChecked().then(checked => expect(checked).toEqual(true));
    tick();

    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader.getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-picker-checkbox-"]' })).then(c => (checkboxes = c));
    tick();

    checkboxes[0].check();
    tick();

    dialog.clickSubmitButton();
    tick();

    expect(component.dialogValue?.visibility).toEqual('PRIVATE');
  }));
});
