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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatButtonHarness } from '@angular/material/button/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

import { ApiGeneralInfoPromoteDialogComponent } from './api-general-info-promote-dialog.component';

import { CurrentUserService, UIRouterState } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiGeneralInfoModule } from '../api-general-info.module';
import { fakePromotion, fakePromotionTarget, Promotion, PromotionTarget } from '../../../../../entities/promotion';
import { fakeApiV2 } from '../../../../../entities/management-api-v2';

describe('ApiPortalDetailsPromoteDialogComponent', () => {
  const API_ID = 'apiId';
  const api = fakeApiV2({
    id: API_ID,
  });
  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u', 'api-definition-d', 'api-definition-c'];
  const fakeAjsState = {
    go: jest.fn().mockReturnValue({}),
  };

  let fixture: ComponentFixture<ApiGeneralInfoPromoteDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiGeneralInfoModule, MatIconTestingModule, MatDialogModule],
      providers: [
        { provide: UIRouterState, useValue: fakeAjsState },
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: { api } },
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
    fixture = TestBed.createComponent(ApiGeneralInfoPromoteDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    GioFormFilePickerInputHarness.forceImageOnload();
  });

  afterEach(() => {
    httpTestingController.verify({ ignoreCancelled: true });
  });

  it('should display meet cockpit', async () => {
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

    const dialogNativeElement = fixture.debugElement.parent.nativeElement;

    expect(dialogNativeElement.querySelector('h2').innerHTML).toEqual('Meet Cockpit');
    expect(dialogNativeElement.querySelector('.meet-cockpit__content > p > a').href).toEqual(
      'https://cockpit.gravitee.io/?utm_source=apim&utm_medium=InApp&utm_campaign=api_promotion',
    );
  });

  it('should promote the API', async () => {
    expectListPromotionPostRequest(API_ID, [fakePromotion()]);
    expectPromotionTargetsGetRequest([fakePromotionTarget()]);

    const okButton = await loader.getHarness(MatButtonHarness.with({ text: 'Promote' }));
    await okButton.click();
    expectPromotePostRequest(API_ID);
  });

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
