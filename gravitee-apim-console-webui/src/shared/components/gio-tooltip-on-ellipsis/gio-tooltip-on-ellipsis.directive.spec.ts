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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';

import { GioTooltipOnEllipsisDirective } from './gio-tooltip-on-ellipsis.directive';
import { GioTooltipOnEllipsisModule } from './gio-tooltip-on-ellipsis.module';

@Component({
  standalone: false,
  template: `
    <span class="truncated" gioTooltipOnEllipsis>{{ truncatedText }}</span>
    <span class="short" gioTooltipOnEllipsis>{{ shortText }}</span>
    <span class="select-trigger" gioTooltipOnEllipsis>{{ matSelectTriggerText }}</span>
    <div class="mat-mdc-select-value-text">
      <span class="select-value" gioTooltipOnEllipsis>{{ selectValueText }}</span>
    </div>
    <div class="role-option" gioTooltipOnEllipsis>
      <span class="mdc-list-item__primary-text">{{ optionRoleName }}</span>
    </div>
  `,
})
class TestHostComponent {
  truncatedText = 'VERY_LONG_ROLE_NAME_THAT_SHOULD_TRUNCATE';
  shortText = 'SHORT';
  matSelectTriggerText = '';
  selectValueText = 'SENIOR_REGIONAL_DEPUTY_VICE_PRESIDENT';
  optionRoleName = 'SENIOR_REGIONAL_DEPUTY_VICE_PRESIDENT_OF_OPERATIONS';
}

describe('GioTooltipOnEllipsisDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TestHostComponent],
      imports: [NoopAnimationsModule, GioTooltipOnEllipsisModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  function setElementDimensions(element: HTMLElement, offsetWidth: number, scrollWidth: number): void {
    Object.defineProperty(element, 'offsetWidth', { configurable: true, value: offsetWidth });
    Object.defineProperty(element, 'scrollWidth', { configurable: true, value: scrollWidth });
  }

  function getDirective(selector: string): GioTooltipOnEllipsisDirective {
    return fixture.debugElement.query(By.css(selector)).injector.get(GioTooltipOnEllipsisDirective);
  }

  function getHostElement(selector: string): HTMLElement {
    return fixture.debugElement.query(By.css(selector)).nativeElement as HTMLElement;
  }

  it('should show tooltip on hover when content is truncated', () => {
    const truncatedEl = getHostElement('.truncated');
    setElementDimensions(truncatedEl, 40, 200);

    const directive = getDirective('.truncated');
    const showSpy = jest.spyOn(directive, 'show');

    truncatedEl.dispatchEvent(new MouseEvent('mouseenter'));
    fixture.detectChanges();

    expect(directive.disabled).toBe(false);
    expect(directive.message).toBe('VERY_LONG_ROLE_NAME_THAT_SHOULD_TRUNCATE');
    expect(showSpy).toHaveBeenCalled();
  });

  it('should not show tooltip on hover when content fits', () => {
    const shortEl = getHostElement('.short');
    setElementDimensions(shortEl, 400, 400);

    const directive = getDirective('.short');
    const showSpy = jest.spyOn(directive, 'show');

    shortEl.dispatchEvent(new MouseEvent('mouseenter'));
    fixture.detectChanges();

    expect(directive.disabled).toBe(true);
    expect(directive.message).toBe('');
    expect(showSpy).not.toHaveBeenCalled();
  });

  it('should show tooltip after trigger text changes following init', () => {
    const triggerEl = getHostElement('.select-trigger');
    const directive = getDirective('.select-trigger');
    const showSpy = jest.spyOn(directive, 'show');

    triggerEl.dispatchEvent(new MouseEvent('mouseenter'));
    fixture.detectChanges();
    expect(showSpy).not.toHaveBeenCalled();

    fixture.componentInstance.matSelectTriggerText = 'SENIOR_REGIONAL_DEPUTY_VICE_PRESIDENT_OF_OPERATIONS';
    fixture.detectChanges();
    setElementDimensions(triggerEl, 80, 400);

    triggerEl.dispatchEvent(new MouseEvent('mouseenter'));
    fixture.detectChanges();

    expect(directive.disabled).toBe(false);
    expect(directive.message).toBe('SENIOR_REGIONAL_DEPUTY_VICE_PRESIDENT_OF_OPERATIONS');
    expect(showSpy).toHaveBeenCalled();
  });

  it('should detect overflow on mat-select value text container', () => {
    const selectValueEl = getHostElement('.select-value');
    const selectValueTextEl = selectValueEl.closest('.mat-mdc-select-value-text') as HTMLElement;

    setElementDimensions(selectValueEl, 160, 160);
    setElementDimensions(selectValueTextEl, 80, 320);

    const directive = getDirective('.select-value');
    const showSpy = jest.spyOn(directive, 'show');

    selectValueEl.dispatchEvent(new MouseEvent('mouseenter'));
    fixture.detectChanges();

    expect(directive.disabled).toBe(false);
    expect(directive.message).toBe('SENIOR_REGIONAL_DEPUTY_VICE_PRESIDENT');
    expect(showSpy).toHaveBeenCalled();
  });

  it('should detect overflow on mat-option primary text', () => {
    const optionEl = getHostElement('.role-option');
    const primaryTextEl = optionEl.querySelector('.mdc-list-item__primary-text') as HTMLElement;

    setElementDimensions(optionEl, 280, 280);
    setElementDimensions(primaryTextEl, 200, 400);

    const directive = getDirective('.role-option');
    const showSpy = jest.spyOn(directive, 'show');

    optionEl.dispatchEvent(new MouseEvent('mouseenter'));
    fixture.detectChanges();

    expect(directive.disabled).toBe(false);
    expect(directive.message).toBe('SENIOR_REGIONAL_DEPUTY_VICE_PRESIDENT_OF_OPERATIONS');
    expect(showSpy).toHaveBeenCalled();
  });
});
