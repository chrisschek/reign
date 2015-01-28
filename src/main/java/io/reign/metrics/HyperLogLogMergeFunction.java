package io.reign.metrics;

import io.reign.data.HLLEncoder;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;

public class HyperLogLogMergeFunction implements MergeFunction<GaugeData<String>, GaugeData<Long>> {

    private static final Logger logger = LoggerFactory.getLogger(HyperLogLogMergeFunction.class);

    @Override
    public GaugeData<Long> merge(List<GaugeData<String>> dataList) {
        GaugeData<Long> gaugeData = new GaugeData<Long>(0l);
        try {
            Iterator<GaugeData<String>> iter = dataList.iterator();
            HyperLogLogPlus hll = HLLEncoder.fromString(iter.next().getValue());
            while (iter.hasNext()) {
                HyperLogLogPlus nextHll = HLLEncoder.fromString(iter.next().getValue());
                hll.merge(nextHll);
            }
            gaugeData = new GaugeData<Long>(hll.cardinality());
        } catch (Exception e) {
            logger.debug("Exception {}", e);
        }
        return gaugeData;
    }
}
