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

import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiV4 } from '../../../../../entities/management-api-v2';
import { ApiEndpointComponent } from './api-endpoint.component';
import { ApiEndpointModule } from './api-endpoint.module';

@Component({
  template: `<api-endpoint #apiEndpoint [api]="api"></api-endpoint>`,
})
class TestComponent {
  @ViewChild('apiEndpoint') apiEndpoint: ApiEndpointComponent;
  api?: ApiV4;
}

describe('ApiEndpointComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  const initComponent = async (api: ApiV4) => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiEndpointModule, MatIconTestingModule],
    });

    fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.api = api;

    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('add endpoints in a group', () => {});
});
