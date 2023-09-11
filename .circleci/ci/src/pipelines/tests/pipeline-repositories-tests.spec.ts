import * as fs from 'fs';
import { generateRepositoriesTestsConfig } from '../pipeline-repositories-tests';

describe('Run Repositories Tests', () => {
  it('should generate Repositories tests pipeline', () => {
    const result = generateRepositoriesTestsConfig({
      action: 'repositories_tests',
      branch: 'master',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0',
      isDryRun: false,
      apimVersionPath: '',
    });

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/repositories-tests/repositories-tests.yml`, 'utf-8');
    expect(result.stringify()).toStrictEqual(expected);
  });
});
