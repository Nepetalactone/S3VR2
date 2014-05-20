package fhv.s3secondsight.filters.ar;

import fhv.s3secondsight.filters.NoneFilter;

public class NoneARFilter extends NoneFilter implements ARFilter{

	@Override
	public float[] getGLPose() {
		return null;
	}

}
