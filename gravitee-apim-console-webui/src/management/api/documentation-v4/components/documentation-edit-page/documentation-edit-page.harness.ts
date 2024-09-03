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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatTabHarness } from '@angular/material/tabs/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class DocumentationEditPageHarness extends ComponentHarness {
  public static hostSelector = 'documentation-edit-page';

  private locateDeleteButton = this.locatorForOptional(MatButtonHarness.with({ text: 'Delete page' }));
  private locatePublishChangesButton = this.locatorFor(MatButtonHarness.with({ text: 'Publish changes' }));
  private locateConfigurePageTab = this.locatorFor(MatTabHarness.with({ label: 'Configure Page' }));
  private locateContentTab = this.locatorFor(MatTabHarness.with({ label: 'Content' }));
  private locateOpenApiConfigurationTab = this.locatorFor(MatTabHarness.with({ label: 'Configure OpenAPI Viewer' }));
  private locateEntrypointsAsServersToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-test-id="entrypoints-as-servers-toggle"]' }),
  );
  private locateContextPathAsServerToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-test-id="context-path-as-server-toggle"]' }),
  );
  private locateBaseUrlInput = this.locatorFor(MatInputHarness.with({ selector: '[data-test-id="base-url"]' }));
  private locateViewerSelect = this.locatorFor(MatSelectHarness.with({ selector: '[data-test-id="open-api-viewer"]' }));
  private locateTryItToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-test-id="try-it"]' }));
  private locateTryItAnonymousToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-test-id="try-it-anonymous"]' }));
  private locateShowUrlToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-test-id="show-url"]' }));
  private locateDisplayOperationIdToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-test-id="display-operation-id"]' }),
  );
  private locateUsePkceToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-test-id="use-pkce"]' }));
  private locateEnableFilteringToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-test-id="enable-filtering"]' }));
  private locateShowExtensionsToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-test-id="show-extensions"]' }));
  private locateShowCommonExtensionsToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-test-id="show-common-extensions"]' }),
  );
  private locateDocExpansionSelect = this.locatorFor(MatSelectHarness.with({ selector: '[data-test-id="doc-expansion"]' }));
  private locateMaxOperationsDisplayedInput = this.locatorFor(MatInputHarness.with({ selector: '[data-test-id="max-displayed-tags"]' }));

  async getDeleteButton(): Promise<MatButtonHarness | undefined> {
    return await this.locateDeleteButton();
  }

  async getPublishChangesButton(): Promise<MatButtonHarness> {
    return await this.locatePublishChangesButton();
  }

  async openConfigurePageTab(): Promise<void> {
    return await this.locateConfigurePageTab().then((tab) => tab.select());
  }

  async openContentTab(): Promise<void> {
    return await this.locateContentTab().then((tab) => tab.select());
  }

  async openOpenApiConfigurationTab(): Promise<void> {
    return await this.locateOpenApiConfigurationTab().then((tab) => tab.select());
  }

  /**
   * OpenAPI viewer configuration
   */
  async getEntrypointsAsServersToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateEntrypointsAsServersToggle();
  }

  async getBaseUrlInput(): Promise<MatInputHarness> {
    return await this.locateBaseUrlInput();
  }

  async getContextPathAsServerToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateContextPathAsServerToggle();
  }

  async getOpenApiViewerSelect(): Promise<MatSelectHarness> {
    return await this.locateViewerSelect();
  }

  async getTryItToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateTryItToggle();
  }

  async getTryItAnonymousToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateTryItAnonymousToggle();
  }

  async getShowUrlToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateShowUrlToggle();
  }

  async getDisplayOperationIdToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateDisplayOperationIdToggle();
  }

  async getUsePkceToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateUsePkceToggle();
  }

  async getEnableFilteringToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateEnableFilteringToggle();
  }

  async getShowExtensionsToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateShowExtensionsToggle();
  }

  async getShowCommonExtensionsToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateShowCommonExtensionsToggle();
  }

  async getDocExpansionSelect(): Promise<MatSelectHarness> {
    return await this.locateDocExpansionSelect();
  }

  async getMaxOperationsDisplayedInput(): Promise<MatInputHarness> {
    return await this.locateMaxOperationsDisplayedInput();
  }
}
