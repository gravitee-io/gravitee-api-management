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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { provideRouter } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { WebhookLogsListComponent } from './webhook-logs-list.component';
import { WebhookLogsListHarness } from './webhook-logs-list.component.harness';

import { fakeConnectionLog } from '../../../../../../entities/management-api-v2';

describe('WebhookLogsListComponent', () => {
  let component: WebhookLogsListComponent;
  let fixture: ComponentFixture<WebhookLogsListComponent>;
  let componentHarness: WebhookLogsListHarness;

  const mockLogs: any[] = [
    {
      ...fakeConnectionLog(),
      requestId: 'req-1',
      timestamp: '2025-06-15T12:00:00.000Z',
      status: 200,
      callbackUrl: 'https://test.com/webhook',
      application: { id: 'app-1', name: 'Test App', type: 'SIMPLE' as any, apiKeyMode: 'UNSPECIFIED' as any },
      duration: '2.8 s',
    },
    {
      ...fakeConnectionLog(),
      requestId: 'req-2',
      timestamp: '2025-06-16T13:15:00.000Z',
      status: 500,
      callbackUrl: 'https://test2.com/webhook',
      application: { id: 'app-2', name: 'Test App 2', type: 'SIMPLE' as any, apiKeyMode: 'UNSPECIFIED' as any },
      duration: '980 ms',
    },
  ];

  const mockPagination = {
    page: 1,
    perPage: 10,
    pageCount: 1,
    pageItemsCount: 2,
    totalCount: 2,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebhookLogsListComponent, NoopAnimationsModule, MatIconTestingModule],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(WebhookLogsListComponent);
    component = fixture.componentInstance;

    // Set required inputs BEFORE any detection or harness creation
    fixture.componentRef.setInput('logs', mockLogs);
    fixture.componentRef.setInput('pagination', mockPagination);

    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, WebhookLogsListHarness);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display correct columns', () => {
    const columns = component.displayedColumns();
    expect(columns).toEqual(['timestamp', 'status', 'callbackUrl', 'application', 'duration', 'actions']);
  });

  it('should render table with logs', async () => {
    fixture.detectChanges();

    const rowCount = await componentHarness.countRows();
    expect(rowCount).toBe(2);
  });

  it('should emit logDetailsClicked when eye icon is clicked', async () => {
    fixture.detectChanges();
    const logDetailsClickedSpy = jest.fn();
    component.logDetailsClicked.subscribe(logDetailsClickedSpy);

    await componentHarness.clickDetailsButton(0);

    expect(logDetailsClickedSpy).toHaveBeenCalledWith(mockLogs[0]);
  });

  it('should emit paginationUpdated when pagination changes', () => {
    fixture.detectChanges();
    const paginationUpdatedSpy = jest.fn();
    component.paginationUpdated.subscribe(paginationUpdatedSpy);

    component.onFiltersChanged({
      searchTerm: '',
      pagination: { index: 2, size: 25 },
    });

    expect(paginationUpdatedSpy).toHaveBeenCalledWith({ index: 2, size: 25 });
  });

  it('should display "No data to display" when logs array is empty', async () => {
    fixture.componentRef.setInput('logs', []);
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const emptyMessage = compiled.querySelector('.table__empty-state');
    expect(emptyMessage).toBeTruthy();
    expect(emptyMessage.textContent).toContain('No data to display');
  });

  it('should format status with correct badge class', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const statusBadges = compiled.querySelectorAll('.gio-badge-success, .gio-badge-error');
    expect(statusBadges.length).toBeGreaterThan(0);
  });
});
