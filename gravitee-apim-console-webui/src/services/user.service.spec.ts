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
import UserService from './user.service';

describe('UserService avatar cache busting', () => {
  const BASE_URL = '/management/organizations/DEFAULT';
  let userService: any;
  let httpPut: jest.Mock;

  beforeEach(() => {
    httpPut = jest.fn(() => Promise.resolve({ data: {} }));
    const $http = { put: httpPut };
    const constants = { org: { baseURL: BASE_URL } };
    const stringService = { hashCode: () => 123 };

    // Constructor order: $http, $q, Constants, RoleService, PermPermissionStore,
    // ApplicationService, ApiService, EnvironmentService, $location, $window, StringService, $rootScope
    userService = new (UserService as any)($http, {}, constants, {}, {}, {}, {}, {}, {}, {}, stringService, {});
    userService.currentUser = { id: 'user-id' };
  });

  it('should build the avatar URL with a stable cache-bust token by default', () => {
    expect(userService.currentUserPicture()).toBe(`${BASE_URL}/user/avatar?123&cacheBust=0`);
  });

  it('should change the cache-bust token after a successful save so the browser re-fetches the avatar', async () => {
    const before = userService.currentUserPicture();

    await userService.save({ username: 'paula' });

    const after = userService.currentUserPicture();
    expect(httpPut).toHaveBeenCalled();
    expect(after).not.toEqual(before);
    expect(after).not.toContain('cacheBust=0');
  });

  it('should return undefined when there is no current user', () => {
    userService.currentUser = undefined;
    expect(userService.currentUserPicture()).toBeUndefined();
  });
});
