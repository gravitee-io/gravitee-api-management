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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { Component, inject } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatDialog } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import {
  ApiProductSectionEditorDialogComponent,
  ApiProductSectionEditorDialogData,
  ApiProductSectionEditorDialogResult,
} from './api-product-section-editor-dialog.component';
import { ApiProductSectionEditorDialogHarness } from './api-product-section-editor-dialog.harness';

import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { fakePortalNavigationFolder } from '../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';

@Component({
  selector: 'test-host-component',
  template: `<button (click)="openDialog()">Open</button>`,
})
class TestHostComponent {
  private readonly matDialog = inject(MatDialog);

  dialogValue?: ApiProductSectionEditorDialogResult;
  dialogData: ApiProductSectionEditorDialogData = {
    mode: 'create',
    parentItem: fakePortalNavigationFolder({ area: 'TOP_NAVBAR' }),
  };

  openDialog(dialogData?: Partial<ApiProductSectionEditorDialogData>): void {
    this.matDialog
      .open<ApiProductSectionEditorDialogComponent, ApiProductSectionEditorDialogData, ApiProductSectionEditorDialogResult>(
        ApiProductSectionEditorDialogComponent,
        {
          width: '800px',
          data: { ...this.dialogData, ...dialogData },
        },
      )
      .afterClosed()
      .subscribe(result => {
        this.dialogValue = result;
      });
  }
}

describe('ApiProductSectionEditorDialogComponent', () => {
  const apiProducts: ApiProduct[] = [
    { id: 'product-1', name: 'First Product', version: '1.0', description: 'First description', apiIds: ['api-1'] },
    { id: 'product-2', name: 'Second Product', version: '2.0', description: 'Second description', apiIds: ['api-2', 'api-3'] },
    { id: 'product-3', name: 'Third Product', version: '3.0', apiIds: [] },
  ];

  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
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

  it('should keep submit disabled when no API Product is selected', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectApiProductSearchResponse(apiProducts);

    let dialog!: ApiProductSectionEditorDialogHarness;
    rootLoader.getHarness(ApiProductSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();

    dialog.isSubmitButtonDisabled().then(isDisabled => expect(isDisabled).toBe(true));
    tick();
  }));

  it('should close without a result when cancelled', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectApiProductSearchResponse(apiProducts);

    let dialog!: ApiProductSectionEditorDialogHarness;
    rootLoader.getHarness(ApiProductSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();
    dialog.clickCancelButton();
    tick();

    expect(component.dialogValue).toBeUndefined();
  }));

  it('should display loading and empty states', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    const request = expectApiProductSearchRequest();
    let dialog!: ApiProductSectionEditorDialogHarness;
    rootLoader.getHarness(ApiProductSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();

    dialog.getLoadingText().then(loadingText => expect(loadingText).toBe('Loading...'));
    tick();

    request.flush({ data: [], pagination: { totalCount: 0 } });
    fixture.detectChanges();
    tick();

    dialog.getEmptyStateText().then(emptyStateText => expect(emptyStateText).toBe('No data to display.'));
    tick();
  }));

  it('should search API Products once with the latest filters', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectApiProductSearchResponse(apiProducts);

    let dialog!: ApiProductSectionEditorDialogHarness;
    rootLoader.getHarness(ApiProductSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();

    dialog.setSearchValue('Second');
    tick(450);

    const requests = httpTestingController.match(
      req => req.method === 'POST' && req.url === `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`,
    );
    expect(requests).toHaveLength(1);
    expect(requests[0].request.body).toEqual({ query: 'Second' });
    expect(requests[0].request.params.get('page')).toBe('1');
    expect(requests[0].request.params.get('perPage')).toBe('10');

    requests[0].flush({ data: [apiProducts[1]], pagination: { totalCount: 1 } });
    fixture.detectChanges();
    tick();
  }));

  it('should return selected products in selection order and allow removing a selection', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectApiProductSearchResponse(apiProducts);

    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader
      .getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-product-picker-checkbox-"]' }))
      .then(result => (checkboxes = result));
    tick();

    checkboxes[1].check();
    checkboxes[0].check();
    checkboxes[1].uncheck();
    checkboxes[2].check();
    tick();

    let dialog!: ApiProductSectionEditorDialogHarness;
    rootLoader.getHarness(ApiProductSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();
    dialog.clickSubmitButton();
    tick();

    expect(component.dialogValue).toEqual({
      visibility: 'PUBLIC',
      apiProducts: [
        { id: 'product-1', name: 'First Product' },
        { id: 'product-3', name: 'Third Product' },
      ],
    });
  }));

  it('should prevent selecting an already linked API Product', fakeAsync(() => {
    component.openDialog({ existingApiProductIds: ['product-1'] });
    fixture.detectChanges();

    tick(350);
    expectApiProductSearchResponse(apiProducts);
    tick();

    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader
      .getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-product-picker-checkbox-"]' }))
      .then(result => (checkboxes = result));
    let dialog!: ApiProductSectionEditorDialogHarness;
    rootLoader.getHarness(ApiProductSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();

    expect(checkboxes).toHaveLength(2);
    dialog.getAlreadyAddedLabel('product-1').then(label => expect(label).toBeTruthy());
    tick();
  }));

  it('should force private visibility when parent is private', fakeAsync(() => {
    component.openDialog({ parentItem: fakePortalNavigationFolder({ area: 'TOP_NAVBAR', visibility: 'PRIVATE' }) });
    fixture.detectChanges();

    tick(350);
    expectApiProductSearchResponse(apiProducts);

    let dialog!: ApiProductSectionEditorDialogHarness;
    rootLoader.getHarness(ApiProductSectionEditorDialogHarness).then(harness => (dialog = harness));
    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader
      .getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="api-product-picker-checkbox-"]' }))
      .then(result => (checkboxes = result));
    tick();

    dialog.isAuthenticationToggleDisabled().then(isDisabled => expect(isDisabled).toBe(true));
    dialog.isAuthenticationToggleChecked().then(isChecked => expect(isChecked).toBe(true));
    checkboxes[0].check();
    tick();
    dialog.clickSubmitButton();
    tick();

    expect(component.dialogValue?.visibility).toBe('PRIVATE');
  }));

  it('should display a load error when API Product search fails', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    const request = expectApiProductSearchRequest();
    request.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    tick();

    let dialog!: ApiProductSectionEditorDialogHarness;
    rootLoader.getHarness(ApiProductSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();
    dialog.getLoadErrorText().then(errorText => expect(errorText).toBe('Unable to load API Products.'));
    tick();
  }));

  function expectApiProductSearchRequest() {
    const request = httpTestingController.expectOne(
      req =>
        req.method === 'POST' &&
        req.url === `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search` &&
        req.params.get('page') === '1' &&
        req.params.get('perPage') === '10',
    );
    expect(request.request.body).toEqual({ query: '' });
    return request;
  }

  function expectApiProductSearchResponse(response: ApiProduct[]): void {
    expectApiProductSearchRequest().flush({
      data: response,
      pagination: { totalCount: response.length },
    });
    fixture.detectChanges();
  }
});
