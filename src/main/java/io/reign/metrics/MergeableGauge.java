package io.reign.metrics;

import com.codahale.metrics.Gauge;

public interface MergeableGauge<T, O> extends Gauge<T> {
    public MergeFunction<GaugeData<T>, GaugeData<O>> getMergeFunction();
}
