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
import { MaskSensitiveHeaderPipe } from './mask-sensitive-header.pipe';

describe('MaskSensitiveHeaderPipe', () => {
  let pipe: MaskSensitiveHeaderPipe;

  beforeEach(() => {
    pipe = new MaskSensitiveHeaderPipe();
  });

  describe('non sensitive headers', () => {
    it('should_return_value_untouched_for_non_sensitive_header', () => {
      expect(pipe.transform('application/json', 'Content-Type')).toEqual('application/json');
    });
  });

  describe('sensitive headers', () => {
    it('should_mask_value_and_keep_known_scheme', () => {
      expect(pipe.transform('Bearer eyJhbGciOiJIUzI1NiJ9.payload.signature', 'Authorization')).toEqual('Bearer ••••••••ture');
    });

    it('should_mask_basic_scheme', () => {
      expect(pipe.transform('Basic dXNlcjpwYXNzd29yZA==', 'Authorization')).toEqual('Basic ••••••••');
    });

    it('should_detect_sensitive_header_regardless_of_case', () => {
      expect(pipe.transform('Bearer some-very-long-token-value', 'AUTHORIZATION')).toEqual('Bearer ••••••••alue');
      expect(pipe.transform('some-very-long-api-key-value', 'X-Api-Key')).toEqual('••••••••alue');
    });

    it('should_mask_proxy_authorization', () => {
      expect(pipe.transform('Bearer some-very-long-token-value', 'Proxy-Authorization')).toEqual('Bearer ••••••••alue');
    });

    it('should_mask_gravitee_api_key', () => {
      expect(pipe.transform('some-very-long-api-key-value', 'X-Gravitee-Api-Key')).toEqual('••••••••alue');
    });

    it('should_mask_value_without_scheme', () => {
      expect(pipe.transform('some-very-long-api-key-value', 'X-Api-Key')).toEqual('••••••••alue');
    });

    it('should_not_treat_unknown_first_token_as_a_scheme', () => {
      expect(pipe.transform('NotAScheme some-very-long-token', 'Authorization')).toEqual('••••••••oken');
    });
  });

  describe('decodable credentials', () => {
    it('should_never_reveal_suffix_of_basic_credentials', () => {
      // The suffix of a base64 credential decodes to the plaintext tail of the password.
      expect(pipe.transform('Basic dXNlcjpwYXNzd29yZGxvbmdlbm91Z2g=', 'Authorization')).toEqual('Basic ••••••••');
    });

    it('should_never_reveal_suffix_of_unpadded_basic_credentials', () => {
      // 'admin:SuperSecretPass123' is a multiple of three bytes, so its base64 carries no padding at all
      // and the four trailing characters decode to exactly '123', the tail of the password.
      expect(pipe.transform('Basic YWRtaW46U3VwZXJTZWNyZXRQYXNzMTIz', 'Authorization')).toEqual('Basic ••••••••');
    });

    it('should_never_reveal_suffix_of_padded_base64_value_without_scheme', () => {
      expect(pipe.transform('dXNlcjpwYXNzd29yZGxvbmdlbm91Z2g=', 'X-Api-Key')).toEqual('••••••••');
    });
  });

  describe('suffix threshold', () => {
    it('should_not_reveal_suffix_when_secret_is_too_short', () => {
      expect(pipe.transform('short-secret', 'X-Api-Key')).toEqual('••••••••');
      expect(pipe.transform('Bearer short-secret', 'Authorization')).toEqual('Bearer ••••••••');
    });

    it('should_not_reveal_suffix_at_exactly_the_threshold', () => {
      expect(pipe.transform('a'.repeat(20), 'X-Api-Key')).toEqual('••••••••');
    });

    it('should_reveal_suffix_when_secret_is_longer_than_threshold', () => {
      expect(pipe.transform('a-secret-value-longer-than-threshold', 'X-Api-Key')).toEqual('••••••••hold');
    });
  });

  describe('mask length', () => {
    it('should_use_a_fixed_mask_length_whatever_the_secret_length', () => {
      const short = pipe.transform('0123456789abcdefghijklmn', 'X-Api-Key');
      const long = pipe.transform('0123456789abcdefghijklmn'.repeat(20), 'X-Api-Key');

      expect(short).toEqual('••••••••klmn');
      expect(long).toEqual('••••••••klmn');
    });
  });

  describe('cookies', () => {
    it('should_fully_mask_cookie_without_revealing_any_suffix', () => {
      expect(pipe.transform('sessionId=some-very-long-session-value; theme=dark', 'Cookie')).toEqual('••••••••');
    });

    it('should_fully_mask_set_cookie_without_revealing_any_suffix', () => {
      expect(pipe.transform('sessionId=some-very-long-session-value; HttpOnly', 'Set-Cookie')).toEqual('••••••••');
    });
  });

  describe('empty values', () => {
    it('should_return_empty_value_untouched', () => {
      expect(pipe.transform('', 'Authorization')).toEqual('');
      expect(pipe.transform(null, 'Authorization')).toEqual(null);
      expect(pipe.transform(undefined, 'Authorization')).toEqual(undefined);
    });
  });
});
