const CircularPercentageComponent: ng.IComponentOptions = {
    bindings: {
      score: "<"
    },
    controller: "CircularPercentageController",
    template: require("./circularPercentage.html")
  };

  export default CircularPercentageComponent;

