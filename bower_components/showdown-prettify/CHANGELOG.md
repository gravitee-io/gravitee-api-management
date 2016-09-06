<a name"1.3.0"></a>
## 1.3.0 (2016-01-06)

#### Release information

This update removes the warning that the extension is deprecated in the lastest versions of showdown.

#### Breaking Changes

Update extension to the new showdown's extenion loading mechanism.



<a name"1.0.2"></a>
### 1.0.2 (2015-06-04)

#### Release information

This is an hotfix for the documentation. (NPM requires a patch bump to update README.md)

<a name"1.0.1"></a>
### 1.0.1 (2015-06-04)

#### Release information

This is an hotfix for the npm package. Previously, the main attribute was poiting to an unexisting file. This hotfix fixes this.

#### Bug Fixes

* **package.json:** fix main attribute in package.json ([d219bf1a](https://github.com/showdownjs/prettify-extension/commit/d219bf1a))


<a name"1.0.0"></a>
## 1.0.0 (2015-06-04)

#### Release information

This release updates the extension in order to make it compatible with showdown v.1.0.x.
However, this version is not compatible with older versions of showdown.
Since prettify extension was previously bundled with showdown, with versions prior to v 1.0.x, use the bundled version instead.

#### Compatibility

**Compatible with showdown v.1.0.x**

#### Features

* **bower.json**: add dependencies to bower json
* **package.json**: add dependencies to package.json

#### Breaking changes

* **renamed files**: src file renamed to `showdown-prettify.js`.
  To update, if your using src files directly, change your dependencies from `prettify.js` to `showdown-prettify.js`
  
