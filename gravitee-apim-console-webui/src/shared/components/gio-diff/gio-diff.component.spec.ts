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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GioDiffHarness } from './gio-diff.harness';
import { GioDiffModule } from './gio-diff.module';

@Component({
  template: `<gio-diff [left]="left" [right]="right"></gio-diff>`,
})
class TestComponent {
  left = '';
  right = '';
}

describe('GioDiffComponent', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [GioDiffModule],
      declarations: [TestComponent],
      providers: [],
    });
    fixture = TestBed.createComponent(TestComponent);
  });
  beforeEach(() => {
    loader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
  });

  describe('with diff between left and right', () => {
    beforeEach(() => {
      component.left = JSON.stringify(
        {
          name: 'Yann',
          age: 30,
          animals: null,
        },
        undefined,
        4,
      );
      component.right = JSON.stringify(
        {
          name: 'Yann',
          age: 31,
          animals: ['🐩'],
        },
        undefined,
        4,
      );
    });

    it('should switch between side-by-side(default) -> raw -> line-by-line format', async () => {
      expect(component).toBeTruthy();

      const gioDiff = await loader.getHarness(GioDiffHarness);
      expect(await gioDiff.hasNoDiffToDisplay()).toEqual(false);

      expect(await gioDiff.getOutputFormat()).toEqual('side-by-side');

      await gioDiff.selectOutputFormat('raw');
      expect(await gioDiff.getOutputFormat()).toEqual('raw');

      await gioDiff.selectOutputFormat('line-by-line');
      expect(await gioDiff.getOutputFormat()).toEqual('line-by-line');
    });
  });

  describe('without diff between left and right', () => {
    it('should force raw format and display only one gv-code', async () => {
      expect(component).toBeTruthy();

      const gioDiff = await loader.getHarness(GioDiffHarness);

      expect(await gioDiff.getOutputFormat()).toEqual('raw');

      // Keep raw format because line-by-line & side-by-side are disabled
      await gioDiff.selectOutputFormat('line-by-line');
      await gioDiff.selectOutputFormat('side-by-side');
      expect(await gioDiff.getOutputFormat()).toEqual('raw');

      expect(await gioDiff.hasNoDiffToDisplay()).toEqual(true);
    });
  });
});
