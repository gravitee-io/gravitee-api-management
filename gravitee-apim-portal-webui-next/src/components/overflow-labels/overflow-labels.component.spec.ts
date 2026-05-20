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
import { Component, input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { computeVisibleCount, OverflowLabelsComponent } from './overflow-labels.component';
import { OverflowLabelsHarness } from './overflow-labels.harness';

describe('computeVisibleCount', () => {
  it('should_return_total_count_when_all_badges_fit', () => {
    expect(computeVisibleCount([50, 60, 40], 200, 30, 4)).toBe(3);
  });

  it('should_return_0_when_container_width_is_0', () => {
    expect(computeVisibleCount([50, 60], 0, 30, 4)).toBe(0);
  });

  it('should_return_0_when_no_badges', () => {
    expect(computeVisibleCount([], 200, 30, 4)).toBe(0);
  });

  it('should_return_0_when_no_badges_fit', () => {
    expect(computeVisibleCount([100, 100], 50, 30, 4)).toBe(0);
  });

  it('should_reserve_space_for_counter_when_not_all_fit', () => {
    // Container: 120px, badges: [50, 50, 50], counter: 30, gap: 4
    // All: 50+4+50+4+50=158 > 120
    // badge1: 50, counter needs 4+30=34 -> 50+34=84 <= 120 -> fits 1
    expect(computeVisibleCount([50, 50, 50], 120, 30, 4)).toBe(1);
  });

  it('should_fit_two_when_space_allows_with_counter', () => {
    // Container: 140px, badges: [50, 50, 50], counter: 30, gap: 4
    // All: 50+4+50+4+50=158 > 140
    // badge1+badge2: 50+4+50=104, counter: 4+30=34 -> 104+34=138 <= 140 -> fits 2
    expect(computeVisibleCount([50, 50, 50], 140, 30, 4)).toBe(2);
  });
});

@Component({
  selector: 'app-test-host',
  imports: [OverflowLabelsComponent],
  template: `<app-overflow-labels [labels]="labels()" />`,
})
class TestHostComponent {
  labels = input<string[]>([]);
}

describe('OverflowLabelsComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let harness: OverflowLabelsHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
  });

  it('should_render_no_badges_for_empty_labels', async () => {
    fixture.componentRef.setInput('labels', []);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, OverflowLabelsHarness);
    expect(await harness.getVisibleBadgeCount()).toBe(0);
    expect(await harness.getOverflowCounterText()).toBeNull();
  });

  it('should_render_overflow_counter_when_labels_present_but_container_has_no_width', async () => {
    fixture.componentRef.setInput('labels', ['type:http', 'scope:proxy']);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, OverflowLabelsHarness);
    // Initial render shows all labels (hasMeasured=false). After afterRenderEffect fires,
    // JSDOM returns 0 for all getBoundingClientRect widths, so computeVisibleCount returns 0
    // and all labels collapse into the overflow counter.
    expect(await harness.getOverflowCounterText()).toContain('+2');
  });
});
