class CircularPercentageController {
    private score: number = 0;
    private percentage: number = 0;
    private percentageCircle: number = 100;
    private qualityMetricCssClass: string;

    $onInit() {

        if (this.score > 0) {
            this.percentage = this.score * 100;
            this.percentageCircle = this.percentage;
        }

        if ( this.percentage < 50 ) {
        this.qualityMetricCssClass = "gravitee-qm-score-bad";
        } else if (this.percentage >= 50 && this.percentage < 80) {
        this.qualityMetricCssClass = "gravitee-qm-score-medium";
        } else {
        this.qualityMetricCssClass =  "gravitee-qm-score-good";
        }
    }
}

export default CircularPercentageController;
