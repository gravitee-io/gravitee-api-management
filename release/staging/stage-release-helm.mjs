/**
 * Use this script only if you want to generate a pre-release to be deployed on master.
 */
import xml2json from 'xml2json';

import { checkToken } from '../helpers/circleci-helper.mjs';

await checkToken();

const pomLocation = path.resolve(__dirname, '..', '..', 'pom.xml');
const pomContent = await fs.readFile(pomLocation, 'utf-8');
const jsonPom = JSON.parse(await xml2json.toJson(pomContent, {}));

const branch = 'master';
const graviteeio_version = jsonPom.project.properties.revision;
const gio_action = 'release_helm';
const dry_run = true;

const pipelineURI = 'https://circleci.com/api/v2/project/gh/gravitee-io/gravitee-api-management/pipeline';

console.log(
    chalk.yellow(
        `This script will stage a new helm release in order to test it on ${branch}.`
    )
);
console.log(
    chalk.yellow(
        `This will be done by overwriting or creating chart apim ${graviteeio_version} at oci://graviteeio.azurecr.io/helm.`)
);
console.log(
    chalk.yellow(
        `If you are performing an actual release, then you're in the wrong place.`
    )
);

console.log();

const iKnowWhatImDoing = await question(
    chalk.blue(
        `Do you want to continue ? (Type 'yes' if you want to)\n`
    )
);

console.log();

if (iKnowWhatImDoing !== 'yes') {
    console.log(chalk.greenBright('👋 Farewell, pal!'));
    process.exit(0);
}

const body = {
    branch,
    parameters: {
        gio_action,
        dry_run,
        graviteeio_version,
    },
};

const response = await fetch(pipelineURI, {
    method: 'post',
    body: JSON.stringify(body),
    headers: {
        'Content-Type': 'application/json',
        'Circle-Token': process.env.CIRCLECI_TOKEN,
    }
});

const data = await response.json();

if (response.status === 201) {
    console.log(chalk.green(`Pipeline created with number: ${data.number}`));
    echo`Follow its progress on: https://app.circleci.com/pipelines/github/gravitee-io/gravitee-api-management/${data.number}`;
} else {
    console.log(chalk.red(`Something went wrong. Expected status 201, got ${response.status}.`));
}
