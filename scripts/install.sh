#!/bin/bash
branch=$(git rev-parse --abbrev-ref HEAD)
if [ "$branch" != "master" ]
then
  rm -rf tmp
  mkdir -p tmp
  pushd tmp
  {
    git clone --single-branch --branch "$branch" https://github.com/gravitee-io/gravitee-clients-sdk.git
    if [ -d "gravitee-clients-sdk" ]
    then
      pushd gravitee-clients-sdk
      npm install --silent
      npm run build --silent
      pushd dist
      npm pack
      mv gravitee-ng-portal-webclient*.tgz gravitee-ng-portal-webclient.tgz
      popd
      popd
      popd
      npm i --save tmp/gravitee-clients-sdk/dist/gravitee-ng-portal-webclient.tgz --silent
    fi
  } || {
    echo "gravitee-io/gravitee-clients-sdk doesn't have branch: $branch"
    popd
  }

  {
    npm install --save gravitee-io/gravitee-ui-components#$branch --silent
  } || {
    echo "gravitee-io/gravitee-ui-components doesn't have branch: $branch"
  }
fi

npm install
