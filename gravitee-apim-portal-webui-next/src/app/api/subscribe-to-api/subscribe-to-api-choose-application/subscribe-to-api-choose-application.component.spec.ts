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
import { signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import {
  ApplicationsPagination,
  DEFAULT_APPLICATIONS_PAGE_SIZE,
  SubscribeToApiChooseApplicationComponent,
} from './subscribe-to-api-choose-application.component';
import { fakeApplication } from '../../../../entities/application/application.fixture';
import { ObservabilityBreakpointService } from '../../../../services/observability-breakpoint.service';

class MockObservabilityBreakpointService {
  static mockIsNarrow: WritableSignal<boolean> = signal(false);
  static mockIsMobile: WritableSignal<boolean> = signal(false);

  get isNarrow() {
    return MockObservabilityBreakpointService.mockIsNarrow.asReadonly();
  }

  get isMobile() {
    return MockObservabilityBreakpointService.mockIsMobile.asReadonly();
  }
}

describe('SubscribeToApiChooseApplicationComponent', () => {
  let component: SubscribeToApiChooseApplicationComponent;
  let fixture: ComponentFixture<SubscribeToApiChooseApplicationComponent>;

  const DEFAULT_PAGINATION: ApplicationsPagination = { currentPage: 1, totalApplications: 0, pageSize: DEFAULT_APPLICATIONS_PAGE_SIZE };

  beforeEach(async () => {
    MockObservabilityBreakpointService.mockIsNarrow = signal(false);
    MockObservabilityBreakpointService.mockIsMobile = signal(false);

    await TestBed.configureTestingModule({
      imports: [SubscribeToApiChooseApplicationComponent, NoopAnimationsModule],
      providers: [{ provide: ObservabilityBreakpointService, useClass: MockObservabilityBreakpointService }],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscribeToApiChooseApplicationComponent);
    component = fixture.componentInstance;
    component.pagination = DEFAULT_PAGINATION;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit selectApplication when an application is selected', () => {
    const app = fakeApplication({ id: 'app-1', name: 'App One' });
    component.applications = [{ ...app, disabled: false }];
    fixture.detectChanges();

    let emitted: unknown;
    component.selectApplication.subscribe(a => (emitted = a));

    component.selectApplication.emit(app);

    expect(emitted).toEqual(app);
  });

  it('should emit pageChange when page changes', () => {
    let emittedPage: number | undefined;
    component.pageChange.subscribe(p => (emittedPage = p));

    component.pageChange.emit(2);

    expect(emittedPage).toBe(2);
  });

  it('should emit pageSizeChange when page size changes', () => {
    let emittedSize: number | undefined;
    component.pageSizeChange.subscribe(s => (emittedSize = s));

    component.pageSizeChange.emit(12);

    expect(emittedSize).toBe(12);
  });
});
