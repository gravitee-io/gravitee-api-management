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

import { NarrowClassDirective } from './narrow-class.directive';
import { ObservabilityBreakpointService } from '../services/observability-breakpoint.service';

@Component({
  template: `<div [appIsNarrow]="'table-narrow'" (triggerBreakpoint)="onBreakpoint($event)">Test Element</div>`,
  standalone: true,
  imports: [NarrowClassDirective],
})
class TestHostWithOutputComponent {
  lastBreakpoint: 'narrow' | null | undefined;
  onBreakpoint(value: 'narrow' | null) {
    this.lastBreakpoint = value;
  }
}

class MockObservabilityBreakpointService {
  static mockIsNarrow: WritableSignal<boolean>;

  get isNarrow() {
    return MockObservabilityBreakpointService.mockIsNarrow.asReadonly();
  }
}

@Component({
  template: `<div [appIsNarrow]="'table-narrow'">Test Element</div>`,
  standalone: true,
  imports: [NarrowClassDirective],
})
class TestHostComponent {}

describe('NarrowClassDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let element: HTMLElement;

  beforeEach(async () => {
    // Reset the static signal before each test
    MockObservabilityBreakpointService.mockIsNarrow = signal(false);

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
    element = fixture.debugElement.query(By.directive(NarrowClassDirective)).nativeElement;
    fixture.detectChanges();
  });

  it('should create an instance', () => {
    const directive = fixture.debugElement.query(By.directive(NarrowClassDirective));
    expect(directive).toBeTruthy();
  });

  it('should NOT have the "table-narrow" class when isNarrow is false', () => {
    // Set signal value
    MockObservabilityBreakpointService.mockIsNarrow.set(false);

    // Trigger change detection
    fixture.detectChanges();

    // Assert
    expect(element.classList.contains('table-narrow')).toBeFalsy();
  });

  it('SHOULD have the "table-narrow" class when isNarrow is true', () => {
    // Set signal value
    MockObservabilityBreakpointService.mockIsNarrow.set(true);

    // Trigger change detection
    fixture.detectChanges();

    // Assert
    expect(element.classList.contains('table-narrow')).toBeTruthy();
  });

  it('should reactively toggle the class when the signal changes', () => {
    // Start as false
    MockObservabilityBreakpointService.mockIsNarrow.set(false);
    fixture.detectChanges();
    expect(element.classList.contains('table-narrow')).toBeFalsy();

    // Change to true
    MockObservabilityBreakpointService.mockIsNarrow.set(true);
    fixture.detectChanges();
    expect(element.classList.contains('table-narrow')).toBeTruthy();

    // Change back to false
    MockObservabilityBreakpointService.mockIsNarrow.set(false);
    fixture.detectChanges();
    expect(element.classList.contains('table-narrow')).toBeFalsy();
  });
});

describe('NarrowClassDirective - triggerBreakpoint output', () => {
  let fixture: ComponentFixture<TestHostWithOutputComponent>;
  let hostComponent: TestHostWithOutputComponent;

  beforeEach(async () => {
    MockObservabilityBreakpointService.mockIsNarrow = signal(false);

    await TestBed.configureTestingModule({
      imports: [TestHostWithOutputComponent],
      providers: [{ provide: ObservabilityBreakpointService, useClass: MockObservabilityBreakpointService }],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostWithOutputComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should emit "narrow" when isNarrow becomes true', () => {
    MockObservabilityBreakpointService.mockIsNarrow.set(true);
    fixture.detectChanges();

    expect(hostComponent.lastBreakpoint).toBe('narrow');
  });

  it('should emit null when isNarrow becomes false after being true', () => {
    MockObservabilityBreakpointService.mockIsNarrow.set(true);
    fixture.detectChanges();

    MockObservabilityBreakpointService.mockIsNarrow.set(false);
    fixture.detectChanges();

    expect(hostComponent.lastBreakpoint).toBeNull();
  });
});
