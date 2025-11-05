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
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import { of } from 'rxjs';

import { WebhookLogsComponent } from './webhook-logs.component';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../../entities/management-api-v2';

describe('WebhookLogsComponent', () => {
  let component: WebhookLogsComponent;
  let fixture: ComponentFixture<WebhookLogsComponent>;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const API_ID = 'test-api-id';
  const queryParams = { page: 1, perPage: 10 };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        WebhookLogsComponent,
        NoopAnimationsModule,
        MatIconTestingModule,
        GioTestingModule,
        OwlDateTimeModule,
        OwlMomentDateTimeModule,
      ],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID }, queryParams: queryParams } } },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');

    fixture = TestBed.createComponent(WebhookLogsComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      fixture.detectChanges();
      expectApiCall();
      expect(component).toBeTruthy();
    });

    it('should initialize with dummy webhook logs data', () => {
      fixture.detectChanges();
      expectApiCall();

      expect(component.applicationsOptions).toHaveLength(5);
      expect(component.callbackUrlOptions.length).toBeGreaterThan(0);
      expect(component.loading).toBe(false);
    });

    it('should show logs when reporting is enabled', () => {
      fixture.detectChanges();
      expectApiCall();

      component.isReportingDisabled$.subscribe((disabled) => {
        expect(disabled).toBe(false);
      });
    });

    it('should show banner when reporting is disabled', () => {
      fixture.detectChanges();
      expectApiCallWithLogDisabled();

      component.isReportingDisabled$.subscribe((disabled) => {
        expect(disabled).toBe(true);
      });
    });
  });

  describe('Filtering', () => {
    beforeEach(() => {
      fixture.detectChanges();
      expectApiCall();
    });

    it('should filter logs by search term', () => {
      const filters = { searchTerm: 'acme warehouse' };
      component.onFiltersChanged(filters);

      expect(routerNavigateSpy).toHaveBeenCalledWith(
        ['.'],
        expect.objectContaining({
          queryParams: expect.objectContaining({ search: 'acme warehouse' }),
        }),
      );
    });

    it('should filter logs by status', () => {
      const filters = { status: ['200'] };
      component.onFiltersChanged(filters);

      expect(routerNavigateSpy).toHaveBeenCalledWith(
        ['.'],
        expect.objectContaining({
          queryParams: expect.objectContaining({ statuses: '200' }),
        }),
      );
    });

    it('should filter logs by application', () => {
      const filters = { application: ['app-1'] };
      component.onFiltersChanged(filters);

      expect(routerNavigateSpy).toHaveBeenCalledWith(
        ['.'],
        expect.objectContaining({
          queryParams: expect.objectContaining({ applicationIds: 'app-1' }),
        }),
      );
    });

    it('should filter logs by timeframe', () => {
      const filters = { timeframe: '-1h' };
      component.onFiltersChanged(filters);

      expect(routerNavigateSpy).toHaveBeenCalledWith(
        ['.'],
        expect.objectContaining({
          queryParams: expect.objectContaining({ timeframe: '-1h' }),
        }),
      );
    });

    it('should update URL query params when filters change', () => {
      const filters = { searchTerm: 'test', status: ['200'], application: ['app-1'], timeframe: '-1h' };
      component.onFiltersChanged(filters);

      expect(routerNavigateSpy).toHaveBeenCalledWith(
        ['.'],
        expect.objectContaining({
          relativeTo: expect.anything(),
          queryParams: expect.objectContaining({
            search: 'test',
            statuses: '200',
            applicationIds: 'app-1',
            timeframe: '-1h',
            page: 1,
          }),
          queryParamsHandling: 'merge',
        }),
      );
    });
  });

  describe('Pagination', () => {
    beforeEach(() => {
      fixture.detectChanges();
      expectApiCall();
    });

    it('should update pagination', () => {
      component.paginationUpdated({ index: 2, size: 25 });

      expect(routerNavigateSpy).toHaveBeenCalledWith(
        ['.'],
        expect.objectContaining({
          queryParams: { page: 2, perPage: 25 },
          queryParamsHandling: 'merge',
        }),
      );
    });
  });

  describe('Details Drawer', () => {
    beforeEach(() => {
      fixture.detectChanges();
      expectApiCall();
    });

    it('should open details drawer when details button is clicked', () => {
      const mockLog = { requestId: 'test-req', callbackUrl: 'https://test.com' } as any;
      component.onLogDetailsClicked(mockLog);

      expect(component.showDetailsDrawer).toBe(true);
      expect(component.selectedLogForDetails).toBe(mockLog);
    });

    it('should close details drawer when close button is clicked', () => {
      const mockLog = { requestId: 'test-req', callbackUrl: 'https://test.com' } as any;
      component.showDetailsDrawer = true;
      component.selectedLogForDetails = mockLog;

      component.closeDetailsDrawer();

      expect(component.showDetailsDrawer).toBe(false);
      expect(component.selectedLogForDetails).toBeNull();
    });
  });

  describe('Settings Dialog', () => {
    beforeEach(() => {
      fixture.detectChanges();
      expectApiCall();
    });

    it('should open settings dialog', () => {
      const openSpy = jest.spyOn(component['dialog'], 'open').mockReturnValue({
        afterClosed: () => of({ saved: false }),
      } as any);

      component.openSettingsDialog();

      expect(openSpy).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({ width: '750px' }));
    });
  });

  describe('Timeframe Parsing', () => {
    beforeEach(() => {
      fixture.detectChanges();
      expectApiCall();
    });

    it('should parse minutes correctly', () => {
      const result = component['parseTimeframeToMs']('-5m');
      expect(result).toBe(5 * 60 * 1000);
    });

    it('should parse hours correctly', () => {
      const result = component['parseTimeframeToMs']('-3h');
      expect(result).toBe(3 * 60 * 60 * 1000);
    });

    it('should parse days correctly', () => {
      const result = component['parseTimeframeToMs']('-7d');
      expect(result).toBe(7 * 24 * 60 * 60 * 1000);
    });

    it('should return null for invalid format', () => {
      const result = component['parseTimeframeToMs']('invalid');
      expect(result).toBeNull();
    });
  });

  function expectApiCall(modifier?: Partial<ApiV4>) {
    let api = fakeApiV4({ id: API_ID, analytics: { enabled: true, logging: { mode: { entrypoint: true } } } });

    if (modifier) {
      api = { ...api, ...modifier };
    }

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
        method: 'GET',
      })
      .flush(api);
  }

  function expectApiCallWithLogDisabled() {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
        method: 'GET',
      })
      .flush(fakeApiV4({ id: API_ID, analytics: { enabled: true, logging: {} } }));
  }
});
