package fhv.s3secondsight.filters.curve;

public class CrossProcessCurveFilter extends CurveFilter {

	public CrossProcessCurveFilter(){
		super(
				new double[] { 0, 255 },
				new double[] { 0, 255 },
				new double[] { 0, 56, 211, 255 },
				new double[] { 0, 22, 255, 255 },
				new double[] { 0, 56, 208, 255 },
				new double[] { 0, 39, 226, 255 },
				new double[] { 0, 255 },
				new double[] { 20, 235 });
	}
}
