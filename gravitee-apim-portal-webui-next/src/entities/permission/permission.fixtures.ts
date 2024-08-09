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
import { isFunction } from 'rxjs/internal/util/isFunction';

import { UserApiPermissions, UserApplicationPermissions, UserEnvironmentPermissions } from './permission';

export function fakeUserEnvironmentPermissions(
  modifier?: Partial<UserEnvironmentPermissions> | ((basePermissions: UserEnvironmentPermissions) => UserEnvironmentPermissions),
): UserEnvironmentPermissions {
  const base: UserEnvironmentPermissions = {
    USER: [],
    APPLICATION: [],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeUserApiPermissions(
  modifier?: Partial<UserApiPermissions> | ((basePermissions: UserApiPermissions) => UserApiPermissions),
): UserApiPermissions {
  const base: UserApiPermissions = {
    DEFINITION: [],
    PLAN: [],
    SUBSCRIPTION: [],
    MEMBER: [],
    METADATA: [],
    ANALYTICS: [],
    EVENT: [],
    HEALTH: [],
    LOG: [],
    DOCUMENTATION: [],
    GATEWAY_DEFINITION: [],
    AUDIT: [],
    RATING: [],
    RATING_ANSWER: [],
    NOTIFICATION: [],
    MESSAGE: [],
    ALERT: [],
    RESPONSE_TEMPLATES: [],
    REVIEWS: [],
    QUALITY_RULE: [],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeUserApplicationPermissions(
  modifier?: Partial<UserApplicationPermissions> | ((basePermissions: UserApplicationPermissions) => UserApplicationPermissions),
): UserApplicationPermissions {
  const base: UserApplicationPermissions = {
    DEFINITION: [],
    MEMBER: [],
    ANALYTICS: [],
    LOG: [],
    SUBSCRIPTION: [],
    NOTIFICATION: [],
    ALERT: [],
    METADATA: [],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
