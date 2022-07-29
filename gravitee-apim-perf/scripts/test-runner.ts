#! ./node_modules/.bin/ts-node
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

import 'dotenv/config';
import { $, fs, chalk, ProcessOutput } from 'zx';
import { resolve } from 'path';
import { fetchGatewaySuccess } from '../lib/gateway';
import { parse } from 'ts-command-line-args';
import { GatewayTestData } from '../lib/test-api';

const testSuffix = '.test.js';
const dist = 'dist';
const assets = 'dist/assets';

interface ScriptArgs {
  file: string;
  verbose?: boolean;
}

const K6_COMMAND = [
  `K6_PROMETHEUS_REMOTE_URL=${process.env.K6_PROMETHEUS_REMOTE_URL}`,
  './bin/k6',
  'run',
  `-e GATEWAY_BASE_URL=${process.env.GATEWAY_BASE_URL}`,
  `-e SKIP_TLS_VERIFY=${process.env.SKIP_TLS_VERIFY}`,
  '-o output-prometheus-remote'
];

let TEARDOWN_ENV_VAR = '';

void (async function () {
  const args = parse<ScriptArgs>({
    // @ts-ignore
    file: { type: String, alias: 'f' },
    verbose: { type: Boolean, alias: 'v', optional: true },
  });
  $.verbose = args.verbose;
  const filePath = args.file;
  if (filePath && filePath.endsWith(testSuffix)) {
    if (!fs.existsSync(dist)) {
      fs.mkdirSync(dist);
    }
    const test = filePath.replace('src/', '').replace(testSuffix, '');
    const setupTestFilePath = `src/${test}.setup.ts`;
    const hasSetupFile = fs.existsSync(setupTestFilePath);
    let tearDownPath;
    if (hasSetupFile) {
      const tsConfigFilePath = buildTsConfig(setupTestFilePath, test);
      await $`yarn tsc --project ${tsConfigFilePath}`;
      const setupTestFiles = buildSetupFiles(test);
      tearDownPath = setupTestFiles.tearDownPath;
      info(`Init hook ${setupTestFiles.initPath}`);
      const dataAsString = output(
        await $`TS_NODE_BASEURL=./dist NODE_TLS_REJECT_UNAUTHORIZED="0" node --loader ts-node/esm -r tsconfig-paths/register ${setupTestFiles.initPath}`,
      );
      let data: GatewayTestData;
      try {
        // Check data format before k6 script
        data = JSON.parse(dataAsString);
      } catch (err) {
        error(err);
        throw err;
      }
      if (data.waitGateway) {
        await fetchGatewaySuccess({ contextPath: data.waitGateway.contextPath });
      }
      const testDataPath = writeTestData(test, dataAsString);
      const absoluteTestDataPath = resolve(testDataPath);
      K6_COMMAND.push(`-e TEST_DATA_PATH=${absoluteTestDataPath}`);
      TEARDOWN_ENV_VAR += `TEST_DATA_PATH=${testDataPath.replace('dist/', '')}`;
    }
    info('Run k6 test');
    const testFile = `src/${test}.test.js`;
    K6_COMMAND.push(testFile);
    await exec(K6_COMMAND.join(' '), true, { resetValue: args.verbose });

    if (hasSetupFile) {
      info(`Teardown hook ${test}`);
      output(
        await exec(`TS_NODE_BASEURL=./dist ${TEARDOWN_ENV_VAR} node --loader ts-node/esm -r tsconfig-paths/register ${tearDownPath}`, true),
      );
    }
  } else {
    error('Missing arg: Should be `./test-runner.ts src/use-case.test.js`');
  }
})();

function buildTsConfig(filePath: string, test: string): string {
  const tsConfigBase = fs.readFileSync('tsconfig.setup.json');
  const tsConfigGenerated = JSON.parse(tsConfigBase.toString());
  tsConfigGenerated.compilerOptions.baseUrl = '../';
  tsConfigGenerated.compilerOptions.typeRoots.push('../');
  tsConfigGenerated.compilerOptions.outDir = '.';
  tsConfigGenerated.include = [`../${filePath}`];
  const generateConfigPath = `dist/tsconfig.${test}.json`;
  fs.writeFileSync(generateConfigPath, JSON.stringify(tsConfigGenerated));
  return generateConfigPath;
}

function buildSetupFiles(test: string) {
  const initPath = `dist/src/${test}.init.js`;
  const tearDownPath = `dist/src/${test}.tearDown.js`;
  fs.writeFileSync(
    initPath,
    `require('./${test}.setup.js').init().then((d) => console.log(JSON.stringify(d, null, 2))).catch(console.error);`,
  );
  fs.writeFileSync(tearDownPath, `require('./${test}.setup.js').tearDown().then(console.log).catch(console.error);`);
  return { initPath, tearDownPath };
}

async function exec(command: string, unsafe = false, verbose?: { resetValue: boolean }) {
  if (verbose != null) {
    $.verbose = true;
  }
  let quote;
  if (unsafe) {
    quote = $.quote;
    $.quote = (v) => v;
  }
  const output = await $`${command}`;
  if (unsafe) {
    $.quote = quote;
  }
  if (verbose != null) {
    $.verbose = verbose.resetValue;
  }
  return output;
}

function writeTestData(test: string, data: any) {
  if (!fs.existsSync(assets)) {
    fs.mkdirSync(assets);
  }
  const testDataPath = `${assets}/${test}.json`;
  fs.writeFileSync(testDataPath, data);
  info(`File "${testDataPath}" written successfully !`);
  return testDataPath;
}

function info(message) {
  console.log(chalk.cyanBright(message));
}

function success(message) {
  console.log(chalk.green(message));
}

function error(message) {
  console.log(chalk.bold.red(message));
}

function warn(message) {
  console.log(chalk.hex('#FFA500')(message));
}

function output(processOutput: ProcessOutput): any {
  if (processOutput.exitCode === 1) {
    error(processOutput.stderr);
    warn(processOutput.stdout);
  } else {
    success(processOutput.stdout);
  }

  return processOutput.stdout;
}
