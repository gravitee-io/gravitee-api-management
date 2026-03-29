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

import { DEFAULT_DOCUMENTATION_SKELETON_ROWS, DocumentationSkeletonComponent } from './documentation-skeleton.component';
import { DocumentationSkeletonComponentHarness } from './documentation-skeleton.component.harness';

describe('DocumentationSkeletonComponent', () => {
  let fixture: ComponentFixture<DocumentationSkeletonComponent>;
  let harness: DocumentationSkeletonComponentHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentationSkeletonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentationSkeletonComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationSkeletonComponentHarness);
  });

  it('should render default rows and bars', async () => {
    const rows = await harness.getRowElements();
    const bars = await harness.getBarElements();
    expect(rows.length).toEqual(DEFAULT_DOCUMENTATION_SKELETON_ROWS.length);
    expect(bars.length).toEqual(DEFAULT_DOCUMENTATION_SKELETON_ROWS.length);
  });

  it('should set data-type from row input', async () => {
    const rows = await harness.getRowElements();
    const types = await Promise.all(rows.map(el => el.getAttribute('data-type')));
    expect(types).toEqual(DEFAULT_DOCUMENTATION_SKELETON_ROWS.map(r => r.type));
  });

  it('should render custom rows', async () => {
    const custom = [
      { type: 'h1' as const, widthPercent: 50 },
      { type: 'body' as const, widthPercent: 90 },
    ];
    fixture.componentRef.setInput('rows', custom);
    fixture.detectChanges();

    const rowEls = await harness.getRowElements();
    const barEls = await harness.getBarElements();
    expect(rowEls.length).toEqual(2);
    expect(barEls.length).toEqual(2);
    expect(await rowEls[0].getAttribute('data-type')).toEqual('h1');
    expect(await rowEls[1].getAttribute('data-type')).toEqual('body');
    expect((await barEls[0].getCssValue('width')).trim()).toContain('50');
  });

  it('should stagger bar animation delay by barStaggerMs', async () => {
    fixture.componentRef.setInput('barStaggerMs', 30);
    fixture.detectChanges();

    const bars = await harness.getBarElements();
    const delays = await Promise.all(bars.map(el => el.getCssValue('animation-delay')));
    expect(delays[0].trim()).toMatch(/^0m?s$/);
    expect(delays[1]).toMatch(/30ms|0\.03s/);
  });
});
