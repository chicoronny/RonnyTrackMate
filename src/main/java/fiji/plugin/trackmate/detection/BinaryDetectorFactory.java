package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeTargetChannel;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;
import ij.ImagePlus;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

@Plugin( type = SpotDetectorFactory.class )
public class BinaryDetectorFactory<T extends RealType< T > & NativeType< T >> implements SpotDetectorFactory<T> {

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "BINARY_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Binary detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>" + "This detector uses a binary image for determining objects." + "</html>";

	public static final String KEY_MIN = "KEY_MIN";

	public static final String KEY_MAX = "KEY_MAX";

	public static final String KEY_OPTIONS = "KEY_OPTIONS";

	public static final int DEFAULT_MIN = 10;

	public static final int DEFAULT_MAX = 10000;

	public static final int DEFAULT_OPTIONS = ParticleAnalyzer.CLEAR_WORKSHEET | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.INCLUDE_HOLES;

	protected ImgPlus<T> img;

	protected Map< String, Object > settings;

	protected String errorMessage;

	private ImagePlus imp;

	@Override
	public ImageIcon getIcon() {
		return null;
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

	@Override
	public String getKey() {
		return DETECTOR_KEY;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean checkSettings(Map<String, Object> settings) {
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_MIN, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_MAX, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_OPTIONS, Integer.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_MIN);
		mandatoryKeys.add( KEY_MAX );
		mandatoryKeys.add( KEY_OPTIONS );
		ok = ok & checkMapKeys( settings, mandatoryKeys, null, errorHolder );
		if ( !ok ){
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public Map<String, Object> getDefaultSettings() {
		final Map< String, Object > settings = new HashMap< String, Object >(4);
		settings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		settings.put( KEY_MIN, DEFAULT_MIN );
		settings.put( KEY_MAX, DEFAULT_MAX );
		settings.put( KEY_OPTIONS, DEFAULT_OPTIONS );
		return settings;
	}

	@Override
	public SpotDetector<T> getDetector(final Interval interval, final int frame) {
		final Integer min = ( Integer ) settings.get( KEY_MIN );
		final Integer max = ( Integer ) settings.get( KEY_MAX );
		final Integer options = ( Integer ) settings.get( KEY_OPTIONS );

		Integer measurements = Analyzer.getMeasurements();
		if ((measurements & Analyzer.FERET) == 0) 
			measurements = measurements | Analyzer.FERET;
		if ((measurements & Analyzer.CIRCULARITY) == 0) 
			measurements = measurements | Analyzer.CIRCULARITY;
		if ((measurements & Analyzer.CENTER_OF_MASS) == 0) 
			measurements = measurements | Analyzer.CENTER_OF_MASS;
		final RandomAccessibleInterval< T > imFrame = prepareFrameImg( frame );
		
		final BinaryDetector<T> detector = new BinaryDetector<T>(imFrame, min, max, options, measurements);
		return detector;
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel(Settings settings, Model model) {
		this.imp = settings.imp;
		return new BinaryDetectorConfigurationPanel( imp, INFO_TEXT, NAME, model);
	}


	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public boolean marshall(Map<String, Object> settings, Element element) {
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = writeTargetChannel( settings, element, errorHolder )
			&& writeAttribute(settings,element,KEY_MAX, Integer.class, errorHolder)
			&& writeAttribute(settings,element,KEY_MIN, Integer.class, errorHolder)
			&& writeAttribute(settings,element,KEY_OPTIONS, Integer.class, errorHolder);
		if ( !ok ){
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean setTarget(ImgPlus<T> img, Map<String, Object> settings) {
		this.img = img;
		this.settings = settings;
		return checkSettings( settings );
	}

	@Override
	public boolean unmarshall(Element element, Map<String, Object> settings) {
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readIntegerAttribute( element, settings, KEY_MIN, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_MAX, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_OPTIONS, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_TARGET_CHANNEL, errorHolder );
		if ( !ok ){
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( settings );
	}
	
	protected RandomAccessibleInterval< T > prepareFrameImg( final int frame )
	{
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		RandomAccessibleInterval< T > imFrame;
		final int cDim = TMUtils.findCAxisIndex( img );
		if ( cDim < 0 )
		{
			imFrame = img;
		}
		else
		{
			// In ImgLib2, dimensions are 0-based.
			final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ) - 1;
			imFrame = Views.hyperSlice( img, cDim, channel );
		}

		int timeDim = TMUtils.findTAxisIndex( img );
		if ( timeDim >= 0 )
		{
			if ( cDim >= 0 && timeDim > cDim )
			{
				timeDim--;
			}
			imFrame = Views.hyperSlice( imFrame, timeDim, frame );
		}

		// In case we have a 1D image.
		if ( img.dimension( 0 ) < 2 )
		{ // Single column image, will be rotated internally.
			calibration[ 0 ] = calibration[ 1 ]; // It gets NaN otherwise
			calibration[ 1 ] = 1;
			imFrame = Views.hyperSlice( imFrame, 0, 0 );
		}
		if ( img.dimension( 1 ) < 2 )
		{ // Single line image
			imFrame = Views.hyperSlice( imFrame, 1, 0 );
		}

		return imFrame;
	}

}
