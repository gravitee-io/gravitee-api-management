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
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule, HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Application } from 'projects/portal-webclient-sdk/src/lib';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { TranslateTestingModule } from '../../test/translate-testing-module';

import { SubscriptionsComponent } from './subscriptions.component';

describe('SubscriptionsComponent', () => {
  const mockActivatedRouteQueryParamMap$ = new BehaviorSubject(new Map());

  let component: SubscriptionsComponent;
  let fixture: ComponentFixture<SubscriptionsComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [SubscriptionsComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      teardown: { destroyAfterEach: false },
      imports: [TranslateTestingModule, HttpClientTestingModule, RouterTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: mockActivatedRouteQueryParamMap$,
          },
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
  }));

  beforeEach(() => {
    mockActivatedRouteQueryParamMap$.next(new Map());
    fixture = TestBed.createComponent(SubscriptionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create', () => {
    expectApplicationsGetRequest();
    expect(component).toBeTruthy();
  });

  it('should default to page=1 when page query param is not a number', () => {
    expectApplicationsGetRequest();

    mockActivatedRouteQueryParamMap$.next(new Map([['page', 'documentation-page-id']]));

    expectApplicationsGetRequest([], { page: 1, size: 10 });
  });

  function expectApplicationsGetRequest(
    applications: Application[] = [],
    pagination: { page: number; size: number } = { page: 1, size: 10 },
  ) {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `http://localhost:8083/portal/environments/DEFAULT/applications?page=${pagination.page}&size=${pagination.size}`,
    });
    expect(req.request.method).toEqual('GET');
    req.flush({
      data: applications,
      metadata: {
        pagination: {
          total: applications.length,
          size: pagination.size,
          last: 42,
          total_pages: 1,
          current_page: pagination.page,
          first: 1,
        },
      },
    });
    fixture.detectChanges();
  }
});
