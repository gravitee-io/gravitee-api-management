require('dotenv').config();

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
