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
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { GioFormCardGroupHarness } from './gio-form-card-group.harness';
import { GioFormCardGroupModule } from './gio-form-card-group.module';

@Component({
  template: `
    <gio-form-card-group [formControl]="selectControl">
      <gio-form-card value="A">Hello</gio-form-card>
      <gio-form-card value="B">Hello</gio-form-card>
      <gio-form-card value="C">Hello</gio-form-card>
      <gio-form-card value="D">Hello</gio-form-card>
      <gio-form-card value="E">Hello</gio-form-card>
    </gio-form-card-group>
  `,
})
class TestComponent {
  selectControl = new FormControl('');
}

describe('GioFormCardGroupModule', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [GioFormCardGroupModule, ReactiveFormsModule],
    });
    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should display form with form value', async () => {
    fixture.detectChanges();

    const formSelectCards = await loader.getHarness(GioFormCardGroupHarness);
    expect(await formSelectCards.getSelectedValue()).toEqual(undefined);

    component.selectControl.setValue('A');
    expect(await formSelectCards.getSelectedValue()).toBe('A');
  });

  it('should change selection', async () => {
    fixture.detectChanges();

    const formSelectCards = await loader.getHarness(GioFormCardGroupHarness);
    expect(await formSelectCards.getSelectedValue()).toEqual(undefined);

    await formSelectCards.select('B');
    expect(await formSelectCards.getSelectedValue()).toEqual('B');
    expect(component.selectControl.value).toEqual('B');
    expect(await formSelectCards.getUnselectedValues()).toEqual(['A', 'C', 'D', 'E']);

    await formSelectCards.select('C');
    expect(await formSelectCards.getSelectedValue()).toEqual('C');
    expect(component.selectControl.value).toEqual('C');
    expect(await formSelectCards.getUnselectedValues()).toEqual(['A', 'B', 'D', 'E']);
  });
});
