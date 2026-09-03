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
  AgentSectionEditorDialogComponent,
  AgentSectionEditorDialogData,
  AgentSectionEditorDialogResult,
} from './agent-section-editor-dialog.component';
import { AgentSectionEditorDialogHarness } from './agent-section-editor-dialog.harness';

import { Api, fakeApiV4, fakePortalNavigationFolder } from '../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';

@Component({
  selector: 'test-host-component',
  template: `<button (click)="openDialog()">Open</button>`,
})
class TestHostComponent {
  private readonly matDialog = inject(MatDialog);

  dialogValue?: AgentSectionEditorDialogResult;
  dialogData: AgentSectionEditorDialogData = {
    mode: 'create',
    parentItem: fakePortalNavigationFolder({ area: 'TOP_NAVBAR' }),
  };

  openDialog(dialogData?: Partial<AgentSectionEditorDialogData>): void {
    this.matDialog
      .open<AgentSectionEditorDialogComponent, AgentSectionEditorDialogData, AgentSectionEditorDialogResult>(
        AgentSectionEditorDialogComponent,
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

describe('AgentSectionEditorDialogComponent', () => {
  const agents: Api[] = [
    fakeApiV4({ id: 'agent-1', name: 'First Agent', apiVersion: '1.0', description: 'First description', type: 'A2A_PROXY' }),
    fakeApiV4({ id: 'agent-2', name: 'Second Agent', apiVersion: '2.0', description: 'Second description', type: 'A2A_PROXY' }),
    fakeApiV4({ id: 'agent-3', name: 'Third Agent', apiVersion: '3.0', description: '', type: 'A2A_PROXY' }),
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

  it('should keep submit disabled when no Agent is selected', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectAgentSearchResponse(agents);

    let dialog!: AgentSectionEditorDialogHarness;
    rootLoader.getHarness(AgentSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();

    dialog.isSubmitButtonDisabled().then(isDisabled => expect(isDisabled).toBe(true));
    tick();
  }));

  it('should close without a result when cancelled', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectAgentSearchResponse(agents);

    let dialog!: AgentSectionEditorDialogHarness;
    rootLoader.getHarness(AgentSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();
    dialog.clickCancelButton();
    tick();

    expect(component.dialogValue).toBeUndefined();
  }));

  it('should display loading and empty states', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    const request = expectAgentSearchRequest();
    let dialog!: AgentSectionEditorDialogHarness;
    rootLoader.getHarness(AgentSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();

    dialog.getLoadingText().then(loadingText => expect(loadingText).toBe('Loading...'));
    tick();

    request.flush({ data: [], pagination: { totalCount: 0 } });
    fixture.detectChanges();
    tick();

    dialog.getEmptyStateText().then(emptyStateText => expect(emptyStateText).toBe('No data to display.'));
    tick();
  }));

  it('should search Agents once with the latest filters', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectAgentSearchResponse(agents);

    let dialog!: AgentSectionEditorDialogHarness;
    rootLoader.getHarness(AgentSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();

    dialog.setSearchValue('Second');
    tick(450);

    const requests = httpTestingController.match(
      req => req.method === 'POST' && req.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search`,
    );
    expect(requests).toHaveLength(1);
    expect(requests[0].request.body).toEqual({ query: 'Second', apiTypes: ['V4_A2A_PROXY'] });
    expect(requests[0].request.params.get('page')).toBe('1');
    expect(requests[0].request.params.get('perPage')).toBe('10');

    requests[0].flush({ data: [agents[1]], pagination: { totalCount: 1 } });
    fixture.detectChanges();
    tick();
  }));

  it('should return selected agents in selection order and allow removing a selection', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    expectAgentSearchResponse(agents);

    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader
      .getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="agent-picker-checkbox-"]' }))
      .then(result => (checkboxes = result));
    tick();

    checkboxes[1].check();
    checkboxes[0].check();
    checkboxes[1].uncheck();
    checkboxes[2].check();
    tick();

    let dialog!: AgentSectionEditorDialogHarness;
    rootLoader.getHarness(AgentSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();
    dialog.clickSubmitButton();
    tick();

    expect(component.dialogValue).toEqual({
      visibility: 'PUBLIC',
      agents: [
        { id: 'agent-1', name: 'First Agent' },
        { id: 'agent-3', name: 'Third Agent' },
      ],
    });
  }));

  it('should prevent selecting an already linked Agent', fakeAsync(() => {
    component.openDialog({ existingAgentIds: ['agent-1'] });
    fixture.detectChanges();

    tick(350);
    expectAgentSearchResponse(agents);
    tick();

    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader
      .getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="agent-picker-checkbox-"]' }))
      .then(result => (checkboxes = result));
    let dialog!: AgentSectionEditorDialogHarness;
    rootLoader.getHarness(AgentSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();

    expect(checkboxes).toHaveLength(2);
    dialog.getAlreadyAddedLabel('agent-1').then(label => expect(label).toBeTruthy());
    tick();
  }));

  it('should force private visibility when parent is private', fakeAsync(() => {
    component.openDialog({ parentItem: fakePortalNavigationFolder({ area: 'TOP_NAVBAR', visibility: 'PRIVATE' }) });
    fixture.detectChanges();

    tick(350);
    expectAgentSearchResponse(agents);

    let dialog!: AgentSectionEditorDialogHarness;
    rootLoader.getHarness(AgentSectionEditorDialogHarness).then(harness => (dialog = harness));
    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader
      .getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid^="agent-picker-checkbox-"]' }))
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

  it('should display a load error when Agent search fails', fakeAsync(() => {
    component.openDialog();
    fixture.detectChanges();

    tick(350);
    const request = expectAgentSearchRequest();
    request.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    tick();

    let dialog!: AgentSectionEditorDialogHarness;
    rootLoader.getHarness(AgentSectionEditorDialogHarness).then(harness => (dialog = harness));
    tick();
    dialog.getLoadErrorText().then(errorText => expect(errorText).toBe('Unable to load Agents.'));
    tick();
  }));

  function expectAgentSearchRequest() {
    const request = httpTestingController.expectOne(
      req =>
        req.method === 'POST' &&
        req.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search` &&
        req.params.get('page') === '1' &&
        req.params.get('perPage') === '10',
    );
    expect(request.request.body).toEqual({ query: '', apiTypes: ['V4_A2A_PROXY'] });
    return request;
  }

  function expectAgentSearchResponse(response: Api[]): void {
    expectAgentSearchRequest().flush({
      data: response,
      pagination: { totalCount: response.length },
    });
    fixture.detectChanges();
  }
});
