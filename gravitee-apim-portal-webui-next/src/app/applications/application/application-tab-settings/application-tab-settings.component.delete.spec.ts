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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogModule } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';

import { ApplicationTabSettingsComponent } from './application-tab-settings.component';
import { DeleteConfirmDialogComponent } from './delete-confirm-dialog/delete-confirm-dialog.component';
import { DeleteConfirmDialogHarness } from './delete-confirm-dialog/delete-confirm-dialog.harness';
import { fakeApplication, fakeSimpleApplicationType } from '../../../../entities/application/application.fixture';
import { fakeUserApplicationPermissions } from '../../../../entities/permission/permission.fixtures';
import { ConfigService } from '../../../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('ApplicationTabSettingsComponent - Test application deletion', () => {
  let component: ApplicationTabSettingsComponent;
  let fixture: ComponentFixture<ApplicationTabSettingsComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  const applicationId = fakeApplication().id;

  async function flushGetApplicationRequest() {
    httpTestingController.match(`${TESTING_BASE_URL}/applications/${applicationId}`).forEach(req => {
      expect(req.request.method).toBe('GET');
      req.flush(fakeApplication());
      fixture.detectChanges();
    });
    await fixture.whenStable();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ApplicationTabSettingsComponent,
        DeleteConfirmDialogComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
        AppTestingModule,
        MatDialogModule,
      ],
      providers: [
        {
          provide: ConfigService,
          useValue: {
            baseURL: TESTING_BASE_URL,
          },
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApplicationTabSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    component = fixture.componentInstance;
    component.applicationId = applicationId;
  });

  async function initRestCalls() {
    component.applicationTypeConfiguration = fakeSimpleApplicationType();
    const application = fakeApplication();
    fixture.detectChanges();

    const applicationUrl = `${TESTING_BASE_URL}/applications/${applicationId}`;

    const applicationRequest = httpTestingController.expectOne(applicationUrl);
    expect(applicationRequest.request.method).toBe('GET');
    applicationRequest.flush(application);
    fixture.detectChanges();
    await fixture.whenStable();

    await flushGetApplicationRequest();
  }

  afterEach(() => {
    httpTestingController.verify();
  });

  it('Should be able to delete application', async () => {
    component.userApplicationPermissions = fakeUserApplicationPermissions({
      DEFINITION: ['D'],
    });
    await initRestCalls();

    fixture.detectChanges();
    await fixture.whenStable();

    const router: Router = TestBed.inject(Router);

    jest.spyOn(router, 'navigate');

    const deleteButton = await getDeleteButton();
    expect(await deleteButton!.isDisabled()).toBeFalsy();
    await deleteButton!.click();
    fixture.detectChanges();
    await fixture.whenStable();

    await flushGetApplicationRequest();

    let confirmDialog = await deleteConfirmDialog();
    expect(confirmDialog).not.toBeNull();
    await confirmDialog!.cancel();

    confirmDialog = await deleteConfirmDialog();
    expect(confirmDialog).toBeNull();
    httpTestingController.expectNone({
      url: `${TESTING_BASE_URL}/applications/${applicationId}`,
      method: 'DELETE',
    });
    expect(router.navigate).not.toHaveBeenCalled();
    await deleteButton!.click();
    await flushGetApplicationRequest();
    confirmDialog = await deleteConfirmDialog();
    expect(confirmDialog).not.toBeNull();
    await confirmDialog!.confirm();

    confirmDialog = await deleteConfirmDialog();
    expect(confirmDialog).toBeNull();
    httpTestingController
      .expectOne({
        url: `${TESTING_BASE_URL}/applications/${applicationId}`,
        method: 'DELETE',
      })
      .flush(null);
    expect(router.navigate).toHaveBeenCalledWith(['/applications']);
  });

  it('Should not be able to delete application', async () => {
    component.userApplicationPermissions = fakeUserApplicationPermissions({
      DEFINITION: ['R'],
    });
    initRestCalls();

    fixture.detectChanges();

    const deleteButton = await getDeleteButton();
    expect(deleteButton).toBeNull();
  });

  async function getDeleteButton(): Promise<MatButtonHarness | null> {
    return await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testId="delete"]' }));
  }

  async function deleteConfirmDialog(): Promise<DeleteConfirmDialogHarness | null> {
    return await rootLoader.getHarnessOrNull(DeleteConfirmDialogHarness);
  }
});
