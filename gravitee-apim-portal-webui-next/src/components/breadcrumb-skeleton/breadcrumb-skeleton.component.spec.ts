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

import { BreadcrumbSkeletonComponent, DEFAULT_BREADCRUMB_SKELETON_SEGMENTS } from './breadcrumb-skeleton.component';
import { BreadcrumbSkeletonComponentHarness } from './breadcrumb-skeleton.component.harness';

describe('BreadcrumbSkeletonComponent', () => {
  let fixture: ComponentFixture<BreadcrumbSkeletonComponent>;
  let harness: BreadcrumbSkeletonComponentHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BreadcrumbSkeletonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BreadcrumbSkeletonComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, BreadcrumbSkeletonComponentHarness);
  });

  it('should render default segments and separators', async () => {
    const segments = await harness.getSegmentElements();
    expect(segments.length).toEqual(DEFAULT_BREADCRUMB_SKELETON_SEGMENTS.length);

    const separators = await harness.getSeparatorElements();
    expect(separators.length).toEqual(DEFAULT_BREADCRUMB_SKELETON_SEGMENTS.length - 1);
  });

  it('should set data-size from segment input', async () => {
    const segments = await harness.getSegmentElements();
    const sizes = await Promise.all(segments.map(el => el.getAttribute('data-size')));
    expect(sizes).toEqual(DEFAULT_BREADCRUMB_SKELETON_SEGMENTS.map(s => s.size));
  });

  it('should render custom segments without trailing separator', async () => {
    fixture.componentRef.setInput('segments', [{ size: 'short' }]);
    fixture.detectChanges();

    const segments = await harness.getSegmentElements();
    const separators = await harness.getSeparatorElements();
    expect(segments.length).toEqual(1);
    expect(separators.length).toEqual(0);
    expect(await segments[0].getAttribute('data-size')).toEqual('short');
  });

  it('should stagger animation delay by barStaggerMs', async () => {
    fixture.componentRef.setInput('barStaggerMs', 50);
    fixture.detectChanges();

    const segments = await harness.getSegmentElements();
    const delays = await Promise.all(segments.map(el => el.getCssValue('animation-delay')));
    expect(delays[0].trim()).toMatch(/^0m?s$/);
    expect(delays[1]).toMatch(/50ms|0\.05s/);
  });
});
