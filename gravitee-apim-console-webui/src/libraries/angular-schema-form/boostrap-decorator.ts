import * as angular from 'angular';

const initTemplateCacheRun = ($templateCache: angular.ITemplateCacheService) => {
  $templateCache.put(
    'directives/decorators/bootstrap/actions-trcl.html',
    '<div class="btn-group schema-form-actions {{form.htmlClass}}" ng-transclude=""></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/actions.html',
    '<div class="btn-group schema-form-actions {{form.htmlClass}}"><input ng-repeat-start="item in form.items" type="submit" class="btn {{ item.style || \'btn-default\' }} {{form.fieldHtmlClass}}" value="{{item.title}}" ng-if="item.type === \'submit\'"> <button ng-repeat-end="" class="btn {{ item.style || \'btn-default\' }} {{form.fieldHtmlClass}}" type="button" ng-disabled="form.readonly" ng-if="item.type !== \'submit\'" ng-click="buttonClick($event,item)"><span ng-if="item.icon" class="{{item.icon}}"></span>{{item.title}}</button></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/array.html',
    '<div sf-array="form" class="schema-form-array {{form.htmlClass}}" ng-model="$$value$$" ng-model-options="form.ngModelOptions"><label class="control-label" ng-show="showTitle()">{{ form.title }}</label><ol class="list-group" ng-model="modelArray" ui-sortable=""><li class="list-group-item {{form.fieldHtmlClass}}" ng-repeat="item in modelArray track by $index"><button ng-hide="form.readonly || form.remove === null" ng-click="deleteFromArray($index)" style="position: relative; z-index: 20;" type="button" class="close pull-right"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button><sf-decorator ng-init="arrayIndex = $index" form="copyWithIndex($index)"></sf-decorator></li></ol><div class="clearfix" style="padding: 15px;"><button ng-hide="form.readonly || form.add === null" ng-click="appendToArray()" type="button" class="btn {{ form.style.add || \'btn-default\' }} pull-right"><i class="glyphicon glyphicon-plus"></i> {{ form.add || \'Add\'}}</button></div><div class="help-block" ng-show="(hasError() && errorMessage(schemaError())) || form.description" ng-bind-html="(hasError() && errorMessage(schemaError())) || form.description"></div></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/checkbox.html',
    '<div class="checkbox schema-form-checkbox {{form.htmlClass}}" ng-class="{\'has-error\': form.disableErrorState !== true && hasError(), \'has-success\': form.disableSuccessState !== true && hasSuccess()}"><label class="{{form.labelHtmlClass}}"><input type="checkbox" sf-changed="form" ng-disabled="form.readonly" ng-model="$$value$$" ng-model-options="form.ngModelOptions" schema-validate="form" class="{{form.fieldHtmlClass}}" name="{{form.key.slice(-1)[0]}}"> <span ng-bind-html="form.title"></span></label><div class="help-block" sf-message="form.description"></div></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/checkboxes.html',
    '<div sf-array="form" ng-model="$$value$$" class="form-group schema-form-checkboxes {{form.htmlClass}}" ng-class="{\'has-error\': form.disableErrorState !== true && hasError(), \'has-success\': form.disableSuccessState !== true && hasSuccess()}"><label class="control-label {{form.labelHtmlClass}}" ng-show="showTitle()">{{form.title}}</label><div class="checkbox" ng-repeat="val in titleMapValues track by $index"><label><input type="checkbox" ng-disabled="form.readonly" sf-changed="form" class="{{form.fieldHtmlClass}}" ng-model="titleMapValues[$index]" name="{{form.key.slice(-1)[0]}}"> <span ng-bind-html="form.titleMap[$index].name"></span></label></div><div class="help-block" sf-message="form.description"></div></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/default.html',
    '<div class="form-group schema-form-{{form.type}} {{form.htmlClass}}" ng-class="{\'has-error\': form.disableErrorState !== true && hasError(), \'has-success\': form.disableSuccessState !== true && hasSuccess(), \'has-feedback\': form.feedback !== false }"><label class="control-label {{form.labelHtmlClass}}" ng-class="{\'sr-only\': !showTitle()}" for="{{form.key.slice(-1)[0]}}">{{form.title}}</label> <input ng-if="!form.fieldAddonLeft && !form.fieldAddonRight" ng-show="form.key" type="{{form.type}}" step="any" sf-changed="form" placeholder="{{form.placeholder}}" class="form-control {{form.fieldHtmlClass}}" id="{{form.key.slice(-1)[0]}}" ng-model-options="form.ngModelOptions" ng-model="$$value$$" ng-disabled="form.readonly" schema-validate="form" name="{{form.key.slice(-1)[0]}}" aria-describedby="{{form.key.slice(-1)[0] + \'Status\'}}"><div ng-if="form.fieldAddonLeft || form.fieldAddonRight" ng-class="{\'input-group\': (form.fieldAddonLeft || form.fieldAddonRight)}"><span ng-if="form.fieldAddonLeft" class="input-group-addon" ng-bind-html="form.fieldAddonLeft"></span> <input ng-show="form.key" type="{{form.type}}" step="any" sf-changed="form" placeholder="{{form.placeholder}}" class="form-control {{form.fieldHtmlClass}}" id="{{form.key.slice(-1)[0]}}" ng-model-options="form.ngModelOptions" ng-model="$$value$$" ng-disabled="form.readonly" schema-validate="form" name="{{form.key.slice(-1)[0]}}" aria-describedby="{{form.key.slice(-1)[0] + \'Status\'}}"> <span ng-if="form.fieldAddonRight" class="input-group-addon" ng-bind-html="form.fieldAddonRight"></span></div><span ng-if="form.feedback !== false" class="form-control-feedback" ng-class="evalInScope(form.feedback) || {\'glyphicon\': true, \'glyphicon-ok\': hasSuccess(), \'glyphicon-remove\': hasError() }" aria-hidden="true"></span> <span ng-if="hasError() || hasSuccess()" id="{{form.key.slice(-1)[0] + \'Status\'}}" class="sr-only">{{ hasSuccess() ? \'(success)\' : \'(error)\' }}</span><div class="help-block" sf-message="form.description"></div></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/fieldset-trcl.html',
    '<fieldset ng-disabled="form.readonly" class="schema-form-fieldset {{form.htmlClass}}"><legend ng-class="{\'sr-only\': !showTitle() }">{{ form.title }}</legend><div class="help-block" ng-show="form.description" ng-bind-html="form.description"></div><div ng-transclude=""></div></fieldset>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/fieldset.html',
    '<fieldset ng-disabled="form.readonly" class="schema-form-fieldset {{form.htmlClass}}"><legend ng-class="{\'sr-only\': !showTitle() }">{{ form.title }}</legend><div class="help-block" ng-show="form.description" ng-bind-html="form.description"></div><sf-decorator ng-repeat="item in form.items" form="item"></sf-decorator></fieldset>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/help.html',
    '<div class="helpvalue schema-form-helpvalue {{form.htmlClass}}" ng-bind-html="form.helpvalue"></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/radio-buttons.html',
    '<div class="form-group schema-form-radiobuttons {{form.htmlClass}}" ng-class="{\'has-error\': form.disableErrorState !== true && hasError(), \'has-success\': form.disableSuccessState !== true && hasSuccess()}"><div><label class="control-label {{form.labelHtmlClass}}" ng-show="showTitle()">{{form.title}}</label></div><div class="btn-group"><label class="btn {{ (item.value === $$value$$) ? form.style.selected || \'btn-default\' : form.style.unselected || \'btn-default\'; }}" ng-class="{ active: item.value === $$value$$ }" ng-repeat="item in form.titleMap"><input type="radio" class="{{form.fieldHtmlClass}}" sf-changed="form" style="display: none;" ng-disabled="form.readonly" ng-model="$$value$$" ng-model-options="form.ngModelOptions" schema-validate="form" ng-value="item.value" name="{{form.key.join(\'.\')}}"> <span ng-bind-html="item.name"></span></label></div><div class="help-block" sf-message="form.description"></div></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/radios-inline.html',
    '<div class="form-group schema-form-radios-inline {{form.htmlClass}}" ng-class="{\'has-error\': form.disableErrorState !== true && hasError(), \'has-success\': form.disableSuccessState !== true && hasSuccess()}"><label ng-model="$$value$$" ng-model-options="form.ngModelOptions" schema-validate="form" class="control-label {{form.labelHtmlClass}}" ng-show="showTitle()">{{form.title}}</label><div><label class="radio-inline" ng-repeat="item in form.titleMap"><input type="radio" class="{{form.fieldHtmlClass}}" sf-changed="form" ng-disabled="form.readonly" ng-model="$$value$$" ng-value="item.value" name="{{form.key.join(\'.\')}}"> <span ng-bind-html="item.name"></span></label></div><div class="help-block" sf-message="form.description"></div></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/radios.html',
    '<div class="form-group schema-form-radios {{form.htmlClass}}" ng-class="{\'has-error\': form.disableErrorState !== true && hasError(), \'has-success\': form.disableSuccessState !== true && hasSuccess()}"><label ng-model="$$value$$" ng-model-options="form.ngModelOptions" schema-validate="form" class="control-label {{form.labelHtmlClass}}" ng-show="showTitle()">{{form.title}}</label><div class="radio" ng-repeat="item in form.titleMap"><label><input type="radio" class="{{form.fieldHtmlClass}}" sf-changed="form" ng-disabled="form.readonly" ng-model="$$value$$" ng-value="item.value" name="{{form.key.join(\'.\')}}"> <span ng-bind-html="item.name"></span></label></div><div class="help-block" sf-message="form.description"></div></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/section.html',
    '<div class="schema-form-section {{form.htmlClass}}"><sf-decorator ng-repeat="item in form.items" form="item"></sf-decorator></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/select.html',
    '<div class="form-group {{form.htmlClass}} schema-form-select" ng-class="{\'has-error\': form.disableErrorState !== true && hasError(), \'has-success\': form.disableSuccessState !== true && hasSuccess(), \'has-feedback\': form.feedback !== false}"><label class="control-label {{form.labelHtmlClass}}" ng-show="showTitle()">{{form.title}}</label><select ng-model="$$value$$" ng-model-options="form.ngModelOptions" ng-disabled="form.readonly" sf-changed="form" class="form-control {{form.fieldHtmlClass}}" schema-validate="form" ng-options="item.value as item.name group by item.group for item in form.titleMap" name="{{form.key.slice(-1)[0]}}"></select><div class="help-block" sf-message="form.description"></div></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/submit.html',
    '<div class="form-group schema-form-submit {{form.htmlClass}}"><input type="submit" class="btn {{ form.style || \'btn-primary\' }} {{form.fieldHtmlClass}}" value="{{form.title}}" ng-disabled="form.readonly" ng-if="form.type === \'submit\'"> <button class="btn {{ form.style || \'btn-default\' }}" type="button" ng-click="buttonClick($event,form)" ng-disabled="form.readonly" ng-if="form.type !== \'submit\'"><span ng-if="form.icon" class="{{form.icon}}"></span> {{form.title}}</button></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/tabarray.html',
    '<div sf-array="form" ng-init="selected = { tab: 0 }" class="clearfix schema-form-tabarray schema-form-tabarray-{{form.tabType || \'left\'}} {{form.htmlClass}}"><div ng-if="!form.tabType || form.tabType !== \'right\'" ng-class="{\'col-xs-3\': !form.tabType || form.tabType === \'left\'}"><ul class="nav nav-tabs" ng-class="{ \'tabs-left\': !form.tabType || form.tabType === \'left\'}"><li ng-repeat="item in modelArray track by $index" ng-click="$event.preventDefault() || (selected.tab = $index)" ng-class="{active: selected.tab === $index}"><a href="#">{{interp(form.title,{\'$index\':$index, value: item}) || $index}}</a></li><li ng-hide="form.readonly" ng-click="$event.preventDefault() || (selected.tab = appendToArray().length - 1)"><a href="#"><i class="glyphicon glyphicon-plus"></i> {{ form.add || \'Add\'}}</a></li></ul></div><div ng-class="{\'col-xs-9\': !form.tabType || form.tabType === \'left\' || form.tabType === \'right\'}"><div class="tab-content {{form.fieldHtmlClass}}"><div class="tab-pane clearfix" ng-repeat="item in modelArray track by $index" ng-show="selected.tab === $index" ng-class="{active: selected.tab === $index}"><sf-decorator ng-init="arrayIndex = $index" form="copyWithIndex($index)"></sf-decorator><button ng-hide="form.readonly" ng-click="selected.tab = deleteFromArray($index).length - 1" type="button" class="btn {{ form.style.remove || \'btn-default\' }} pull-right"><i class="glyphicon glyphicon-trash"></i> {{ form.remove || \'Remove\'}}</button></div></div></div><div ng-if="form.tabType === \'right\'" class="col-xs-3"><ul class="nav nav-tabs tabs-right"><li ng-repeat="item in modelArray track by $index" ng-click="$event.preventDefault() || (selected.tab = $index)" ng-class="{active: selected.tab === $index}"><a href="#">{{interp(form.title,{\'$index\':$index, value: item}) || $index}}</a></li><li ng-hide="form.readonly" ng-click="$event.preventDefault() || appendToArray()"><a href="#"><i class="glyphicon glyphicon-plus"></i> {{ form.add || \'Add\'}}</a></li></ul></div></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/tabs.html',
    '<div ng-init="selected = { tab: 0 }" class="schema-form-tabs {{form.htmlClass}}"><ul class="nav nav-tabs"><li ng-repeat="tab in form.tabs" ng-disabled="form.readonly" ng-click="$event.preventDefault() || (selected.tab = $index)" ng-class="{active: selected.tab === $index}"><a href="#">{{ tab.title }}</a></li></ul><div class="tab-content {{form.fieldHtmlClass}}"><div class="tab-pane" ng-disabled="form.readonly" ng-repeat="tab in form.tabs" ng-show="selected.tab === $index" ng-class="{active: selected.tab === $index}"><bootstrap-decorator ng-repeat="item in tab.items" form="item"></bootstrap-decorator></div></div></div>',
  );
  $templateCache.put(
    'directives/decorators/bootstrap/textarea.html',
    '<div class="form-group has-feedback {{form.htmlClass}} schema-form-textarea" ng-class="{\'has-error\': form.disableErrorState !== true && hasError(), \'has-success\': form.disableSuccessState !== true && hasSuccess()}"><label class="{{form.labelHtmlClass}}" ng-class="{\'sr-only\': !showTitle()}" for="{{form.key.slice(-1)[0]}}">{{form.title}}</label> <textarea ng-if="!form.fieldAddonLeft && !form.fieldAddonRight" class="form-control {{form.fieldHtmlClass}}" id="{{form.key.slice(-1)[0]}}" sf-changed="form" placeholder="{{form.placeholder}}" ng-disabled="form.readonly" ng-model="$$value$$" ng-model-options="form.ngModelOptions" schema-validate="form" name="{{form.key.slice(-1)[0]}}"></textarea><div ng-if="form.fieldAddonLeft || form.fieldAddonRight" ng-class="{\'input-group\': (form.fieldAddonLeft || form.fieldAddonRight)}"><span ng-if="form.fieldAddonLeft" class="input-group-addon" ng-bind-html="form.fieldAddonLeft"></span> <textarea class="form-control {{form.fieldHtmlClass}}" id="{{form.key.slice(-1)[0]}}" sf-changed="form" placeholder="{{form.placeholder}}" ng-disabled="form.readonly" ng-model="$$value$$" ng-model-options="form.ngModelOptions" schema-validate="form" name="{{form.key.slice(-1)[0]}}"></textarea> <span ng-if="form.fieldAddonRight" class="input-group-addon" ng-bind-html="form.fieldAddonRight"></span></div><span class="help-block" sf-message="form.description"></span></div>',
  );
};
initTemplateCacheRun.$inject = ['$templateCache'];
angular.module('schemaForm').run(initTemplateCacheRun);

const bootstrapDecoratorConfig = (schemaFormDecoratorsProvider) => {
  const base = 'directives/decorators/bootstrap/';

  schemaFormDecoratorsProvider.defineDecorator(
    'bootstrapDecorator',
    {
      textarea: { template: base + 'textarea.html', replace: false },
      fieldset: { template: base + 'fieldset.html', replace: false },
      /* fieldset: {template: base + 'fieldset.html', replace: true, builder: function(args) {
 var children = args.build(args.form.items, args.path + '.items');
 console.log('fieldset children frag', children.childNodes)
 args.fieldFrag.childNode.appendChild(children);
 }},*/
      array: { template: base + 'array.html', replace: false },
      tabarray: { template: base + 'tabarray.html', replace: false },
      tabs: { template: base + 'tabs.html', replace: false },
      section: { template: base + 'section.html', replace: false },
      conditional: { template: base + 'section.html', replace: false },
      actions: { template: base + 'actions.html', replace: false },
      select: { template: base + 'select.html', replace: false },
      checkbox: { template: base + 'checkbox.html', replace: false },
      checkboxes: { template: base + 'checkboxes.html', replace: false },
      number: { template: base + 'default.html', replace: false },
      password: { template: base + 'default.html', replace: false },
      submit: { template: base + 'submit.html', replace: false },
      button: { template: base + 'submit.html', replace: false },
      radios: { template: base + 'radios.html', replace: false },
      'radios-inline': { template: base + 'radios-inline.html', replace: false },
      radiobuttons: { template: base + 'radio-buttons.html', replace: false },
      help: { template: base + 'help.html', replace: false },
      default: { template: base + 'default.html', replace: false },
    },
    [],
  );

  // manual use directives
  schemaFormDecoratorsProvider.createDirectives({
    textarea: base + 'textarea.html',
    select: base + 'select.html',
    checkbox: base + 'checkbox.html',
    checkboxes: base + 'checkboxes.html',
    number: base + 'default.html',
    submit: base + 'submit.html',
    button: base + 'submit.html',
    text: base + 'default.html',
    date: base + 'default.html',
    password: base + 'default.html',
    datepicker: base + 'datepicker.html',
    input: base + 'default.html',
    radios: base + 'radios.html',
    'radios-inline': base + 'radios-inline.html',
    radiobuttons: base + 'radio-buttons.html',
  });
};
bootstrapDecoratorConfig.$inject = ['schemaFormDecoratorsProvider'];
angular
  .module('schemaForm')
  .config(bootstrapDecoratorConfig)
  .directive('sfFieldset', () => {
    return {
      transclude: true,
      scope: true,
      templateUrl: 'directives/decorators/bootstrap/fieldset-trcl.html',
      link(scope: any, element, attrs) {
        scope.title = scope.$eval(attrs.title);
      },
    };
  });
