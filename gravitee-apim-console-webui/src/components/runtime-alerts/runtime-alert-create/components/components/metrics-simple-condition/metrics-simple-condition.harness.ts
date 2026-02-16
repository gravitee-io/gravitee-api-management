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
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatInputHarness } from '@angular/material/input/testing';

export class MetricsSimpleConditionHarness extends ComponentHarness {
  static readonly hostSelector = 'metrics-simple-condition';

  private getMetricsSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="metric"]' }));
  private getTypeSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="type"]' }));
  private getOperatorSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="operator"]' }));
  private getThresholdInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="threshold"]' }));
  private getLowThresholdInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="lowThreshold"]' }));
  private getHighThresholdInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="highThreshold"]' }));
  private getMultiplierInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="multiplier"]' }));
  private getPropertySelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="property"]' }));
  private getReferenceSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="pattern"]' }));
  private getReferenceInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="pattern"]' }));

  public async getMetricOptions() {
    return this.getOptions(await this.getMetricsSelect());
  }

  public async selectMetric(text: string) {
    return this.selectOption(await this.getMetricsSelect(), text);
  }

  public async getSelectedMetric() {
    return this.getSelectedOption(await this.getMetricsSelect());
  }

  public async getTypeOptions() {
    return this.getTypeSelect().then(async select => this.getOptions(select));
  }

  public async isTypeSelectDisabled() {
    return this.getTypeSelect().then(select => select.isDisabled());
  }

  public async selectType(text: string) {
    return this.selectOption(await this.getTypeSelect(), text);
  }

  public async getSelectedType() {
    return this.getSelectedOption(await this.getTypeSelect());
  }

  public async getOperatorOptions() {
    return this.getOptions(await this.getOperatorSelect());
  }

  public async selectOperator(text: string) {
    return this.selectOption(await this.getOperatorSelect(), text);
  }

  public async getSelectedOperator() {
    return this.getSelectedOption(await this.getOperatorSelect());
  }

  public async setThresholdValue(value: string) {
    return this.getThresholdInput().then(input => input.setValue(value));
  }

  public async getThresholdValue() {
    return this.getThresholdInput().then(input => input.getValue());
  }

  public async setLowThresholdValue(value: string) {
    return this.getLowThresholdInput().then(input => input.setValue(value));
  }

  public async getLowThresholdValue() {
    return this.getLowThresholdInput().then(input => input.getValue());
  }

  public async setHighThresholdValue(value: string) {
    return this.getHighThresholdInput().then(input => input.setValue(value));
  }

  public async getMultiplierValue() {
    return this.getMultiplierInput().then(input => input.getValue());
  }

  public async setMultiplierValue(value: string) {
    return this.getMultiplierInput().then(input => input.setValue(value));
  }

  public async isHighThresholdInvalid() {
    return this.getHighThresholdInput()
      .then(input => input.host())
      .then(host => host.hasClass('ng-invalid'));
  }

  public async getHighThresholdValue() {
    return this.getHighThresholdInput().then(input => input.getValue());
  }

  public async getPropertyOptions() {
    return this.getOptions(await this.getPropertySelect());
  }

  public async selectProperty(text: string) {
    return this.selectOption(await this.getPropertySelect(), text);
  }

  public async getSelectedProperty() {
    return this.getSelectedOption(await this.getPropertySelect());
  }

  public async getReferenceOptions() {
    return this.getOptions(await this.getReferenceSelect());
  }

  public async selectReference(text: string) {
    return this.selectOption(await this.getReferenceSelect(), text);
  }

  public async getSelectedReference() {
    return this.getSelectedOption(await this.getReferenceSelect());
  }

  public async getReferenceValue() {
    return this.getReferenceInput().then(input => input.getValue());
  }

  public async setReferenceValue(text: string) {
    return this.getReferenceInput().then(input => input.setValue(text));
  }

  private async getOptions(select: MatSelectHarness) {
    await select.open();
    const options = await select.getOptions();
    return Promise.all(options.map(async o => o.getText()));
  }

  private async getSelectedOption(select: MatSelectHarness) {
    return select.getValueText();
  }

  private async selectOption(select: MatSelectHarness, text: string) {
    return select.clickOptions({ text });
  }
}
