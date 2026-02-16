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
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { isEmpty, toString } from 'lodash';

export type StatusCode = {
  code: number;
  label: string;
};

const STATUS_CODES: StatusCode[] = [
  { code: 100, label: 'CONTINUE' },
  { code: 101, label: 'SWITCHING_PROTOCOLS' },
  { code: 102, label: 'PROCESSING' },
  { code: 200, label: 'OK' },
  { code: 201, label: 'CREATED' },
  { code: 202, label: 'ACCEPTED' },
  { code: 203, label: 'NON_AUTHORITATIVE_INFORMATION' },
  { code: 204, label: 'NO_CONTENT' },
  { code: 205, label: 'RESET_CONTENT' },
  { code: 206, label: 'PARTIAL_CONTENT' },
  { code: 207, label: 'MULTI_STATUS' },
  { code: 300, label: 'MULTIPLE_CHOICES' },
  { code: 301, label: 'MOVED_PERMANENTLY' },
  { code: 302, label: 'MOVED_TEMPORARILY' },
  { code: 302, label: 'FOUND' },
  { code: 303, label: 'SEE_OTHER' },
  { code: 304, label: 'NOT_MODIFIED' },
  { code: 305, label: 'USE_PROXY' },
  { code: 307, label: 'TEMPORARY_REDIRECT' },
  { code: 400, label: 'BAD_REQUEST' },
  { code: 401, label: 'UNAUTHORIZED' },
  { code: 402, label: 'PAYMENT_REQUIRED' },
  { code: 403, label: 'FORBIDDEN' },
  { code: 404, label: 'NOT_FOUND' },
  { code: 405, label: 'METHOD_NOT_ALLOWED' },
  { code: 406, label: 'NOT_ACCEPTABLE' },
  { code: 407, label: 'PROXY_AUTHENTICATION_REQUIRED' },
  { code: 408, label: 'REQUEST_TIMEOUT' },
  { code: 409, label: 'CONFLICT' },
  { code: 410, label: 'GONE' },
  { code: 411, label: 'LENGTH_REQUIRED' },
  { code: 412, label: 'PRECONDITION_FAILED' },
  { code: 413, label: 'REQUEST_ENTITY_TOO_LARGE' },
  { code: 414, label: 'REQUEST_URI_TOO_LONG' },
  { code: 415, label: 'UNSUPPORTED_MEDIA_TYPE' },
  { code: 416, label: 'REQUESTED_RANGE_NOT_SATISFIABLE' },
  { code: 417, label: 'EXPECTATION_FAILED' },
  { code: 422, label: 'UNPROCESSABLE_ENTITY' },
  { code: 423, label: 'LOCKED' },
  { code: 424, label: 'FAILED_DEPENDENCY' },
  { code: 429, label: 'TOO_MANY_REQUESTS' },
  { code: 500, label: 'INTERNAL_SERVER_ERROR' },
  { code: 501, label: 'NOT_IMPLEMENTED' },
  { code: 502, label: 'BAD_GATEWAY' },
  { code: 503, label: 'SERVICE_UNAVAILABLE' },
  { code: 504, label: 'GATEWAY_TIMEOUT' },
  { code: 505, label: 'HTTP_VERSION_NOT_SUPPORTED' },
  { code: 507, label: 'INSUFFICIENT_STORAGE' },
];

export const HttpUtil = {
  statusCodes: STATUS_CODES,
  // Common validator for host and hostDomain
  statusCodeValidator: (): ValidatorFn => {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;

      if (isEmpty(value)) {
        // not validate if is empty. Required validator will do the job
        return null;
      }

      if (STATUS_CODES.find(statusCode => toString(statusCode.code) === toString(value))) {
        return null;
      }

      return { statusCode: `Invalid status code: ${value}.` };
    };
  },
};
