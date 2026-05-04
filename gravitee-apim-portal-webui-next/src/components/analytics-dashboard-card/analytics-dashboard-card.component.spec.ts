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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { AnalyticsDashboardCardComponent } from './analytics-dashboard-card.component';
import { AnalyticsDashboardCardHarness } from './analytics-dashboard-card.harness';
import { fakeDashboard } from '../../entities/analytics-dashboard/analytics-dashboard.fixtures';

describe('AnalyticsDashboardCardComponent', () => {
  let fixture: ComponentFixture<AnalyticsDashboardCardComponent>;
  let component: AnalyticsDashboardCardComponent;
  let harness: AnalyticsDashboardCardHarness;

  const dashboard = fakeDashboard({
    id: 'dashboard-1',
    name: 'HTTP Overview',
    labels: { type: 'http', scope: 'proxy' },
    widgets: [{} as never, {} as never],
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalyticsDashboardCardComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsDashboardCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('dashboard', dashboard);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AnalyticsDashboardCardHarness);
  });

  it('should_render_dashboard_name', async () => {
    expect(await harness.getTitle()).toContain('HTTP Overview');
  });

  it('should_render_all_labels_when_count_is_within_visible_limit', async () => {
    expect(await harness.getLabelCount()).toBe(2);
    expect(await harness.getOverflowCounter()).toBeNull();
  });

  it('should_render_overflow_counter_when_labels_exceed_visible_limit', async () => {
    fixture.componentRef.setInput(
      'dashboard',
      fakeDashboard({
        labels: { type: 'http', scope: 'proxy', env: 'dev', team: 'core' },
      }),
    );
    fixture.detectChanges();
    expect(await harness.getLabelCount()).toBe(3);
    expect(await harness.getOverflowCounter()).toContain('+2');
  });

  it('should_render_widget_count', async () => {
    expect(await harness.getWidgetCount()).toContain('2');
  });

  it('should_render_last_modified_date', async () => {
    expect(await harness.getLastModified()).toBeTruthy();
  });

  it('should_not_render_labels_when_empty', async () => {
    fixture.componentRef.setInput('dashboard', fakeDashboard({ labels: {} }));
    fixture.detectChanges();
    expect(await harness.getLabelCount()).toBe(0);
  });

  it('should_emit_card_select_on_click', async () => {
    const emitSpy = jest.spyOn(component.cardSelect, 'emit');
    await harness.click();
    expect(emitSpy).toHaveBeenCalledWith('dashboard-1');
  });

  it('should_show_pin_button', async () => {
    expect(await harness.getPinButton()).toBeTruthy();
  });

  it('should_emit_pin_toggle_on_pin_click', async () => {
    const emitSpy = jest.spyOn(component.pinToggle, 'emit');
    const pinButton = await harness.getPinButton();
    await pinButton!.click();
    expect(emitSpy).toHaveBeenCalledWith('dashboard-1');
  });

  it('should_not_emit_pin_toggle_when_not_pinned_and_cannot_pin', async () => {
    fixture.componentRef.setInput('isPinned', false);
    fixture.componentRef.setInput('canPin', false);
    fixture.detectChanges();
    const emitSpy = jest.spyOn(component.pinToggle, 'emit');
    const pinButton = await harness.getPinButton();
    await pinButton!.click();
    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('should_mark_pin_button_as_aria_disabled_when_cannot_pin', async () => {
    fixture.componentRef.setInput('isPinned', false);
    fixture.componentRef.setInput('canPin', false);
    fixture.detectChanges();
    const pinButton = await harness.getPinButton();
    const host = await pinButton!.host();
    expect(await host.getAttribute('aria-disabled')).toBe('true');
  });

  it('should_allow_unpin_even_when_cannot_pin_more', async () => {
    fixture.componentRef.setInput('isPinned', true);
    fixture.componentRef.setInput('canPin', false);
    fixture.detectChanges();
    const emitSpy = jest.spyOn(component.pinToggle, 'emit');
    const pinButton = await harness.getPinButton();
    await pinButton!.click();
    expect(emitSpy).toHaveBeenCalledWith('dashboard-1');
  });
});
