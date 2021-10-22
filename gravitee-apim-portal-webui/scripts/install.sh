#!/bin/bash
branch=$(git rev-parse --abbrev-ref HEAD)
if [ "$branch" != "master" ]
then
  {
    npm install --save gravitee-io/gravitee-ui-components#$branch --silent
  } || {
    echo "gravitee-io/gravitee-ui-components doesn't have branch: $branch"
  }
fi

npm install
