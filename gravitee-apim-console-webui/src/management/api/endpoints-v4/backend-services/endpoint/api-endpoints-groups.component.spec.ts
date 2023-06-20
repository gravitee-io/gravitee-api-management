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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiEndpointsGroupsComponent } from './api-endpoints-groups.component';

import { GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../../../entities/management-api-v2';
import { ApiProxyEndpointModule } from '../../../proxy/endpoints/api-proxy-endpoints.module';

describe('ApiEndpointsGroupsComponent', () => {
  const API_ID = 'apiId';
  let fixture: ComponentFixture<ApiEndpointsGroupsComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  const initComponent = (api: ApiV4): void => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyEndpointModule, MatIconTestingModule],
    });

    fixture = TestBed.createComponent(ApiEndpointsGroupsComponent);
    fixture.componentInstance.api = api;

    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  it('should init the component with proxy API V4', async () => {
    initComponent(fakeApiV4({ id: API_ID }));

    expect(fixture.componentInstance).toBeTruthy();
  });
});
