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
export const HttpStatusCodeDescription: Record<number, { description: string; constant: string }> = {
  100: {
    description: 'Continue',
    constant: 'CONTINUE',
  },
  101: {
    description: 'Switching Protocols',
    constant: 'SWITCHING_PROTOCOLS',
  },
  102: {
    description: 'Processing',
    constant: 'PROCESSING',
  },
  200: {
    description: 'OK',
    constant: 'OK',
  },
  201: {
    description: 'Created',
    constant: 'CREATED',
  },
  202: {
    description: 'Accepted',
    constant: 'ACCEPTED',
  },
  203: {
    description: 'Non Authoritative Information',
    constant: 'NON_AUTHORITATIVE_INFORMATION',
  },
  204: {
    description: 'No Content',
    constant: 'NO_CONTENT',
  },
  205: {
    description: 'Reset Content',
    constant: 'RESET_CONTENT',
  },
  206: {
    description: 'Partial Content',
    constant: 'PARTIAL_CONTENT',
  },
  207: {
    description: 'Multi-Status',
    constant: 'MULTI_STATUS',
  },
  300: {
    description: 'Multiple Choices',
    constant: 'MULTIPLE_CHOICES',
  },
  301: {
    description: 'Moved Permanently',
    constant: 'MOVED_PERMANENTLY',
  },
  302: {
    description: 'Moved Temporarily',
    constant: 'MOVED_TEMPORARILY',
  },
  303: {
    description: 'See Other',
    constant: 'SEE_OTHER',
  },
  304: {
    description: 'Not Modified',
    constant: 'NOT_MODIFIED',
  },
  305: {
    description: 'Use Proxy',
    constant: 'USE_PROXY',
  },
  307: {
    description: 'Temporary Redirect',
    constant: 'TEMPORARY_REDIRECT',
  },
  308: {
    description: 'Permanent Redirect',
    constant: 'PERMANENT_REDIRECT',
  },
  400: {
    description: 'Bad Request',
    constant: 'BAD_REQUEST',
  },
  401: {
    description: 'Unauthorized',
    constant: 'UNAUTHORIZED',
  },
  402: {
    description: 'Payment Required',
    constant: 'PAYMENT_REQUIRED',
  },
  403: {
    description: 'Forbidden',
    constant: 'FORBIDDEN',
  },
  404: {
    description: 'Not Found',
    constant: 'NOT_FOUND',
  },
  405: {
    description: 'Method Not Allowed',
    constant: 'METHOD_NOT_ALLOWED',
  },
  406: {
    description: 'Not Acceptable',
    constant: 'NOT_ACCEPTABLE',
  },
  407: {
    description: 'Proxy Authentication Required',
    constant: 'PROXY_AUTHENTICATION_REQUIRED',
  },
  408: {
    description: 'Request Timeout',
    constant: 'REQUEST_TIMEOUT',
  },
  409: {
    description: 'Conflict',
    constant: 'CONFLICT',
  },
  410: {
    description: 'Gone',
    constant: 'GONE',
  },
  411: {
    description: 'Length Required',
    constant: 'LENGTH_REQUIRED',
  },
  412: {
    description: 'Precondition Failed',
    constant: 'PRECONDITION_FAILED',
  },
  413: {
    description: 'Request Entity Too Large',
    constant: 'REQUEST_ENTITY_TOO_LARGE',
  },
  414: {
    description: 'Request-URI Too Long',
    constant: 'REQUEST_URI_TOO_LONG',
  },
  415: {
    description: 'Unsupported Media Type',
    constant: 'UNSUPPORTED_MEDIA_TYPE',
  },
  416: {
    description: 'Requested Range Not Satisfiable',
    constant: 'REQUESTED_RANGE_NOT_SATISFIABLE',
  },
  417: {
    description: 'Expectation Failed',
    constant: 'EXPECTATION_FAILED',
  },
  418: {
    description: "I'm a teapot",
    constant: "I'M_A_TEAPOT",
  },
  419: {
    description: 'Insufficient Space on Resource',
    constant: 'INSUFFICIENT_SPACE_ON_RESOURCE',
  },
  420: {
    description: 'Method Failure',
    constant: 'METHOD_FAILURE',
  },
  422: {
    description: 'Unprocessable Entity',
    constant: 'UNPROCESSABLE_ENTITY',
  },
  423: {
    description: 'Locked',
    constant: 'LOCKED',
  },
  424: {
    description: 'Failed Dependency',
    constant: 'FAILED_DEPENDENCY',
  },
  428: {
    description: 'Precondition Required',
    constant: 'PRECONDITION_REQUIRED',
  },
  429: {
    description: 'Too Many Requests',
    constant: 'TOO_MANY_REQUESTS',
  },
  431: {
    description: 'Request Header Fields Too Large',
    constant: 'REQUEST_HEADER_FIELDS_TOO_LARGE',
  },
  451: {
    description: 'Unavailable For Legal Reasons',
    constant: 'UNAVAILABLE_FOR_LEGAL_REASONS',
  },
  500: {
    description: 'Internal Server Error',
    constant: 'INTERNAL_SERVER_ERROR',
  },
  501: {
    description: 'Not Implemented',
    constant: 'NOT_IMPLEMENTED',
  },
  502: {
    description: 'Bad Gateway',
    constant: 'BAD_GATEWAY',
  },
  503: {
    description: 'Service Unavailable',
    constant: 'SERVICE_UNAVAILABLE',
  },
  504: {
    description: 'Gateway Timeout',
    constant: 'GATEWAY_TIMEOUT',
  },
  505: {
    description: 'HTTP Version Not Supported',
    constant: 'HTTP_VERSION_NOT_SUPPORTED',
  },
  507: {
    description: 'Insufficient Storage',
    constant: 'INSUFFICIENT_STORAGE',
  },
  511: {
    description: 'Network Authentication Required',
    constant: 'NETWORK_AUTHENTICATION_REQUIRED',
  },
};
