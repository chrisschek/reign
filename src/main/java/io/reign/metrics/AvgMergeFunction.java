package io.reign.metrics;

import java.util.List;

/**
 * 
 * @author ypai
 *
 */
public class AvgMergeFunction<T extends Number> implements MergeFunction<GaugeData<T>> {

    @Override
    public GaugeData<T> merge(List<GaugeData<T>> dataList) {
        if (dataList == null || dataList.size() < 1) {
            throw new IllegalStateException("dataList is null or empty.");
        }

        Number value = null;
        int i = 0;
        for (i = 0; i < dataList.size(); i++) {
            value = dataList.get(i).getValue();
            if (value != null) {
                break;
            }
        }

        if (value instanceof Long) {
            long sum = 0;
            int samples = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    sum += value.longValue();
                    samples++;
                }
            }
            return (GaugeData<T>) new GaugeData<Long>(sum / samples);

        } else if (value instanceof Double) {
            double sum = 0;
            int samples = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    sum += value.doubleValue();
                    samples++;
                }
            }
            return (GaugeData<T>) new GaugeData<Double>(sum / samples);

        } else if (value instanceof Float) {
            float sum = 0;
            int samples = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    sum += value.floatValue();
                    samples++;
                }
            }
            return (GaugeData<T>) new GaugeData<Float>(sum / samples);

        } else if (value instanceof Integer) {
            int sum = 0;
            int samples = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    sum += value.intValue();
                    samples++;
                }
            }
            return (GaugeData<T>) new GaugeData<Integer>(sum / samples);

        } else if (value instanceof Short) {
            short sum = 0;
            short samples = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    sum += value.shortValue();
                    samples++;
                }
            }
            return (GaugeData<T>) new GaugeData<Short>((short) (sum / samples));

        } else if (value instanceof Byte) {
            byte sum = 0;
            byte samples = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    sum += value.byteValue();
                    samples++;
                }
            }
            return (GaugeData<T>) new GaugeData<Byte>((byte) (sum / samples));

        } else {
            throw new IllegalArgumentException("Unsupported type:  " + value.getClass());
        }

    }
}
