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
import { RestrictedDomain } from './restrictedDomain';

export function fakeRestrictedDomain(attributes?: Partial<RestrictedDomain>): RestrictedDomain {
  const base: RestrictedDomain = {
    domain: 'my-custom-domain',
    secured: false,
  };

  return {
    ...base,
    ...attributes,
  };
}

export function fakeRestrictedDomains(domain: string[], attributes?: Partial<RestrictedDomain>): RestrictedDomain[] {
  return domain.map(value => {
    const base: RestrictedDomain = {
      domain: value,
      secured: false,
    };

    return {
      ...base,
      ...attributes,
    };
  });
}
