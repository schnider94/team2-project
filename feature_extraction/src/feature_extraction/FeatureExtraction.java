package feature_extraction;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class FeatureExtraction {
	DescriptiveStatistics data;
	double mean=0;
	double variance=0;
	double energy=0;
	double avg_abs_diff=0;
	double skew=0;
	double kurt = 0;

	double rms=0;//root mean square
	
	
	FeatureExtraction(int windowSize){
		data = new DescriptiveStatistics(windowSize);
		
	}
	void doFeatureExtraction() {
		mean = data.getMean();
		variance = data.getVariance();
		skew = data.getSkewness();
		kurt = data.getKurtosis();
		energy = data.getSumsq()/data.getN();

		rms = Math.sqrt(energy);
		
		avg_abs_diff = getAvgAbsDiff();

		
		
	}
	double getAvgAbsDiff() {
		double[] array = data.getValues();
		double sumAbs = 0;
		for(int i = 0; i < array.length;i++) {
			sumAbs += array[i];
		}
		return sumAbs/data.getN();
	}
public static void main(String[] args) {
	FeatureExtraction featExtr =new FeatureExtraction(3);
	featExtr.data.addValue(3.676);
	featExtr.data.addValue(4.540);
	featExtr.data.addValue(1.3024);
	featExtr.doFeatureExtraction();
	double[] array =featExtr.data.getValues();
	for(int i = 0; i < array.length;i++) {
		System.out.println(array[i]);
	}
	
	
	System.out.println("avg abs diff:" + featExtr.avg_abs_diff);
	System.out.println("mean:" + featExtr.mean);
	System.out.println("energy: "+ featExtr.energy);
	System.out.println("variance: " + featExtr.variance);
	System.out.println("skew: "+ featExtr.skew);
	System.out.println("kurtosis:" + featExtr.avg_abs_diff );
	System.out.println("root mean square:" + featExtr.avg_abs_diff);
	featExtr.data.addValue(0.02456);
	featExtr.doFeatureExtraction();
	System.out.println("avg abs diff:" + featExtr.avg_abs_diff);
	System.out.println("mean:" + featExtr.mean);
	System.out.println("energy: "+ featExtr.energy);
	System.out.println("variance: " + featExtr.variance);
	System.out.println("skew: "+ featExtr.skew);
	System.out.println("kurtosis:" + featExtr.avg_abs_diff );
	System.out.println("root mean square:" + featExtr.avg_abs_diff);
}       
}
