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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatTooltip } from '@angular/material/tooltip';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { MatTooltipOnEllipsisDirective } from './mat-tooltip-on-ellipsis.directive';

@Component({
  template: `<div data-testid="host" matTooltip="Help text" appMatTooltipOnEllipsis>
    {{ label }}
  </div>`,
  standalone: true,
  imports: [MatTooltip, MatTooltipOnEllipsisDirective],
})
class TestHostComponent {
  label = 'Short';
}

@Component({
  template: `<div data-testid="host" matTooltip="Help text" [matTooltipDisabled]="tooltipDisabled" appMatTooltipOnEllipsis>
    {{ label }}
  </div>`,
  standalone: true,
  imports: [MatTooltip, MatTooltipOnEllipsisDirective],
})
class TestHostWithTooltipDisabledComponent {
  label = 'Long label';
  tooltipDisabled = true;
}

function mockElementWidths(element: HTMLElement, clientWidth: number, scrollWidth: number): void {
  Object.defineProperty(element, 'clientWidth', { configurable: true, value: clientWidth });
  Object.defineProperty(element, 'scrollWidth', { configurable: true, value: scrollWidth });
}

describe('MatTooltipOnEllipsisDirective', () => {
  describe('with default tooltip', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let hostEl: HTMLElement;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [TestHostComponent],
        providers: [provideNoopAnimations()],
      }).compileComponents();

      fixture = TestBed.createComponent(TestHostComponent);
      hostEl = fixture.nativeElement.querySelector('[data-testid="host"]');
      fixture.detectChanges();
    });

    function matTooltip(): MatTooltip {
      return fixture.debugElement.query(By.css('[data-testid="host"]')).injector.get(MatTooltip);
    }

    it('should attach to host', () => {
      const dir = fixture.debugElement.query(By.directive(MatTooltipOnEllipsisDirective));
      expect(dir).toBeTruthy();
    });

    it('should disable MatTooltip when text is not truncated on mouseenter and restore after mouseleave', () => {
      mockElementWidths(hostEl, 200, 200);
      hostEl.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
      expect(matTooltip().disabled).toBe(true);
      hostEl.dispatchEvent(new MouseEvent('mouseleave', { bubbles: true }));
      expect(matTooltip().disabled).toBe(false);
    });

    it('should enable MatTooltip when text is truncated on mouseenter and keep enabled after mouseleave', () => {
      mockElementWidths(hostEl, 100, 250);
      hostEl.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
      expect(matTooltip().disabled).toBe(false);
      hostEl.dispatchEvent(new MouseEvent('mouseleave', { bubbles: true }));
      expect(matTooltip().disabled).toBe(false);
    });

    it('should enable MatTooltip when text is truncated on focusin and keep enabled after focusout', () => {
      mockElementWidths(hostEl, 100, 250);
      hostEl.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
      expect(matTooltip().disabled).toBe(false);
      hostEl.dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
      expect(matTooltip().disabled).toBe(false);
    });

    it('should disable MatTooltip when text is not truncated on focusin and restore after focusout', () => {
      mockElementWidths(hostEl, 200, 200);
      hostEl.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
      expect(matTooltip().disabled).toBe(true);
      hostEl.dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
      expect(matTooltip().disabled).toBe(false);
    });
  });

  describe('with an already disabled tooltip', () => {
    let fixture: ComponentFixture<TestHostWithTooltipDisabledComponent>;
    let hostEl: HTMLElement;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [TestHostWithTooltipDisabledComponent],
        providers: [provideNoopAnimations()],
      }).compileComponents();

      fixture = TestBed.createComponent(TestHostWithTooltipDisabledComponent);
      hostEl = fixture.nativeElement.querySelector('[data-testid="host"]') as HTMLElement;
      fixture.detectChanges();
    });

    function matTooltip(): MatTooltip {
      return fixture.debugElement.query(By.css('[data-testid="host"]')).injector.get(MatTooltip);
    }

    it('should not enable MatTooltip on mouseenter even if truncated', () => {
      mockElementWidths(hostEl, 100, 250);
      hostEl.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
      expect(matTooltip().disabled).toBe(true);
    });

    it('should leave MatTooltip disabled after mouseleave', () => {
      mockElementWidths(hostEl, 100, 250);
      hostEl.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
      hostEl.dispatchEvent(new MouseEvent('mouseleave', { bubbles: true }));
      expect(matTooltip().disabled).toBe(true);
    });

    it('should not enable MatTooltip on focusin even if truncated and keep disabled after focusout', () => {
      mockElementWidths(hostEl, 100, 250);
      hostEl.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
      expect(matTooltip().disabled).toBe(true);
      hostEl.dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
      expect(matTooltip().disabled).toBe(true);
    });
  });
});
