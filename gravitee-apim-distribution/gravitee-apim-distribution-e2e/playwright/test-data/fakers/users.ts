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
import { BasicAuthentication } from '@model/users';

export const API_PUBLISHER_USER: BasicAuthentication = {
  username: process.env.PW_API_PUBLISHER_USER_LOGIN ?? 'api1',
  password: process.env.PW_API_PUBLISHER_USER_PASSWORD ?? 'api1',
};
export const ADMIN_USER: BasicAuthentication = {
  username: process.env.PW_ADMIN_USER_LOGIN ?? 'admin',
  password: process.env.PW_ADMIN_USER_PASSWORD ?? 'admin',
};
export const APPLICATION_USER: BasicAuthentication = {
  username: process.env.PW_APPLICATION_USER_LOGIN ?? 'application1',
  password: process.env.PW_APPLICATION_USER_PASSWORD ?? 'application1',
};
export const LOW_PERMISSION_USER: BasicAuthentication = {
  username: process.env.PW_LOW_PERMISSION_USER_LOGIN ?? 'user',
  password: process.env.PW_LOW_PERMISSION_USER_PASSWORD ?? 'password',
};
