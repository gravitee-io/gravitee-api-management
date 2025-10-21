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
import { Component, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { MobileClassDirective } from './mobile-class.directive';
import { ObservabilityBreakpointService } from '../services/observability-breakpoint.service';

class MockObservabilityBreakpointService {
  static mockIsMobile: WritableSignal<boolean>;

  get isMobile() {
    return MockObservabilityBreakpointService.mockIsMobile.asReadonly();
  }
}

@Component({
  template: `<div [appIsMobile]="'table-mobile'">Test Element</div>`,
  standalone: true,
  imports: [MobileClassDirective],
})
class TestHostComponent {}

describe('TableMobileDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let element: HTMLElement;

  beforeEach(async () => {
    // Reset the static signal before each test
    MockObservabilityBreakpointService.mockIsMobile = signal(false);

    await TestBed.configureTestingModule({
      // Import the host component (which imports the directive)
      imports: [TestHostComponent],
      providers: [
        // Provide the mock service
        {
          provide: ObservabilityBreakpointService,
          useClass: MockObservabilityBreakpointService,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    // Find the element the directive is applied to
    element = fixture.debugElement.query(By.directive(MobileClassDirective)).nativeElement;
    fixture.detectChanges();
  });

  it('should create an instance', () => {
    const directive = fixture.debugElement.query(By.directive(MobileClassDirective));
    expect(directive).toBeTruthy();
  });

  it('should NOT have the "table-mobile" class when isMobile is false', () => {
    // Set signal value
    MockObservabilityBreakpointService.mockIsMobile.set(false);

    // Trigger change detection
    fixture.detectChanges();

    // Assert
    expect(element.classList.contains('table-mobile')).toBeFalsy();
  });

  it('SHOULD have the "table-mobile" class when isMobile is true', () => {
    // Set signal value
    MockObservabilityBreakpointService.mockIsMobile.set(true);

    // Trigger change detection
    fixture.detectChanges();

    // Assert
    expect(element.classList.contains('table-mobile')).toBeTruthy();
  });

  it('should reactively toggle the class when the signal changes', () => {
    // Start as false
    MockObservabilityBreakpointService.mockIsMobile.set(false);
    fixture.detectChanges();
    expect(element.classList.contains('table-mobile')).toBeFalsy();

    // Change to true
    MockObservabilityBreakpointService.mockIsMobile.set(true);
    fixture.detectChanges();
    expect(element.classList.contains('table-mobile')).toBeTruthy();

    // Change back to false
    MockObservabilityBreakpointService.mockIsMobile.set(false);
    fixture.detectChanges();
    expect(element.classList.contains('table-mobile')).toBeFalsy();
  });
});
