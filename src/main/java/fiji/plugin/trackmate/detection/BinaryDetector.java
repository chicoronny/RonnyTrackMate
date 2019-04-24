package fiji.plugin.trackmate.detection;

import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;

public class BinaryDetector<T extends RealType< T > & NativeType< T >> implements SpotDetector<T> {
	
	private final static String BASE_ERROR_MESSAGE = "LogDetector: ";
	private ImageProcessor ip;

	protected String errorMessage;
	private long processingTime;
	private ImagePlus imp;
	/** The list of {@link Spot} that will be populated by this detector. */
	protected List< Spot > spots = new ArrayList< Spot >();
	private int minSize;
	private int maxSize;
	private int options;
	private int measurements;
	private double circMin;
	private double circMax;

	public BinaryDetector(final RandomAccessibleInterval<T> img, final int minSize, final int maxSize, final double circMin, final double circMax, final int options, final int measurements) {
		this.imp = ImageJFunctions.wrap(img, "");
		this.ip = imp.getProcessor();
		this.minSize = minSize;
		this.maxSize = maxSize;
		this.circMin = circMin;
		this.circMax = circMax;
		this.options = options;
		this.measurements = measurements;
	}

	@Override
	public List<Spot> getResult() {
		return spots;
	}

	@Override
	public boolean checkInput() {
		if ( null == imp ){
			errorMessage = BASE_ERROR_MESSAGE + "Image is null.";
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public boolean process() {
		final long start = System.currentTimeMillis();
		if (!ip.isInvertedLut())
			ip.invertLut();
		final ResultsTable results = new ResultsTable();
		final ParticleAnalyzer analyzer = new ParticleAnalyzer(options, measurements, results, minSize, maxSize, circMin, circMax);
		analyzer.setHideOutputImage(true);
		analyzer.analyze(imp, ip);
		ResultsTableToSpots(results);
		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
		return true;
	}

	@Override
	public long getProcessingTime() {
		return processingTime;
	}
	
	private void ResultsTableToSpots(ResultsTable rt){
		final Map<String,double[]> table = new LinkedHashMap<String, double[]>();
		final String[] headings = rt.getHeadings();
		for (int i=0; i<headings.length; i++){
			int ret = rt.getColumnIndex(headings[i]);
			if (ret == ResultsTable.COLUMN_NOT_FOUND) continue;
			double[] col = rt.getColumnAsDoubles(ret);
			if (col == null) continue;
			table.put(headings[i], col);
		}
		for (int j=0;j<rt.getCounter();j++){
			final double radius = (table.get("Feret")[j]+table.get("MinFeret")[j])/4;
			final Spot spot = new Spot(table.get("XM")[j], table.get("YM")[j] , 0, radius, table.get("Max")[j]);
			spot.putFeature("Area", table.get("Area")[j]);
			spot.putFeature("Circ.", table.get("Circ.")[j]);
			spots.add(spot);
		}
		return;
	}

}
