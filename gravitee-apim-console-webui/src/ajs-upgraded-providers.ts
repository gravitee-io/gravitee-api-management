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

/**
 * Provider to temporarily ensure compatibility between AngularJs and Angular
 */
import { InjectionToken, Injector } from '@angular/core';

/**
 * @Deprecated
 * - For testing use GioTestingPermissionProvider
 * - For current user use ngx CurrentUserService
 */
export const CurrentUserService = new InjectionToken('CurrentUserService');

function currentUserServiceFactory(i: any) {
  return i.get('UserService');
}
export const currentUserProvider = {
  provide: CurrentUserService,
  useFactory: currentUserServiceFactory,
  deps: ['$injector'],
};

// Used to provide the $scope to the AngularJS components
export const ajsScopeProvider = {
  deps: ['$injector'],
  provide: '$scope',
  useFactory: (injector: Injector) => injector.get('$rootScope').$new(),
};
