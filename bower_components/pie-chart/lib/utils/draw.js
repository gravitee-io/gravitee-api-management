draw: function(svg) {
  svg.append("g")
    .attr({
      "id": "n3-pie-arcs"
    })
  ;

  return this;
},

updatePaths: function(svg, data, dimensions, options) {
  var tools = this.getTools(dimensions, options);
  
  var tween = function(d) {
    var oldAngles = this.__current ? this.__current : {startAngle: d.startAngle, endAngle: d.startAngle};
    var newAngles = {startAngle: d.startAngle, endAngle: d.endAngle};

    var i = d3.interpolate(oldAngles, newAngles);
    
    return function(t) {return tools.arc(i(t)); };
  };
  
  var paths = svg.selectAll("#n3-pie-arcs")
    .selectAll('.arc')
    .data(tools.pie(data), function(d) {return d.data.label;})
  
  paths.enter()
    .append("path")
      .attr({
        "class": "arc",
        "id": function(d, i) {return "arc_" + i;}
      })
      .style({
        "fill": function(d) {return d.data.color;},
        "fill-opacity": 0.8
      })
  ;
  
  paths
    .transition()
      .duration(250)
      .attrTween("d", tween)
      .each("end", function(d) {
        this.__current = {startAngle: d.startAngle, endAngle: d.endAngle};
      })
  ;
  
  paths.exit().remove();
  
  return this;
},

getTools: function(dimensions, options) {
  var outerRadius = this.getRadius(dimensions);
  var innerRadius = Math.max(outerRadius - options.thickness, 0);

  var arc = d3.svg.arc()
    .outerRadius(outerRadius)
    .innerRadius(innerRadius);

  var pieLayout = d3.layout.pie()
    .value(function(d) { return d.value; });

  if (options.mode === "gauge") {
    pieLayout.sort(null);
  }
  
  return {pie: pieLayout, arc: arc};
}