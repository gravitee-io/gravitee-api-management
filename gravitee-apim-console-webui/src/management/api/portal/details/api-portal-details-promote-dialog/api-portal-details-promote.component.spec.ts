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
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioFormFilePickerInputHarness } from '@gravitee/ui-particles-angular';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatProgressBarHarness } from '@angular/material/progress-bar/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { Category } from '@uirouter/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { UIRouterState, UIRouterStateParams, CurrentUserService } from '../../../../../ajs-upgraded-providers';
import { Api } from '../../../../../entities/api';
import { fakeApi } from '../../../../../entities/api/Api.fixture';
import { User } from '../../../../../entities/user';
import { GioHttpTestingModule, CONSTANTS_TESTING } from '../../../../../shared/testing';
import { ApiPortalDetailsComponent } from '../api-portal-details.component';
import { ApiPortalDetailsModule } from '../api-portal-details.module';
import { fakePromotion, fakePromotionTarget, Promotion, PromotionTarget } from '../../../../../entities/promotion';

describe('ApiPortalDetailsComponent', () => {
  const API_ID = 'apiId';
  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u', 'api-definition-d', 'api-definition-c'];
  const fakeAjsState = {
    go: jest.fn().mockReturnValue({}),
  };

  let fixture: ComponentFixture<ApiPortalDetailsComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiPortalDetailsModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeAjsState },
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: CurrentUserService, useValue: { currentUser } },
        {
          provide: 'Constants',
          useValue: {
            ...CONSTANTS_TESTING,
          },
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        // This traps focus checks and so avoid warnings when dealing with
        isFocusable: () => true,
        isTabbable: () => true,
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiPortalDetailsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    GioFormFilePickerInputHarness.forceImageOnload();
  });

  afterEach(() => {
    httpTestingController.verify({ ignoreCancelled: true });
  });

  it('should display meet cockpit', async () => {
    const api = fakeApi({
      id: API_ID,
    });
    expectApiGetRequest(api);
    expectCategoriesGetRequest();

    // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
    await waitImageCheck();

    const button = await loader.getHarness(MatButtonHarness.with({ text: /Promote/ }));
    await button.click();

    const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#promoteApiDialog' }));

    // Check loading
    await confirmDialog.getHarness(MatProgressBarHarness);

    // Simulate APIM with no cockpit
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/promotion-targets`, method: 'GET' }).flush(
      {
        message: 'Installation [eef6a6b7-0de3-4536-b6a6-b70de3a536d8] not accepted.',
        parameters: {
          cockpitURL: 'https://cockpit.gravitee.io',
          installationId: 'eef6a6b7-0de3-4536-b6a6-b70de3a536d8',
          status: 'NOT_LINKED',
        },
        technicalCode: 'installation.notAccepted',
        http_status: 400,
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    const dialogNativeElement = fixture.debugElement.parent.nativeElement.querySelector('#promoteApiDialog');

    expect(dialogNativeElement.querySelector('h2').innerHTML).toEqual('Meet Cockpit');
    expect(dialogNativeElement.querySelector('.meet-cockpit__content > p > a').href).toEqual(
      'https://cockpit.gravitee.io/?utm_source=apim&utm_medium=InApp&utm_campaign=api_promotion',
    );

    const okButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Ok' }));
    await okButton.click();
  });

  it('should promote the API', async () => {
    const api = fakeApi({
      id: API_ID,
    });
    expectApiGetRequest(api);
    expectCategoriesGetRequest();

    // Wait image to be loaded (fakeAsync is not working with getBase64 🤷‍♂️)
    await waitImageCheck();

    const button = await loader.getHarness(MatButtonHarness.with({ text: /Promote/ }));
    await button.click();

    const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#promoteApiDialog' }));

    await confirmDialog.getHarness(MatProgressBarHarness);

    expectListPromotionPostRequest(API_ID, [fakePromotion()]);
    expectPromotionTargetsGetRequest([fakePromotionTarget()]);

    const okButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Promote' }));
    await okButton.click();
    expectPromotePostRequest(API_ID);
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectCategoriesGetRequest(categories: Category[] = []) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/configuration/categories`, method: 'GET' }).flush(categories);
    fixture.detectChanges();
  }

  function expectPromotionTargetsGetRequest(promotionTarget: PromotionTarget[] = []) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/promotion-targets`, method: 'GET' }).flush(promotionTarget);
    fixture.detectChanges();
  }

  function expectListPromotionPostRequest(apiId: string, promotions: Promotion[] = []) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/promotions/_search?apiId=${apiId}&statuses=CREATED&statuses=TO_BE_VALIDATED`,
        method: 'POST',
      })
      .flush(promotions);
    fixture.detectChanges();
  }

  function expectPromotePostRequest(apiId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/_promote`,
        method: 'POST',
      })
      .flush({});
  }
});

const waitImageCheck = () => new Promise((resolve) => setTimeout(resolve, 100));
