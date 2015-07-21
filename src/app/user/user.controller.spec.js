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
/* global describe:false, beforeEach:false, it:false */
(function() {
  'use strict';

  describe('Controllers : UserController', function(){

    var UserController, scope, mockUserService, mockUsers, mockUser, mockTeams, mockRoles;

    beforeEach(function () {
      module('gravitee');

      inject(function ($controller, $rootScope, $q) {

        scope = $rootScope.$new();

        mockUser = {
          lastName: 'Toto',
          firstName: 'Titi',
          roles: ['ROLE_ADMIN', 'ROLE_APIS']
        };

        mockUsers = [mockUser];

        mockTeams = ['Gravitee'];

        mockRoles = ['ROLE_ADMIN', 'ROLE_APIS', 'ROLE_USER'];

        mockUserService = {
          list: jasmine.createSpy().and.returnValue($q.when({data: mockUsers})),
          get: jasmine.createSpy().and.returnValue($q.when(mockUser)),
          listTeams: jasmine.createSpy().and.returnValue($q.when(mockTeams)),
          listRoles: jasmine.createSpy().and.returnValue($q.when(mockRoles))
        };

        UserController = $controller('UserController', {
          UserService: mockUserService
        });
      })
    });

    it('should list users', function() {
      UserController.list();
      expect(mockUserService.list).toHaveBeenCalled();
      scope.$apply();
      expect(UserController.users).toBe(mockUsers);
    });
  });
})();
