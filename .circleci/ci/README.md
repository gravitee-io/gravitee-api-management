# ci

## How it works

APIM CI run in two steps:

1. A pre-step will run the `generate-config` job generate from typescript code the circleCI workflow to execute. (via `npm run generate /tmp/dynamicConfig.yml`)
2. The CircleCI orbs `continuation/continue` execute the generated workflow.

## Install

### Prerequisites:

- Install https://github.com/nvm-sh/nvm[nvm]
- Use with `nvm use` or install with `nvm install` the version of Node.js declared in `.nvmrc`
- Install dependencies with:

```bash
npm install
```


## Development workflow:

1. Add typescript test with your expected CircleCI config output
2. Write the typescript code which generate CircleCI yaml config
4. Validate locally with `npm run test` and `npm run lint`
5. Run the CI to validate that the generated yaml file work as expected (if not, go to step 1)
6. Done


## Troubleshooting

### Generate CircleCI config locally:

```bash
#export CIRCLE_BRANCH=my-awesome-branch-🦄
#export GIT_BASE_BRANCH=master
export CI_ACTION=pull_requests
#export CIRCLE_TAG=4.1.0
export CI_TAG=4.1.0
export CIRCLE_SHA1=784ff35ca

npm run generate
```

It creates a local file `dynamicConfig.yml` that you can check if it looks like as expected.
