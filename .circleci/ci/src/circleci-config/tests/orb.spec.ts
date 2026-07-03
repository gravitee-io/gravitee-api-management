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
import { OrbImport, OrbRef } from '../orb';
import { CustomParameter, CustomParametersList } from '../parameters';

describe('OrbImport', () => {
  it('emits the alias mapped to namespace/orb@version', () => {
    const keeper = new OrbImport('keeper', 'gravitee-io', 'keeper', '0.7.0');
    expect(keeper.generate()).toStrictEqual({ keeper: 'gravitee-io/keeper@0.7.0' });
  });

  it('builds fully-qualified OrbRef commands from a manifest', () => {
    const keeper = new OrbImport('keeper', 'gravitee-io', 'keeper', '0.7.0', undefined, {
      commands: {
        'env-export': new CustomParametersList([new CustomParameter('secret-url', 'string'), new CustomParameter('var-name', 'string')]),
      },
    });
    expect(keeper.commands['env-export']).toBeInstanceOf(OrbRef);
    expect(keeper.commands['env-export'].name).toBe('keeper/env-export');
  });

  it('supports refs assigned after construction', () => {
    const aquasec = new OrbImport('aquasec', 'gravitee-io', 'aquasec', '1.0.5');
    aquasec.jobs.fs_scan = new OrbRef('fs_scan', new CustomParametersList(), aquasec);
    expect(aquasec.jobs.fs_scan.name).toBe('aquasec/fs_scan');
  });
});

describe('OrbRef', () => {
  it('qualifies its name with the orb alias', () => {
    const gravitee = new OrbImport('gravitee', 'gravitee-io', 'gravitee', 'dev:4.5.0');
    const ref = new OrbRef('docker-load-image-from-workspace', new CustomParametersList(), gravitee);
    expect(ref.name).toBe('gravitee/docker-load-image-from-workspace');
    expect(ref.generate()).toBe('gravitee/docker-load-image-from-workspace');
  });
});
