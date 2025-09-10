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
import { MiddleEllipsisPipe } from './middle-ellipsis.pipe';

describe('MiddleEllipsisPipe', () => {
  it('create an instance', () => {
    const pipe = new MiddleEllipsisPipe();
    expect(pipe).toBeTruthy();
  });

  it('should return empty string when input is empty', () => {
    const pipe = new MiddleEllipsisPipe();
    expect(pipe.transform('')).toEqual('');
  });

  it('should return empty string when input is null or undefined', () => {
    const pipe = new MiddleEllipsisPipe();
    expect(pipe.transform(null)).toEqual('');
    expect(pipe.transform(undefined)).toEqual('');
  });

  it('should return the original string when length is less than or equal to frontChars + backChars', () => {
    const pipe = new MiddleEllipsisPipe();
    const shortString = 'short@example.com';
    expect(pipe.transform(shortString)).toEqual(shortString);
  });

  it('should truncate the string with ellipsis when length exceeds frontChars + backChars', () => {
    const pipe = new MiddleEllipsisPipe();
    const longString = 'verylongemailaddress@example.com';
    expect(pipe.transform(longString)).toEqual('verylongemailad...xample.com');
  });

  it('should use default values (frontChars=15, backChars=10) when no parameters are provided', () => {
    const pipe = new MiddleEllipsisPipe();
    const longString = 'abcdefghijklmnopqrstuvwxyz1234567890';
    expect(pipe.transform(longString)).toEqual('abcdefghijklmno...1234567890');
  });

  it('should use custom frontChars and backChars values when provided', () => {
    const pipe = new MiddleEllipsisPipe();
    const longString = 'abcdefghijklmnopqrstuvwxyz1234567890';
    expect(pipe.transform(longString, 5, 5)).toEqual('abcde...67890');
  });

  it('should handle edge case with very small frontChars and backChars values', () => {
    const pipe = new MiddleEllipsisPipe();
    const longString = 'abcdefghijklmnopqrstuvwxyz1234567890';
    expect(pipe.transform(longString, 1, 1)).toEqual('a...0');
  });
});
