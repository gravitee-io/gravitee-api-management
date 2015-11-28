# 3.2.0 Ladybugs
- Fix for applyAsync typo and multiple executions of startBecauseMouseMoved #45

### Changes merged from dragula 3.5.2:
- Fixed a bug where `<select>` inputs couldn't be focused
- Fixed a bug when determining the mouse button being pressed
- Fixed a bug when determining the element behind the mouse cursor when `ignoreInputTextSelection` was enabled
- Added a feature where users are able to select text ranges with their mouse in inputs within a dragular container
- Fixed a bug where text in inputs inside containers assigned to `dragula` couldn't be selected
- Fixed a bug where `out` would be emitted with an `undefined` container
- Fixed a fringe bug [(#207)](https://github.com/bevacqua/dragula/pull/207) where the click handler wouldn't work
- Fixed a bug where `drop` events would sometimes not receive the current sibling

# 3.1.0 Brotherhood

- added `dragular-model` atribute as new optional way of provideng model when using dragular directive
- fixed gh-pages missed prefixes in Events example

### Changes merged from dragula 3.3.0:
- You can now use `options.copySortSource` to enable sorting in `copy`-source containers
- The `options.moves` callback now receives a fourth parameter, the `sibling` found after `el`

# 3.0.2 fix

- Fixed drop event firing before models updated

# 3.0.1 fix

- Fixed shared.item undefined in grab fn in some cases

# 3.0.0 Interdragular

- **[Braking change!]** scope events are renamed (drop => dragulardrop, shadow => dragularshadow, etc..)
- **[Braking change!]** options.accepts is splited into two methods options.accepts applied with target container options and options.canBeAccepted applied with source container options
- if options.isContainer is used model can be provided via function options.isContainerModel(containerElm)
- **[Braking change!]** removed dragOverClasses (dragOver events can be used instead);
- share containers between instances, so no need to nameSpace if containers are initialised separately (CAUTION: in case of nested containers you need to use nameSpace for each group/level_of_nesting)
- example with containers which has different options each
- **[Braking change!]** Now all non-nameSpaced continers are in same domain
- **[Braking change!]** Models are not updated until drag ends
- css selectors supportet in service `containers` parameter
- removed npm workflow

### Changes merged from dragula 3.1.0:
- You can now set `options.copy` to a method. It'll be invoked once per drag to ask whether the element being dragged should be treated as a copy or not
- Fixed a bug where starting a drag programatically while an element was being dragged resulted in an exception
- Fixed a bug where `mousedown` would be prevented and focusing draggable inputs wouldn't be possible
- Fixed a historical bug, where click on anchors would be ignored within dragula containers in mobile
- Fixed a bug where events wouldn't be gracefully removed if drake were destroyed during a drag event
- Now emits dragend after out to preserve consistency (because drag is emitted before over)
- Fixed another old bug where attempting to remove elements using removeOnSpill on mobile would fail
- Fixed a bug in mobile, caused by 3.0.0, where scrolling would be impossible
- Fixed a bug where dragging would cause text selection in IE8
- **[Braking change!]** Removed addContainer method, which was previously deprecated
- **[Braking change!]** Removed removeContainer method, which was previously deprecated
- **[Braking change!]** Removed delay option in favor of using mousemove
- Drag events now start on the first occurrence of a mousemove event
- If mousemove never fires, then the drag machinery won't start, either
- Changed default value for invalid, now always returns false by default
- Added mirrorContainer option to determine where the mirror gets appended to (defaults to document.body)

# 2.1.3 hotfix

- fixing makeArray internal function which causes reversed arrays and so unexpected behaviour

# 2.1.2 Damn

- fixing release (unminidied files had still 2.1.0 version)

# 2.1.1 patch

- fixing drake.destroy()

# 2.1.0 Elder Scrolls

- fixed scrolling while drag #14
- added new events fired on elements behind cursor while dragging (dragularenter, dragularleave), could be used to add/remove classes
- added new scope event 'release' when mouse button is released whatever ending it has (drop, cancel, revert..)

# 2.0.1 Lightning

- fixed forgoten lint errors

# 2.0.0 Aloha

- ngRepeat can be synced with provided model
- gulp workflow (thanks to @alferov)
- gh-pages refactored
- Fixed a bug where `shadow` would trigger multiple times while dragging an element over the same spot
- Fixed a bug where adding and removing classes might've caused issues on elements that had foreign CSS classes
- Added an argument to `cloned` event that specifies the kind of clone. Possible values include `mirror` and `copy` at the moment
- Added `over` event that fires whenever an element is dragged over a container _(or whenever a drag event starts)_
- Added `out` event that fires whenever an element is dragged out of a container _(or whenever a drag event ends)_
- Fixed a bug caused in `2.0.6` where anything would be regarded as a `drake` container
- Fixed a bug where `isContainer` would be called with a `el=null` in some situations
- Set `gu-transit` after a drag event has fully started
- Fixed a bug where using `.cancel` would throw an exception
- Fixed a bug where dragging a copy back to origin after hovering over another container would still result in a copy being made if you never spilled the item
- Deprecated addContainer method
- Deprecated removeContainer method
- Exposed dragula.containers collection
- Introduced dynamic isContainer method
- Can now omit containers argument to dragula(containers, options)
- Can now pass containers as an option
- Differentiate between drag and click using delay option
- Ability to specify which event targets are invalid drag triggers

# 1.3.2 Buggydown

- mousedown handlers moved from document to containers, due to bugs in namespacing-multiple-instances enviroment

# 1.3.1 Fix6

- fixes #6

# 1.3.0 Dragula203

- changes of dragula 2.0.3 against 1.6.1 implemented
- usage description augmented (#5)

# 1.2.1 Damn build

- forgot to build.. sorry

# 1.2.0 Directive

- feat(directive): accept options as JSON string
- fix(directive): minify-safe syntax added to directive, closes #3

# 1.1.0 tadaaa

- started correctly using Semantic Versioning 2.0.0 http://semver.org/
- avoiding angular.merge, so polyfill part was removed
- boundingBox (dragging element can me moved only in specific area)
- lockX/Y (dragging element can me moved only in specific direction)
- readded feature: automatic direction

# 1.0.3 nameSpaced containers

- allowing share of containers groups

# 1.0.2 customClasses

- custom classes via option.classes;

# 1.0.1 arraylike

- accepting arraylike objects as containers array;

# 1.0.0 IPO

- Initial Public Release (dragula 1.6.1, angular 1.4.1);
