/*! pie-chart - v1.0.0 - 2013-11-11
* https://github.com/n3-charts/pie-chart
* Copyright (c) 2013 n3-charts  Licensed ,  */
'use strict';

/*global window */

describe('n3-piechart', function() {
  beforeEach(module('n3-pie-chart'));

  beforeEach(function() {
    this.addMatchers({
      toBeSameStyleAs: function(expected) {
        var actual = this.actual;
        var notText = this.isNot ? " not" : "";

        this.message = function () {
          return "Expected " + actual + notText + " to be same as " + expected;
        };

        var exp = expected.split(/\s*;\s*/g);
        var comp = actual.split(/\s*;\s*/g);

        var colors = ['fill', 'stroke'];

        for (var key in exp) {
          if (comp[key] === undefined) {
            return false;
          }

          if (comp[key] !== exp[key]) {
            var spl = comp[key].split(/\s*:\s*/);
            var expSpl = exp[key].split(/\s*:\s*/);

            if (colors.indexOf(spl[0]) !== -1) {
              if (d3.rgb(spl[1]).toString() !== d3.rgb(expSpl[1]).toString()) {
                return false;
              }
            } else {
              return false;
            }
          }
        }

        return true;
      },
      
      toBeSamePathAs: function(expected) {
        var actual = this.actual;
        var notText = this.isNot ? " not" : "";

        var errorMsg = "";

        this.message = function () {
          return "Expected " + actual + notText + " to be same path as " + expected + " (" + errorMsg + ")";
        };
        
        if (!actual) {
          return false;
        }
        
        var getInstructions = function(path) {
          var a = [];
          
          var sp = path.split(/([A-Z])/g);
          
          for (var i = 1; i < sp.length; i+=2) {
            a.push({type: sp[i], values: sp[i+1].split(/[\s,]/)});
          }
          
          return a;
        };
        
        var expectedArray = getInstructions(expected);
        var actualArray = getInstructions(actual);
        
        if (expectedArray.length !== actualArray.length) {
          return false;
        }
        
        for (var i = 0; i < expectedArray.length; i++) {
          var a = expectedArray[i];
          var b = actualArray[i];
          
          if (a.type !== b.type) {
            errorMsg = a.type + " !== " + b.type;
            return false;
          }
          
          if (a.values.length !== b.values.length) {
            errorMsg = "different values length";
            return false;
          }
          
          for (var j = 0; j < a.values.length; j++) {
            var vA = a.values[j];
            var vB = b.values[j];
            
            if (parseInt(vA, 10) !== parseInt(vB, 10) && vA !== "" && vB !== "") {
              errorMsg = vA + " !== " + vB;
              return false;
            }
          }
        }
        
        return true;
      }
      
    });
  });

describe("gauge mode", function() {
  beforeEach(inject(function($utils) {
    spyOn($utils, 'getDefaultMargins').andReturn(
      {top: 20, right: 50, bottom: 30, left: 50}
    );
  }));
  
  var elm, $scope, isolatedScope;
  beforeEach(inject(function($rootScope, $compile) {
    elm = angular.element('<div id="toto">' +
      '<pie-chart data="data" options="options"></pie-chart>' +
      '</div>');

    $scope = $rootScope;
    $compile(elm)($scope);
    $scope.$digest();
  }));
  
  var svg, content;
  beforeEach(function() {
    $scope.data = [
      {label: "CPU", value: 84, color: "#0a182d"}
    ];   
    $scope.options = {mode: "gauge", thickness: 5};
    $scope.$apply();
    
    svg = elm[0].childNodes[0].childNodes[0];
    content = svg.childNodes[0];
  });
  
  describe("data array", function() {
    it("should be left untouched", function() {
      expect($scope.data).toEqual([
        {label: "CPU", value: 84, color: "#0a182d"}
      ]);
    });
  });
  
  describe("update", function() {
    beforeEach(inject(function($utils) {
      spyOn($utils, 'draw').andCallThrough();
      spyOn($utils, 'updatePaths').andCallThrough();
    }));
    
    it("should be called instead of redraw if only data changes", inject(function($utils) {
      expect($utils.draw.callCount).toBe(0);
      expect($utils.updatePaths.callCount).toBe(0);
      
      $scope.data = [
        {label: "CPU", value: 34, color: "#0a182d"}
      ];
      $scope.$apply();
      
      expect($utils.draw.callCount).toBe(0);
      expect($utils.updatePaths.callCount).toBe(1);
      
      $scope.data = [
        {label: "CPU", value: 42, color: "#0a182d"}
      ];
      $scope.$apply();
      
      expect($utils.draw.callCount).toBe(0);
      expect($utils.updatePaths.callCount).toBe(2);
    }));
  });
  
  describe("arcs", function() {
    var arcs;
    beforeEach(function() {
      arcs = content.childNodes[0].childNodes;
    });
    
    it("should be in a single group", function() {
      expect(content.childNodes.length).toBe(2);
      expect(content.childNodes[0].getAttribute("id")).toBe("n3-pie-arcs");
    });
    
    it("should be two", function() {
      expect(arcs.length).toBe(2);
    });
    
    it("should create arcs paths", function() {
      var arcs = content.childNodes[0];
      
      var expected = [
        {"class": "arc", "id": "arc_0", "style": "fill: #0a182d; fill-opacity: 0.8;"},
        {"class": "arc", "id": "arc_1", "style": "fill: white; fill-opacity: 0.8;"}
      ];
      
      expected.forEach(function(d, i) {
        expect(arcs.childNodes[i].nodeName).toBe("path");
        expect(arcs.childNodes[i].getAttribute("class")).toBe(d["class"]);
        expect(arcs.childNodes[i].getAttribute("id")).toBe(d["id"]);
        expect(arcs.childNodes[i].getAttribute("style").trim()).toBeSameStyleAs(d["style"]);
      });
      
      expected = [
        "M1.3777276490407723e-14,-225A225,225 0 1,1 -189.97378323795337," +
        "-120.56102887027427L-185.75214361044328,-117.88189489537929A220," +
        "220 0 1,0 1.3471114790620885e-14,-220Z",
        "M-189.97378323795337,-120.56102887027427A225,225 0 0,1 -4.13318" +
        "2947122317e-14,-225L-4.0413344371862656e-14,-220A220,220 0 0,0 -" +
        "185.75214361044328,-117.88189489537929Z"
      ];
      
      waits(500);

      runs(function () {
        expected.forEach(function(d, i) {
          expect(arcs.childNodes[i].getAttribute("d")).toBeSamePathAs(d);
        });
      });
    });
    
    it("should not be sorted", function() {
      $scope.data = [
        {label: "CPU", value: 4, color: "#0a182d"}
      ];
      $scope.$apply();
      
      
      var expected = [
        "M1.3777276490407723e-14,-225A225,225 0 0,1 55.955224612092316," +
        "-217.931211253942L54.71177517626804,-213.08829544829882A220,220" +
        " 0 0,0 1.3471114790620885e-14,-220Z",
        "M55.955224612092316,-217.931211253942A225,225 0 1,1 1.58508314" +
        "961305e-13,-225L1.5498590796216488e-13,-220A220,220 0 1,0 54.71" +
        "177517626804,-213.08829544829882Z"
      ];
      
      // The transition lasts 250ms...
      waits(300);

      svg = elm[0].childNodes[0].childNodes[0];
      content = svg.childNodes[0];
      arcs = content.childNodes[0].childNodes;
      runs(function () {
        expected.forEach(function(d, i) {
          expect(arcs[i].getAttribute("d")).toBeSamePathAs(d);
        });
      });
    });
  });
  
  describe("legend items", function() {
    it("should be in a single legend group", function() {
      expect(content.childNodes.length).toBe(2);
      expect(content.childNodes[1].getAttribute("id")).toBe("n3-pie-legend");
    });
    
    it("should be two : one title and one value", function() {
      var legendItems = content.childNodes[1].childNodes;
      
      expect(legendItems.length).toBe(2);
    });
    
    describe("title", function() {
      var title;
      beforeEach(function() {
        title = content.childNodes[1].childNodes[0];
      });
      
      it("should be a stylized text", function() {
        expect(title.nodeName).toBe("text");
        expect(title.getAttribute("text-anchor")).toBe("middle");
        expect(title.getAttribute("style").trim()).toBeSameStyleAs(
          "font-size: 55px; fill: #0a182d; fill-opacity: 0.8;"
        );
      });
      
      it("should be classed as legend-title", function() {
        expect(title.getAttribute("class")).toBe("legend-title");
      });
      
      it("should contain the series label", function() {
        expect(title.textContent).toBe("CPU");
      });
    });
    
    describe("value", function() {
      var value;
      beforeEach(function() {
        value = content.childNodes[1].childNodes[1];
      });
      
      it("should be a stylized text", function() {
        expect(value.nodeName).toBe("text");
        expect(value.getAttribute("text-anchor")).toBe("middle");
        expect(value.getAttribute("style").trim()).toBeSameStyleAs(
          "font-size: 110px; fill: #0a182d; fill-opacity: 0.8;"
        );
      });
      
      it("should be classed as legend-value", function() {
        expect(value.getAttribute("class")).toBe("legend-value");
      });
      
      it("should contain the series label", function() {
        expect(value.textContent).toBe("84");
      });
      
      it("should be updated", function() {
        $scope.data = [
          {label: "CPU", value: 34, color: "#0a182d"}
        ];
        $scope.$apply();
        
        expect(content.childNodes[1].childNodes[1].textContent).toBe("34");
      });
      
      it("should be allow suffix", function() {
        $scope.data = [
          {label: "RAM", value: 34, suffix: "%", color: "#0a182d"}
        ];
        $scope.$apply();
        
        expect(content.childNodes[1].childNodes[1].textContent).toBe("34%");
      });
      
    });
    
  });
  
  
  
});
describe("legend", function() {
  
  describe("getLegendLabel", function() {
    it("should normalize label", inject(function($utils) {
      expect($utils.getLegendLabel("Series", 12.25, 20)).toBe(
        "Series ....... 12.25"
      );
    }));
  });
  
  describe("getLegendLabelFunction", function() {
    it("should return nothing if chart too small", inject(function($utils) {
      var fakeData = {label: "My series", value: 42};
      
      var f = $utils.getLegendLabelFunction(39);
      expect(f(fakeData)).toBe("");
      
      f = $utils.getLegendLabelFunction(99);
      expect(f(fakeData)).toBe("My series");
      
      f = $utils.getLegendLabelFunction(100);
      expect(f(fakeData)).toBe("My series ....... 42");
    }));
  });
  
  describe("items", function() {
    beforeEach(inject(function($utils) {
      spyOn($utils, 'getDefaultMargins').andReturn(
        {top: 20, right: 50, bottom: 30, left: 50}
      );
    }));
    
    var elm, $scope, isolatedScope;
    beforeEach(inject(function($rootScope, $compile) {
      elm = angular.element('<div id="toto">' +
        '<pie-chart data="data" options="options"></pie-chart>' +
        '</div>');

      $scope = $rootScope;
      $compile(elm)($scope);
      $scope.$digest();
    }));
    
    var svg, content;
    beforeEach(function() {
      $scope.data = [
        {label: "one", value: 12.2, color: "red"}, 
        {label: "two", value: 45, color: "rgb(10, 24, 45)"},
        {label: "three", value: 10, color: "rgba(10, 24, 45, 0.7)"},
        {label: "Fourth series", value: 50, color: "#123456"}
      ];   
      $scope.options = {thickness: 5};
      $scope.$apply();
      
      svg = elm[0].childNodes[0].childNodes[0];
      content = svg.childNodes[0];
    });
    
    it("should be in a single legend group", function() {
      expect(content.childNodes.length).toBe(2);
      expect(content.childNodes[1].getAttribute("id")).toBe("n3-pie-legend");
    });
    
    it("should be four", function() {
      var legendItems = content.childNodes[1].childNodes;
      
      expect(legendItems.length).toBe(4);
    });
    
    it("should be stylized", function() {
      var legendItems = content.childNodes[1].childNodes;
      
      var expected = [
        "font-family: monospace; font-size: 22px; fill: #ff0000; fill-opacity: 0.8;",
        "font-family: monospace; font-size: 22px; fill: #0a182d; fill-opacity: 0.8;",
        "font-family: monospace; font-size: 22px; fill: rgba(10, 24, 45, 0.70196); fill-opacity: 0.8;",
        "font-family: monospace; font-size: 22px; fill: #123456; fill-opacity: 0.8;"
      ];
      
      expected.forEach(function(d, i) {
        expect(legendItems[i].getAttribute("style").trim()).toBeSameStyleAs(d);
      });
    });
    
    it("should be translated", function() {
      var legendItems = content.childNodes[1].childNodes;
      
      var expected = [
        "translate(0, -88)",
        "translate(0, -29.333333333333336)", // this is ugly...
        "translate(0, 29.33333333333333)",
        "translate(0, 88)"
      ];
      
      expected.forEach(function(d, i) {
        expect(legendItems[i].nodeName).toBe("text");
        expect(legendItems[i].getAttribute("transform")).toBe(d);
      });
    });
    
    it("should be text elements", function() {
      var legendItems = content.childNodes[1].childNodes;
      
      var expected = [
        {"text-anchor": "middle", content: "one ........... 12.2"},
        {"text-anchor": "middle", content: "two ............. 45"},
        {"text-anchor": "middle", content: "three ........... 10"},
        {"text-anchor": "middle", content: "Fourth series ... 50"}
      ];
      
      var p = function(i) {return legendItems[i];};
      expected.forEach(function(d, i) {
        expect(p(i).nodeName).toBe("text");
        expect(p(i).textContent).toBe(d.content);
        expect(p(i).getAttribute("text-anchor")).toBe(d["text-anchor"]);
      });
    });

    it("should call mouseover", inject(function($utils) {
      var arcs = content.childNodes[0].childNodes;
      var legendItems = content.childNodes[1].childNodes;
      
      var expectedLegends = [
        "font-family: monospace; font-size: 22px; fill: #ff0000; fill-opacity: 0.8; opacity: 1;",
        "font-family: monospace; font-size: 22px; fill: #0a182d; fill-opacity: 0.8; opacity: 0.4;",
        "font-family: monospace; font-size: 22px; fill: rgba(10, 24, 45, 0.70196); fill-opacity: 0.8; opacity: 0.4;",
        "font-family: monospace; font-size: 22px; fill: #123456; fill-opacity: 0.8; opacity: 0.4;"
      ];
      
      var expectedArcs = [
        {id: "arc_0", style: "fill: #ff0000; fill-opacity: 0.8; opacity: 1;"},
        {id: "arc_1", style: "fill: #0a182d; fill-opacity: 0.8; opacity: 0.4;"},
        {id: "arc_2", style: "fill: rgba(10, 24, 45, 0.70196); fill-opacity: 0.8; opacity: 0.4;"},
        {id: "arc_3", style: "fill: #123456; fill-opacity: 0.8; opacity: 0.4;"}
      ];
      
      
      var e = document.createEvent("MouseEvents");
      e.initMouseEvent("mouseover");
      legendItems[0].dispatchEvent(e);
      
      
      // There is a transition we have to wait for.
      // BTW : Jasmine is awesome.
      waits(50);

      runs(function () {
        expectedLegends.forEach(function(d, i) {
          expect(legendItems[i].getAttribute("style").trim()).toBeSameStyleAs(d);
        });
        
        expectedArcs.forEach(function(d, i) {
          expect(arcs[i].getAttribute("style").trim()).toBeSameStyleAs(d.style);
          expect(arcs[i].getAttribute("id")).toBe(d.id);
        });
      });
    }));
    
    it("should call mouseout", inject(function($utils) {
      var arcs = content.childNodes[0].childNodes;
      var legendItems = content.childNodes[1].childNodes;
      
      var expectedLegends = [
        "font-family: monospace; font-size: 22px; fill: #ff0000; fill-opacity: 0.8; opacity: 1;",
        "font-family: monospace; font-size: 22px; fill: #0a182d; fill-opacity: 0.8; opacity: 1;",
        "font-family: monospace; font-size: 22px; fill: rgba(10, 24, 45, 0.70196); fill-opacity: 0.8; opacity: 1;",
        "font-family: monospace; font-size: 22px; fill: #123456; fill-opacity: 0.8; opacity: 1;"
      ];
      
      var expectedArcs = [
        {id: "arc_0", style: "fill: #ff0000; fill-opacity: 0.8; opacity: 1;"},
        {id: "arc_1", style: "fill: #0a182d; fill-opacity: 0.8; opacity: 1;"},
        {id: "arc_2", style: "fill: rgba(10, 24, 45, 0.70196); fill-opacity: 0.8; opacity: 1;"},
        {id: "arc_3", style: "fill: #123456; fill-opacity: 0.8; opacity: 1;"}
      ];
      
      runs(function () {
        var e = document.createEvent("MouseEvents");
        e.initMouseEvent("mouseout");
        legendItems[0].dispatchEvent(e);
      });
      
      // There is a transition we have to wait for.
      // BTW : Jasmine is awesome.
      waits(50);

      runs(function () {
        expectedLegends.forEach(function(d, i) {
          expect(legendItems[i].getAttribute("style").trim()).toBeSameStyleAs(d);
        });
        
        expectedArcs.forEach(function(d, i) {
          expect(arcs[i].getAttribute("style").trim()).toBeSameStyleAs(d.style);
          expect(arcs[i].getAttribute("id")).toBe(d.id);
        });
      });
    }));
  })
});
describe("misc", function() {
  describe("looksLikeSameSeries", function() {
    it("should be false when different amount of series", inject(function($utils) {
      var newData = [];
      var oldData = [{label: "foo", value: 42, color: "red"}];
      
      expect($utils.looksLikeSameSeries(newData, oldData)).toBeFalsy();
    }));
    
    it("should be true when only the value changed", inject(function($utils) {
      var oldData = [{label: "foo", value: 42, color: "red"}];
      var newData = [{label: "foo", value: 37, color: "red"}];
      
      expect($utils.looksLikeSameSeries(newData, oldData)).toBeTruthy();
    }));
    
    it("should be false when a label has changed", inject(function($utils) {
      var oldData = [{label: "foo", value: 42, color: "red"}];
      var newData = [{label: "bar", value: 42, color: "red"}];
      
      expect($utils.looksLikeSameSeries(newData, oldData)).toBeFalsy();
    }));
    
    it("should be false when a color has changed", inject(function($utils) {
      var oldData = [{label: "bar", value: 42, color: "blue"}];
      var newData = [{label: "bar", value: 42, color: "red"}];
      
      expect($utils.looksLikeSameSeries(newData, oldData)).toBeFalsy();
    }));
  });
});
describe("options", function() {
  describe("sanitizeOptions", function() {
    it("should handle thickness", inject(function($utils) {
      expect($utils.sanitizeOptions({})).toEqual({thickness: 20});
      
      expect($utils.sanitizeOptions({thickness: "10"})).toEqual({thickness: 10});
    }));
    
    it("should handle gauge mode - default total to 100", inject(function($utils) {
      expect($utils.sanitizeOptions({mode: "gauge"})).toEqual({
        mode: "gauge",
        thickness: 20,
        total: 100
      });
      
      expect($utils.sanitizeOptions({mode: "gauge", total: "80"})).toEqual({
        mode: "gauge",
        thickness: 20,
        total: 80
      });
    }));
  });
});
describe('size', function() {
  beforeEach(inject(function($utils) {
    spyOn($utils, 'getDefaultMargins').andReturn(
      {top: 20, right: 50, bottom: 30, left: 50}
    );
    
    spyOn($utils, 'draw').andCallThrough();
  }));
  
  var elm, $scope, isolatedScope;
  beforeEach(inject(function($rootScope, $compile) {
    elm = angular.element('<div id="toto">' +
      '<pie-chart data="data" options="options"></pie-chart>' +
      '</div>');

    $scope = $rootScope;
    $compile(elm)($scope);
    $scope.$digest();
  }));

  xit('should update when $window resize', inject(function($window, $utils) {
    $scope.data = [
      {label: "one", value: 12.2, color: "red"}, 
      {label: "two", value: 45, color: "green"},
      {label: "three", value: 10, color: "blue"},
      {label: "Fourth series", value: 50, color: "yellow"}
    ];
      
    $scope.options = {thickness: 5};
    $scope.$apply();
    expect($utils.draw.callCount).toBe(2);
    
    var e = document.createEvent('HTMLEvents');
    e.initEvent('resize', true, false);
    $window.dispatchEvent(e);
    expect($utils.draw.callCount).toBe(3);
  }));
});
describe('standard mode', function() {
  
  var elm, $scope, isolatedScope;
  
  beforeEach(inject(function($rootScope, $compile) {
    elm = angular.element('<div id="toto">' +
      '<pie-chart data="data" options="options"></pie-chart>' +
      '</div>');

    $scope = $rootScope;
    $compile(elm)($scope);
    $scope.$digest();
  }));
  
  beforeEach(inject(function($utils) {
    spyOn($utils, 'getDefaultMargins').andReturn(
      {top: 20, right: 50, bottom: 30, left: 50}
    );
    
    spyOn($utils, 'draw').andCallThrough();
  }));
  
  it('should call redraw() when data or options change', inject(function($utils) {
    $scope.data = [
      {label: "one", value: 12.2, color: "red"}, 
      {label: "two", value: 45, color: "green"},
      {label: "three", value: 10, color: "blue"},
      {label: "Fourth series", value: 50, color: "yellow"}
    ];
    $scope.$apply();
    expect($utils.draw.callCount).toBe(0);
      
    $scope.options = {thickness: 5};
    $scope.$apply();
    expect($utils.draw.callCount).toBe(1);
    
    $scope.options.thickness = 10;
    $scope.$apply();
    expect($utils.draw.callCount).toBe(2);
    
    // $scope.data.shift();
    // $scope.$apply();
    // expect($utils.draw.callCount).toBe(3);
  }));
  
  describe("content generation", function() {
    var svg, content;
    
    beforeEach(function() {
      $scope.data = [
        {label: "one", value: 12.2, color: "red"}, 
        {label: "two", value: 45, color: "rgb(10, 24, 45)"},
        {label: "three", value: 10, color: "rgba(10, 24, 45, 0.7)"},
        {label: "Fourth series", value: 50, color: "#123456"}
      ];   
      $scope.options = {thickness: 5};
      $scope.$apply();
      
      svg = elm[0].childNodes[0].childNodes[0];
      content = svg.childNodes[0];
    });
    
    it('should create the svg and the main group', function() {
      expect(svg.nodeName).toBe("svg");
      expect(svg.childNodes.length).toBe(1);
      expect(content.nodeName).toBe("g");
    });
    
    it("should create two groups : arcs and legend items", function() {
      expect(content.childNodes.length).toBe(2);
      
      expect(content.childNodes[0].getAttribute("id")).toBe("n3-pie-arcs");
      expect(content.childNodes[1].getAttribute("id")).toBe("n3-pie-legend");
    });
    
    it("should create paths with proper style", function() {
      var arcs = content.childNodes[0];
      
      var expected = [
        {"id": "arc_0", "style": "fill: #ff0000; fill-opacity: 0.8;"},
        {"id": "arc_1", "style": "fill: #0a182d; fill-opacity: 0.8;"},
        {"id": "arc_2", "style": "fill: rgba(10, 24, 45, 0.70196); fill-opacity: 0.8;"},
        {"id": "arc_3", "style": "fill: #123456; fill-opacity: 0.8;"}
      ];
      
      expected.forEach(function(d, i) {
        expect(arcs.childNodes[i].getAttribute("class")).toBe("arc");
        expect(arcs.childNodes[i].getAttribute("id")).toBe(d.id);
        expect(arcs.childNodes[i].getAttribute("style").trim()).toBe(d.style);
      });
    });
    
    it("should create paths with proper data", function() {
      var arcs = content.childNodes[0];
      
      var expected = [
        "M-222.82279198005287,-89.16279142228608A240,240 0 0,1 -122.59052896853278,-206.32877212646648L-120.03655961502169,-202.03025604049841A235,235 0 0,0 -218.18065048046842,-87.30523326765511Z",
        "M106.77390401186753,214.9403019958437A240,240 0 0,1 -222.82279198005287,-89.16279142228608L-218.18065048046842,-87.30523326765511A235,235 0 0,0 104.54944767828695,210.46237903759697Z",
        "M-122.59052896853278,-206.32877212646648A240,240 0 0,1 -4.408728476930471e-14,-240L-4.3168799669944197e-14,-235A235,235 0 0,0 -120.03655961502169,-202.03025604049841Z",
        "M1.469576158976824e-14,-240A240,240 0 0,1 106.77390401186753,214.9403019958437L104.54944767828695,210.46237903759697A235,235 0 0,0 1.43895998899814e-14,-235Z"
      ];
      
      // The transition lasts 300ms...
      waits(300);

      runs(function () {
        expected.forEach(function(d, i) {
          expect(arcs.childNodes[i].nodeName).toBe("path");
          expect(arcs.childNodes[i].getAttribute("d")).toBeSamePathAs(d);
        });
      });
    });
  });
});
});