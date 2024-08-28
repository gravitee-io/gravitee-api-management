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
import { isFunction } from 'lodash';

import { CreatePortalMenuLink, PortalMenuLink, UpdatePortalMenuLink } from './portalMenuLink';

export function fakeCreatePortalMenuLink(
  modifier?: Partial<CreatePortalMenuLink> | ((base: CreatePortalMenuLink) => CreatePortalMenuLink),
): CreatePortalMenuLink {
  const base: CreatePortalMenuLink = {
    name: 'create - link name',
    target: 'create - link target',
    type: 'EXTERNAL',
    visibility: 'PUBLIC',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeUpdatePortalMenuLink(
  modifier?: Partial<UpdatePortalMenuLink> | ((base: UpdatePortalMenuLink) => UpdatePortalMenuLink),
): UpdatePortalMenuLink {
  const base: UpdatePortalMenuLink = {
    name: 'update - link name',
    target: 'update - link target',
    visibility: 'PUBLIC',
    order: 1,
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakePortalMenuLink(modifier?: Partial<PortalMenuLink> | ((base: PortalMenuLink) => PortalMenuLink)): PortalMenuLink {
  const base: PortalMenuLink = {
    id: 'link-id',
    name: 'link name',
    type: 'EXTERNAL',
    target: 'link target',
    visibility: 'PRIVATE',
    order: 1,
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
