package io.reign.metrics;

import java.util.List;

public class MeterMergeFunction implements MergeFunction<MeterData> {

    @Override
    public MeterData merge(List<MeterData> dataList) {
        int datumCount = dataList.size();
        long samples = 0;
        double meanRateSum = 0;
        double m1RateSum = 0;
        double m5RateSum = 0;
        double m15RateSum = 0;
        for (MeterData data : dataList) {
            meanRateSum += (data.getMeanRate() * data.getCount());
            m1RateSum += (data.getM1Rate() * data.getCount());
            m5RateSum += (data.getM5Rate() * data.getCount());
            m15RateSum += (data.getM15Rate() * data.getCount());
            samples += data.getCount();
        }

        MeterData meterData = new MeterData();
        meterData.setCount(samples);

        if (dataList.size() > 0) {
            meterData.setRateUnit(dataList.get(0).getRateUnit());
        }

        if (samples > 0) {
            // average rates and then multiple by number of data points to get
            // aggregate rates
            meterData.setMeanRate(meanRateSum / samples * datumCount);
            meterData.setM1Rate(m1RateSum / samples * datumCount);
            meterData.setM5Rate(m5RateSum / samples * datumCount);
            meterData.setM15Rate(m15RateSum / samples * datumCount);
        }

        return meterData;
    }

}
