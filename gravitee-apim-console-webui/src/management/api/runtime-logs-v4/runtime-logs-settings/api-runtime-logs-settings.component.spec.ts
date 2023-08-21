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

import { ApiRuntimeLogsSettingsModule } from './api-runtime-logs-settings.module';
import { ApiRuntimeLogsSettingsComponent } from './api-runtime-logs-settings.component';

import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { User } from '../../../../entities/user';
import { GioHttpTestingModule } from '../../../../shared/testing';

describe('ApiRuntimeLogsSettingsComponent', () => {
  const API_ID = 'apiId';
  let fixture: ComponentFixture<ApiRuntimeLogsSettingsComponent>;
  let httpTestingController: HttpTestingController;

  const initComponent = () => {
    const currentUser = new User();
    currentUser.userPermissions = ['api-definition-u'];

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiRuntimeLogsSettingsModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: UIRouterState, useValue: { go: jest.fn() } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    });

    fixture = TestBed.createComponent(ApiRuntimeLogsSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create component', () => {
    initComponent();
    expect(fixture).toBeTruthy();
  });
});
