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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { GioWidgetLayoutComponent, GioWidgetLayoutState } from './gio-widget-layout.component';
import { GioWidgetLayoutHarness } from './gio-widget-layout.harness';

import { GioTestingModule } from '../../testing';

@Component({
  template: `
    <gio-widget-layout [title]="title" [state]="state" [tooltip]="tooltip" [errors]="errors">
      <div gioWidgetLayoutChart data-testid="dummy-chart-content">
        <span>Dummy Chart Content</span>
      </div>
    </gio-widget-layout>
  `,
  standalone: true,
  imports: [GioWidgetLayoutComponent],
})
class TestComponent {
  title = 'Test Widget';
  state: GioWidgetLayoutState = 'loading';
  tooltip?: string;
  errors?: string[];
}

describe('GioWidgetLayoutComponent', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let harness: GioWidgetLayoutHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent, GioTestingModule, NoopAnimationsModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, GioWidgetLayoutHarness);
  });

  it('should show loading state', async () => {
    component.state = 'loading';
    fixture.detectChanges();

    expect(component.state).toBe('loading');
    expect(await harness.isLoading()).toBe(true);
    expect(await harness.getTitleText()).toBe('Test Widget');
  });

  it('should show success state', async () => {
    component.state = 'success';
    fixture.detectChanges();

    expect(component.state).toBe('success');
    expect(await harness.hasContent()).toBe(true);
    expect(await harness.getTitleText()).toBe('Test Widget');
  });

  it('should project content in success state', async () => {
    component.state = 'success';
    fixture.detectChanges();

    // Verify that the dummy content is projected
    const dummyContent = fixture.nativeElement.querySelector('[data-testid="dummy-chart-content"]');
    expect(dummyContent).toBeTruthy();
    expect(dummyContent.textContent).toContain('Dummy Chart Content');
  });

  it('should not show projected content in other states', async () => {
    component.state = 'loading';
    fixture.detectChanges();

    // Verify that the dummy content is not shown in loading state
    const dummyContent = fixture.nativeElement.querySelector('[data-testid="dummy-chart-content"]');
    expect(dummyContent).toBeFalsy();
  });

  it('should show empty state', async () => {
    component.state = 'empty';
    fixture.detectChanges();

    expect(component.state).toBe('empty');
    expect(await harness.isEmpty()).toBe(true);
    expect(await harness.getEmptyText()).toBe('No data available');
    expect(await harness.getTitleText()).toBe('Test Widget');
  });

  it('should show error state with errors', async () => {
    component.state = 'error';
    component.errors = ['Test error message'];
    fixture.detectChanges();

    expect(component.state).toBe('error');
    expect(await harness.hasError()).toBe(true);
    expect(await harness.hasErrorIcon()).toBe(true);
    expect(await harness.getErrorText()).toBe('Test error message');
    expect(await harness.getTitleText()).toBe('Test Widget');
  });

  it('should show tooltip when provided', async () => {
    component.state = 'success';
    component.tooltip = 'Test tooltip';
    fixture.detectChanges();

    expect(component.tooltip).toBe('Test tooltip');
    expect(await harness.hasTooltipIcon()).toBe(true);
  });

  it('should not show tooltip when not provided', async () => {
    component.state = 'success';
    component.tooltip = undefined;
    fixture.detectChanges();

    expect(component.tooltip).toBeUndefined();
    expect(await harness.hasTooltipIcon()).toBe(false);
  });
});
