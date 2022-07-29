#! ./node_modules/.bin/ts-node
"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
require("dotenv/config");
const zx_1 = require("zx");
const path_1 = require("path");
const gateway_1 = require("../lib/gateway");
const ts_command_line_args_1 = require("ts-command-line-args");
const testSuffix = '.test.js';
const dist = 'dist';
const assets = 'dist/assets';
void (async function () {
    console.log(process.argv);
    const args = (0, ts_command_line_args_1.parse)({
        // @ts-ignore
        file: { type: String, alias: 'f' },
        verbose: { type: Boolean, alias: 'v', optional: true },
    }, { argv: process.argv, stopAtFirstUnknown: false });
    console.log(args);
    zx_1.$.verbose = args.verbose;
    const filePath = args.file;
    if (filePath && filePath.endsWith(testSuffix)) {
        if (!zx_1.fs.existsSync(dist)) {
            zx_1.fs.mkdirSync(dist);
        }
        const test = filePath.replace('src/', '').replace(testSuffix, '');
        const setupTestFile = `src/${test}.setup.ts`;
        const tsConfigFilePath = buildTsConfig(setupTestFile, test);
        await (0, zx_1.$) `yarn tsc --project ${tsConfigFilePath}`;
        const { initPath, tearDownPath } = buildSetupFiles(test);
        info(`Init hook ${initPath}`);
        const data = output(await (0, zx_1.$) `TS_NODE_BASEURL=./dist NODE_TLS_REJECT_UNAUTHORIZED="0" node --loader ts-node/esm -r tsconfig-paths/register ${initPath}`);
        // @ts-ignore
        if (data.waitGateway) {
            // @ts-ignore
            await (0, gateway_1.fetchGatewaySuccess)({ contextPath: data.waitGateway.contextPaths[0] });
        }
        const testDataPath = writeTestData(test, data);
        const absoluteTestDataPath = (0, path_1.resolve)(testDataPath);
        info('Run k6 test');
        const testFile = `src/${test}.test.js`;
        zx_1.$.verbose = true;
        await (0, zx_1.$) `k6 run -e TEST_DATA_PATH=${absoluteTestDataPath} -e GATEWAY_BASE_URL=${process.env.GATEWAY_BASE_URL} -e SKIP_TLS_VERIFY=${process.env.SKIP_TLS_VERIFY} ${testFile}`;
        zx_1.$.verbose = false;
        info(`Teardown hook ${test}`);
        output(await (0, zx_1.$) `TS_NODE_BASEURL=./dist TEST_DATA_PATH=${testDataPath.replace('dist/', '')} node --loader ts-node/esm -r tsconfig-paths/register ${tearDownPath}`);
    }
    else {
        error('Missing arg: Should be `./test-runner.ts src/use-case.test.js`');
    }
})();
function buildTsConfig(filePath, test) {
    const tsConfigBase = zx_1.fs.readFileSync('tsconfig.setup.json');
    const tsConfigGenerated = JSON.parse(tsConfigBase.toString());
    tsConfigGenerated.compilerOptions.baseUrl = '../';
    tsConfigGenerated.compilerOptions.typeRoots.push('../');
    tsConfigGenerated.compilerOptions.outDir = '.';
    tsConfigGenerated.include = [`../${filePath}`];
    const generateConfigPath = `dist/tsconfig.${test}.json`;
    zx_1.fs.writeFileSync(generateConfigPath, JSON.stringify(tsConfigGenerated));
    return generateConfigPath;
}
function buildSetupFiles(test) {
    const initPath = `dist/src/${test}.init.js`;
    const tearDownPath = `dist/src/${test}.tearDown.js`;
    zx_1.fs.writeFileSync(initPath, `require('./${test}.setup.js').init().then((d) => console.log(JSON.stringify(d, null, 2))).catch(console.error);`);
    zx_1.fs.writeFileSync(tearDownPath, `require('./${test}.setup.js').tearDown().then(console.log).catch(console.error);`);
    return { initPath, tearDownPath };
}
function writeTestData(test, data) {
    if (!zx_1.fs.existsSync(assets)) {
        zx_1.fs.mkdirSync(assets);
    }
    const testDataPath = `${assets}/${test}.json`;
    zx_1.fs.writeFileSync(testDataPath, data);
    try {
        // Check data format before k6 script
        JSON.parse(data);
    }
    catch (err) {
        error(err);
        throw err;
    }
    info(`File "${testDataPath}" written successfully !`);
    return testDataPath;
}
function info(message) {
    console.log(zx_1.chalk.cyanBright(message));
}
function success(message) {
    console.log(zx_1.chalk.green(message));
}
function error(message) {
    console.log(zx_1.chalk.bold.red(message));
}
function warn(message) {
    console.log(zx_1.chalk.hex('#FFA500')(message));
}
function output(processOutput) {
    if (processOutput.exitCode === 1) {
        error(processOutput.stderr);
        warn(processOutput.stdout);
    }
    else {
        success(processOutput.stdout);
    }
    return processOutput.stdout;
}
