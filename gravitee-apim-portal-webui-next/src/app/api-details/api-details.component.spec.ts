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
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ApiDetailsComponent } from './api-details.component';
import { fakeApi } from '../../entities/api/api.fixtures';
import { AppTestingModule } from '../../testing/app-testing.module';

describe('ApiDetailsComponent', () => {
  let component: ApiDetailsComponent;
  let fixture: ComponentFixture<ApiDetailsComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiDetailsComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { data: of(fakeApi({ id: 'api-id' })) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiDetailsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    component = fixture.componentInstance;
    component.api = fakeApi();
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.match('assets/images/lightbulb_24px.svg');
    httpTestingController.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
