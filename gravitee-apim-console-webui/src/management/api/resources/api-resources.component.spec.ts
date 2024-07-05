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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute } from '@angular/router';
import { HttpTestingController } from '@angular/common/http/testing';

import { ApiResourcesHarness } from './api-resources.harness';
import { ApiResourcesComponent } from './api-resources.component';

import { ApiV2, ApiV4, fakeApiV4 } from '../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ResourceListItem } from '../../../entities/resource/resourceListItem';

describe('ApiResourcesComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiResourcesComponent>;
  let componentHarness: ApiResourcesHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatIconTestingModule, GioTestingModule, NoopAnimationsModule, ApiResourcesComponent],
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
    }).compileComponents();

    fixture = TestBed.createComponent(ApiResourcesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiResourcesHarness);
    fixture.detectChanges();
  });

  it('should display resources table', async () => {
    const table = await componentHarness.getTable();
    expect(await table.getCellTextByIndex()).toStrictEqual([['Loading...']]);

    expectApiGetRequest(
      fakeApiV4({
        id: API_ID,
        resources: [
          { name: 'MyCache', type: 'cache', configuration: '', enabled: true },
          {
            name: 'MyOAuth2',
            type: 'oauth2',
            configuration: '',
            enabled: false,
          },
        ],
      }),
    );
    expectListResourcesRequest([
      { id: 'cache', name: 'Cache', version: '', description: '', icon: '', schema: '' },
      { id: 'oauth2', name: 'OAuth2', version: '', description: '', icon: '', schema: '' },
    ]);

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['MyCache', 'Cache', ''],
      ['MyOAuth2 Disabled', 'OAuth2', ''],
    ]);
  });

  function expectApiGetRequest(api: ApiV2 | ApiV4) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET',
      })
      .flush(api);
  }

  function expectListResourcesRequest(resourceListItems: ResourceListItem[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/resources?expand=schema&expand=icon`,
        method: 'GET',
      })
      .flush(resourceListItems);
  }
});
