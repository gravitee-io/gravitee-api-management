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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatDatepickerInputHarness } from '@angular/material/datepicker/testing';

import { MetadataFormat } from '../../../entities/metadata/metadata';

export class GioMetadataDialogHarness extends ComponentHarness {
  public static hostSelector = 'gio-metadata-dialog';

  protected getKeyField = this.locatorFor(MatInputHarness.with({ selector: '#key' }));
  protected getNameField = this.locatorFor(MatInputHarness.with({ selector: '#name' }));
  protected getFormatField = this.locatorFor(MatSelectHarness);
  protected getValueStringField = this.locatorFor(MatInputHarness.with({ selector: '#value-string' }));
  protected getValueMailField = this.locatorFor(MatInputHarness.with({ selector: '#value-mail' }));
  protected getValueNumericField = this.locatorFor(MatInputHarness.with({ selector: '#value-numeric' }));

  protected getValueUrlField = this.locatorFor(MatInputHarness.with({ selector: '#value-url' }));
  protected getValueDateField = this.locatorFor(MatDatepickerInputHarness);
  protected getValueBooleanField = this.locatorFor(MatSelectHarness.with({ selector: '#value-boolean' }));
  protected getSaveButton = this.locatorFor(MatButtonHarness.with({ text: 'Save' }));

  async saveButtonExists(): Promise<boolean> {
    return this.getSaveButton().then(btn => (btn ? true : false));
  }

  async saveButtonEnabled(): Promise<boolean> {
    return this.getSaveButton()
      .then(btn => btn.isDisabled())
      .then(disabled => !disabled);
  }

  async clickSave(): Promise<void> {
    return this.getSaveButton().then(btn => btn.click());
  }

  async keyFieldExists() {
    return this.getKeyField().then(x => (x ? true : false));
  }

  async nameFieldExists() {
    return this.getNameField().then(x => (x ? true : false));
  }

  async formatFieldExists() {
    return this.getFormatField().then(x => (x ? true : false));
  }

  async valueStringFieldExists() {
    return this.getValueStringField().then(x => (x ? true : false));
  }

  async getValueStringFieldValue(): Promise<string> {
    return this.getValueStringField().then(field => field.getValue());
  }

  async getValueUrlFieldValue(): Promise<string> {
    return this.getValueUrlField().then(field => field.getValue());
  }

  async getValueMailFieldValue(): Promise<string> {
    return this.getValueMailField().then(field => field.getValue());
  }

  async getValueDatePicker(): Promise<MatDatepickerInputHarness> {
    return this.getValueDateField();
  }

  async getValueBooleanSelect(): Promise<MatSelectHarness> {
    return this.getValueBooleanField();
  }

  async fillOutName(name: string) {
    return this.getNameField().then(input => input.setValue(name));
  }
  async selectFormat(format: string) {
    await this.getFormatField().then(select => select.open());
    return this.getFormatField()
      .then(select => select.getOptions({ text: format }))
      .then(options => options[0].click());
  }
  async fillOutValue(format: MetadataFormat, value: string) {
    switch (format) {
      case 'NUMERIC':
        return this.getValueNumericField().then(input => input.setValue(value));
      case 'URL':
        return this.getValueUrlField().then(input => input.setValue(value));
      case 'MAIL':
        return this.getValueMailField().then(input => input.setValue(value));
      default:
        return this.getValueStringField().then(input => input.setValue(value));
    }
  }

  async canSaveForm(): Promise<boolean> {
    return this.getSaveButton()
      .then(btn => btn.isDisabled())
      .then(isDisabled => !isDisabled);
  }
}
