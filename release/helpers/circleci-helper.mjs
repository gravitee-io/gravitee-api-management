require('dotenv').config({ quiet: true });

/**
 * Check if user has a valid CircleCI Token. If no token, asks him to enter one.
 * @return {Promise<void>}
 */
export async function checkToken() {
  if (!process.env.CIRCLECI_TOKEN) {
    console.log(chalk.red('Create .env file and add the variable CIRCLECI_TOKEN'));
    process.exit();
  }

  let response = await fetch('https://circleci.com/api/v2/me', {
    headers: {
      'Circle-Token': process.env.CIRCLECI_TOKEN,
    },
  });

  let body = await response.json();
  if (response.status === 401) {
    console.log(chalk.red('Unauthorized CircleCI token'));
    process.exit();
  } else {
    console.log(chalk.green(`Logged as ${body.login}\n`));
  }
}

/**
 * Starts a pipeline on CircleCI and reports where to watch it.

 * @param {string} branch the branch the pipeline runs on
 * @param {object} parameters the pipeline parameters
 */
export async function triggerPipeline(branch, parameters) {
  const response = await fetch('https://circleci.com/api/v2/project/gh/gravitee-io/gravitee-api-management/pipeline', {
    method: 'post',
    body: JSON.stringify({ branch, parameters }),
    headers: {
      'Content-Type': 'application/json',
      'Circle-Token': process.env.CIRCLECI_TOKEN,
    },
  });

  const data = await response.json();
  if (response.status !== 201) {
    console.log(chalk.red('Something went wrong'));
    console.log(data);
    process.exit(1);
  }

  console.log(chalk.green(`Pipeline created with number: ${data.number}`));
  echo`Follow its progress on: https://app.circleci.com/pipelines/github/gravitee-io/gravitee-api-management/${data.number}`;
}
