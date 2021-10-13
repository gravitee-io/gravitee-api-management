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
import { Installation } from './installation';

export function fakeInstallation(attributes?: Partial<Installation>): Installation {
  const base: Installation = {
    id: '5fba3da7-17e1-4064-ba3d-a717e1906425',
    cockpitURL: 'https://cockpit.gravitee.io',
    additionalInformation: {},
    createdAt: 1629888106121,
    updatedAt: 1629888106121,
  };

  return {
    ...base,
    ...attributes,
  };
}
