/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { TestBed } from '@angular/core/testing';

import { TokenService } from './token.service';

describe('TokenService', () => {
  let service: TokenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TokenService],
    });
    service = TestBed.inject(TokenService);
  });

  describe('parseToken', () => {
    it('should return null for a null token', () => {
      const token = null;
      const result = service.parseToken(token);
      expect(result).toBeNull();
    });

    it('should return null for an invalid token', () => {
      const token = 'invalid.token';
      const result = service.parseToken(token);
      expect(result).toBeNull();
    });

    it('should return null for a token with invalid Base64 characters', () => {
      const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.base64#.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c';
      const result = service.parseToken(token);
      expect(result).toBeNull();
    });

    it('should correctly parse a valid token', () => {
      const token =
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c';
      const result = service.parseToken(token);
      expect(result).toEqual({
        sub: '1234567890',
        name: 'John Doe',
        iat: 1516239022,
      });
    });
  });

  describe('isParsedTokenExpired', () => {
    it('should return true if the token is expired', () => {
      const parsedToken = Math.floor(Date.now() / 1000) - 1000;
      const result = service.isParsedTokenExpired(parsedToken);
      expect(result).toBe(true);
    });

    it('should return false if the token is not expired', () => {
      const parsedToken = Math.floor(Date.now() / 1000) + 1000;
      const result = service.isParsedTokenExpired(parsedToken);
      expect(result).toBe(false);
    });

    it('should return false if the token is null', () => {
      const parsedToken = null;
      const result = service.isParsedTokenExpired(parsedToken);
      expect(result).toBe(false);
    });

    it('should return false if the token is undefined', () => {
      const parsedToken = undefined;
      const result = service.isParsedTokenExpired(parsedToken);
      expect(result).toBe(false);
    });
  });
});
