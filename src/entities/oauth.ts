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

export class GrantType {
  public code: string;
  public type: string;
  public name: string;
  public response_types: string[];

  static AUTHORIZATION_CODE = new GrantType('authorization_code', 'authorization_code', 'Authorization Code', ['code']);
  static IMPLICIT = new GrantType('implicit', 'implicit', 'Implicit', ['token', 'id_token']);
  static IMPLICIT_HYBRID = new GrantType('implicit_hybrid', 'implicit', 'Implicit (Hybrid)', ['token', 'id_token']);
  static REFRESH_TOKEN = new GrantType('refresh_token', 'refresh_token', 'Refresh Token', []);
  static PASSWORD = new GrantType('password', 'password', 'Resource Owner Password', []);
  static CLIENT_CREDENTIALS = new GrantType('client_credentials', 'client_credentials', 'Client Credentials', []);

  static TYPES: GrantType[] = [GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT, GrantType.IMPLICIT_HYBRID, GrantType.REFRESH_TOKEN, GrantType.PASSWORD, GrantType.CLIENT_CREDENTIALS];

  constructor(code: string, type: string, name: string, response_types: string[]) {
    this.code = code;
    this.type = type;
    this.name = name;
    this.response_types = response_types;
  }
}
