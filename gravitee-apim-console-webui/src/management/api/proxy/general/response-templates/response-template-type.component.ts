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

import * as _ from 'lodash';

const ApiResponseTemplateTypeComponent: ng.IComponentOptions = {
  template: require('./response-template-type.html'),
  bindings: {
    template: '<',
  },
  require: {
    parent: '^gvResponseTemplate',
  },
  controller: function () {
    'ngInject';

    this.statuses = [
      { code: 100, label: '100 - CONTINUE' },
      { code: 101, label: '101 - SWITCHING_PROTOCOLS' },
      { code: 102, label: '102 - PROCESSING' },
      { code: 200, label: '200 - OK' },
      { code: 201, label: '201 - CREATED' },
      { code: 202, label: '202 - ACCEPTED' },
      { code: 203, label: '203 - NON_AUTHORITATIVE_INFORMATION' },
      { code: 204, label: '204 - NO_CONTENT' },
      { code: 205, label: '205 - RESET_CONTENT' },
      { code: 206, label: '206 - PARTIAL_CONTENT' },
      { code: 207, label: '207 - MULTI_STATUS' },
      { code: 300, label: '300 - MULTIPLE_CHOICES' },
      { code: 301, label: '301 - MOVED_PERMANENTLY' },
      { code: 302, label: '302 - MOVED_TEMPORARILY' },
      { code: 302, label: '302 - FOUND' },
      { code: 303, label: '303 - SEE_OTHER' },
      { code: 304, label: '304 - NOT_MODIFIED' },
      { code: 305, label: '305 - USE_PROXY' },
      { code: 307, label: '307 - TEMPORARY_REDIRECT' },
      { code: 400, label: '400 - BAD_REQUEST' },
      { code: 401, label: '401 - UNAUTHORIZED' },
      { code: 402, label: '402 - PAYMENT_REQUIRED' },
      { code: 403, label: '403 - FORBIDDEN' },
      { code: 404, label: '404 - NOT_FOUND' },
      { code: 405, label: '405 - METHOD_NOT_ALLOWED' },
      { code: 406, label: '406 - NOT_ACCEPTABLE' },
      { code: 407, label: '407 - PROXY_AUTHENTICATION_REQUIRED' },
      { code: 408, label: '408 - REQUEST_TIMEOUT' },
      { code: 409, label: '409 - CONFLICT' },
      { code: 410, label: '410 - GONE' },
      { code: 411, label: '411 - LENGTH_REQUIRED' },
      { code: 412, label: '412 - PRECONDITION_FAILED' },
      { code: 413, label: '413 - REQUEST_ENTITY_TOO_LARGE' },
      { code: 414, label: '414 - REQUEST_URI_TOO_LONG' },
      { code: 415, label: '415 - UNSUPPORTED_MEDIA_TYPE' },
      { code: 416, label: '416 - REQUESTED_RANGE_NOT_SATISFIABLE' },
      { code: 417, label: '417 - EXPECTATION_FAILED' },
      { code: 422, label: '422 - UNPROCESSABLE_ENTITY' },
      { code: 423, label: '423 - LOCKED' },
      { code: 424, label: '424 - FAILED_DEPENDENCY' },
      { code: 429, label: '429 - TOO_MANY_REQUESTS' },
      { code: 500, label: '500 - INTERNAL_SERVER_ERROR' },
      { code: 501, label: '501 - NOT_IMPLEMENTED' },
      { code: 502, label: '502 - BAD_GATEWAY' },
      { code: 503, label: '503 - SERVICE_UNAVAILABLE' },
      { code: 504, label: '504 - GATEWAY_TIMEOUT' },
      { code: 505, label: '505 - HTTP_VERSION_NOT_SUPPORTED' },
      { code: 507, label: '507 - INSUFFICIENT_STORAGE' },
    ];

    this.$onInit = function () {
      this.selectedStatusCode = _.find(this.statuses, (status) => status.code === this.template.status);

      this.bodyOptions = {
        placeholder: 'Edit your response body here.',
        lineWrapping: true,
        lineNumbers: true,
        allowDropFileTypes: true,
        autoCloseTags: true,
        mode: this.template.type,
      };
    };

    this.addHTTPHeader = function () {
      this.template.headers.push({ name: '', value: '' });
    };

    this.removeHTTPHeader = function (idx) {
      if (this.template.headers !== undefined) {
        this.template.headers.splice(idx, 1);
        this.parent.formResponseTemplate.$setDirty();
      }
    };

    this.onSelectedStatusCode = function (status) {
      if (status) {
        this.template.status = status.code;
      }
    };

    this.querySearchStatusCode = function (query) {
      return query ? this.statuses.filter(this.createFilterForStatusCode(query)) : this.statuses;
    };

    this.createFilterForStatusCode = function (query) {
      return function filterFn(state) {
        return _.includes(state.label.toLowerCase(), query.toLowerCase());
      };
    };

    this.deleteTemplate = function () {
      this.parent.deleteTemplate(this.template.type);
    };
  },
};

export default ApiResponseTemplateTypeComponent;
