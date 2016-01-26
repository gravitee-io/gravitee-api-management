package io.gravitee.gateway.core.http.loadbalancer;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class WeightRatio {

    private int position;
    private int distribution;
    private int runtime;

    public WeightRatio(int position, int distribution) {
        this(position, distribution, distribution);
    }

    public WeightRatio(int processorPosition, int distribution, int runtime) {
        this.position = position;
        this.distribution = distribution;
        this.runtime = runtime;
    }

    public int getDistribution() {
        return distribution;
    }

    public void setDistribution(int distribution) {
        this.distribution = distribution;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }
}
