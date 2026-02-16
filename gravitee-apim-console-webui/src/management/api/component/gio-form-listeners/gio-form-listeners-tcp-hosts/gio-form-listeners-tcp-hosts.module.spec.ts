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

import { GioFormListenersTcpHostsModule } from './gio-form-listeners-tcp-hosts.module';
import { GioFormListenersTcpHostsHarness } from './gio-form-listeners-tcp-hosts.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { Constants } from '../../../../../entities/Constants';
import { ListenerType } from '../../../../../entities/management-api-v2';

@Component({
  template: ` <gio-form-listeners-tcp-hosts [formControl]="formControl"></gio-form-listeners-tcp-hosts> `,
  standalone: false,
})
class TestComponent {
  public formControl = new FormControl([]);
}

@Component({
  template: ` <gio-form-listeners-tcp-hosts [formControl]="formControl" apiId="api-id"></gio-form-listeners-tcp-hosts> `,
  standalone: false,
})
class TestComponentWithApiId {
  public formControl = new FormControl([]);
}

describe('GioFormListenersTcpHostsModule', () => {
  const fakeConstants = CONSTANTS_TESTING;
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let testComponent: TestComponent;
  let httpTestingController: HttpTestingController;

  const LISTENERS = [
    {
      host: 'host1',
    },
    {
      host: 'host2',
    },
  ];

  describe('without api id', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponent],
        imports: [NoopAnimationsModule, GioFormListenersTcpHostsModule, MatIconTestingModule, ReactiveFormsModule, GioTestingModule],
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
    });

    afterEach(() => {
      httpTestingController.verify({ ignoreCancelled: true });
    });

    it('should display hosts', async () => {
      testComponent.formControl.setValue(LISTENERS);

      const formHosts = await loader.getHarness(GioFormListenersTcpHostsHarness);

      const hostRows = await formHosts.getListenerRows();

      const hosts = await Promise.all(
        hostRows.map(async row => ({
          host: await row.hostInput.getValue(),
        })),
      );

      expect(hosts).toEqual([...LISTENERS]);
    });

    it('should add new host', async () => {
      const formHosts = await loader.getHarness(GioFormListenersTcpHostsHarness);

      expect((await formHosts.getListenerRows()).length).toEqual(1);

      // Set first row
      const hostRowAdded = await formHosts.getLastListenerRow();
      await hostRowAdded.hostInput.setValue('new-host');
      expectVerifyHosts(['new-host']);

      // Expect new row was added
      await formHosts.addListener({ host: 'new-host-2' });
      expect((await formHosts.getListenerRows()).length).toEqual(2);
      expectVerifyHosts(['new-host-2']);

      expect(testComponent.formControl.value).toEqual([{ host: 'new-host' }, { host: 'new-host-2' }]);
    });

    describe('Validation', () => {
      it('should mark controls as invalid if host is already defined', async () => {
        const formHosts = await loader.getHarness(GioFormListenersTcpHostsHarness);
        await formHosts.addListenerRow();

        const rows = await formHosts.getListenerRows();
        expect(rows.length).toEqual(2);

        await rows[0].hostInput.setValue('my-host');
        expectVerifyHosts(['my-host']);

        await rows[1].hostInput.setValue('my-host');
        expectVerifyHosts(['my-host']);

        expect(await loader.getHarness(SpanHarness.with({ text: /Duplicated hosts not allowed/ }))).toBeTruthy();
      });

      it.each`
        reason                                                               | isValid  | host
        ${'Host cannot be empty'}                                            | ${false} | ${''}
        ${'Host with only whitespace are considered empty'}                  | ${false} | ${'    '}
        ${'Total length should not be greater than 255 chars'}               | ${false} | ${'a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host'}
        ${'Simple host label should be lower than 63 chars'}                 | ${false} | ${'thisisalonghostnamewithmorethan63charactereswhichisnotvalidinourcase'}
        ${'A label within a hostname should not be more than 63 chars long'} | ${false} | ${'host.thisisalonghostnamewithmorethan63charactereswhichisnotvalidinourcase.gravitee'}
        ${'Can not start with dash'}                                         | ${false} | ${'-simple-host'}
        ${'Can not end with dash'}                                           | ${false} | ${'simple-host-'}
        ${'Host label can not end with dash'}                                | ${false} | ${'simple-host-.host'}
        ${'Can not start with underscore'}                                   | ${false} | ${'_simple-host'}
        ${'Can not end with underscore'}                                     | ${false} | ${'simple-host_'}
        ${'Host label can not end with underscore'}                          | ${false} | ${'simple-host_.host'}
        ${'Cannot contain uppercase'}                                        | ${false} | ${'simple-Host'}
        ${'IPv4 should be a valid host'}                                     | ${true}  | ${'127.0.0.1'}
        ${'Can contain multiple host label'}                                 | ${true}  | ${'dev.simple-host.gravitee.io'}
        ${'Can contain dash'}                                                | ${true}  | ${'simple-host'}
        ${'Can contain underscore'}                                          | ${true}  | ${'simple_host'}
        ${'Can contain dash and underscore'}                                 | ${true}  | ${'simple-host_underscored'}
        ${'Can contain lowercase, numbers, dash and underscore'}             | ${true}  | ${'simple1-host_underscored33'}
      `('should validate `$reason`: is valid=$isValid', async ({ isValid, host }) => {
        const formHosts = await loader.getHarness(GioFormListenersTcpHostsHarness);

        expect((await formHosts.getListenerRows()).length).toEqual(1);

        // Add host on last host row
        const emptyLastHostRow = await formHosts.getLastListenerRow();
        const hostInputHost = await emptyLastHostRow.hostInput.host();

        await emptyLastHostRow.hostInput.setValue(host);
        expect(await hostInputHost.hasClass('ng-invalid')).toEqual(!isValid);
        if (isValid) {
          expectVerifyHosts([host]);
        }
      });
    });

    it('should edit host', async () => {
      testComponent.formControl.setValue(LISTENERS);
      const formHosts = await loader.getHarness(GioFormListenersTcpHostsHarness);

      const hostRowToEdit = (await formHosts.getListenerRows())[1];
      await hostRowToEdit.hostInput.setValue('host-modified');
      expectVerifyHosts(['host-modified']);

      const editedHostRow = (await formHosts.getListenerRows())[1];
      expect({ host: await editedHostRow.hostInput.getValue() }).toEqual({
        host: 'host-modified',
      });

      expect(testComponent.formControl.value).toEqual([
        LISTENERS[0],
        {
          host: 'host-modified',
        },
      ]);
    });

    it('should remove host row', async () => {
      testComponent.formControl.setValue(LISTENERS);
      const formHosts = await loader.getHarness(GioFormListenersTcpHostsHarness);

      const initialHostRows = await formHosts.getListenerRows();
      expect(initialHostRows.length).toEqual(2);

      const hostRowToRemove = initialHostRows[1];
      await hostRowToRemove.removeButton?.click();

      const newHostRows = await formHosts.getListenerRows();
      expect(newHostRows.length).toEqual(1);

      // Check last row does have disabled remove button
      expect(newHostRows[0].removeButton.isDisabled()).toBeTruthy();

      expect(testComponent.formControl.value).toEqual([LISTENERS[0]]);
    });

    it('should handle touched & dirty on focus and change value', async () => {
      testComponent.formControl = new FormControl(LISTENERS);
      const formHosts = await loader.getHarness(GioFormListenersTcpHostsHarness);

      expect(testComponent.formControl.touched).toEqual(false);
      expect(testComponent.formControl.dirty).toEqual(false);

      await (await formHosts.getListenerRows())[0].hostInput.focus();

      expect(testComponent.formControl.touched).toEqual(true);
      expect(testComponent.formControl.dirty).toEqual(false);

      await (await formHosts.getListenerRows())[0].hostInput.setValue('another-host');
      expectVerifyHosts(['another-host']);

      expect(testComponent.formControl.touched).toEqual(true);
      expect(testComponent.formControl.dirty).toEqual(true);
    });

    it('should not show add button or delete button and be unmodifiable when disabled', async () => {
      testComponent.formControl = new FormControl({ value: LISTENERS, disabled: true });
      const formHosts = await loader.getHarness(GioFormListenersTcpHostsHarness);

      const hostRow = (await formHosts.getListenerRows())[1];

      expect(await hostRow.hostInput.isDisabled()).toEqual(true);
      expect(await hostRow.removeButton).toBeFalsy();

      await formHosts
        .getAddButton()
        .then(_ => fail('The add button should not appear'))
        .catch(err => expect(err).toBeTruthy());
    });
  });

  describe('with api id', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponentWithApiId],
        imports: [NoopAnimationsModule, GioFormListenersTcpHostsModule, MatIconTestingModule, ReactiveFormsModule, GioTestingModule],
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
    });

    afterEach(() => {
      httpTestingController.verify({ ignoreCancelled: true });
    });

    describe('Validation', () => {
      it('should mark controls as invalid if host is already defined', async () => {
        const formHosts = await loader.getHarness(GioFormListenersTcpHostsHarness);
        await formHosts.addListenerRow();

        const rows = await formHosts.getListenerRows();
        expect(rows.length).toEqual(2);

        await rows[0].hostInput.setValue('my-host');
        expectVerifyHosts(['my-host'], true);

        await rows[1].hostInput.setValue('my-host');
        expectVerifyHosts(['my-host'], true);

        expect(await loader.getHarness(SpanHarness.with({ text: /Duplicated hosts not allowed/ }))).toBeTruthy();
      });

      it.each`
        reason                                                               | isValid  | host
        ${'Host cannot be empty'}                                            | ${false} | ${''}
        ${'Host with only whitespace are considered empty'}                  | ${false} | ${'    '}
        ${'Total length should not be greater than 255 chars'}               | ${false} | ${'a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host'}
        ${'Simple host label should be lower than 63 chars'}                 | ${false} | ${'thisisalonghostnamewithmorethan63charactereswhichisnotvalidinourcase'}
        ${'A label within a hostname should not be more than 63 chars long'} | ${false} | ${'host.thisisalonghostnamewithmorethan63charactereswhichisnotvalidinourcase.gravitee'}
        ${'Can not start with dash'}                                         | ${false} | ${'-simple-host'}
        ${'Can not end with dash'}                                           | ${false} | ${'simple-host-'}
        ${'Host label can not end with dash'}                                | ${false} | ${'simple-host-.host'}
        ${'Can not start with underscore'}                                   | ${false} | ${'_simple-host'}
        ${'Can not end with underscore'}                                     | ${false} | ${'simple-host_'}
        ${'Host label can not end with underscore'}                          | ${false} | ${'simple-host_.host'}
        ${'Cannot contain uppercase'}                                        | ${false} | ${'simple-Host'}
        ${'IPv4 should be a valid host'}                                     | ${true}  | ${'127.0.0.1'}
        ${'Can contain multiple host label'}                                 | ${true}  | ${'dev.simple-host.gravitee.io'}
        ${'Can contain dash'}                                                | ${true}  | ${'simple-host'}
        ${'Can contain underscore'}                                          | ${true}  | ${'simple_host'}
        ${'Can contain dash and underscore'}                                 | ${true}  | ${'simple-host_underscored'}
        ${'Can contain lowercase, numbers, dash and underscore'}             | ${true}  | ${'simple1-host_underscored33'}
      `('should validate `$reason`: is valid=$isValid', async ({ isValid, host }) => {
        const formHosts = await loader.getHarness(GioFormListenersTcpHostsHarness);

        expect((await formHosts.getListenerRows()).length).toEqual(1);

        // Add host on last host row
        const emptyLastHostRow = await formHosts.getLastListenerRow();
        const hostInputHost = await emptyLastHostRow.hostInput.host();

        await emptyLastHostRow.hostInput.setValue(host);
        expect(await hostInputHost.hasClass('ng-invalid')).toEqual(!isValid);
        if (isValid) {
          expectVerifyHosts([host], true);
        }
      });

      it('should send the API ID when verifying hosts', async () => {
        const formPaths = await loader.getHarness(GioFormListenersTcpHostsHarness);

        expect((await formPaths.getListenerRows()).length).toEqual(1);

        // Add path on last path row
        const emptyLastContextPathRow = await formPaths.getLastListenerRow();
        const pathInputHost = await emptyLastContextPathRow.hostInput.host();

        expectApiVerify();
        httpTestingController.verify({ ignoreCancelled: true });

        await emptyLastContextPathRow.hostInput.setValue('host');
        expect(await pathInputHost.hasClass('ng-invalid')).toEqual(false);

        const req = httpTestingController.match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/hosts`, method: 'POST' });
        req
          .filter(r => !r.cancelled)
          .forEach(req => {
            expect(req.request.body.apiId).toEqual('api-id');
          });
        httpTestingController.verify({ ignoreCancelled: true });
      });
    });
  });

  const expectApiVerify = (inError = false) => {
    httpTestingController
      .match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/hosts`, method: 'POST' })
      .filter(r => !r.cancelled)
      .map(c => c.flush({ ok: !inError, reason: inError ? 'error reason' : '' }));
  };

  function expectVerifyHosts(hosts: string[], withApiId = false) {
    const requests = httpTestingController.match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/hosts`, method: 'POST' });
    hosts.forEach((host, index) => {
      const request = requests[index];
      const expectedResult: { apiId?: string; hosts: [string]; listenerType: ListenerType } = { hosts: [host], listenerType: 'TCP' };
      if (withApiId) {
        expectedResult.apiId = 'api-id';
      }
      expect(request.request.body).toEqual(expectedResult);
      if (!request.cancelled) request.flush({ ok: true });
    });
  }
});
