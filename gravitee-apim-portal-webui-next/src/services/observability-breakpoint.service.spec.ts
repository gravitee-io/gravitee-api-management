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
import { BreakpointObserver, Breakpoints, BreakpointState } from '@angular/cdk/layout';
import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';

import { ObservabilityBreakpointService } from './observability-breakpoint.service';

describe('ObservabilityBreakpointService', () => {
  let service: ObservabilityBreakpointService;
  let observeSubject: Subject<BreakpointState>;

  const breakpointObserverMock = {
    observe: jest.fn(),
  };

  beforeEach(() => {
    observeSubject = new Subject<BreakpointState>();
    breakpointObserverMock.observe.mockReturnValue(observeSubject.asObservable());

    TestBed.configureTestingModule({
      providers: [ObservabilityBreakpointService, { provide: BreakpointObserver, useValue: breakpointObserverMock }],
    });

    service = TestBed.inject(ObservabilityBreakpointService);
  });

  describe('isMobile$ observable', () => {
    it('should emit true when screen matches XSmall breakpoint (max-width: 599.98px)', () => {
      const breakpointState: BreakpointState = {
        matches: true,
        breakpoints: { [Breakpoints.XSmall]: true },
      };

      let result: boolean;
      service.isMobile$.subscribe(value => {
        result = value;
      });

      observeSubject.next(breakpointState);

      expect(result!).toBe(true);
      expect(breakpointObserverMock.observe).toHaveBeenCalledWith([Breakpoints.XSmall]);
    });

    it('should emit false when screen does not match XSmall breakpoint (desktop/laptop/tablet)', () => {
      const breakpointState: BreakpointState = {
        matches: false,
        breakpoints: { [Breakpoints.XSmall]: false },
      };

      let result: boolean;
      service.isMobile$.subscribe(value => {
        result = value;
      });

      observeSubject.next(breakpointState);

      expect(result!).toBe(false);
      expect(breakpointObserverMock.observe).toHaveBeenCalledWith([Breakpoints.XSmall]);
    });
  });

  describe('signals', () => {
    it('should update isMobile signal when isMobile$ emits true', () => {
      const breakpointState: BreakpointState = {
        matches: true,
        breakpoints: { [Breakpoints.XSmall]: true },
      };

      observeSubject.next(breakpointState);

      expect(service.isMobile()).toBe(true);
    });

    it('should update isMobile signal when isMobile$ emits false', () => {
      const breakpointState: BreakpointState = {
        matches: false,
        breakpoints: { [Breakpoints.XSmall]: false },
      };

      observeSubject.next(breakpointState);

      expect(service.isMobile()).toBe(false);
    });
  });

  describe('observable behavior', () => {
    it('should use shareReplay for isMobile$ observable', done => {
      const mobileState: BreakpointState = {
        matches: true,
        breakpoints: { [Breakpoints.XSmall]: true },
      };

      let subscriptionCount = 0;
      service.isMobile$.subscribe(() => {
        subscriptionCount++;
        if (subscriptionCount === 1) {
          // Second subscription should get the cached value
          service.isMobile$.subscribe(() => {
            expect(subscriptionCount).toBe(1); // Should not trigger new emission
            done();
          });
        }
      });

      observeSubject.next(mobileState);
    });
  });

  describe('breakpoint state changes', () => {
    it('should handle isMobile signal state changes', () => {
      const initialState: BreakpointState = { matches: true, breakpoints: { [Breakpoints.XSmall]: true } };
      observeSubject.next(initialState);
      expect(service.isMobile()).toBe(initialState.matches);

      const newState: BreakpointState = { matches: false, breakpoints: { [Breakpoints.XSmall]: false } };
      observeSubject.next(newState);
      expect(service.isMobile()).toBe(newState.matches);
    });
  });

  describe('edge cases', () => {
    it('should handle multiple rapid state changes', () => {
      const states = [
        { matches: true, breakpoints: { [Breakpoints.XSmall]: true } },
        { matches: false, breakpoints: { [Breakpoints.XSmall]: false } },
        { matches: true, breakpoints: { [Breakpoints.XSmall]: true } },
        { matches: false, breakpoints: { [Breakpoints.XSmall]: false } },
      ];

      states.forEach(state => {
        observeSubject.next(state);
        expect(service.isMobile()).toBe(state.matches);
      });
    });
  });
});
