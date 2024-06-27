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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Component } from '@angular/core';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { DeveloperPortalTopBarHarness } from './developer-portal-top-bar.harness';
import { DeveloperPortalTopBarComponent } from './developer-portal-top-bar.component';

@Component({
  selector: 'test-component',
  template: ` <developer-portal-top-bar [dataSource]="dataSource" [loading]="loading"></developer-portal-top-bar>`,
})
class TestComponent {
  loading = false;
  dataSource = [{ name: 'foo' }];
}

describe('DeveloperPortalTopBarComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let componentHarness: DeveloperPortalTopBarHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [MatIconTestingModule, NoopAnimationsModule, DeveloperPortalTopBarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    componentHarness = await loader.getHarness(DeveloperPortalTopBarHarness);
    fixture.detectChanges();
  });

  it('should display table', async () => {
    const { headerCells, rowCells } = await componentHarness.computeTableCells();
    expect(headerCells).toStrictEqual([{ name: 'Name', actions: 'Action' }]);
    expect(rowCells).toEqual([['foo', '']]);
  });

  it('should edit row', async () => {
    await componentHarness.editRow(0);
    // add some tests
  });

  it('should delete row', async () => {
    await componentHarness.deleteRow(0);
    // add some tests
  });
});
