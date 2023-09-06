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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatButtonHarness } from '@angular/material/button/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { GioConfirmAndValidateDialogHarness, GioLicenseService, LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { HarnessLoader } from '@angular/cdk/testing';
import { SimpleChange } from '@angular/core';
import { of } from 'rxjs';

import { ApiGeneralInfoDangerZoneComponent } from './api-general-info-danger-zone.component';

import { ApiGeneralInfoModule } from '../api-general-info.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { CurrentUserService, UIRouterState } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { Api, fakeApiV2, fakeApiV4 } from '../../../../../entities/management-api-v2';

describe('ApiGeneralInfoDangerZoneComponent', () => {
  const API_ID = 'apiId';
  const fakeAjsState = {
    go: jest.fn(),
  };
  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u', 'api-definition-d'];

  let fixture: ComponentFixture<ApiGeneralInfoDangerZoneComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let component: ApiGeneralInfoDangerZoneComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiGeneralInfoModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeAjsState },
        { provide: CurrentUserService, useValue: { currentUser } },
        {
          provide: 'LicenseConfiguration',
          useValue: LICENSE_CONFIGURATION_TESTING,
        },
        {
          provide: 'Constants',
          useValue: {
            ...CONSTANTS_TESTING,
            env: {
              ...CONSTANTS_TESTING.env,
              settings: {
                ...CONSTANTS_TESTING.env.settings,
                apiReview: {
                  enabled: true,
                },
              },
            },
            org: {
              ...CONSTANTS_TESTING.org,
              settings: {
                ...CONSTANTS_TESTING.org.settings,
                emulateV4Engine: {
                  defaultValue: 'v4-emulation-engine',
                },
              },
            },
          },
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  const createComponent = (api: Api) => {
    fixture = TestBed.createComponent(ApiGeneralInfoDangerZoneComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    component = fixture.componentInstance;
    component.api = api;
    fixture.detectChanges();
    component.ngOnChanges({ api: {} as SimpleChange });
    jest.spyOn(component.reloadDetails, 'emit');
  };

  it('should ask for review', async () => {
    const api = fakeApiV2({
      id: API_ID,
      workflowState: 'DRAFT',
    });
    createComponent(api);

    const button = await loader.getHarness(MatButtonHarness.with({ text: 'Ask for a review' }));
    await button.click();

    const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#reviewApiDialog' }));
    const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Ask for review' }));
    await confirmDialogSwitchButton.click();

    httpTestingController
      .expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/reviews/_ask`,
      })
      .flush({});

    expect(component.reloadDetails.emit).toHaveBeenCalled();
  });

  it('should start the api', async () => {
    const api = fakeApiV2({
      id: API_ID,
      state: 'STOPPED',
    });
    createComponent(api);

    const button = await loader.getHarness(MatButtonHarness.with({ text: 'Start the API' }));
    await button.click();

    const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#lifecycleDialog' }));
    const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Start' }));
    await confirmDialogSwitchButton.click();

    httpTestingController
      .expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/_start`,
      })
      .flush({});

    expect(component.reloadDetails.emit).toHaveBeenCalled();
  });

  it('should stop the api', async () => {
    const api = fakeApiV2({
      id: API_ID,
      state: 'STARTED',
    });
    createComponent(api);

    const button = await loader.getHarness(MatButtonHarness.with({ text: 'Stop the API' }));
    await button.click();

    const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#lifecycleDialog' }));
    const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Stop' }));
    await confirmDialogSwitchButton.click();

    httpTestingController
      .expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/_stop`,
      })
      .flush({});

    expect(component.reloadDetails.emit).toHaveBeenCalled();
  });

  it('should publish the api', async () => {
    const api = fakeApiV2({
      id: API_ID,
      lifecycleState: 'CREATED',
    });
    createComponent(api);

    const button = await loader.getHarness(MatButtonHarness.with({ text: 'Publish the API' }));
    await button.click();

    const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#apiLifecycleDialog' }));
    const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Publish' }));
    await confirmDialogSwitchButton.click();

    expectApiGetRequest(api);
    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
    expect(req.request.body.lifecycleState).toEqual('PUBLISHED');
    req.flush({});

    expect(component.reloadDetails.emit).toHaveBeenCalled();
  });

  it('should not display the upgrade banner', async () => {
    const api = fakeApiV2({
      id: API_ID,
      lifecycleState: 'CREATED',
    });

    createComponent(api);

    const upgradeButton = fixture.debugElement.query(
      (elem) => elem.name === 'button' && elem.nativeElement.textContent === 'Request upgrade',
    );
    expect(upgradeButton).toBeNull();
  });

  it('should display the upgrade banner', async () => {
    const api = fakeApiV4({
      id: API_ID,
      lifecycleState: 'CREATED',
    });

    createComponent(api);

    const licenseService = fixture.debugElement.injector.get(GioLicenseService);
    spyOn(licenseService, 'isMissingFeature$').and.returnValue(of(true));
    fixture.detectChanges();

    const upgradeButton = fixture.debugElement.query(
      (elem) => elem.name === 'button' && elem.nativeElement.textContent === 'Request upgrade',
    );
    expectLicenseGetRequest();
    expect(upgradeButton).not.toBeNull();
  });

  it('should make public the api', async () => {
    const api = fakeApiV2({
      id: API_ID,
      visibility: 'PRIVATE',
    });
    createComponent(api);

    const button = await loader.getHarness(MatButtonHarness.with({ text: 'Make Public' }));
    await button.click();

    const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#apiLifecycleDialog' }));
    const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Make Public' }));
    await confirmDialogSwitchButton.click();

    expectApiGetRequest(api);
    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
    expect(req.request.body.visibility).toEqual('PUBLIC');
    req.flush({});

    expect(component.reloadDetails.emit).toHaveBeenCalled();
  });

  it('should delete the api', async () => {
    const api = fakeApiV2({
      id: API_ID,
      lifecycleState: 'CREATED',
      visibility: 'PRIVATE',
      state: 'STOPPED',
    });
    createComponent(api);

    const button = await loader.getHarness(MatButtonHarness.with({ text: 'Delete' }));
    await button.click();

    const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
    await confirmDialog.confirm();

    httpTestingController.expectOne({ method: 'DELETE', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` }).flush({});
    expect(fakeAjsState.go).toHaveBeenCalledWith('management.apis-list');

    expect(component.reloadDetails.emit).not.toHaveBeenCalled();
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectLicenseGetRequest() {
    httpTestingController.expectOne({ url: LICENSE_CONFIGURATION_TESTING.resourceURL, method: 'GET' }).flush({ features: [], packs: [] });
  }
});
