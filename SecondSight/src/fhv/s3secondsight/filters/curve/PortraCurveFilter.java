package fhv.s3secondsight.filters.curve;

public class PortraCurveFilter extends CurveFilter {
	
	public PortraCurveFilter() {
		super(
				new double[] { 0, 23, 157, 255 },
				new double[] { 0, 20, 173, 255 },
				new double[] { 0, 69, 213, 255 },
				new double[] { 0, 69, 218, 255 },
				new double[] { 0, 52, 189, 255 },
				new double[] { 0, 47, 196, 255 },
				new double[] { 0, 41, 231, 255 },
				new double[] { 0, 46, 228, 255 });
	}
}
