package fhv.s3secondsight.filters.curve;

public class VelviaCurveFilter extends CurveFilter {

	public VelviaCurveFilter() {
		super(
				new double[] { 0, 128, 221, 255 },
				new double[] { 0, 118, 215, 255 },
				new double[] { 0, 25, 122, 255 },
				new double[] { 0, 21, 123, 255 },
				new double[] { 0, 25, 95, 255 },
				new double[] { 0, 21, 102, 255 },
				new double[] { 0, 35, 205, 255 },
				new double[] { 0, 25, 227, 255 });
	}
}
