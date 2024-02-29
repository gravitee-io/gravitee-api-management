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
import { ActivatedRoute } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ApiHistoryV4DeploymentInfoComponent } from './api-history-v4-deployment-info.component';

import { fakeEvent } from '../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiHistoryV4Module } from '../api-history-v4.module';

describe('DeploymentInfoComponent', () => {
  const API_ID = 'an-api-id';
  const API_VERSION_ID = 'event-version-id';

  let component: ApiHistoryV4DeploymentInfoComponent;
  let fixture: ComponentFixture<ApiHistoryV4DeploymentInfoComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiHistoryV4Module, NoopAnimationsModule, GioTestingModule],
      declarations: [ApiHistoryV4DeploymentInfoComponent],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID, apiVersionId: API_VERSION_ID } } } }],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiHistoryV4DeploymentInfoComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    expectApiEventRequest();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create component with proper page title', async () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(getPageTitle()).toEqual('Version 1');
    expect(getSubtitle()).toEqual(' This is the JSON API definition of version 1 ');
    expect(getDate()).toEqual('Date: Jan 1, 2021, 12:00:00 AM');
    expect(getUser()).toEqual('User: John Doe');
    expect(getLabel()).toEqual('Label: sample-label');
  });

  function expectApiEventRequest(response = fakeEvent()) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/events/${API_VERSION_ID}`);
    req.flush(response);
  }

  const getPageTitle = () => {
    return fixture.nativeElement.querySelector('h3').textContent;
  };
  const getSubtitle = () => {
    return fixture.nativeElement.querySelector('#subtitle').textContent;
  };
  const getDate = () => {
    return fixture.nativeElement.querySelector('#date').textContent;
  };
  const getUser = () => {
    return fixture.nativeElement.querySelector('#user').textContent;
  };
  const getLabel = () => {
    return fixture.nativeElement.querySelector('#label').textContent;
  };
});
