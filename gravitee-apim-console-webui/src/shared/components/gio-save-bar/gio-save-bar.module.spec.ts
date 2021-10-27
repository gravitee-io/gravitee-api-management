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
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatInputHarness } from '@angular/material/input/testing';

import { GioSaveBarModule } from './gio-save-bar.module';
import { GioSaveBarHarness } from './gio-save-bar.harness';

describe('GioFormCardGroupModule', () => {
  describe('simple usage', () => {
    const onResetMock = jest.fn();
    const onSubmitMock = jest.fn();
    const onSubmitInvalidStateMock = jest.fn();

    @Component({
      template: `
        <div>
          <input />
          <gio-save-bar
            [opened]="opened"
            [invalidState]="invalidState"
            (reset)="onReset($event)"
            (submit)="onSubmit($event)"
            (submitInvalidState)="onSubmitInvalidState($event)"
          ></gio-save-bar>
        </div>
      `,
    })
    class TestComponent {
      opened = false;
      invalidState = false;
      onReset = onResetMock;
      onSubmit = onSubmitMock;
      onSubmitInvalidState = onSubmitInvalidStateMock;
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

    it('should invalidate form submit button', async () => {
      fixture.detectChanges();
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      // Visible after opened
      expect(await saveBar.isVisible()).toBeFalsy();
      component.opened = true;
      fixture.detectChanges();
      expect(await saveBar.isVisible()).toBeTruthy();

      // Invalidate button when invalidState input is true
      expect(await saveBar.isSubmitButtonInvalid()).toBeFalsy();
      component.invalidState = true;
      fixture.detectChanges();
      expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();

      // submit output is not triggered when invalidState is true
      await saveBar.clickSubmit();
      expect(onSubmitMock).not.toHaveBeenCalled();
      expect(onSubmitInvalidStateMock).toHaveBeenCalled();
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

    it('should invalidate form submit button', async () => {
      aForm.controls.anInput.addValidators(Validators.required);
      fixture.detectChanges();
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      const input = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=anInput]' }));

      // Visible after a change
      expect(await saveBar.isVisible()).toBeFalsy();
      await input.setValue('New value');
      expect(await saveBar.isVisible()).toBeTruthy();

      // Invalidate button when form is invalid
      expect(await saveBar.isSubmitButtonInvalid()).toBeFalsy();
      await input.setValue('');
      expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();
    });

    it('should markAllAsTouched on submit when form is invalid', async () => {
      aForm.controls.anInput.addValidators(Validators.required);
      aForm.controls.anInput.setValue('');
      fixture.detectChanges();
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      // Open save bar
      aForm.markAsDirty();
      expect(await saveBar.isVisible()).toBeTruthy();

      // touch all form after summit
      expect(aForm.controls.anInput.touched).toEqual(false);
      await saveBar.clickSubmit();
      expect(aForm.controls.anInput.touched).toEqual(true);
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

  describe('use creation mode', () => {
    const onSubmitMock = jest.fn();

    @Component({
      template: `
        <div>
          <input />
          <gio-save-bar creationMode="true" (submit)="onSubmit($event)"></gio-save-bar>
        </div>
      `,
    })
    class TestComponent {
      onSubmit = onSubmitMock;
    }

    let fixture: ComponentFixture<TestComponent>;
    let loader: HarnessLoader;

    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponent],
        imports: [GioSaveBarModule, ReactiveFormsModule, NoopAnimationsModule],
      });
      fixture = TestBed.createComponent(TestComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
    });

    afterEach(() => {
      jest.clearAllMocks();
    });

    it('save bar should be opened by default', async () => {
      fixture.detectChanges();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toEqual(true);
      expect(await saveBar.isResetButtonVisible()).toEqual(false);
    });

    it('should send a submit', async () => {
      fixture.detectChanges();
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      await saveBar.clickSubmit();
      expect(onSubmitMock).toHaveBeenCalledTimes(1);
    });
  });
});
