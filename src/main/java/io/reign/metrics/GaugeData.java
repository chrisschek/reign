package io.reign.metrics;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author ypai
 * 
 */
public class GaugeData<T> {
    private static final Logger logger = LoggerFactory.getLogger(GaugeData.class);

    private T value;

    public T getValue() {
        return value;
    }

    void setValue(T value) {

        try {
            Double doubleValue = Double.parseDouble(value.toString());
            this.value = (T) doubleValue;
        } catch (NumberFormatException e) {
            logger.error(e + "", e);
            this.value = value;
        }

    }

    public static GaugeData merge(List<GaugeData> dataList) {
        int samples = 0;
        double sum = 0;
        for (GaugeData data : dataList) {
            Object value = data.getValue();
            if (value instanceof Number) {
                sum += ((Number) value).doubleValue();
                samples++;
            } else if (value instanceof String) {
                try {
                    Double doubleValue = Double.parseDouble(value.toString());
                    sum += doubleValue;
                    samples++;
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        GaugeData gaugeData = new GaugeData();
        if (samples > 0) {
            gaugeData.setValue(sum / samples);
        }
        return gaugeData;
    }
}
