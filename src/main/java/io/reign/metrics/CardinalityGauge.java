package io.reign.metrics;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;

public class CardinalityGauge implements MergeableGauge<String, Long> {

    private HyperLogLogPlus hll;

    public CardinalityGauge() {
        hll = new HyperLogLogPlus(13);
    }

    public CardinalityGauge(int p) {
        hll = new HyperLogLogPlus(p);
    }

    public CardinalityGauge(int p, int sp) {
        hll = new HyperLogLogPlus(p, sp);
    }

    public void offer(Object o) {
        hll.offer(o);
    }

    @Override
    public String getValue() {
        return HLLEncoder.toString(hll);
    }

    @Override
    public MergeFunction<GaugeData<String>, GaugeData<Long>> getMergeFunction() {
        return new HyperLogLogMergeFunction();
    }

}
