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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';

import {
  ApiSectionEditorDialogComponent,
  ApiSectionEditorDialogData,
  ApiSectionEditorDialogResult,
} from './api-section-editor-dialog.component';
import { ApiSectionEditorDialogHarness } from './api-section-editor-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeApiFederated, fakeApiFederatedAgent, fakeApiV1, fakeApiV2, fakeApiV4 } from '../../../entities/management-api-v2';

@Component({
  selector: 'test-host-component',
  template: `<button (click)="clicked()">Click me</button>`,
})
class TestHostComponent {
  dialogValue: ApiSectionEditorDialogResult;
  private matDialog = inject(MatDialog);

  public clicked(): void {
    const data: ApiSectionEditorDialogData = { mode: 'create' };
    this.matDialog
      .open<ApiSectionEditorDialogComponent, ApiSectionEditorDialogData>(ApiSectionEditorDialogComponent, {
        width: '500px',
        data,
      })
      .afterClosed()
      .subscribe({
        next: (result) => {
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
      (request) =>
        request.method === 'POST' &&
        request.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search` &&
        (request.params.get('page') ?? '1') === '1' &&
        (request.params.get('perPage') ?? '10') === '10',
    );

    expect(req.request.body).toEqual({ query: '' });

    req.flush({
      data: [
        fakeApiV1({ id: 'api-v1', name: 'API V1' }),
        fakeApiV2({ id: 'api-v2', name: 'API V2' }),
        fakeApiV4({ id: 'api-v4', name: 'API V4' }),
        fakeApiFederated({ id: 'api-fed', name: 'API Federated', definitionVersion: 'FEDERATED' }),
        fakeApiFederatedAgent({ id: 'api-fed-agent', name: 'API Federated Agent' }),
      ],
      pagination: { totalCount: 5 },
    });
  }

  it('should not allow submit when no api is selected', fakeAsync(() => {
    component.clicked();
    fixture.detectChanges();

    tick(350);
    expectApiSearchResponse();

    let dialog: ApiSectionEditorDialogHarness;
    rootLoader.getHarness(ApiSectionEditorDialogHarness).then((h) => (dialog = h));
    tick();

    dialog.isSubmitButtonDisabled().then((disabled) => expect(disabled).toEqual(true));
    tick();
  }));

  it('should save selected api ids', fakeAsync(() => {
    component.clicked();
    fixture.detectChanges();

    tick(350);
    expectApiSearchResponse();

    let dialog: ApiSectionEditorDialogHarness;
    rootLoader.getHarness(ApiSectionEditorDialogHarness).then((h) => (dialog = h));
    tick();

    let checkboxes: MatCheckboxHarness[] = [];
    rootLoader.getAllHarnesses(MatCheckboxHarness.with({ selector: '[data-testid="api-picker-checkbox"]' })).then((c) => (checkboxes = c));
    tick();

    expect(checkboxes.length).toBeGreaterThan(0);

    checkboxes[0].check();
    tick();

    dialog.clickSubmitButton();
    tick();

    expect(component.dialogValue?.visibility).toEqual('PUBLIC');
    expect(Array.isArray(component.dialogValue?.apiIds)).toEqual(true);
    expect(component.dialogValue?.apiIds?.length).toBeGreaterThan(0);
    expect(component.dialogValue?.apiId).toEqual(component.dialogValue?.apiIds?.[0]);
  }));
});
