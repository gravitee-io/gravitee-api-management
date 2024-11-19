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
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';

import { GioFormListenersKafkaHostHarness } from './gio-form-listeners-kafka-host.harness';
import { GioFormListenersKafkaHostComponent, KafkaHostData } from './gio-form-listeners-kafka-host.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { Constants } from '../../../../../entities/Constants';

@Component({
  template: ` <form [formGroup]="form"><gio-form-listeners-kafka-host formControlName="kafka" /></form> `,
})
class TestComponent {
  public form = new FormGroup({
    kafka: new FormControl({}),
  });
}

@Component({
  template: ` <form [formGroup]="form"><gio-form-listeners-kafka-host formControlName="kafka" apiId="api-id" /></form> `,
})
class TestComponentWithApiId {
  public form = new FormGroup({
    kafka: new FormControl({}),
  });
}

describe('GioFormListenersKafkaHostComponent', () => {
  const fakeConstants = CONSTANTS_TESTING;
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let testComponent: TestComponent;
  let httpTestingController: HttpTestingController;

  const KAFKA_CONFIG: KafkaHostData = {
    host: 'host1',
  };

  describe('without api id', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponent],
        imports: [GioFormListenersKafkaHostComponent, NoopAnimationsModule, MatIconTestingModule, ReactiveFormsModule, GioTestingModule],
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

    it('should display host', async () => {
      testComponent.form.setValue({ kafka: KAFKA_CONFIG });

      const formHarness = await loader.getHarness(GioFormListenersKafkaHostHarness);

      expect(await formHarness.getHostInput().then((host) => host.getValue())).toEqual(KAFKA_CONFIG.host);
    });

    describe('Validation', () => {
      it.each`
        reason                                                               | isValid  | host
        ${'Host cannot be empty'}                                            | ${false} | ${''}
        ${'Host with only whitespace are considered empty'}                  | ${false} | ${'    '}
        ${'Total length should not be greater than 255 chars'}               | ${false} | ${'a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host'}
        ${'Simple host label should be lower than 63 chars'}                 | ${false} | ${'ThisIsALongHostNameWithMoreThan63CharactersWhichIsNotValidInOurCase'}
        ${'A label within a hostname should not be more than 63 chars long'} | ${false} | ${'host.ThisIsALongHostNameWithMoreThan63CharactersWhichIsNotValidInOurCase.gravitee'}
        ${'Can not start with dash'}                                         | ${false} | ${'-simple-host'}
        ${'Can not end with dash'}                                           | ${false} | ${'simple-host-'}
        ${'Host label can not end with dash'}                                | ${false} | ${'simple-host-.host'}
        ${'Can not start with underscore'}                                   | ${false} | ${'_simple-host'}
        ${'Can not end with underscore'}                                     | ${false} | ${'simple-host_'}
        ${'Host label can not end with underscore'}                          | ${false} | ${'simple-host_.host'}
        ${'IPv4 should be a valid host'}                                     | ${true}  | ${'127.0.0.1'}
        ${'Can contain multiple host label'}                                 | ${true}  | ${'dev.simple-host.gravitee.io'}
        ${'Can contain dash'}                                                | ${true}  | ${'simple-host'}
        ${'Can contain underscore'}                                          | ${true}  | ${'simple_host'}
        ${'Can contain dash and underscore'}                                 | ${true}  | ${'simple-host_underscored'}
        ${'Can contain uppercase, numbers, dash and underscore'}             | ${true}  | ${'simple1-Host_underscored33'}
      `('should validate `$reason`: is valid=$isValid', async ({ isValid, host }) => {
        const formHarness = await loader.getHarness(GioFormListenersKafkaHostHarness);
        const hostInput = await formHarness.getHostInput();

        expect(await hostInput.getValue()).toEqual('');

        await hostInput.setValue(host);
        expect(await hostInput.host().then((host) => host.hasClass('ng-invalid'))).toEqual(!isValid);
        if (isValid) {
          expectVerifyHosts([host]);
        }
      });
    });

    it('should edit host', async () => {
      testComponent.form.setValue({ kafka: KAFKA_CONFIG });
      const formHarness = await loader.getHarness(GioFormListenersKafkaHostHarness);

      const hostRowToEdit = await formHarness.getHostInput();
      await hostRowToEdit.setValue('host-modified');
      expectVerifyHosts(['host-modified']);

      expect(testComponent.form.controls.kafka.value).toEqual({ host: 'host-modified' });
    });

    it('should handle touched & dirty on focus and change value', async () => {
      testComponent.form.reset();
      const formHarness = await loader.getHarness(GioFormListenersKafkaHostHarness);

      expect(testComponent.form.touched).toEqual(false);
      expect(testComponent.form.dirty).toEqual(false);

      await formHarness.getHostInput().then((host) => host.focus());

      expect(testComponent.form.touched).toEqual(true);
      expect(testComponent.form.dirty).toEqual(false);

      await formHarness.getHostInput().then((host) => host.setValue('another-host'));
      expectVerifyHosts(['another-host']);

      expect(testComponent.form.touched).toEqual(true);
      expect(testComponent.form.dirty).toEqual(true);
    });
  });

  describe('with api id', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponentWithApiId],
        imports: [GioFormListenersKafkaHostComponent, NoopAnimationsModule, MatIconTestingModule, ReactiveFormsModule, GioTestingModule],
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
      it.each`
        reason                                                               | isValid  | host
        ${'Host cannot be empty'}                                            | ${false} | ${''}
        ${'Host with only whitespace are considered empty'}                  | ${false} | ${'    '}
        ${'Total length should not be greater than 255 chars'}               | ${false} | ${'a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host'}
        ${'Simple host label should be lower than 63 chars'}                 | ${false} | ${'ThisIsALongHostNameWithMoreThan63CharactersWhichIsNotValidInOurCase'}
        ${'A label within a hostname should not be more than 63 chars long'} | ${false} | ${'host.ThisIsALongHostNameWithMoreThan63CharactersWhichIsNotValidInOurCase.gravitee'}
        ${'Can not start with dash'}                                         | ${false} | ${'-simple-host'}
        ${'Can not end with dash'}                                           | ${false} | ${'simple-host-'}
        ${'Host label can not end with dash'}                                | ${false} | ${'simple-host-.host'}
        ${'Can not start with underscore'}                                   | ${false} | ${'_simple-host'}
        ${'Can not end with underscore'}                                     | ${false} | ${'simple-host_'}
        ${'Host label can not end with underscore'}                          | ${false} | ${'simple-host_.host'}
        ${'IPv4 should be a valid host'}                                     | ${true}  | ${'127.0.0.1'}
        ${'Can contain multiple host label'}                                 | ${true}  | ${'dev.simple-host.gravitee.io'}
        ${'Can contain dash'}                                                | ${true}  | ${'simple-host'}
        ${'Can contain underscore'}                                          | ${true}  | ${'simple_host'}
        ${'Can contain dash and underscore'}                                 | ${true}  | ${'simple-host_underscored'}
        ${'Can contain uppercase, numbers, dash and underscore'}             | ${true}  | ${'simple1-Host_underscored33'}
      `('should validate `$reason`: is valid=$isValid', async ({ isValid, host }) => {
        const formHarness = await loader.getHarness(GioFormListenersKafkaHostHarness);
        const hostInput = await formHarness.getHostInput();

        expect(await hostInput.getValue()).toEqual('');

        await hostInput.setValue(host);
        expect(await hostInput.host().then((host) => host.hasClass('ng-invalid'))).toEqual(!isValid);

        if (isValid) {
          expectVerifyHosts([host], true);
        }
      });

      it('should send the API ID when verifying hosts', async () => {
        const formPaths = await loader.getHarness(GioFormListenersKafkaHostHarness);

        const host = await formPaths.getHostInput();

        expectApiVerify();
        httpTestingController.verify({ ignoreCancelled: true });

        await host.setValue('host');
        expect(await host.host().then((hst) => hst.hasClass('ng-invalid'))).toEqual(false);

        const req = httpTestingController.match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/hosts`, method: 'POST' });
        req
          .filter((r) => !r.cancelled)
          .forEach((req) => {
            expect(req.request.body.apiId).toEqual('api-id');
          });
        httpTestingController.verify({ ignoreCancelled: true });
      });
    });
  });

  const expectApiVerify = (inError = false) => {
    httpTestingController
      .match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/hosts`, method: 'POST' })
      .filter((r) => !r.cancelled)
      .map((c) => c.flush({ ok: !inError, reason: inError ? 'error reason' : '' }));
  };

  function expectVerifyHosts(hosts: string[], withApiId = false) {
    const requests = httpTestingController.match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/hosts`, method: 'POST' });
    hosts.forEach((host, index) => {
      const request = requests[index];
      const expectedResult: { apiId?: string; hosts: [string] } = { hosts: [host] };
      if (withApiId) {
        expectedResult.apiId = 'api-id';
      }
      expect(request.request.body).toEqual(expectedResult);
      if (!request.cancelled) request.flush({ ok: true });
    });
  }
});
