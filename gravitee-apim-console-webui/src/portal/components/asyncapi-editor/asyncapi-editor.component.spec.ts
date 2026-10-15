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
import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AsyncApiEditorComponent } from './asyncapi-editor.component';
import { AsyncApiEditorHarness } from './asyncapi-editor.harness';

@Component({
  template: ` <asyncapi-editor [formControl]="contentControl" [showPreview]="showPreview" /> `,
  standalone: true,
  imports: [AsyncApiEditorComponent, ReactiveFormsModule],
})
class TestHostComponent {
  @ViewChild(AsyncApiEditorComponent) asyncApiEditor!: AsyncApiEditorComponent;

  contentControl = new FormControl<string | null>('asyncapi: 3.0.0');
  showPreview = true;
}

describe('AsyncApiEditorComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it('should create and render', async () => {
    const asyncApiEditor = await loader.getHarness(AsyncApiEditorHarness);

    expect(asyncApiEditor).toBeTruthy();
    expect(await asyncApiEditor.hasMonacoEditor()).toBe(true);
    expect(await asyncApiEditor.getPreview()).not.toBeNull();
  });

  it('should default to an empty value when writeValue receives null', async () => {
    host.contentControl.setValue(null);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(host.asyncApiEditor._value).toBe('');
  });

  it('should round-trip value changes through the form control', async () => {
    const asyncApiEditor = await loader.getHarness(AsyncApiEditorHarness);

    expect(await asyncApiEditor.getValue()).toBe('asyncapi: 3.0.0');

    host.contentControl.setValue('asyncapi: 3.0.0 - updated');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(await asyncApiEditor.getValue()).toBe('asyncapi: 3.0.0 - updated');

    await asyncApiEditor.setValue('asyncapi: 3.0.0 - from editor');

    expect(host.contentControl.value).toBe('asyncapi: 3.0.0 - from editor');
  });

  it('should disable itself and propagate disabled state to gio-monaco-editor', async () => {
    const asyncApiEditor = await loader.getHarness(AsyncApiEditorHarness);

    host.contentControl.disable();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(host.asyncApiEditor.disabled).toBe(true);
    expect(await asyncApiEditor.isMonacoEditorDisabled()).toBe(true);
  });

  describe('preview content', () => {
    const avroContent = `asyncapi: 3.0.0
info: { title: Users, version: 1.0.0 }
channels:
  userSignedUp:
    messages:
      UserSignedUp:
        payload:
          schemaFormat: application/vnd.apache.avro;version=1.9.0
          schema: { type: record, name: UserSignedUp, fields: [{ name: userId, type: string }] }
`;
    const xsdContent = `asyncapi: 3.0.0
info: { title: Orders, version: 1.0.0 }
channels:
  orderCreated:
    messages:
      OrderCreated:
        payload:
          schemaFormat: application/xml
          schema: '<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>'
`;

    it('should preview an Avro payload as written', async () => {
      host.contentControl.setValue(avroContent);
      fixture.detectChanges();

      const asyncApiEditor = await loader.getHarness(AsyncApiEditorHarness);
      expect(await asyncApiEditor.getPreviewSchema()).toEqual(avroContent);
    });

    it('should preview an XML Schema payload as its quoted schema', async () => {
      host.contentControl.setValue(xsdContent);
      fixture.detectChanges();

      const asyncApiEditor = await loader.getHarness(AsyncApiEditorHarness);
      const payload = JSON.parse(await asyncApiEditor.getPreviewSchema()).channels.orderCreated.messages.OrderCreated.payload;
      expect(payload.schemaFormat).toBeUndefined();
      expect(payload.type).toEqual('string');
      expect(payload.description).toContain('<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>');
    });
  });

  describe('showPreview input', () => {
    it('should render asyncapi-component when showPreview is true (default)', async () => {
      const asyncApiEditor = await loader.getHarness(AsyncApiEditorHarness);
      expect(await asyncApiEditor.getPreview()).not.toBeNull();
    });

    it('should not render asyncapi-component when showPreview is false', async () => {
      host.showPreview = false;
      fixture.detectChanges();

      const asyncApiEditor = await loader.getHarness(AsyncApiEditorHarness);
      expect(await asyncApiEditor.getPreview()).toBeNull();
    });
  });
});
