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
import { ActivatedRoute } from '@angular/router';

import { ApiCreationV4ConfirmationComponent } from './api-creation-v4-confirmation.component';
import { ApiCreationV4Module } from './api-creation-v4.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { Api, fakeApiV4 } from '../../../entities/management-api-v2';

describe('ApiCreationV4ConfirmationComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiCreationV4ConfirmationComponent>;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiCreationV4Module, MatIconTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: {
                apiId: API_ID,
              },
            },
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
    it('should display API name and created', async () => {
      const api = fakeApiV4({ id: API_ID, name: 'my brand new API', lifecycleState: 'CREATED' });
      expectOneGet(api);

      const mainCard = await rootLoader.getHarness(MatCardHarness);
      const innerText = await mainCard.getText();
      expect(innerText.startsWith('my brand new API' + ' has been created')).toBeTruthy();
    });
  });

  describe('API deployed', () => {
    it('should display API name and deployed', async () => {
      const api = fakeApiV4({ id: API_ID, name: 'my brand new API', lifecycleState: 'PUBLISHED' });
      expectOneGet(api);

      const mainCard = await rootLoader.getHarness(MatCardHarness);
      const innerText = await mainCard.getText();
      expect(innerText.startsWith('my brand new API' + ' has been deployed')).toBeTruthy();
    });
  });

  function expectOneGet(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
  }
});
