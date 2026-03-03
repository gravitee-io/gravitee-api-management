/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ApplicationTabSettingsReadHarness } from './application-tab-settings-read/application-tab-settings-read.harness';
import { ApplicationTabSettingsComponent } from './application-tab-settings.component';
import { ConfirmDialogComponent } from '../../../../components/confirm-dialog/confirm-dialog.component';
import { Application, ApplicationType } from '../../../../entities/application/application';
import {
  fakeApplication,
  fakeBackendToBackendApplicationType,
  fakeBrowserApplicationType,
  fakeNativeApplicationType,
  fakeSimpleApplicationType,
  fakeWebApplicationType,
} from '../../../../entities/application/application.fixture';
import { fakeUserApplicationPermissions } from '../../../../entities/permission/permission.fixtures';
import { ConfigService } from '../../../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('ApplicationTabSettingsComponent - Read view', () => {
  let fixture: ComponentFixture<ApplicationTabSettingsComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let readonlyHarness: ApplicationTabSettingsReadHarness;
  const applicationId = 'id1';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationTabSettingsComponent, ConfirmDialogComponent, HttpClientTestingModule, NoopAnimationsModule, AppTestingModule],
      providers: [
        {
          provide: ConfigService,
          useValue: {
            baseURL: TESTING_BASE_URL,
          },
        },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApplicationTabSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);

    fixture.componentRef.setInput('applicationId', applicationId);
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ DEFINITION: ['R'] }));
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function flushGetRequests(application: Application) {
    httpTestingController.match(`${TESTING_BASE_URL}/applications/${applicationId}`).forEach(req => {
      expect(req.request.method).toBe('GET');
      req.flush(application);
      fixture.detectChanges();
    });
  }

  async function initRestCalls(application: Application, applicationType: ApplicationType) {
    fixture.componentRef.setInput('applicationTypeConfiguration', applicationType);
    fixture.detectChanges();

    flushGetRequests(application);
    await fixture.whenStable();
    flushGetRequests(application);
    await fixture.whenStable();

    readonlyHarness = await loader.getHarness(ApplicationTabSettingsReadHarness);
  }

  describe('Display a simple application', () => {
    const simpleApplication = fakeApplication({
      id: applicationId,
      name: 'Simple application',
      description: 'Simple description',
      applicationType: 'SIMPLE',
    });

    beforeEach(async () => {
      await initRestCalls(simpleApplication, fakeSimpleApplicationType());
    });

    it('Should display application name, owner, type, security type and description', async () => {
      expect(await readonlyHarness.getName()).toEqual('Simple application');
      expect(await readonlyHarness.getOwner()).toEqual('Admin master');
      expect(await readonlyHarness.getType()).toEqual('Simple');
      expect(await readonlyHarness.getSecurityType()).toEqual('Simple');
      expect(await readonlyHarness.getDescription()).toEqual('Simple description');
      expect(await readonlyHarness.canEdit()).toBeFalsy();
    });
  });

  describe('Display Backend to backend Application', () => {
    const b2bApplication = fakeApplication({
      id: applicationId,
      name: 'B2b application',
      description: 'B2b description',
      applicationType: 'BACKEND_TO_BACKEND',
    });

    beforeEach(async () => {
      await initRestCalls(b2bApplication, fakeBackendToBackendApplicationType());
    });

    it('Should display application name, owner, type, security type and description', async () => {
      expect(await readonlyHarness.getName()).toEqual('B2b application');
      expect(await readonlyHarness.getOwner()).toEqual('Admin master');
      expect(await readonlyHarness.getType()).toEqual('Backend to backend');
      expect(await readonlyHarness.getSecurityType()).toEqual('Backend to backend');
      expect(await readonlyHarness.getDescription()).toEqual('B2b description');
      expect(await readonlyHarness.canEdit()).toBeFalsy();
    });
  });

  describe('Display Native Application', () => {
    const nativeApplication = fakeApplication({
      id: applicationId,
      name: 'Native application',
      description: 'Native description',
      applicationType: 'NATIVE',
    });

    beforeEach(async () => {
      await initRestCalls(nativeApplication, fakeNativeApplicationType());
    });

    it('Should display application name, owner, type, security type and description', async () => {
      expect(await readonlyHarness.getName()).toEqual('Native application');
      expect(await readonlyHarness.getOwner()).toEqual('Admin master');
      expect(await readonlyHarness.getType()).toEqual('Native');
      expect(await readonlyHarness.getSecurityType()).toEqual('Native');
      expect(await readonlyHarness.getDescription()).toEqual('Native description');
      expect(await readonlyHarness.canEdit()).toBeFalsy();
    });
  });

  describe('Display Browser Application', () => {
    const browserApplication = fakeApplication({
      id: applicationId,
      name: 'Browser application',
      description: 'Browser description',
      applicationType: 'BROWSER',
    });

    beforeEach(async () => {
      await initRestCalls(browserApplication, fakeBrowserApplicationType());
    });

    it('Should display application name, owner, type, security type and description', async () => {
      expect(await readonlyHarness.getName()).toEqual('Browser application');
      expect(await readonlyHarness.getOwner()).toEqual('Admin master');
      expect(await readonlyHarness.getType()).toEqual('SPA');
      expect(await readonlyHarness.getSecurityType()).toEqual('SPA');
      expect(await readonlyHarness.getDescription()).toEqual('Browser description');
      expect(await readonlyHarness.canEdit()).toBeFalsy();
    });
  });

  describe('Display Web Application', () => {
    const webApplication = fakeApplication({
      id: applicationId,
      name: 'Web application',
      description: 'Web description',
      applicationType: 'WEB',
    });

    beforeEach(async () => {
      await initRestCalls(webApplication, fakeWebApplicationType());
    });

    it('Should display application name, owner, type, security type and description', async () => {
      expect(await readonlyHarness.getName()).toEqual('Web application');
      expect(await readonlyHarness.getOwner()).toEqual('Admin master');
      expect(await readonlyHarness.getType()).toEqual('Web');
      expect(await readonlyHarness.getSecurityType()).toEqual('Web');
      expect(await readonlyHarness.getDescription()).toEqual('Web description');
      expect(await readonlyHarness.canEdit()).toBeFalsy();
    });
  });
});
