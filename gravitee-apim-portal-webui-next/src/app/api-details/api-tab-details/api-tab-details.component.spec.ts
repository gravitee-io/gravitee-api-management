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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ApiTabDetailsComponent } from './api-tab-details.component';
import { fakeApi } from '../../../entities/api/api.fixtures';
import { AppTestingModule } from '../../../testing/app-testing.module';

describe('ApiTabDetailsComponent', () => {
  let component: ApiTabDetailsComponent;
  let fixture: ComponentFixture<ApiTabDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiTabDetailsComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { parent: { data: of(fakeApi({ id: 'api-id' })) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiTabDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
