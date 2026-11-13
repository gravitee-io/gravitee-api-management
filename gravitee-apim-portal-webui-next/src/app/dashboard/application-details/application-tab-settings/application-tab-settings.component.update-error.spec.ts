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

import { ApplicationTabSettingsEditHarness } from './application-tab-settings-edit/application-tab-settings-edit.harness';
import { ApplicationTabSettingsComponent } from './application-tab-settings.component';
import { ConfirmDialogComponent } from '../../../../components/confirm-dialog/confirm-dialog.component';
import { Application } from '../../../../entities/application/application';
import { fakeApplication, fakeSimpleApplicationType } from '../../../../entities/application/application.fixture';
import { fakeUserApplicationPermissions } from '../../../../entities/permission/permission.fixtures';
import { ConfigService } from '../../../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('ApplicationTabSettingsComponent - update rejected by the API', () => {
  let fixture: ComponentFixture<ApplicationTabSettingsComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  const applicationId = 'id1';

  let simpleApplication: Application;

  beforeEach(async () => {
    simpleApplication = fakeApplication({
      id: applicationId,
      name: 'Simple application',
      applicationType: 'SIMPLE',
      settings: { app: { type: 'Custom Application Type', client_id: 'client-one' } },
    });

    await TestBed.configureTestingModule({
      imports: [ApplicationTabSettingsComponent, ConfirmDialogComponent, HttpClientTestingModule, NoopAnimationsModule, AppTestingModule],
      providers: [{ provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } }],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationTabSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.componentRef.setInput('applicationId', applicationId);
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ DEFINITION: ['U'] }));
    fixture.componentRef.setInput('applicationTypeConfiguration', fakeSimpleApplicationType());
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function flushGetRequests(application: Application) {
    httpTestingController.match(`${TESTING_BASE_URL}/applications/${applicationId}`).forEach(req => {
      req.flush(application);
      fixture.detectChanges();
    });
  }

  async function saveWithClientId(clientId: string, errorBody: object | null) {
    fixture.detectChanges();
    flushGetRequests(simpleApplication);
    await fixture.whenStable();
    flushGetRequests(simpleApplication);
    await fixture.whenStable();

    fixture.componentRef.instance.isEditing.set(true);
    fixture.detectChanges();
    await fixture.whenStable();
    flushGetRequests(simpleApplication);
    await fixture.whenStable();

    const editHarness = await loader.getHarness(ApplicationTabSettingsEditHarness);
    await editHarness.changeSimpleClientId(clientId);
    await editHarness.saveApplication();

    httpTestingController
      .expectOne({ url: `${TESTING_BASE_URL}/applications/${applicationId}`, method: 'PUT' })
      .flush(errorBody, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function saveErrorText(): string | undefined {
    return (fixture.nativeElement as HTMLElement).querySelector('[data-testId="save-error"]')?.textContent?.trim();
  }

  it('should show the API error when the client ID already exists', async () => {
    const message = 'An application already exists with client_id [client-two]';

    await saveWithClientId('client-two', {
      errors: [{ code: 'errors.application.clientId.already.exists', status: '400', message }],
    });

    expect(saveErrorText()).toEqual(message);
    expect(await loader.getHarnessOrNull(ApplicationTabSettingsEditHarness)).not.toBeNull();
  });

  it('should show a generic error when the API error carries no message', async () => {
    await saveWithClientId('client-two', {});

    expect(saveErrorText()).toEqual('An error occurred while saving the application. Please try again.');
  });
});
