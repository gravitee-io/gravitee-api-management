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
import { BasicAuthentication } from 'model/users';

export const API_PUBLISHER_USER: BasicAuthentication = {
  username: Cypress.env('api_publisher_user_login'),
  password: Cypress.env('api_publisher_user_password'),
};
export const ADMIN_USER: BasicAuthentication = { username: Cypress.env('admin_user_login'), password: Cypress.env('admin_user_password') };
export const APPLICATION_USER: BasicAuthentication = {
  username: Cypress.env('application_user_login'),
  password: Cypress.env('application_user_password'),
};
export const LOW_PERMISSION_USER: BasicAuthentication = {
  username: Cypress.env('low_permission_user_login'),
  password: Cypress.env('low_permission_user_password'),
};
