/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ApiDetailsComponent } from './api-details.component';
import { fakeApi } from '../../entities/api/api.fixtures';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('ApiDetailsComponent', () => {
  let component: ApiDetailsComponent;
  let fixture: ComponentFixture<ApiDetailsComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiDetailsComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiDetailsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    component = fixture.componentInstance;
    component.apiId = 'api-id';
    fixture.detectChanges();
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/api-id`).flush(fakeApi({ id: 'api-id' }));
  });

  afterEach(() => {
    httpTestingController.match('assets/images/lightbulb_24px.svg');
    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/api-id/pages?homepage=true&page=1&size=-1`);
    expect(req.request.method).toEqual('GET');
    httpTestingController.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
