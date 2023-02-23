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

import { GioFormListenersVirtualHostModule } from './gio-form-listeners-virtual-host.module';
import { GioFormListenersVirtualHostHarness } from './gio-form-listeners-virtual-host.harness';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { AjsRootScope } from '../../../ajs-upgraded-providers';

@Component({
  template: ` <gio-form-listeners-virtual-host [formControl]="formControl"></gio-form-listeners-virtual-host> `,
})
class TestComponent {
  public formControl = new FormControl([]);
}

describe('GioFormListenersVirtualHostModule', () => {
  const fakeConstants = CONSTANTS_TESTING;
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let testComponent: TestComponent;

  const LISTENERS = [
    {
      host: 'api.gravitee.io',
      path: '/api/my-api1',
      overrideAccess: false,
    },
    {
      host: 'demo.gravitee.io',
      path: '/api/my-api-2',
      overrideAccess: true,
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioFormListenersVirtualHostModule, MatIconTestingModule, ReactiveFormsModule, GioHttpTestingModule],
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

  it('should display virtual hosts', async () => {
    testComponent.formControl.setValue(LISTENERS);
    const formVirtualHosts = await loader.getHarness(GioFormListenersVirtualHostHarness);

    const virtualHostRows = await formVirtualHosts.getListenerRows();

    const listeners = await Promise.all(
      virtualHostRows.map(async (row) => ({
        host: await row.hostInput.getValue(),
        path: await row.pathInput.getValue(),
        overrideAccess: await row.overrideAccessInput.isChecked(),
      })),
    );

    expect(listeners).toEqual([
      ...LISTENERS,
      {
        host: '',
        path: '',
        overrideAccess: false,
      },
    ]);
  });

  it('should add new virtual host', async () => {
    const formVirtualHosts = await loader.getHarness(GioFormListenersVirtualHostHarness);

    expect((await formVirtualHosts.getListenerRows()).length).toEqual(1);

    // Add path on last path row
    const emptyLastContextPathRow = await formVirtualHosts.getLastListenerRow();
    await emptyLastContextPathRow.hostInput.setValue('hostname');
    await emptyLastContextPathRow.pathInput.setValue('/api/my-api-3');

    // Expect new row was added
    expect((await formVirtualHosts.getListenerRows()).length).toEqual(2);

    await emptyLastContextPathRow.hostInput.setValue('localhost');
    await emptyLastContextPathRow.pathInput.setValue('/api/my-api-4');
    await emptyLastContextPathRow.overrideAccessInput.check();

    let addedContextPathRow = (await formVirtualHosts.getListenerRows())[0];
    expect({
      host: await addedContextPathRow.hostInput.getValue(),
      path: await addedContextPathRow.pathInput.getValue(),
      overrideAccess: await addedContextPathRow.overrideAccessInput.isChecked(),
    }).toEqual({
      host: 'localhost',
      path: '/api/my-api-4',
      overrideAccess: true,
    });

    // Expect new row was added
    await formVirtualHosts.addListener({ host: 'my-host', path: '/api/my-api-5', overrideAccess: true });

    addedContextPathRow = (await formVirtualHosts.getListenerRows())[1];
    expect({
      host: await addedContextPathRow.hostInput.getValue(),
      path: await addedContextPathRow.pathInput.getValue(),
      overrideAccess: await addedContextPathRow.overrideAccessInput.isChecked(),
    }).toEqual({
      host: 'my-host',
      path: '/api/my-api-5',
      overrideAccess: true,
    });

    expect(testComponent.formControl.value).toEqual([
      {
        _hostDomain: '',
        _hostSubDomain: 'localhost',
        host: 'localhost',
        path: '/api/my-api-4',
        overrideAccess: true,
      },
      {
        _hostDomain: '',
        _hostSubDomain: 'my-host',
        host: 'my-host',
        path: '/api/my-api-5',
        overrideAccess: true,
      },
    ]);
  });

  it('should validate host', async () => {
    const formVirtualHosts = await loader.getHarness(GioFormListenersVirtualHostHarness);

    expect((await formVirtualHosts.getListenerRows()).length).toEqual(1);

    // Add path on last path row
    const emptyLastContextPathRow = await formVirtualHosts.getLastListenerRow();
    await emptyLastContextPathRow.hostInput.setValue('');
    await emptyLastContextPathRow.pathInput.setValue('/api/my-api-3');

    const hostTestElement = await emptyLastContextPathRow.hostInput.host();
    expect(await hostTestElement.hasClass('ng-invalid')).toEqual(true);
    await emptyLastContextPathRow.hostInput.setValue('localhost');
    expect(await hostTestElement.hasClass('ng-invalid')).toEqual(false);
  });

  it('should edit virtual host', async () => {
    testComponent.formControl.setValue(LISTENERS);
    const formContextPaths = await loader.getHarness(GioFormListenersVirtualHostHarness);

    const contextPathRowToEdit = (await formContextPaths.getListenerRows())[1];

    await contextPathRowToEdit.pathInput.setValue('/api/my-api-6');

    const editedContextPathRow = (await formContextPaths.getListenerRows())[1];
    expect({
      host: await editedContextPathRow.hostInput.getValue(),
      path: await editedContextPathRow.pathInput.getValue(),
      overrideAccess: await editedContextPathRow.overrideAccessInput.isChecked(),
    }).toEqual({
      host: 'demo.gravitee.io',
      path: '/api/my-api-6',
      overrideAccess: true,
    });

    expect(testComponent.formControl.value).toEqual([
      {
        ...LISTENERS[0],
        _hostDomain: '',
        _hostSubDomain: 'api.gravitee.io',
      },
      {
        _hostDomain: '',
        _hostSubDomain: 'demo.gravitee.io',
        host: 'demo.gravitee.io',
        path: '/api/my-api-6',
        overrideAccess: true,
      },
    ]);
  });

  it('should remove virtual host row', async () => {
    testComponent.formControl.setValue(LISTENERS);
    const formContextPaths = await loader.getHarness(GioFormListenersVirtualHostHarness);

    const initialContextPathRows = await formContextPaths.getListenerRows();
    expect(initialContextPathRows.length).toEqual(3);

    const contextPathRowToRemove = initialContextPathRows[1];
    await contextPathRowToRemove.removeButton?.click();

    const newContextPathRows = await formContextPaths.getListenerRows();
    expect(newContextPathRows.length).toEqual(2);

    // Check last row does have disabled button
    expect(newContextPathRows[1].removeButton.isDisabled()).toBeTruthy();

    expect(testComponent.formControl.value).toEqual([{ ...LISTENERS[0], _hostDomain: '', _hostSubDomain: 'api.gravitee.io' }]);
  });

  it('should handle touched & dirty on focus and change value', async () => {
    testComponent.formControl.setValue(LISTENERS);
    const formContextPaths = await loader.getHarness(GioFormListenersVirtualHostHarness);

    expect(testComponent.formControl.touched).toEqual(false);
    expect(testComponent.formControl.dirty).toEqual(false);

    await (await formContextPaths.getListenerRows())[0].pathInput.focus();

    expect(testComponent.formControl.touched).toEqual(true);
    expect(testComponent.formControl.dirty).toEqual(false);

    await (await formContextPaths.getListenerRows())[0].pathInput.setValue('Content-Type');

    expect(testComponent.formControl.touched).toEqual(true);
    expect(testComponent.formControl.dirty).toEqual(true);
  });

  it('should handle touched & dirty on focus and change value', async () => {
    testComponent.formControl.setValue(LISTENERS);
    const formHeaders = await loader.getHarness(GioFormListenersVirtualHostHarness);

    expect(testComponent.formControl.touched).toEqual(false);
    expect(testComponent.formControl.dirty).toEqual(false);

    await (await formHeaders.getListenerRows())[0].removeButton?.click();

    expect(testComponent.formControl.touched).toEqual(true);
    expect(testComponent.formControl.dirty).toEqual(true);
  });
});
