/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { MatErrorHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { keyBy } from 'lodash';

import { KEYSTORE_TYPE_LABELS } from './ssl-key-store.model';
import { SslKeyStore } from '../../../../../../entities/ssl';

const KEYSTORE_TYPE_LABELS_TO_KEY = keyBy(KEYSTORE_TYPE_LABELS, 'value');

export class SslKeyStoreHarness extends ComponentHarness {
  static hostSelector = 'app-ssl-keystore';

  async selectType(type: string) {
    const typeSelect = await this.getType();
    return typeSelect.clickOptions({ text: KEYSTORE_TYPE_LABELS_TO_KEY[type].label });
  }

  async setInputValueFromFormControlName(formControlName: string, value: string) {
    const input = await this.getInputHarness(formControlName);
    return input.setValue(value);
  }

  async getError(): Promise<string> {
    const matError = await this.locatorFor(MatErrorHarness)();
    return matError.getText();
  }

  async getValues(): Promise<SslKeyStore> {
    const type = await this.getType();
    const typeValue = await type.getValueText();

    switch (typeValue) {
      case KEYSTORE_TYPE_LABELS_TO_KEY['JKS'].label: {
        const jksPasswordInput = await this.getInputHarness('jksPassword');
        const jksPassword = await jksPasswordInput.getValue();
        const jksPathInput = await this.getInputHarness('jksPath');
        const jksPath = await jksPathInput.getValue();
        const jksContentInput = await this.getInputHarness('jksContent');
        const jksContent = await jksContentInput.getValue();
        const jksAliasInput = await this.getInputHarness('jksAlias');
        const jksAlias = await jksAliasInput.getValue();
        const jksKeyPasswordInput = await this.getInputHarness('jksKeyPassword');
        const jksKeyPassword = await jksKeyPasswordInput.getValue();
        return {
          type: KEYSTORE_TYPE_LABELS_TO_KEY['JKS'].value,
          password: jksPassword,
          path: jksPath,
          content: jksContent,
          alias: jksAlias,
          keyPassword: jksKeyPassword,
        };
      }
      case KEYSTORE_TYPE_LABELS_TO_KEY['PKCS12'].label: {
        const pkcs12PasswordInput = await this.getInputHarness('pkcs12Password');
        const pkcs12Password = await pkcs12PasswordInput.getValue();
        const pkcs12PathInput = await this.getInputHarness('pkcs12Path');
        const pkcs12Path = await pkcs12PathInput.getValue();
        const pkcs12ContentInput = await this.getInputHarness('pkcs12Content');
        const pkcs12Content = await pkcs12ContentInput.getValue();
        const pkcs12AliasInput = await this.getInputHarness('pkcs12Alias');
        const pkcs12Alias = await pkcs12AliasInput.getValue();
        const pkcs12KeyPasswordInput = await this.getInputHarness('pkcs12KeyPassword');
        const pkcs12KeyPassword = await pkcs12KeyPasswordInput.getValue();
        return {
          type: KEYSTORE_TYPE_LABELS_TO_KEY['PKCS12'].value,
          password: pkcs12Password,
          path: pkcs12Path,
          content: pkcs12Content,
          alias: pkcs12Alias,
          keyPassword: pkcs12KeyPassword,
        };
      }
      case KEYSTORE_TYPE_LABELS_TO_KEY['PEM'].label: {
        const pemKeyPathInput = await this.getInputHarness('pemKeyPath');
        const pemKeyPath = await pemKeyPathInput.getValue();
        const pemKeyContentInput = await this.getInputHarness('pemKeyContent');
        const pemKeyContent = await pemKeyContentInput.getValue();
        const pemCertPathInput = await this.getInputHarness('pemCertPath');
        const pemCertPath = await pemCertPathInput.getValue();
        const pemCertContentInput = await this.getInputHarness('pemCertContent');
        const pemCertContent = await pemCertContentInput.getValue();
        return {
          type: KEYSTORE_TYPE_LABELS_TO_KEY['PEM'].value,
          keyPath: pemKeyPath,
          keyContent: pemKeyContent,
          certPath: pemCertPath,
          certContent: pemCertContent,
        };
      }
      case KEYSTORE_TYPE_LABELS_TO_KEY['NONE'].label: {
        return {
          type: KEYSTORE_TYPE_LABELS_TO_KEY['NONE'].value,
        };
      }
      default:
        throw new Error(`Unknown type ${typeValue}`);
    }
  }

  private getInputHarness(formControlName: string) {
    return this.locatorFor(MatInputHarness.with({ selector: `[formControlName="${formControlName}"]` }))();
  }

  private getType() {
    return this.locatorFor(MatSelectHarness.with({ selector: `[formControlName="type"]` }))();
  }
}
