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
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { GioFormColorInputHarness } from './gio-form-color-input.harness';
import { GioFormColorInputModule } from './gio-form-color-input.module';

@Component({
  template: `
    <mat-form-field>
      <mat-label>My color</mat-label>
      <gio-form-color-input [required]="required" [placeholder]="placeholder" [formControl]="colorControl"></gio-form-color-input>
      <mat-error *ngIf="colorControl.hasError('color')">
        {{ colorControl.getError('color').message }}
      </mat-error>
      <mat-error *ngIf="!colorControl.hasError('color')"> Has Error </mat-error>
    </mat-form-field>
  `,
  standalone: false,
})
class TestComponent {
  required = false;
  placeholder = 'Select color';

  colorControl = new FormControl(null);
}

describe('GioFormColorInputModule', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioFormColorInputModule, MatFormFieldModule, ReactiveFormsModule],
    });
    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should display formControl color', async () => {
    const formColorInput = await loader.getHarness(GioFormColorInputHarness.with({ selector: 'gio-form-color-input' }));

    expect(await formColorInput.getValue()).toEqual('');
    expect(component.colorControl.dirty).toBeFalsy();
    expect(component.colorControl.touched).toBeFalsy();
    expect(component.colorControl.invalid).toBeFalsy();

    component.colorControl.setValue('#ff0000');
    expect(await formColorInput.getValue()).toEqual('#ff0000');
    expect(component.colorControl.dirty).toBeTruthy();
    expect(component.colorControl.touched).toBeFalsy();
    expect(component.colorControl.invalid).toBeFalsy();
  });

  it('should display change formControl color', async () => {
    const formColorInput = await loader.getHarness(GioFormColorInputHarness.with({ selector: 'gio-form-color-input' }));

    expect(await formColorInput.getValue()).toEqual('');

    await formColorInput.setValue('#ff0000');

    expect(await formColorInput.getValue()).toEqual('#ff0000');
    expect(component.colorControl.value).toEqual('#ff0000');
  });

  it('should disable', async () => {
    const formColorInput = await loader.getHarness(GioFormColorInputHarness.with({ selector: 'gio-form-color-input' }));

    component.colorControl.setValue('#ff0000');

    component.colorControl.disable();
    expect(await formColorInput.isDisabled()).toEqual(true);

    component.colorControl.enable();
    expect(await formColorInput.isDisabled()).toEqual(false);

    component.colorControl.disable();
    expect(await formColorInput.isDisabled()).toEqual(true);
  });

  it('should validate', async () => {
    const formField = await loader.getHarness(MatFormFieldHarness);
    const formColorInput = await formField.getControl<GioFormColorInputHarness>(GioFormColorInputHarness);

    component.colorControl.setValue('#ff0000');

    expect(await formColorInput.getValue()).toEqual('#ff0000');

    await formColorInput.setValue('🦊');

    expect(await formField.hasErrors()).toEqual(true);
    expect(await formField.getTextErrors()).toEqual(['"🦊" is not a valid color']);
  });

  it('should update error state when control is touched', async () => {
    component.colorControl.addValidators(Validators.required);
    component.required = true;

    const matFormFieldHarness = await loader.getHarness(MatFormFieldHarness);

    expect(await matFormFieldHarness.hasErrors()).toBe(false);

    component.colorControl.markAsTouched();

    expect(await matFormFieldHarness.hasErrors()).toBe(true);
  });
});
