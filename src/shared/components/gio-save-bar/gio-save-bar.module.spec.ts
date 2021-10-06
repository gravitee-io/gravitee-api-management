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
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatInputHarness } from '@angular/material/input/testing';

import { GioSaveBarModule } from './gio-save-bar.module';
import { GioSaveBarHarness } from './gio-save-bar.harness';

describe('GioFormCardGroupModule', () => {
  describe('simple usage', () => {
    const onResetMock = jest.fn();
    const onSubmitMock = jest.fn();

    @Component({
      template: `
        <div>
          <input />
          <gio-save-bar [opened]="opened" (reset)="onReset($event)" (submit)="onSubmit($event)"></gio-save-bar>
        </div>
      `,
    })
    class TestComponent {
      opened = false;
      onReset = onResetMock;
      onSubmit = onSubmitMock;
    }

    let component: TestComponent;
    let fixture: ComponentFixture<TestComponent>;
    let loader: HarnessLoader;

    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponent],
        imports: [GioSaveBarModule, ReactiveFormsModule, NoopAnimationsModule],
      });
      fixture = TestBed.createComponent(TestComponent);
      component = fixture.componentInstance;
      loader = TestbedHarnessEnvironment.loader(fixture);
    });

    afterEach(() => {
      jest.clearAllMocks();
    });

    it('should open the save bar', async () => {
      fixture.detectChanges();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toEqual(false);

      component.opened = true;
      fixture.detectChanges();
      expect(await saveBar.isVisible()).toEqual(true);
    });

    it('should send a submit and a reset event', async () => {
      component.opened = true;
      fixture.detectChanges();
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      await saveBar.clickSubmit();
      expect(onSubmitMock).toHaveBeenCalledTimes(1);

      await saveBar.clickReset();
      expect(onResetMock).toHaveBeenCalledTimes(1);
    });
  });

  describe('ReactiveForm usage', () => {
    const onSubmitMock = jest.fn();
    const inputDefaultValue = 'Edit me to display the save bar';
    const inputUpdatedValue = 'A value';
    let aForm: FormGroup;

    @Component({
      template: `
        <form [formGroup]="form" (ngSubmit)="ngSubmit()">
          <input matInput formControlName="anInput" />
          <gio-save-bar [form]="form" [formInitialValues]="formInitialValues"></gio-save-bar>
        </form>
      `,
    })
    class TestComponentWithForm {
      form = aForm;
      formInitialValues = this.form.getRawValue();
      ngSubmit = onSubmitMock;
    }

    let fixture: ComponentFixture<TestComponentWithForm>;
    let loader: HarnessLoader;

    beforeEach(() => {
      aForm = new FormGroup({
        anInput: new FormControl(inputDefaultValue),
      });

      TestBed.configureTestingModule({
        declarations: [TestComponentWithForm],
        imports: [GioSaveBarModule, ReactiveFormsModule, NoopAnimationsModule],
      });
      fixture = TestBed.createComponent(TestComponentWithForm);
      loader = TestbedHarnessEnvironment.loader(fixture);
    });

    afterEach(() => {
      jest.clearAllMocks();
    });

    it('should open the save bar when the form is dirty', async () => {
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toEqual(false);

      const input = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=anInput]' }));
      await input.setValue(inputUpdatedValue);

      expect(await saveBar.isVisible()).toEqual(true);
    });

    it('should trigger form submit event', async () => {
      fixture.detectChanges();
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      const input = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=anInput]' }));
      await input.setValue(inputUpdatedValue);

      await saveBar.clickSubmit();
      expect(onSubmitMock).toHaveBeenCalledTimes(1);
    });

    it('should reset the form', async () => {
      fixture.detectChanges();
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      expect(aForm.getRawValue()).toEqual({
        anInput: inputDefaultValue,
      });

      const input = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=anInput]' }));
      await input.setValue(inputUpdatedValue);

      expect(aForm.getRawValue()).toEqual({
        anInput: inputUpdatedValue,
      });

      await saveBar.clickReset();

      expect(aForm.getRawValue()).toEqual({
        anInput: inputDefaultValue,
      });
      expect(await saveBar.isVisible()).toEqual(false);
    });
  });
});
