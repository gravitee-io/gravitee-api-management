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

  describe('Services : UserService', function(){

    var UserService, httpBackend;

    beforeEach(function () {
      module('gravitee');

      inject(function (_UserService_, _$httpBackend_) {
        UserService = _UserService_;
        httpBackend = _$httpBackend_;
      });
    });

    it('should list users', function () {
      httpBackend.expectGET('').respond(true);
      var data = false;
      UserService.list().then(function (response) {
        data = response.data;
      });
      httpBackend.flush();
      expect(data).toBe(true);
    });

    it('should get user', function () {
      httpBackend.expectGET('').respond(true);
      var data = false;
      UserService.get('myUser').then(function (response) {
        data = response.data;
      });
      httpBackend.flush();
      expect(data).toBe(true);
    });

    it('should get user teams', function () {
      httpBackend.expectGET('').respond(true);
      var data = false;
      UserService.listTeams('myUser').then(function (response) {
        data = response.data;
      });
      httpBackend.flush();
      expect(data).toBe(true);
    });

    it('should create user', function () {
      httpBackend.expectPOST('').respond(true);
      var data = false;
      UserService.create({firstName: 'Toto', lastName: 'Titi'}).then(function (response) {
        data = response.data;
      });
      httpBackend.flush();
      expect(data).toBe(true);
    });
  });
})();
