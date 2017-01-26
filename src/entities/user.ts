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
import { Authority } from "./authority";

export class User {
  constructor(
    public username?: string,
    private password?: string,
    private authorities?: Authority[],
    private accountNonExpired?: boolean,
    private accountNonLocked?: boolean,
    private credentialsNonExpired?: boolean,
    private enabled?: boolean,
    public picture?: string
  ) {}

  allowedTo(roles: string[]): boolean {
    if (!roles || !this.authorities) {
      return false;
    }

    return this.authorities
        .some(authority => roles.indexOf(authority.authority) !== -1);
  }
}
