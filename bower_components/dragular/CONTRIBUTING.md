# How to contribute

It's important to us that you feel you can contribute towards the evolution of Dragular. This can take many forms: from helping to fix bugs or improve the docs, to adding in new features to the source. This guide should help you in making that process as smooth as possible.

Before contributing, please read the [code of conduct](https://github.com/luckylooke/dragular/blob/master/CODE_OF_CONDUCT.md).

## Reporting issues

[GitHub Issues][0] is the place to report bugs you may have found in either the core library or any of the examples that are part of the repository. When submitting a bug please do the following:

**1. Search for existing issues.** Your bug may have already been fixed or addressed in a development branch version of Dragular, so be sure to search the issues first before putting in a duplicate issue.

**2. Not sure if it's a bug?.** Then please ask via issues and tag it [question].

**3. Create an isolated and reproducible test case.** If you are reporting a bug, make sure you also have a minimal, runnable, code example that reproduces the problem you have.

**4. Include a live example.** After narrowing your code down to only the problem areas, make use of [jsFiddle][1], [jsBin][2], or a link to your live site so that we can view a live example of the problem.

**5. Share as much information as possible.** Include browser version affected, your OS, version of the library, steps to reproduce, etc. "X isn't working!!!1!" will probably just be closed.

## Dev vs. Master

The dev branch of Dragular is our 'current working' version. It is always ahead of the master branch in terms of features and fixes. However it's also bleeding-edge and experimental and we cannot and do not guarantee it will compile or work for you. Very often we have to break things for a few days while we rebuild and patch. So by all means please export the dev branch and contribute towards it, indeed that is where all Pull Requests should be sent, but do so understanding the API may change beneath you.


## Making Changes

To take advantage of our npm build script and jshint config it will be easiest for you if you have node.js installed locally.

You can download node.js from [nodejs.org][3].

After that you can clone the repository and run `npm i` inside the cloned folder. This will install dependencies necessary for building the project. For development workflow automation dragular uses `gulp >= 3.9.0`. Before starting development, make sure that `gulp` is installed on your machine globally: `npm i -g gulp`.

### Developing

There are several gulp tasks that are used for generating different builds:

- `gulp dev` - Serves files with BrowserSync server, watches & automatically refreshes connected browsers on changes, generates non-minified but concatenated styles & scripts from the dragular source.
- `gulp dev:docs` - Does exactly the same as `gulp dev`, except it works with the documentation source.
- `gulp build` - Concatenates and minifies dragular source files.
- `gulp build:docs` - Concatenates and minifies documentation source files.

### Linting

- `gulp lint` & `gulp lint:docs` - Lint JavaScript files.

### Making a pull request

Once that is ready, make your changes and submit a Pull Request:

- **Send Pull Requests to the `dev` branch.** All Pull Requests must be sent to the `dev` branch, `master` is the latest release and PRs to that branch will be closed.

- **Ensure changes are jshint validated.** Our JSHint configuration file is provided in the repository and you should check against it before submitting.

- **Only commit relevant changes.** Don't include changes that are not directly relevant to the fix you are making. The more focused a PR is, the faster it will get attention and be merged. Extra files changing only whitespace or trash files will likely get your PR closed.


Dependencies for building from source and running tests:


## Coding style preferences are not contributions

If your PR is doing little more than changing the Dragular source code into a format / coding style that you prefer then we will automatically close it. All PRs must adhere to the coding style already set-out across the lines of code in Dragular. Your personal preferences for how things should "look" or be structured do not apply here, sorry. PRs should fix bugs, fix documentation or add features. No changes for the sake of change.


## I don't really like git / node.js, but I can fix this bug

That is fine too. While Pull Requests are the best thing in the world for us, they are not the only way to help. You're welcome to post fixes to our forum or even just email them to us. All we ask is that you still adhere to the guidelines presented here re: JSHint, etc.


[0]: https://github.com/luckylooke/dragular/issues
[1]: http://jsfiddle.net
[2]: http://jsbin.com/
[3]: http://nodejs.org
