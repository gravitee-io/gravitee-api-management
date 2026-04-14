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

export type ApiImportV4Format = 'gravitee' | 'openapi' | 'wsdl';
export type ApiImportV4FileSourceType = 'local' | 'remote';

export interface ApiImportV4WizardPayload {
  apiFormat: ApiImportV4Format;
  fileSourceType: ApiImportV4FileSourceType;
  fileContent: string | null;
  remoteUrl: string | null;
  remoteAuthorizationHeader: string | null;
  createDocPage: boolean;
  addSpecValidation: boolean;
  detectedImportType: string | undefined;
}

export interface ApiImportV4DialogData {
  apiId: string;
  apiName: string;
}
