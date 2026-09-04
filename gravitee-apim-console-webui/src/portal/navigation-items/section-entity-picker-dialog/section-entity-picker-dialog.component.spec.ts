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
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { Component, inject } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatDialog } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import {
  SectionEntityKind,
  SectionEntityPickerDialogComponent,
  SectionEntityPickerDialogData,
  SectionEntityPickerDialogResult,
  SelectedSectionEntity,
} from './section-entity-picker-dialog.component';
import { SectionEntityPickerDialogHarness } from './section-entity-picker-dialog.harness';

import { fakeApiV4, fakePortalNavigationFolder } from '../../../entities/management-api-v2';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';

@Component({
  selector: 'test-host-component',
  template: `<button (click)="openDialog()">Open</button>`,
})
class TestHostComponent {
  private readonly matDialog = inject(MatDialog);

  dialogValue?: SectionEntityPickerDialogResult;
  dialogData: SectionEntityPickerDialogData = {
    kind: 'AGENT',
    parentItem: fakePortalNavigationFolder({ area: 'TOP_NAVBAR' }),
  };

  openDialog(dialogData?: Partial<SectionEntityPickerDialogData>): void {
    this.matDialog
      .open<SectionEntityPickerDialogComponent, SectionEntityPickerDialogData, SectionEntityPickerDialogResult>(
        SectionEntityPickerDialogComponent,
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

interface KindScenario {
  kind: SectionEntityKind;
  title: string;
  loadErrorText: string;
  searchUrl: string;
  searchBody: (query: string) => unknown;
  page: { data: unknown[]; pagination: { totalCount: number } };
  narrowedPage: { data: unknown[]; pagination: { totalCount: number } };
  firstId: string;
  expectedSelection: SelectedSectionEntity[];
}

const agents = [
  fakeApiV4({ id: 'agent-1', name: 'First Agent', apiVersion: '1.0', description: 'First description', type: 'A2A_PROXY' }),
  fakeApiV4({ id: 'agent-2', name: 'Second Agent', apiVersion: '2.0', description: 'Second description', type: 'A2A_PROXY' }),
  fakeApiV4({ id: 'agent-3', name: 'Third Agent', apiVersion: '3.0', description: '', type: 'A2A_PROXY' }),
];

const apiProducts: ApiProduct[] = [
  { id: 'product-1', name: 'First Product', version: '1.0', description: 'First description', apiIds: ['api-1'] },
  { id: 'product-2', name: 'Second Product', version: '2.0', description: 'Second description', apiIds: ['api-2', 'api-3'] },
  { id: 'product-3', name: 'Third Product', version: '3.0', apiIds: [] },
];

const scenarios: KindScenario[] = [
  {
    kind: 'AGENT',
    title: 'Add Agents',
    loadErrorText: 'Unable to load Agents.',
    searchUrl: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search`,
    searchBody: query => ({ query, apiTypes: ['V4_A2A_PROXY'] }),
    page: { data: agents, pagination: { totalCount: agents.length } },
    narrowedPage: { data: [agents[1]], pagination: { totalCount: 1 } },
    firstId: 'agent-1',
    expectedSelection: [
      { id: 'agent-1', name: 'First Agent' },
      { id: 'agent-3', name: 'Third Agent' },
    ],
  },
  {
    kind: 'API_PRODUCT',
    title: 'Add API Products',
    loadErrorText: 'Unable to load API Products.',
    searchUrl: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/_search`,
    searchBody: query => ({ query }),
    page: { data: apiProducts, pagination: { totalCount: apiProducts.length } },
    narrowedPage: { data: [apiProducts[1]], pagination: { totalCount: 1 } },
    firstId: 'product-1',
    expectedSelection: [
      { id: 'product-1', name: 'First Product' },
      { id: 'product-3', name: 'Third Product' },
    ],
  },
];

describe.each(scenarios)('SectionEntityPickerDialogComponent - $kind', (scenario: KindScenario) => {
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
    component.dialogData = { kind: scenario.kind, parentItem: fakePortalNavigationFolder({ area: 'TOP_NAVBAR' }) };
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should show the title for its kind and keep submit disabled until something is selected', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectSearchResponse();

    let dialog!: SectionEntityPickerDialogHarness;
    rootLoader.getHarness(SectionEntityPickerDialogHarness).then(harness => (dialog = harness));
    tick();

    dialog.getDialogTitle().then(title => expect(title).toBe(scenario.title));
    dialog.isSubmitButtonDisabled().then(isDisabled => expect(isDisabled).toBe(true));
    tick();
  }));

  it('should close without a result when cancelled', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectSearchResponse();

    let dialog!: SectionEntityPickerDialogHarness;
    rootLoader.getHarness(SectionEntityPickerDialogHarness).then(harness => (dialog = harness));
    tick();
    dialog.clickCancelButton();
    tick();

    expect(component.dialogValue).toBeUndefined();
  }));

  it('should display loading and empty states', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    const request = expectSearchRequest();
    let dialog!: SectionEntityPickerDialogHarness;
    rootLoader.getHarness(SectionEntityPickerDialogHarness).then(harness => (dialog = harness));
    tick();

    dialog.getLoadingText().then(loadingText => expect(loadingText).toBe('Loading...'));
    tick();

    request.flush({ data: [], pagination: { totalCount: 0 } });
    fixture.detectChanges();
    tick();

    dialog.getEmptyStateText().then(emptyStateText => expect(emptyStateText).toBe('No data to display.'));
    tick();
  }));

  it('should search once with the latest filters', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectSearchResponse();

    let dialog!: SectionEntityPickerDialogHarness;
    rootLoader.getHarness(SectionEntityPickerDialogHarness).then(harness => (dialog = harness));
    tick();

    dialog.setSearchValue('Second');
    tick(450);

    const requests = httpTestingController.match(req => req.method === 'POST' && req.url === scenario.searchUrl);
    expect(requests).toHaveLength(1);
    expect(requests[0].request.body).toEqual(scenario.searchBody('Second'));
    expect(requests[0].request.params.get('page')).toBe('1');
    expect(requests[0].request.params.get('perPage')).toBe('10');

    requests[0].flush(scenario.narrowedPage);
    fixture.detectChanges();
    tick();
  }));

  it('should return the selection in selection order and allow removing a selection', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectSearchResponse();

    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader
      .getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="picker-checkbox-"]' }))
      .then(result => (checkboxes = result));
    tick();

    checkboxes[1].check();
    checkboxes[0].check();
    checkboxes[1].uncheck();
    checkboxes[2].check();
    tick();

    let dialog!: SectionEntityPickerDialogHarness;
    rootLoader.getHarness(SectionEntityPickerDialogHarness).then(harness => (dialog = harness));
    tick();
    dialog.clickSubmitButton();
    tick();

    expect(component.dialogValue).toEqual({
      visibility: 'PUBLIC',
      entities: scenario.expectedSelection,
    });
  }));

  it('should prevent selecting an already linked entity', fakeAsync(() => {
    component.openDialog({ existingIds: [scenario.firstId] });
    fixture.detectChanges();

    tick(350);
    expectSearchResponse();
    tick();

    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader
      .getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="picker-checkbox-"]' }))
      .then(result => (checkboxes = result));
    let dialog!: SectionEntityPickerDialogHarness;
    rootLoader.getHarness(SectionEntityPickerDialogHarness).then(harness => (dialog = harness));
    tick();

    expect(checkboxes).toHaveLength(2);
    dialog.getAlreadyAddedLabel(scenario.firstId).then(label => expect(label).toBeTruthy());
    tick();
  }));

  it('should force private visibility when parent is private', fakeAsync(() => {
    component.openDialog({ parentItem: fakePortalNavigationFolder({ area: 'TOP_NAVBAR', visibility: 'PRIVATE' }) });
    fixture.detectChanges();

    tick(350);
    expectSearchResponse();

    let dialog!: SectionEntityPickerDialogHarness;
    rootLoader.getHarness(SectionEntityPickerDialogHarness).then(harness => (dialog = harness));
    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader
      .getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="picker-checkbox-"]' }))
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

  it('should display a load error when the search fails', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    const request = expectSearchRequest();
    request.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    tick();

    let dialog!: SectionEntityPickerDialogHarness;
    rootLoader.getHarness(SectionEntityPickerDialogHarness).then(harness => (dialog = harness));
    tick();
    dialog.getLoadErrorText().then(errorText => expect(errorText).toBe(scenario.loadErrorText));
    tick();
  }));

  function expectSearchRequest(): TestRequest {
    const request = httpTestingController.expectOne(
      req =>
        req.method === 'POST' && req.url === scenario.searchUrl && req.params.get('page') === '1' && req.params.get('perPage') === '10',
    );
    expect(request.request.body).toEqual(scenario.searchBody(''));
    return request;
  }

  function expectSearchResponse(): void {
    expectSearchRequest().flush(scenario.page);
    fixture.detectChanges();
  }
});
