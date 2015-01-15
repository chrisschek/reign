package io.reign.metrics;

import com.codahale.metrics.Gauge;

public interface MergeableGauge<T> extends Gauge<T> {
    public MergeFunction<GaugeData<T>> getMergeFunction();
}
