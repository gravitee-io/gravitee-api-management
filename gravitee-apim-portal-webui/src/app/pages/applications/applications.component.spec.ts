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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateTestingModule } from '../../test/translate-testing-module';

import { ApplicationsComponent } from './applications.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Application } from 'projects/portal-webclient-sdk/src/lib';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

describe('ApplicationsComponent', () => {
  const mockActivatedRouteQueryParamMap$ = new BehaviorSubject(new Map());

  let component: ApplicationsComponent;
  let fixture: ComponentFixture<ApplicationsComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ApplicationsComponent],
      imports: [TranslateTestingModule, HttpClientTestingModule, RouterTestingModule],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: mockActivatedRouteQueryParamMap$,
          },
        },
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ApplicationsComponent);
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

  it('should get applications', () => {
    const fakeApplication1 = {
      id: 'application1',
      name: 'application1',
      description: 'application1 description',
    };
    expectApplicationsGetRequest([fakeApplication1]);
    expectCurrentUserPermissionsGetRequest('application1');

    expect(component.nbApplications).toEqual(1);
    expect(component.applications).toEqual([{ item: fakeApplication1, metrics: expect.any(Promise) }]);
  });

  it('should displays page=2 with size=24', () => {
    const fakeApplication1 = {
      id: 'application1',
      name: 'application1',
      description: 'application1 description',
    };
    expectApplicationsGetRequest([fakeApplication1]);
    expectCurrentUserPermissionsGetRequest('application1');

    expect(component.paginationData).toEqual({ current_page: 1, first: 1, last: 42, size: 12, total: 1, total_pages: 1 });
    expect(component.paginationSize).toEqual(12);
    expect(component.paginationPageSizes).toEqual([6, 12, 24, 48, 96]);

    mockActivatedRouteQueryParamMap$.next(
      new Map([
        ['page', '2'],
        ['size', '24'],
      ]),
    );

    expectApplicationsGetRequest([fakeApplication1], { page: 2, size: 24 });
    expectCurrentUserPermissionsGetRequest('application1');

    expect(component.paginationData).toEqual({ current_page: 2, first: 1, last: 42, size: 24, total: 1, total_pages: 1 });
    expect(component.paginationSize).toEqual(24);
  });

  function expectApplicationsGetRequest(
    applications: Application[] = [],
    pagination: { page: number; size: number } = { page: 1, size: 12 },
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

  function expectCurrentUserPermissionsGetRequest(applicationId: string) {
    const req = httpTestingController.expectOne({
      url: `http://localhost:8083/portal/environments/DEFAULT/permissions?applicationId=${applicationId}`,
      method: 'GET',
    });
    req.flush({});
    fixture.detectChanges();
  }
});
