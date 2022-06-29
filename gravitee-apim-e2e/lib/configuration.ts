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
import 'dotenv/config';
import fetchApi from 'node-fetch';
import { BasicAuthentication } from '@model/users';
import { Configuration as ManagementConfiguration } from './management-webclient-sdk/src/lib/runtime';
import { Configuration as PortalConfiguration, HTTPHeaders } from './portal-webclient-sdk/src/lib';

export const ADMIN_USER = {
  username: process.env.ADMIN_USERNAME,
  password: process.env.ADMIN_PASSWORD,
};

export const API_USER = {
  username: process.env.API_USERNAME,
  password: process.env.API_PASSWORD,
};

export const APP_USER = {
  username: process.env.APP_USERNAME,
  password: process.env.APP_PASSWORD,
};

export const SIMPLE_USER = {
  username: process.env.SIMPLE_USERNAME,
  password: process.env.SIMPLE_PASSWORD,
};

export const ANONYMOUS = null;

export const forManagementAsAdminUser = () => {
  return forManagement();
};

export const forManagementAsApiUser = () => {
  return forManagement(API_USER);
};

export const forManagementAsAppUser = () => {
  return forManagement(APP_USER);
};

export const forManagementAsSimpleUser = () => {
  return forManagement(SIMPLE_USER);
};

export const forManagement = (auth: BasicAuthentication = ADMIN_USER, headers = {}) => {
  return new ManagementConfiguration({
    basePath: process.env.MANAGEMENT_BASE_URL,
    fetchApi,
    ...auth,
    headers: { ...defaultHeaders, ...headers },
  });
};

const defaultHeaders = {
  Accept: '*/*',
  'Accept-Encoding': 'gzip, deflate, br',
  'Cache-Control': 'no-cache',
  Connection: 'keep-alive',
  'User-Agent': 'node-fetch',
};

export const forPortal = ({ auth = ANONYMOUS, envId = 'DEFAULT', headers = {} }) => {
  return new PortalConfiguration({
    basePath: `${process.env.PORTAL_BASE_URL}/${envId}`,
    fetchApi,
    ...auth,
    headers: { ...defaultHeaders, ...headers },
  });
};

export const forPortalAsAnonymous = (headers: HTTPHeaders = {}) => {
  return forPortal({ headers });
};

export const forPortalAsApplicationUser = (headers: HTTPHeaders = {}) => {
  return forPortal({ auth: APP_USER, headers });
};

export const forPortalAsApplicationFrenchUser = (headers: HTTPHeaders = {}) => {
  return forPortalAsApplicationUser({ 'Accept-Language': 'fr-FR,fr;q=0.9' });
};

export const forPortalAsAdminUser = (headers: HTTPHeaders = {}) => {
  return forPortal({ auth: ADMIN_USER, headers });
};

export const forPortalAsApiUser = (headers: HTTPHeaders = {}) => {
  return forPortal({ auth: API_USER, headers });
};

export const forPortalAsAppUser = (headers: HTTPHeaders = {}) => {
  return forPortal({ auth: APP_USER, headers });
};

export const forPortalAsSimpleUser = (headers: HTTPHeaders = {}) => {
  return forPortal({ auth: SIMPLE_USER, headers });
};

export const forPortalWithWrongPassword = () => {
  return forPortal({
    auth: {
      username: process.env.API_USERNAME,
      password: 'wrongPassword',
    },
  });
};

export const forManagementWithWrongPassword = () => {
  return forManagement({
    username: process.env.API_USERNAME,
    password: 'wrongPassword',
  });
};
