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
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';

import { ApiNavigationHeaderComponent } from './api-navigation-header.component';

import { MenuItemHeader, MenuItemHeaderAction } from '../MenuGroupItem';

describe('ApiNavigationHeaderComponent', () => {
  let component: ApiNavigationHeaderComponent;
  let fixture: ComponentFixture<ApiNavigationHeaderComponent>;
  let mockRouter: jest.Mocked<Router>;
  let mockActivatedRoute: jest.Mocked<ActivatedRoute>;

  beforeEach(async () => {
    mockRouter = {
      navigate: jest.fn(),
    } as any;

    mockActivatedRoute = {
      snapshot: {
        queryParams: {},
      },
    } as any;

    await TestBed.configureTestingModule({
      declarations: [ApiNavigationHeaderComponent],
      imports: [MatButtonModule, MatTooltipModule, NoopAnimationsModule],
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiNavigationHeaderComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Component Rendering', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should not render anything when menuItemHeader is not provided', () => {
      component.menuItemHeader = undefined;
      fixture.detectChanges();

      const headerElement = fixture.debugElement.query(By.css('.header'));
      expect(headerElement).toBeNull();
    });

    it('should render header with title and subtitle', () => {
      const mockMenuItemHeader: MenuItemHeader = {
        title: 'Test Title',
        subtitle: 'Test Subtitle',
      };

      component.menuItemHeader = mockMenuItemHeader;
      fixture.detectChanges();

      const titleElement = fixture.debugElement.query(By.css('.mat-h2'));
      const subtitleElement = fixture.debugElement.query(By.css('.header__information__subtitle'));

      expect(titleElement.nativeElement.textContent).toBe('Test Title');
      expect(subtitleElement.nativeElement.textContent).toBe('Test Subtitle');
    });

    describe('Action Button Rendering', () => {
      it('should render action button with targetUrl', () => {
        const mockAction: MenuItemHeaderAction = {
          text: 'Open External',
          targetUrl: 'https://example.com',
          disabled: false,
        };

        component.menuItemHeader = {
          title: 'Test',
          action: mockAction,
        };
        fixture.detectChanges();

        const buttonElement = fixture.debugElement.query(By.css('a[mat-raised-button][target="_blank"]'));
        expect(buttonElement).toBeTruthy();
        expect(buttonElement.nativeElement.href).toBe('https://example.com/');
      });

      it('should render action button with navigateTo', () => {
        const mockAction: MenuItemHeaderAction = {
          text: 'Navigate Internal',
          navigateTo: '/internal-page',
          disabled: false,
        };

        component.menuItemHeader = {
          title: 'Test',
          action: mockAction,
        };
        fixture.detectChanges();

        const buttonElement = fixture.debugElement.query(By.css('a[mat-raised-button]:not([target="_blank"])'));
        expect(buttonElement).toBeTruthy();
        expect(buttonElement.nativeElement.textContent.trim()).toBe('Navigate Internal');
      });
    });
  });

  describe('Navigation Action', () => {
    const mockTimestamp = 1640995200000;
    const mockCustomFromTimestamp = 1640908800000;
    const mockCustomToTimestamp = 1640995200000;

    beforeEach(() => {
      jest.useFakeTimers();
      jest.setSystemTime(new Date(mockTimestamp));
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('should navigate to runtime-logs with custom timestamp parameters', () => {
      mockActivatedRoute.snapshot.queryParams = {
        period: 'custom',
        from: mockCustomFromTimestamp.toString(),
        to: mockCustomToTimestamp.toString(),
        httpStatuses: '200,404',
        plans: 'plan1,plan2',
        applications: 'app1,app2',
      };

      component.navigateToURL('/v4/runtime-logs');

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/v4/runtime-logs'], {
        relativeTo: mockActivatedRoute,
        queryParams: {
          from: mockCustomFromTimestamp,
          to: mockCustomToTimestamp,
          statuses: '200,404',
          planIds: 'plan1,plan2',
          applicationIds: 'app1,app2',
        },
      });
    });

    it('should navigate to runtime-logs with predefined time period parameters', () => {
      mockActivatedRoute.snapshot.queryParams = {
        period: '1h',
        httpStatuses: '200',
        plans: 'testPlan',
        applications: 'testApp',
      };

      component.navigateToURL('/v4/runtime-logs');

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/v4/runtime-logs'], {
        relativeTo: mockActivatedRoute,
        queryParams: expect.objectContaining({
          from: expect.any(Number),
          to: expect.any(Number),
          statuses: '200',
          planIds: 'testPlan',
          applicationIds: 'testApp',
        }),
      });
    });
  });
});
