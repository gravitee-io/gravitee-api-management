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
angular.module('gvConstants', []).constant('Constants', {
  // if the management REST API is on a different domain, put something like: http://demo.gravitee.io/management/
  'baseURL': '/management/',
  'version': '0.19.2',
  'portalTitle': 'Gravitee.io Portal',
  'devMode': false,
  'userCreationEnabled': true
});
