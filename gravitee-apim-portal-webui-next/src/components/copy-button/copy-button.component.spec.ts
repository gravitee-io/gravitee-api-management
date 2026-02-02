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

import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { CopyButtonComponent } from './copy-button.component';
import { CopyButtonHarness } from './copy-button.component.harness';

describe('CopyButtonComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let harness: CopyButtonHarness;
  let originalClipboard: any;

  @Component({
    imports: [CopyButtonComponent],
    template: `<app-copy-button [content]="content" [label]="label"></app-copy-button>`,
  })
  class TestHostComponent {
    content = 'test content';
    label = 'test label';
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, CopyButtonComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.loader(fixture).getHarness(CopyButtonHarness);
    originalClipboard = (navigator as any).clipboard;
  });

  afterEach(() => {
    try {
      Object.defineProperty(navigator, 'clipboard', { value: originalClipboard, configurable: true });
    } catch (e) {
      // ignore restore errors in some environments
    }
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it('renders initial icon content_copy', async () => {
    expect(await harness.getIconText()).toBe('content_copy');
  });

  it('aria-label includes provided label', async () => {
    const aria = await harness.getAriaLabel();
    expect(aria).toContain('test label');
  });

  it('calls clipboard.writeText and shows check icon then resets after 2000ms', async () => {
    const writeTextMock = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: writeTextMock }, configurable: true });

    jest.useFakeTimers();

    // 1. Trigger the click manually to avoid Harness waiting for stability
    const buttonElement = fixture.nativeElement.querySelector('button');
    buttonElement.click();

    // 2. Advance time slightly to let the Promise resolve (microtasks)
    // but NOT enough to finish the 2000ms timer
    jest.advanceTimersByTime(100);
    fixture.detectChanges();

    // 3. Check the intermediate state (Manually, to avoid Harness locking up)
    const iconElement = fixture.nativeElement.querySelector('.material-icons');
    expect(writeTextMock).toHaveBeenCalledWith('test content');
    expect(iconElement.textContent.trim()).toBe('check');

    // 4. Advance time to finish the reset timer
    jest.advanceTimersByTime(2000);
    fixture.detectChanges();

    // 5. Check the final state
    expect(iconElement.textContent.trim()).toBe('content_copy');
  });

  it('does not set copied when writeText rejects', async () => {
    const writeTextMock = jest.fn().mockRejectedValue(new Error('fail'));
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: writeTextMock }, configurable: true });

    jest.useFakeTimers();

    // 1. Trigger manually
    const buttonElement = fixture.nativeElement.querySelector('button');
    buttonElement.click();

    // 2. Flush promises
    jest.advanceTimersByTime(100);
    fixture.detectChanges();

    // 3. Verify it didn't change
    const iconElement = fixture.nativeElement.querySelector('.material-icons');
    expect(iconElement.textContent.trim()).toBe('content_copy');
  });

  it('calls clipboard.writeText and shows check icon then resets after 2000ms', async () => {
    const writeTextMock = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: writeTextMock }, configurable: true });

    jest.useFakeTimers();

    await harness.click();

    await fixture.whenStable();
    fixture.detectChanges();

    expect(writeTextMock).toHaveBeenCalledWith('test content');
    expect(await harness.getIconText()).toBe('check');

    jest.advanceTimersByTime(2000);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.getIconText()).toBe('content_copy');
  });

  it('does not set copied when writeText rejects', async () => {
    const writeTextMock = jest.fn().mockRejectedValue(new Error('fail'));
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: writeTextMock }, configurable: true });

    jest.useFakeTimers();

    await harness.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await harness.getIconText()).toBe('content_copy');
  });
});
