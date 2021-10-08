has_been_modified () {
    target_branch=""
    pr_number=${CIRCLE_PULL_REQUEST##*/}

    # If we are on a PULL REQUEST
    if [[ ! -z $CIRCLE_PULL_REQUEST ]]; then
        curl -L "https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64" -o jq
        chmod +x jq
        url="https://api.github.com/repos/gravitee-io/${CIRCLE_PROJECT_REPONAME}/pulls/$pr_number"
        target_branch=$(curl "$url" | ./jq '.base.ref' | tr -d '"')
    fi
    echo "Target branch is $target_branch"
    # Check the diff between target branch and current branch
    git diff --name-only origin/${target_branch}...${CIRCLE_BRANCH} | grep "^$1" > /dev/null 2>&1
}

# Build systematically master branch
if [[ "${CIRCLE_BRANCH}" = "master" ]]; then exit 0; fi

# Return true if folder has been modified -> we should build
if has_been_modified $1; then exit 0; fi

# by default, do not build
exit 1