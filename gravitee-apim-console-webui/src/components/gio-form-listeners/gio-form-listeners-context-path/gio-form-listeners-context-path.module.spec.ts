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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { GioFormListenersContextPathModule } from './gio-form-listeners-context-path.module';
import { GioFormListenersContextPathHarness } from './gio-form-listeners-context-path.harness';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { AjsRootScope } from '../../../ajs-upgraded-providers';

@Component({
  template: ` <gio-form-listeners-context-path [formControl]="formControl"></gio-form-listeners-context-path> `,
})
class TestComponent {
  public formControl = new FormControl([]);
}

describe('GioFormListenersContextPathModule', () => {
  const fakeConstants = CONSTANTS_TESTING;
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let testComponent: TestComponent;

  const LISTENERS = [
    {
      path: '/api/my-api1',
    },
    {
      path: '/api/my-api-2',
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioFormListenersContextPathModule, MatIconTestingModule, ReactiveFormsModule, GioHttpTestingModule],
      providers: [
        {
          provide: 'Constants',
          useValue: fakeConstants,
        },
        { provide: AjsRootScope, useValue: { $broadcast: jest.fn() } },
      ],
    });
    fixture = TestBed.createComponent(TestComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    testComponent = fixture.componentInstance;
  });

  it('should display paths', async () => {
    testComponent.formControl.setValue(LISTENERS);
    const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

    const pathRows = await formPaths.getListenerRows();

    const paths = await Promise.all(
      pathRows.map(async (row) => ({
        path: await row.pathInput.getValue(),
      })),
    );

    expect(paths).toEqual([
      ...LISTENERS,
      {
        path: '',
      },
    ]);
  });

  it('should add new context path', async () => {
    const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

    expect((await formPaths.getListenerRows()).length).toEqual(1);

    // Add path on last path row
    const emptyLastContextPathRow = await formPaths.getLastListenerRow();
    await emptyLastContextPathRow.pathInput.setValue('/api/my-api-3');

    // Expect new row was added
    expect((await formPaths.getListenerRows()).length).toEqual(2);

    await emptyLastContextPathRow.pathInput.setValue('/api/my-api-4');

    let addedContextPathRow = (await formPaths.getListenerRows())[0];
    expect({ path: await addedContextPathRow.pathInput.getValue() }).toEqual({
      path: '/api/my-api-4',
    });

    // Expect new row was added
    await formPaths.addListener({ path: '/api/my-api-5' });

    addedContextPathRow = (await formPaths.getListenerRows())[1];
    expect({ path: await addedContextPathRow.pathInput.getValue() }).toEqual({
      path: '/api/my-api-5',
    });

    expect(testComponent.formControl.value).toEqual([{ path: '/api/my-api-4' }, { path: '/api/my-api-5' }]);
  });

  it('should validate path', async () => {
    const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

    expect((await formPaths.getListenerRows()).length).toEqual(1);

    // Add path on last path row
    const emptyLastContextPathRow = await formPaths.getLastListenerRow();
    const pathInputHost = await emptyLastContextPathRow.pathInput.host();

    await emptyLastContextPathRow.pathInput.setValue('bad-path');
    expect(await pathInputHost.hasClass('ng-invalid')).toEqual(true);

    await emptyLastContextPathRow.pathInput.setValue('/abc yeh');
    expect(await pathInputHost.hasClass('ng-invalid')).toEqual(true);

    await emptyLastContextPathRow.pathInput.setValue('/b');
    expect(await pathInputHost.hasClass('ng-invalid')).toEqual(true);

    await emptyLastContextPathRow.pathInput.setValue('/ba');
    expect(await pathInputHost.hasClass('ng-invalid')).toEqual(false);

    await emptyLastContextPathRow.pathInput.setValue('/good-path');
    expect(await pathInputHost.hasClass('ng-invalid')).toEqual(false);

    const secondLine = await formPaths.getLastListenerRow();
    await secondLine.pathInput.setValue('/good-path');
    expect(await pathInputHost.hasClass('ng-invalid')).toEqual(true);
    expect(await (await secondLine.pathInput.host()).hasClass('ng-invalid')).toEqual(true);
  });

  it('should edit context path', async () => {
    testComponent.formControl.setValue(LISTENERS);
    const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

    const contextPathRowToEdit = (await formPaths.getListenerRows())[1];

    await contextPathRowToEdit.pathInput.setValue('/api/my-api-6');

    const editedContextPathRow = (await formPaths.getListenerRows())[1];
    expect({ path: await editedContextPathRow.pathInput.getValue() }).toEqual({
      path: '/api/my-api-6',
    });

    expect(testComponent.formControl.value).toEqual([
      LISTENERS[0],
      {
        path: '/api/my-api-6',
      },
    ]);
  });

  it('should remove context path row', async () => {
    testComponent.formControl.setValue(LISTENERS);
    const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

    const initialContextPathRows = await formPaths.getListenerRows();
    expect(initialContextPathRows.length).toEqual(3);

    const contextPathRowToRemove = initialContextPathRows[1];
    await contextPathRowToRemove.removeButton?.click();

    const newContextPathRows = await formPaths.getListenerRows();
    expect(newContextPathRows.length).toEqual(2);

    // Check last row does have disabled remove button
    expect(newContextPathRows[1].removeButton.isDisabled()).toBeTruthy();

    expect(testComponent.formControl.value).toEqual([LISTENERS[0]]);
  });

  it('should handle touched & dirty on focus and change value', async () => {
    testComponent.formControl.setValue(LISTENERS);
    const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

    expect(testComponent.formControl.touched).toEqual(false);
    expect(testComponent.formControl.dirty).toEqual(false);

    await (await formPaths.getListenerRows())[0].pathInput.focus();

    expect(testComponent.formControl.touched).toEqual(true);
    expect(testComponent.formControl.dirty).toEqual(false);

    await (await formPaths.getListenerRows())[0].pathInput.setValue('Content-Type');

    expect(testComponent.formControl.touched).toEqual(true);
    expect(testComponent.formControl.dirty).toEqual(true);
  });

  it('should handle touched & dirty on focus and change value', async () => {
    testComponent.formControl.setValue(LISTENERS);
    const formHeaders = await loader.getHarness(GioFormListenersContextPathHarness);

    expect(testComponent.formControl.touched).toEqual(false);
    expect(testComponent.formControl.dirty).toEqual(false);

    await (await formHeaders.getListenerRows())[0].removeButton?.click();

    expect(testComponent.formControl.touched).toEqual(true);
    expect(testComponent.formControl.dirty).toEqual(true);
  });
});
