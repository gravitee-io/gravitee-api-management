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

  describe('Controllers : ApiController', function(){

    var ApiController, scope, mockApiService, mockApis;

    beforeEach(function () {
      module('gravitee');

      inject(function ($controller, $rootScope, $q) {

        scope = $rootScope.$new();

        mockApis = {
          name: 'My URL',
          public: '/myURL',
          target: 'http://myURL.com',
          state: 'STARTED',
          policies: []
        };

        mockApiService = {
          list: jasmine.createSpy().and.returnValue($q.when({data: mockApis})),
          start: jasmine.createSpy().and.returnValue($q.when({})),
          stop: jasmine.createSpy().and.returnValue($q.when({})),
          reload: jasmine.createSpy().and.returnValue($q.when({}))
        };

        ApiController = $controller('ApiController', {
          ApiService: mockApiService
        });
      })
    });

    it('should list APIs', function() {
      ApiController.list();
      expect(mockApiService.list).toHaveBeenCalled();
      scope.$apply();
      expect(ApiController.apis).toBe(mockApis);
    });

    it('should start API', function() {
      ApiController.start('myApi');
      expect(mockApiService.start).toHaveBeenCalledWith('myApi');
      scope.$apply();
      expect(mockApiService.reload).toHaveBeenCalledWith('myApi');
      expect(mockApiService.list).toHaveBeenCalled();
      expect(ApiController.apis).toBe(mockApis);
    });

    it('should stop API', function() {
      ApiController.stop('myApi');
      expect(mockApiService.stop).toHaveBeenCalledWith('myApi');
      scope.$apply();
      expect(mockApiService.reload).toHaveBeenCalledWith('myApi');
      expect(mockApiService.list).toHaveBeenCalled();
      expect(ApiController.apis).toBe(mockApis);
    });
  });
})();
