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
import { provideRouter } from '@angular/router';

import ApplicationComponent from './application.component';
import { fakeApplication } from '../../../entities/application/application.fixture';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { applicationListBreadcrumb } from '../applications/application-breadcrumbs';

describe('ApplicationComponent', () => {
  let fixture: ComponentFixture<ApplicationComponent>;
  let breadcrumbService: BreadcrumbService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationComponent);
    breadcrumbService = TestBed.inject(BreadcrumbService);
    fixture.componentRef.setInput('application', fakeApplication({ id: 'app-1', name: 'My App' }));
    fixture.detectChanges();
  });

  it('should set breadcrumbs for application details', () => {
    expect(breadcrumbService.breadcrumbs()).toEqual([applicationListBreadcrumb(true), { id: 'application-app-1', label: 'My App' }]);
  });
});
