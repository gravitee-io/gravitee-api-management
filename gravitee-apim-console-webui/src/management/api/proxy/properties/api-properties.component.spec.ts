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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiPropertiesComponent } from './api-properties.component';
import { ApiPropertiesModule } from './api-properties.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { User } from '../../../../entities/user';
import { CurrentUserService, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioUiRouterTestingModule } from '../../../../shared/testing/gio-uirouter-testing-module';
import { Api, fakeApiV4 } from '../../../../entities/management-api-v2/api';

describe('ApiPropertiesComponent', () => {
  const API_ID = 'apiId';
  let fixture: ComponentFixture<ApiPropertiesComponent>;
  let component: ApiPropertiesComponent;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  const currentUser = new User();
  currentUser.userPermissions = ['api-plan-r', 'api-plan-u'];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiPropertiesModule, GioUiRouterTestingModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        {
          provide: CurrentUserService,
          useValue: { currentUser },
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiPropertiesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should work', async () => {
    expect(component).toBeTruthy();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '[aria-label="API Properties"]' }));

    const loadingRow = await table.getCellTextByIndex();
    expect(loadingRow).toEqual([['Loading...']]);

    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [
          { key: 'key1', value: 'value1', encrypted: false },
          { key: 'key2', value: 'value2', encrypted: true },
        ],
      }),
    );

    const rows = await table.getCellTextByIndex();
    expect(rows).toEqual([
      ['key1', 'value1', '', ''],
      ['key2', 'value2', '', ''],
    ]);
  });

  function expectGetApi(api: Api) {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
    });
    req.flush(api);
  }
});
