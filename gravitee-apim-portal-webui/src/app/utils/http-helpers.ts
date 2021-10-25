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

export interface HttpStatus {
  value: string;
  label: string;
}

export class HttpHelpers {
  static httpStatus: Array<HttpStatus> = [
    { value: '100', label: '100 - CONTINUE' },
    { value: '101', label: '101 - SWITCHING PROTOCOLS' },
    { value: '102', label: '102 - PROCESSING' },
    { value: '200', label: '200 - OK' },
    { value: '201', label: '201 - CREATED' },
    { value: '202', label: '202 - ACCEPTED' },
    { value: '203', label: '203 - NON AUTHORITATIVE INFORMATION' },
    { value: '204', label: '204 - NO CONTENT' },
    { value: '205', label: '205 - RESET CONTENT' },
    { value: '206', label: '206 - PARTIAL CONTENT' },
    { value: '207', label: '207 - MULTI STATUS' },
    { value: '300', label: '300 - MULTIPLE CHOICES' },
    { value: '301', label: '301 - MOVED PERMANENTLY' },
    { value: '302', label: '302 - FOUND' },
    { value: '303', label: '303 - SEE OTHER' },
    { value: '304', label: '304 - NOT MODIFIED' },
    { value: '305', label: '305 - USE PROXY' },
    { value: '307', label: '307 - TEMPORARY REDIRECT' },
    { value: '400', label: '400 - BAD REQUEST' },
    { value: '401', label: '401 - UNAUTHORIZED' },
    { value: '402', label: '402 - PAYMENT REQUIRED' },
    { value: '403', label: '403 - FORBIDDEN' },
    { value: '404', label: '404 - NOT FOUND' },
    { value: '405', label: '405 - METHOD NOT ALLOWED' },
    { value: '406', label: '406 - NOT ACCEPTABLE' },
    { value: '407', label: '407 - PROXY AUTHENTICATION REQUIRED' },
    { value: '408', label: '408 - REQUEST TIMEOUT' },
    { value: '409', label: '409 - CONFLICT' },
    { value: '410', label: '410 - GONE' },
    { value: '411', label: '411 - LENGTH REQUIRED' },
    { value: '412', label: '412 - PRECONDITION FAILED' },
    { value: '413', label: '413 - REQUEST ENTITY TOO LARGE' },
    { value: '414', label: '414 - REQUEST URI TOO LONG' },
    { value: '415', label: '415 - UNSUPPORTED MEDIA TYPE' },
    { value: '416', label: '416 - REQUESTED RANGE NOT SATISFIABLE' },
    { value: '417', label: '417 - EXPECTATION FAILED' },
    { value: '422', label: '422 - UNPROCESSABLE ENTITY' },
    { value: '423', label: '423 - LOCKED' },
    { value: '424', label: '424 - FAILED DEPENDENCY' },
    { value: '429', label: '429 - TOO MANY REQUESTS' },
    { value: '500', label: '500 - INTERNAL SERVER ERROR' },
    { value: '501', label: '501 - NOT IMPLEMENTED' },
    { value: '502', label: '502 - BAD GATEWAY' },
    { value: '503', label: '503 - SERVICE UNAVAILABLE' },
    { value: '504', label: '504 - GATEWAY TIMEOUT' },
    { value: '505', label: '505 - HTTP VERSION NOT SUPPORTED' },
    { value: '507', label: '507 - INSUFFICIENT STORAGE' },
  ];
}
