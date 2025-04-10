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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ExposedEntrypointsComponent } from './exposed-entrypoints.component';
import { ExposedEntrypointsHarness } from './exposed-entrypoints.harness';

import { GioTestingModule } from '../../../../shared/testing';
import { fakeExposedEntrypoint } from '../../../../entities/management-api-v2/api/exposedEntrypoint.fixture';

describe('ExposedEntrypointsComponent', () => {
  let fixture: ComponentFixture<ExposedEntrypointsComponent>;
  let componentHarness: ExposedEntrypointsHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExposedEntrypointsComponent, GioTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(ExposedEntrypointsComponent);
  });

  it('should display exposed entrypoints', async () => {
    fixture.componentRef.setInput('exposedEntrypoints', [
      fakeExposedEntrypoint({ value: 'fox.kafka.ue:9092' }),
      fakeExposedEntrypoint({ value: 'fox.kafka.us:9092' }),
    ]);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ExposedEntrypointsHarness);
    fixture.detectChanges();

    expect(await componentHarness.getValues()).toEqual(['fox.kafka.ue:9092', 'fox.kafka.us:9092']);
  });

  it('should display empty card with no entrypoints', async () => {
    fixture.componentRef.setInput('exposedEntrypoints', []);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ExposedEntrypointsHarness);
    fixture.detectChanges();

    expect(await componentHarness.isEmpty()).toBe(true);
  });
});
