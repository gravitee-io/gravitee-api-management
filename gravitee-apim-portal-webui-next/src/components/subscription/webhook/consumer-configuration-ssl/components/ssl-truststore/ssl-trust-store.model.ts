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

import { SslTrustStoreType } from '../../../../../../entities/ssl';

export type SslTruststoreFormValue = {
  type: SslTrustStoreType;
  jksPath?: string;
  jksContent?: string;
  jksPassword?: string;
  pkcs12Path?: string;
  pkcs12Content?: string;
  pkcs12Password?: string;
  pemPath?: string;
  pemContent?: string;
};

export const TRUSTSTORE_TYPE_LABELS: { label: string; value: SslTrustStoreType }[] = [
  {
    label: 'None',
    value: '',
  },
  {
    label: 'Java Trust Store (.jks)',
    value: 'JKS',
  },
  {
    label: 'PKCS#12 (.p12) / PFX (.pfx)',
    value: 'PKCS12',
  },
  {
    label: 'PEM (.pem)',
    value: 'PEM',
  },
];
