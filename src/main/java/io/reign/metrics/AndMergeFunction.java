package io.reign.metrics;

import java.util.List;

/**
 * 
 * @author ypai
 *
 */
public class AndMergeFunction implements MergeFunction<GaugeData<Boolean>> {

    private boolean defaultValue = false;

    /** null is interpreted as false; if false, null is ignored */
    private boolean nullEqualsFalse = true;

    public AndMergeFunction defaultValue(boolean defaultValue) {
        setDefaultValue(defaultValue);
        return this;
    }

    public AndMergeFunction nullEqualsFalse(boolean nullEqualsFalse) {
        setNullEqualsFalse(nullEqualsFalse);
        return this;
    }

    public void setDefaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setNullEqualsFalse(boolean nullEqualsFalse) {
        this.nullEqualsFalse = nullEqualsFalse;
    }

    @Override
    public GaugeData<Boolean> merge(List<GaugeData<Boolean>> dataList) {
        if (dataList == null || dataList.size() < 1) {
            return new GaugeData<Boolean>(defaultValue);
        }

        Boolean result = true;
        for (GaugeData<Boolean> g : dataList) {
            Boolean value = g.getValue();
            if (value == null) {
                if (nullEqualsFalse) {
                    result = false;
                    break;
                }
            } else {
                if (!value) {
                    result = false;
                    break;
                }
            }
        }
        return new GaugeData<Boolean>(result);
    }
}
