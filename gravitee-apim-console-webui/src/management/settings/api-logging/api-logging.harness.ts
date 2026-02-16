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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
export class ApiLoggingHarness extends ComponentHarness {
  static hostSelector = 'api-logging';

  private readonly getSaveButton = this.locatorFor(GioSaveBarHarness);

  private readonly getMaxDurationFormField = this.locatorFor(MatFormFieldHarness.with({ floatingLabelText: 'Max Duration (in ms)' }));

  private readonly getAuditEnabledSlideToggle = this.locatorFor(MatSlideToggleHarness.with({ name: 'enabled' }));
  private readonly getAuditTrailEnabledSlideToggle = this.locatorFor(MatSlideToggleHarness.with({ name: 'trail.enabled' }));

  private readonly getUserDisplayedSlideToggle = this.locatorFor(MatSlideToggleHarness.with({ name: 'displayed' }));

  private readonly getSamplingCountDefaultField = this.locatorFor(MatFormFieldHarness.with({ selector: '[data-testid=count_default]' }));
  private readonly getSamplingCountLimitField = this.locatorFor(MatFormFieldHarness.with({ selector: '[data-testid=count_limit]' }));

  private readonly getSamplingProbabilisticDefaultField = this.locatorFor(
    MatFormFieldHarness.with({ selector: '[data-testid=probabilistic_default]' }),
  );
  private readonly getSamplingProbabilisticLimitField = this.locatorFor(
    MatFormFieldHarness.with({ selector: '[data-testid=probabilistic_limit]' }),
  );

  private readonly getSamplingTemporalDefaultField = this.locatorFor(
    MatFormFieldHarness.with({ selector: '[data-testid=temporal_default]' }),
  );
  private readonly getSamplingTemporalLimitField = this.locatorFor(MatFormFieldHarness.with({ selector: '[data-testid=temporal_limit]' }));
  private readonly getSamplingWindowedCountDefaultField = this.locatorFor(
    MatFormFieldHarness.with({ selector: '[data-testid=windowedCount_default]' }),
  );
  private readonly getSamplingWindowedCountLimitField = this.locatorFor(
    MatFormFieldHarness.with({ selector: '[data-testid=windowedCount_limit]' }),
  );

  public saveSettings = async (): Promise<void> => {
    return this.getSaveButton().then(saveButton => saveButton.clickSubmit());
  };

  public isSaveButtonInvalid = async (): Promise<boolean> => {
    return this.getSaveButton().then(saveButton => saveButton.isSubmitButtonInvalid());
  };

  public isMaxDurationFieldDisabled = async (): Promise<boolean> => {
    return this.isFormFieldDisabled(this.getMaxDurationFormField());
  };

  public setMaxDuration = async (value: string): Promise<void> => {
    return this.setValueFor(this.getMaxDurationFormField(), value);
  };

  public isAuditEnabledToggleDisabled = async (): Promise<boolean> => {
    return this.isToggleDisabled(this.getAuditEnabledSlideToggle());
  };

  public toggleAuditEnabled = async (): Promise<void> => {
    return this.getAuditEnabledSlideToggle().then(toggle => toggle.toggle());
  };

  public isAuditTrailEnabledToggleDisabled = async (): Promise<boolean> => {
    return this.isToggleDisabled(this.getAuditTrailEnabledSlideToggle());
  };

  public toggleAuditTrailEnabled = async (): Promise<void> => {
    return this.getAuditTrailEnabledSlideToggle().then(toggle => toggle.toggle());
  };

  public isUserDisplayedToggleDisabled = async (): Promise<boolean> => {
    return this.isToggleDisabled(this.getUserDisplayedSlideToggle());
  };

  public toggleUserDisplayed = async (): Promise<void> => {
    return this.getUserDisplayedSlideToggle().then(toggle => toggle.toggle());
  };

  public setCountDefault = async (value: string): Promise<void> => {
    return this.setValueFor(this.getSamplingCountDefaultField(), value);
  };

  public countDefaultHasErrors = async (): Promise<boolean> => {
    return this.fieldHasErrors(this.getSamplingCountDefaultField());
  };

  public countDefaultIsDisabled = async (): Promise<boolean> => {
    return this.isFormFieldDisabled(this.getSamplingCountDefaultField());
  };

  public countDefaultTextErrors = async (): Promise<string[]> => {
    return this.getTextErrorsFor(this.getSamplingCountDefaultField());
  };

  public setCountLimit = async (value: string): Promise<void> => {
    return this.setValueFor(this.getSamplingCountLimitField(), value);
  };

  public countLimitHasErrors = async (): Promise<boolean> => {
    return this.fieldHasErrors(this.getSamplingCountLimitField());
  };

  public countLimitIsDisabled = async (): Promise<boolean> => {
    return this.isFormFieldDisabled(this.getSamplingCountLimitField());
  };

  public countLimitTextErrors = async (): Promise<string[]> => {
    return this.getTextErrorsFor(this.getSamplingCountLimitField());
  };

  public setProbabilisticDefault = async (value: string): Promise<void> => {
    return this.setValueFor(this.getSamplingProbabilisticDefaultField(), value);
  };

  public probabilisticDefaultHasErrors = async (): Promise<boolean> => {
    return this.fieldHasErrors(this.getSamplingProbabilisticDefaultField());
  };

  public probabilisticDefaultIsDisabled = async (): Promise<boolean> => {
    return this.isFormFieldDisabled(this.getSamplingProbabilisticDefaultField());
  };

  public probabilisticDefaultTextErrors = async (): Promise<string[]> => {
    return this.getTextErrorsFor(this.getSamplingProbabilisticDefaultField());
  };

  public setProbabilisticLimit = async (value: string): Promise<void> => {
    return this.setValueFor(this.getSamplingProbabilisticLimitField(), value);
  };

  public probabilisticLimitHasErrors = async (): Promise<boolean> => {
    return this.fieldHasErrors(this.getSamplingProbabilisticLimitField());
  };

  public probabilisticLimitIsDisabled = async (): Promise<boolean> => {
    return this.isFormFieldDisabled(this.getSamplingProbabilisticLimitField());
  };

  public probabilisticLimitTextErrors = async (): Promise<string[]> => {
    return this.getTextErrorsFor(this.getSamplingProbabilisticLimitField());
  };

  public setTemporalDefault = async (value: string): Promise<void> => {
    return this.setValueFor(this.getSamplingTemporalDefaultField(), value);
  };

  public temporalDefaultHasErrors = async (): Promise<boolean> => {
    return this.fieldHasErrors(this.getSamplingTemporalDefaultField());
  };

  public temporalDefaultIsDisabled = async (): Promise<boolean> => {
    return this.isFormFieldDisabled(this.getSamplingTemporalDefaultField());
  };

  public temporalDefaultTextErrors = async (): Promise<string[]> => {
    return this.getTextErrorsFor(this.getSamplingTemporalDefaultField());
  };

  public setTemporalLimit = async (value: string): Promise<void> => {
    return this.setValueFor(this.getSamplingTemporalLimitField(), value);
  };

  public temporalLimitHasErrors = async (): Promise<boolean> => {
    return this.fieldHasErrors(this.getSamplingTemporalLimitField());
  };

  public temporalLimitIsDisabled = async (): Promise<boolean> => {
    return this.isFormFieldDisabled(this.getSamplingTemporalLimitField());
  };

  public temporalLimitTextErrors = async (): Promise<string[]> => {
    return this.getTextErrorsFor(this.getSamplingTemporalLimitField());
  };
  public setWindowedCountDefault = async (value: string): Promise<void> => {
    return this.setValueFor(this.getSamplingWindowedCountDefaultField(), value);
  };

  public windowedCountDefaultHasErrors = async (): Promise<boolean> => {
    return this.fieldHasErrors(this.getSamplingWindowedCountDefaultField());
  };

  public windowedCountDefaultIsDisabled = async (): Promise<boolean> => {
    return this.isFormFieldDisabled(this.getSamplingWindowedCountDefaultField());
  };

  public windowedCountDefaultTextErrors = async (): Promise<string[]> => {
    return this.getTextErrorsFor(this.getSamplingWindowedCountDefaultField());
  };

  public setWindowedCountLimit = async (value: string): Promise<void> => {
    return this.setValueFor(this.getSamplingWindowedCountLimitField(), value);
  };

  public windowedCountLimitHasErrors = async (): Promise<boolean> => {
    return this.fieldHasErrors(this.getSamplingWindowedCountLimitField());
  };

  public windowedCountLimitIsDisabled = async (): Promise<boolean> => {
    return this.isFormFieldDisabled(this.getSamplingWindowedCountLimitField());
  };

  public windowedCountLimitTextErrors = async (): Promise<string[]> => {
    return this.getTextErrorsFor(this.getSamplingWindowedCountLimitField());
  };

  private setValueFor = async (formFieldPromise: Promise<MatFormFieldHarness>, value: string): Promise<void> => {
    return formFieldPromise.then(formField => formField.getControl(MatInputHarness)).then(input => input.setValue(value));
  };

  private fieldHasErrors = async (formFieldPromise: Promise<MatFormFieldHarness>): Promise<boolean> => {
    return formFieldPromise.then(formField => formField.hasErrors());
  };

  private getTextErrorsFor = async (formFieldPromise: Promise<MatFormFieldHarness>): Promise<string[]> => {
    return formFieldPromise.then(formField => formField.getTextErrors());
  };

  private isToggleDisabled = async (togglePromise: Promise<MatSlideToggleHarness>) => {
    return togglePromise.then(formField => formField.isDisabled());
  };

  private isFormFieldDisabled = async (formFieldPromise: Promise<MatFormFieldHarness>) => {
    return formFieldPromise.then(formField => formField.isDisabled());
  };
}
