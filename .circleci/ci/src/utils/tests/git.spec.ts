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
import { refsUnder } from '../git';

describe('refsUnder', () => {
  // `git ls-remote --tags --refs origin`, as it comes: one `<sha>\t<ref>` per line.
  const TAGS = [
    '6e63f853294a0c66a671bf9d24c4c1e7ae7c1cab\trefs/tags/0.1.0',
    '6e1fa64e00aefc17fd2ff24c17c4646791cd9aef\trefs/tags/4.12.0',
    '11937c094dbaee4fb094fa4d5cd190ed8c9e66e3\trefs/tags/core_4.13.0',
  ].join('\n');

  it('should name the refs under the prefix', () => {
    expect(refsUnder(TAGS, 'refs/tags/')).toStrictEqual(['0.1.0', '4.12.0', 'core_4.13.0']);
  });

  it('should read heads the same way', () => {
    const heads =
      'eae3a8ab7687f7aa543539757a39ae6f2b7bb315\trefs/heads/3.10.x\ndf67ea7ace37b20f03b38ed67751197708b5805c\trefs/heads/4.12.x';

    expect(refsUnder(heads, 'refs/heads/')).toStrictEqual(['3.10.x', '4.12.x']);
  });

  // The callers ask which versions exist: a line they cannot name is a line they must not count.
  it('should drop what does not sit under the prefix', () => {
    expect(refsUnder(TAGS, 'refs/heads/')).toStrictEqual([]);
  });

  it('should answer nothing on an empty output', () => {
    expect(refsUnder('', 'refs/tags/')).toStrictEqual([]);
  });
});
