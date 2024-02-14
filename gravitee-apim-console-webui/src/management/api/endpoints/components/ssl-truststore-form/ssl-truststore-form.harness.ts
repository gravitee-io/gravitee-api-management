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
import { MatLegacyInputHarness as MatInputHarness } from '@angular/material/legacy-input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { keyBy } from 'lodash';

import { TRUSTSTORE_TYPE_LABELS } from './ssl-truststore-form.component';

import { JKSTrustStore, TrustStore } from '../../../../../entities/management-api-v2';

const TRUSTSTORE_TYPE_LABELS_TO_KEY = keyBy(TRUSTSTORE_TYPE_LABELS, 'value');

export class SslTrustStoreFormHarness extends ComponentHarness {
  static hostSelector = 'ssl-truststore-form';

  protected getInputHarness = (formControlName: string) =>
    this.locatorFor(MatInputHarness.with({ selector: `[formControlName="${formControlName}"]` }))();

  protected getType = () => this.locatorFor(MatSelectHarness.with({ selector: `[formControlName="type"]` }))();

  async setJksTrustStore(jks: JKSTrustStore) {
    const type = await this.getType();
    await type.clickOptions({ text: TRUSTSTORE_TYPE_LABELS_TO_KEY['JKS'].label });

    const passwordInput = await this.getInputHarness('jksPassword');
    await passwordInput.setValue(jks.password);

    if (jks.path) {
      const pathInput = await this.getInputHarness('jksPath');
      await pathInput.setValue(jks.path);
    }

    if (jks.content) {
      const contentInput = await this.getInputHarness('jksContent');
      await contentInput.setValue(jks.content);
    }
  }

  async getValues(): Promise<TrustStore> {
    const type = await this.getType();
    const typeValue = await type.getValueText();

    switch (typeValue) {
      case TRUSTSTORE_TYPE_LABELS_TO_KEY['JKS'].label: {
        const passwordInput = await this.getInputHarness('jksPassword');
        const password = await passwordInput.getValue();
        const pathInput = await this.getInputHarness('jksPath');
        const path = await pathInput.getValue();
        const contentInput = await this.getInputHarness('jksContent');
        const content = await contentInput.getValue();
        return {
          type: TRUSTSTORE_TYPE_LABELS_TO_KEY['JKS'].value,
          password,
          path,
          content,
        };
      }
      case TRUSTSTORE_TYPE_LABELS_TO_KEY['PEM'].label: {
        const pemPathInput = await this.getInputHarness('pemPath');
        const pemPath = await pemPathInput.getValue();
        const pemContentInput = await this.getInputHarness('pemContent');
        const pemContent = await pemContentInput.getValue();
        return {
          type: TRUSTSTORE_TYPE_LABELS_TO_KEY['PEM'].value,
          path: pemPath,
          content: pemContent,
        };
      }
      case TRUSTSTORE_TYPE_LABELS_TO_KEY['PKCS12'].label: {
        const pkcs12PasswordInput = await this.getInputHarness('pkcs12Password');
        const pkcs12Password = await pkcs12PasswordInput.getValue();
        const pkcs12PathInput = await this.getInputHarness('pkcs12Path');
        const pkcs12Path = await pkcs12PathInput.getValue();
        const pkcs12ContentInput = await this.getInputHarness('pkcs12Content');
        const pkcs12Content = await pkcs12ContentInput.getValue();
        return {
          type: TRUSTSTORE_TYPE_LABELS_TO_KEY['PKCS12'].value,
          password: pkcs12Password,
          path: pkcs12Path,
          content: pkcs12Content,
        };
      }
      case TRUSTSTORE_TYPE_LABELS_TO_KEY['NONE'].label: {
        return {
          type: TRUSTSTORE_TYPE_LABELS_TO_KEY['NONE'].value,
        };
      }
      default:
        throw new Error(`Unknown type ${typeValue}`);
    }
  }
}
