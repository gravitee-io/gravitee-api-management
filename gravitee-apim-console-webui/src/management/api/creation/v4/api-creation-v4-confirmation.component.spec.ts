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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatCardHarness } from '@angular/material/card/testing';

import { ApiCreationV4ConfirmationComponent } from './api-creation-v4-confirmation.component';
import { ApiCreationV4Module } from './api-creation-v4.module';

import { fakeApiEntity } from '../../../../entities/api-v4';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';

describe('ApiCreationV4ConfirmationComponent', () => {
  const API_ID = 'api-id';

  const fakeAjsState = {
    go: jest.fn(),
  };

  let fixture: ComponentFixture<ApiCreationV4ConfirmationComponent>;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioPermissionModule, GioHttpTestingModule, ApiCreationV4Module, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeAjsState },
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiCreationV4ConfirmationComponent);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  };

  beforeEach(async () => await init());

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('API created', () => {
    it('should display API name', async () => {
      const createdApi = fakeApiEntity({ id: API_ID, name: 'my brand new API' });

      httpTestingController
        .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/v4/apis/${createdApi.id}`, method: 'GET' })
        .flush(createdApi);

      const mainCard = await rootLoader.getHarness(MatCardHarness);
      const innerText = await mainCard.getText();
      expect(innerText.startsWith('my brand new API' + ' has been created')).toBeTruthy();
    });
  });
});
