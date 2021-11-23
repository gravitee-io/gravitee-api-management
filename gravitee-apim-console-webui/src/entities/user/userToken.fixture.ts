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
import { NewToken, Token } from './userTokens';

export function fakeNewUserToken(name?: string): NewToken {
  return {
    name: name ?? 'J.R.R Token',
  } as NewToken;
}

export function fakeUserToken(attributes?: Partial<Token>): Token {
  const base: Token = {
    id: 'f7b34e11-a476-4af1-b34e-11a4768af103',
    created_at: 1630373735403,
    expires_at: 1631017105670,
    last_use_at: 1631017105654,
    name: 'J.R.R Token',
    token: 'MiThr4d1L',
  };

  return { ...base, ...attributes };
}
