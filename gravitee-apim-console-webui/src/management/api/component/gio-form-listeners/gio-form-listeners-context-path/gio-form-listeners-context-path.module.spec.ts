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
import { HttpTestingController } from '@angular/common/http/testing';
import { SpanHarness } from '@gravitee/ui-particles-angular/testing';

import { GioFormListenersContextPathModule } from './gio-form-listeners-context-path.module';
import { GioFormListenersContextPathHarness } from './gio-form-listeners-context-path.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { PortalSettings } from '../../../../../entities/portal/portalSettings';
import { Constants } from '../../../../../entities/Constants';

@Component({
  template: ` <gio-form-listeners-context-path [formControl]="formControl"></gio-form-listeners-context-path> `,
})
class TestComponent {
  public formControl = new FormControl([]);
}

@Component({
  template: ` <gio-form-listeners-context-path [formControl]="formControl" apiId="api-id"></gio-form-listeners-context-path> `,
})
class TestComponentWithApiId {
  public formControl = new FormControl([]);
  public apiId?: string;
}

describe('GioFormListenersContextPathModule', () => {
  const fakeConstants = CONSTANTS_TESTING;
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let testComponent: TestComponent;
  let httpTestingController: HttpTestingController;

  const LISTENERS = [
    {
      path: '/api/my-api1',
    },
    {
      path: '/api/my-api-2',
    },
  ];

  const expectGetPortalSettings = () => {
    const requests = httpTestingController
      .match({
        url: `${CONSTANTS_TESTING.env.baseURL}/portal`,
        method: 'GET',
      });
    expect(requests.length).toBeLessThanOrEqual(1);
  };

  const expectApiVerify = (inError = false) => {
    httpTestingController
      .match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/paths`, method: 'POST' })
      .filter((r) => !r.cancelled)
      .map((c) => c.flush({ ok: !inError, reason: inError ? 'error reason' : '' }));
  };

  describe('without api id', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponent],
        imports: [NoopAnimationsModule, GioFormListenersContextPathModule, MatIconTestingModule, ReactiveFormsModule, GioTestingModule],
        providers: [
          {
            provide: Constants,
            useValue: fakeConstants,
          },
        ],
      });
      fixture = TestBed.createComponent(TestComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      testComponent = fixture.componentInstance;
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
      expectGetPortalSettings();
      expectApiVerify();
    });

    afterEach(() => {
      httpTestingController.verify({ ignoreCancelled: true });
    });

    it('should display paths', async () => {
      testComponent.formControl.setValue(LISTENERS);

      const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

      const pathRows = await formPaths.getListenerRows();
      expectApiVerify();

      const paths = await Promise.all(
        pathRows.map(async (row) => ({
          path: await row.pathInput.getValue(),
        })),
      );

      expect(paths).toEqual([...LISTENERS]);
    });

    it('should add new context path', async () => {
      const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

      expect((await formPaths.getListenerRows()).length).toEqual(1);

      // Expect new row was added
      const contextPathRowAdded = await formPaths.getLastListenerRow();
      await contextPathRowAdded.pathInput.setValue('/api/my-api-4');

      // Expect new row was added
      await formPaths.addListener({ path: '/api/my-api-5' });
      expect((await formPaths.getListenerRows()).length).toEqual(2);

      expect(testComponent.formControl.value).toEqual([{ path: '/api/my-api-4' }, { path: '/api/my-api-5' }]);
      expectApiVerify();
    });

    it('should validate path', async () => {
      const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

      expect((await formPaths.getListenerRows()).length).toEqual(1);

      // Add path on last path row
      const emptyLastContextPathRow = await formPaths.getLastListenerRow();
      const pathInputHost = await emptyLastContextPathRow.pathInput.host();

      // Invalid: should start with /
      await emptyLastContextPathRow.pathInput.setValue('bad-path');
      expect(await pathInputHost.hasClass('ng-invalid')).toEqual(true);

      // Invalid: should not contain //
      await emptyLastContextPathRow.pathInput.setValue('/bad//path');
      expect(await pathInputHost.hasClass('ng-invalid')).toEqual(true);

      // Invalid: contains invalid char
      await emptyLastContextPathRow.pathInput.setValue('/abc yeh');
      expect(await pathInputHost.hasClass('ng-invalid')).toEqual(true);

      // Invalid: contains < 3 chars
      await emptyLastContextPathRow.pathInput.setValue('/ba');
      expect(await pathInputHost.hasClass('ng-invalid')).toEqual(true);

      // Valid
      await emptyLastContextPathRow.pathInput.setValue('/good-path');
      expect(await pathInputHost.hasClass('ng-invalid')).toEqual(false);
      expectApiVerify();
    });

    it('should mark control as invalid if path is already defined', async () => {
      const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);
      await formPaths.addListenerRow();

      const rows = await formPaths.getListenerRows();
      expect(rows.length).toEqual(2);

      await rows[0].pathInput.setValue('/api');
      expectApiVerify();

      await rows[1].pathInput.setValue('/api');
      expectApiVerify();

      expect(await loader.getHarness(SpanHarness.with({ text: /Duplicated context path not allowed/ }))).toBeTruthy();
    });

    it('should mark control as valid is path is using the same root but different path', async () => {
      const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);
      await formPaths.getAddButton().then((b) => b.click());
      const rows = await formPaths.getListenerRows();
      expect(rows.length).toEqual(2);

      await rows[0].pathInput.setValue('/api/delete');
      expectApiVerify();

      await rows[1].pathInput.setValue('/api/create');
      expectApiVerify();

      fixture.detectChanges();
      expect(await (await rows[0].pathInput.host()).hasClass('ng-invalid')).toEqual(false);
      expect(await (await rows[1].pathInput.host()).hasClass('ng-invalid')).toEqual(false);
    });

    it('should edit context path', async () => {
      testComponent.formControl.setValue(LISTENERS);
      const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

      const contextPathRowToEdit = (await formPaths.getListenerRows())[1];

      await contextPathRowToEdit.pathInput.setValue('/api/my-api-6');
      expectApiVerify();

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
      expect(initialContextPathRows.length).toEqual(2);

      const contextPathRowToRemove = initialContextPathRows[1];
      await contextPathRowToRemove.removeButton?.click();

      const newContextPathRows = await formPaths.getListenerRows();
      expectApiVerify();
      expect(newContextPathRows.length).toEqual(1);

      // Check last row does have disabled remove button
      expect(newContextPathRows[0].removeButton.isDisabled()).toBeTruthy();

      expect(testComponent.formControl.value).toEqual([LISTENERS[0]]);
    });

    it('should handle touched & dirty on focus and change value', async () => {
      testComponent.formControl = new FormControl(LISTENERS);
      const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

      expect(testComponent.formControl.touched).toEqual(false);
      expect(testComponent.formControl.dirty).toEqual(false);

      await (await formPaths.getListenerRows())[0].pathInput.focus();

      expect(testComponent.formControl.touched).toEqual(true);
      expect(testComponent.formControl.dirty).toEqual(false);

      await (await formPaths.getListenerRows())[0].pathInput.setValue('Content-Type');
      expectApiVerify();

      expect(testComponent.formControl.touched).toEqual(true);
      expect(testComponent.formControl.dirty).toEqual(true);
    });

    it('should not show add button or delete button and be unmodifiable when disabled', async () => {
      testComponent.formControl = new FormControl({ value: LISTENERS, disabled: true });

      const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

      const contextPathRow = (await formPaths.getListenerRows())[1];
      expectApiVerify();

      expect(await contextPathRow.pathInput.isDisabled()).toEqual(true);
      expect(await contextPathRow.removeButton).toBeFalsy();

      await formPaths
        .getAddButton()
        .then((_) => fail('The add button should not appear'))
        .catch((err) => expect(err).toBeTruthy());
    });
  });

  describe('with api id', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponentWithApiId],
        imports: [NoopAnimationsModule, GioFormListenersContextPathModule, MatIconTestingModule, ReactiveFormsModule, GioTestingModule],
        providers: [
          {
            provide: Constants,
            useValue: fakeConstants,
          },
        ],
      });
      fixture = TestBed.createComponent(TestComponentWithApiId);
      loader = TestbedHarnessEnvironment.loader(fixture);
      testComponent = fixture.componentInstance;
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
      expectGetPortalSettings();
      expectApiVerify();
    });

    afterEach(() => {
      httpTestingController.verify({ ignoreCancelled: true });
    });

    it('should send the API ID when verifying paths', async () => {
      const formPaths = await loader.getHarness(GioFormListenersContextPathHarness);

      expect((await formPaths.getListenerRows()).length).toEqual(1);

      // Add path on last path row
      const emptyLastContextPathRow = await formPaths.getLastListenerRow();
      const pathInputHost = await emptyLastContextPathRow.pathInput.host();

      expectApiVerify();
      httpTestingController.verify({ ignoreCancelled: true });

      await emptyLastContextPathRow.pathInput.setValue('/ignored-path');
      expect(await pathInputHost.hasClass('ng-invalid')).toEqual(false);

      const req = httpTestingController.match({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/paths`,
        method: 'POST',
      });
      req
        .filter((r) => !r.cancelled)
        .forEach((req) => {
          expect(req.request.body.apiId).toEqual('api-id');
        });
      httpTestingController.verify({ ignoreCancelled: true });
    });
  });
});
