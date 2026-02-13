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
import { Component } from '@angular/core';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { EnvLogsDetailsRowComponent } from './env-logs-details-row.component';
import { EnvLogsDetailsRowHarness } from './env-logs-details-row.harness';

@Component({
  standalone: true,
  imports: [EnvLogsDetailsRowComponent],
  template: `<env-logs-details-row [label]="label" [dataTestId]="dataTestId">{{ value }}</env-logs-details-row>`,
})
class TestHostComponent {
  label = 'Test Label';
  dataTestId: string | undefined;
  value: string | undefined = 'some value';
}

describe('EnvLogsDetailsRowComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;
  let harness: EnvLogsDetailsRowHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EnvLogsDetailsRowHarness);
  });

  it('should display the label', async () => {
    expect(await harness.getLabel()).toBe('Test Label');
  });

  it('should display projected text content', async () => {
    host.value = 'https://api.example.com';
    fixture.detectChanges();

    expect(await harness.getContent()).toBe('https://api.example.com');
  });

  it('should set data-testid when dataTestId is provided', async () => {
    host.dataTestId = 'my-test-id';
    fixture.detectChanges();

    expect(await harness.getDataTestId()).toBe('my-test-id');
  });

  it('should not set data-testid when dataTestId is not provided', async () => {
    expect(await harness.getDataTestId()).toBeNull();
  });
});
