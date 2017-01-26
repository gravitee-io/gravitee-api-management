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
'use strict';

describe('The policy view', function () {
  var page;

  browser.addMockModule('httpBackendMockModule', require('../../mocked-backend').httpBackendMock);

  beforeEach(function () {
    browser.get('#!/apis/swapi/settings/policies');
    page = require('./policy.po');
  });

  it('should add a policy configuration item on click on add button', function () {
    expect(page.rateLimitPolicyConfigurationBlocks.count()).toEqual(0);
    page.rateLimitPolicy.click();
    expect(page.rateLimitPolicyConfigurationBlocks.count()).toEqual(1);
    page.rateLimitPolicyAddButton.click();
    expect(page.rateLimitPolicyConfigurationBlocks.count()).toEqual(2);
  });
});
