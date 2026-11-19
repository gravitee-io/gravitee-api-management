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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import {
  ApplicationsPagination,
  DEFAULT_APPLICATIONS_PAGE_SIZE,
  SubscribeToApiChooseApplicationComponent,
} from './subscribe-to-api-choose-application.component';
import { fakeApplication } from '../../../../entities/application/application.fixture';

describe('SubscribeToApiChooseApplicationComponent', () => {
  let component: SubscribeToApiChooseApplicationComponent;
  let fixture: ComponentFixture<SubscribeToApiChooseApplicationComponent>;

  const DEFAULT_PAGINATION: ApplicationsPagination = { currentPage: 1, totalApplications: 0, pageSize: DEFAULT_APPLICATIONS_PAGE_SIZE };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscribeToApiChooseApplicationComponent, NoopAnimationsModule],
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

    component.pageSizeChange.emit(15);

    expect(emittedSize).toBe(15);
  });

  it('should emit searchChange when searching applications', () => {
    let emittedQuery: string | undefined;
    component.searchChange.subscribe(query => (emittedQuery = query));

    component.searchChange.emit('Payments');

    expect(emittedQuery).toBe('Payments');
  });

  it('should emit createApplication when the toolbar create button is clicked', () => {
    let created = false;
    component.canCreateApplication = true;
    component.createApplication.subscribe(() => (created = true));
    fixture.detectChanges();

    const createButton: HTMLButtonElement | null = fixture.nativeElement.querySelector(
      '[data-testid="subscribe-create-application"]',
    );
    expect(createButton).toBeTruthy();
    createButton!.click();

    expect(created).toBe(true);
  });

  it('should show list view and create button without cards toggle', () => {
    const app = fakeApplication({ id: 'app-1', name: 'App One' });
    component.applications = [{ ...app, disabled: false }];
    component.canCreateApplication = true;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.applications-table__table')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="subscribe-create-application"]')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Create application & subscribe');
    expect(fixture.nativeElement.textContent).not.toContain('Cards');
    expect(fixture.nativeElement.querySelector('.applications')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-button-toggle-group')).toBeNull();
  });
});
