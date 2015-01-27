package io.reign.metrics;

import java.util.List;

public class HistogramMergeFunction implements MergeFunction<HistogramData, HistogramData> {

    @Override
    public HistogramData merge(List<HistogramData> dataList) {
        long samples = 0;
        double meanSum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        double stddevSum = 0;

        double p50Sum = 0;
        double p75Sum = 0;
        double p95Sum = 0;
        double p98Sum = 0;
        double p99Sum = 0;
        double p999Sum = 0;

        for (HistogramData data : dataList) {
            samples += data.getCount();
            meanSum += data.getMean() * data.getCount();
            min = Math.min(data.getMin(), min);
            max = Math.max(data.getMax(), max);

            stddevSum += Math.pow(data.getStddev(), 2);

            p50Sum += data.getP50() * data.getCount();
            p75Sum += data.getP75() * data.getCount();
            p95Sum += data.getP95() * data.getCount();
            p98Sum += data.getP98() * data.getCount();
            p99Sum += data.getP99() * data.getCount();
            p999Sum += data.getP999() * data.getCount();
        }

        HistogramData data = new HistogramData();
        data.setCount(samples);
        data.setMin(samples > 0 ? min : 0);
        data.setMax(samples > 0 ? max : 0);

        // sqrt of variances
        data.setStddev(Math.sqrt(stddevSum));

        // weighted avgs
        if (samples > 0) {
            data.setMean(meanSum / samples);
            data.setP50(p50Sum / samples);
            data.setP75(p75Sum / samples);
            data.setP95(p95Sum / samples);
            data.setP98(p98Sum / samples);
            data.setP99(p99Sum / samples);
            data.setP999(p999Sum / samples);
        }

        return data;
    }

}
