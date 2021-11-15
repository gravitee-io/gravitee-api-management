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

import { GioFormTagsInputHarness } from './gio-form-tags-input.harness';
import { GioFormTagsInputModule } from './gio-form-tags-input.module';

@Component({
  template: `
    <mat-form-field appearance="fill">
      <mat-label>My tags</mat-label>
      <gio-form-tags-input
        [required]="required"
        [placeholder]="placeholder"
        [formControl]="tagsControl"
        [tagValidationHook]="tagValidationHook"
      ></gio-form-tags-input>
      <mat-error>Error</mat-error>
    </mat-form-field>
  `,
})
class TestComponent {
  required = false;
  placeholder = 'Add a tag';
  tagValidationHook = undefined;

  tagsControl = new FormControl(null, Validators.required);
}

describe('GioFormTagsInputModule', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioFormTagsInputModule, MatFormFieldModule, ReactiveFormsModule],
    });
    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should display tags from formControl', async () => {
    fixture.detectChanges();

    const formTagsInputHarness = await loader.getHarness(GioFormTagsInputHarness);
    expect(await formTagsInputHarness.getTags()).toEqual([]);

    component.tagsControl.setValue(['tag1', 'tag2']);

    expect(await formTagsInputHarness.getTags()).toEqual(['tag1', 'tag2']);
  });

  it('should add / remove tags to formControl', async () => {
    fixture.detectChanges();

    const formTagsInputHarness = await loader.getHarness(GioFormTagsInputHarness);

    await formTagsInputHarness.addTag('tag1');
    await formTagsInputHarness.addTag('tag2', 'blur');

    expect(await formTagsInputHarness.getTags()).toEqual(['tag1', 'tag2']);
    expect(component.tagsControl.value).toEqual(['tag1', 'tag2']);

    await formTagsInputHarness.removeTag('tag1');

    expect(await formTagsInputHarness.getTags()).toEqual(['tag2']);
    expect(component.tagsControl.value).toEqual(['tag2']);
  });

  it('should add tag with confirm function before added', async () => {
    component.tagValidationHook = (tag: string, validationCb: (shouldAddTag: boolean) => void) => {
      validationCb(tag.startsWith('*'));
    };
    fixture.detectChanges();

    const formTagsInputHarness = await loader.getHarness(GioFormTagsInputHarness);

    await formTagsInputHarness.addTag('*');
    await formTagsInputHarness.addTag('tag2');

    expect(await formTagsInputHarness.getTags()).toEqual(['*']);
    expect(component.tagsControl.value).toEqual(['*']);
  });

  it('should handle error state with control', async () => {
    component.tagsControl.addValidators(Validators.required);
    fixture.detectChanges();

    const formTagsInputHarness = await loader.getHarness(GioFormTagsInputHarness);

    await formTagsInputHarness.addTag('A');
    await formTagsInputHarness.removeTag('A');

    const matFormFieldHarness = await loader.getHarness(MatFormFieldHarness);

    expect(await matFormFieldHarness.hasErrors()).toBe(true);
    expect(component.tagsControl.valid).toEqual(false);
  });

  it('should handle disabled state with control', async () => {
    component.tagsControl.disable();
    fixture.detectChanges();

    const matFormFieldHarness = await loader.getHarness(MatFormFieldHarness);

    expect(await matFormFieldHarness.isDisabled()).toBe(true);
  });

  it('should update error state when control is touched', async () => {
    component.tagsControl.addValidators(Validators.required);
    fixture.detectChanges();
    const matFormFieldHarness = await loader.getHarness(MatFormFieldHarness);

    expect(await matFormFieldHarness.hasErrors()).toBe(false);

    component.tagsControl.markAsTouched();

    expect(await matFormFieldHarness.hasErrors()).toBe(true);
  });
});
