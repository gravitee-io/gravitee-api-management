# ci

## How it works

APIM CI run in two steps:

1. A pre-step will run the `generate-config` job generate from typescript code the circleCI workflow to execute. (via `npm run generate /tmp/dynamicConfig.yml`)
2. The CircleCI orbs `continuation/continue` execute the generated workflow.

### Conditions to generate the pipeline
The pipeline generation is available according to different conditions:
    - if the branch is supported ( CIRCLE_BRANCH is master or a support branch )
    - if we are working on a branch with changes committed on the base branch


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
export APIM_VERSION_PATH=/replace-me/gravitee/gravitee-api-management/pom.xml
export CIRCLE_BRANCH=master
export CI_ACTION=publish_docker_images
export CIRCLE_SHA1=784ff35ca
export CI_DRY_RUN=false
export CI_DOCKER_TAG_AS_LATEST=false
export CI_GRAVITEEIO_VERSION=4.1.0
npm run generate
```

It creates a local file `dynamicConfig.yml` that you can check if it looks like as expected.

### Run CI for testing purpose

After checked that the `dynamicConfig.yml` file looks like as expected. You still don't have the guaranty that CircleCI
will execute what you have in mind. To do so, you need to *really* execute code on CI.

> [!WARNING]
> Be careful, the fact that CI is running mean that it is executed for real, even if dry-run mode is enabled. So double check what you are doing and do not hesitate to ask for review before any execution.

Example to trigger `build_rpm` on current branch with latest tag:

```bash
export APIM_VERSION_PATH="${PWD%gravitee-api-management/*}/gravitee-api-management/pom.xml"     
export CIRCLE_BRANCH="$(git branch --show-current)"                                          
export CI_ACTION="build_rpm"
export CIRCLE_SHA1="$(git rev-parse --short HEAD)"
export CI_DRY_RUN=true 
export CI_DOCKER_TAG_AS_LATEST=false
export CI_GRAVITEEIO_VERSION="$(git tag -l | sed '/-/!{s/$/_/;}; s/-patch/_patch/' | sort -V | sed 's/_$//; s/_patch/-patch/' | tail -n 1)"

npm run generate
```

Then, if you are satisfied by the yaml output, you can add a *temporary commit* to force CI to trigger a specific build by updating the `config.yml` file:

```yaml
        environment:
            CI_ACTION: "build_rpm"
            CI_DRY_RUN: "true"
            CI_GRAVITEEIO_VERSION: "4.7.2"
            CI_DOCKER_TAG_AS_LATEST: "false"
```
> [!WARNING]
> Be sure to use dry-run and control what will be run as it will be executed for real.
