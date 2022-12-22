package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;

@Plugin( type = SpotAnalyzerFactory.class, priority = 0d )
public class MySpotRadiusEstimatorFactory< T extends RealType< T > & NativeType< T >> implements SpotAnalyzerFactory< T >
{

	/*
	 * CONSTANT
	 */

	/** The single feature key name that this analyzer computes. */
	public static final String ESTIMATED_DIAMETER = "ESTIMATED_DIAMETER";

	public static final ArrayList< String > FEATURES = new ArrayList< String >( 1 );

	public static final HashMap< String, String > FEATURE_NAMES = new HashMap< String, String >( 1 );

	public static final HashMap< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >( 1 );

	public static final HashMap< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 1 );

	public static final Map< String, Boolean > IS_INT = new HashMap< String, Boolean >( 1 );

	static
	{
		FEATURES.add( ESTIMATED_DIAMETER );
		FEATURE_NAMES.put( ESTIMATED_DIAMETER, "Estimated diameter" );
		FEATURE_SHORT_NAMES.put( ESTIMATED_DIAMETER, "Diam." );
		FEATURE_DIMENSIONS.put( ESTIMATED_DIAMETER, Dimension.LENGTH );
		IS_INT.put( ESTIMATED_DIAMETER, Boolean.FALSE );
	}

	public static final String KEY = "Spot radius estimator";


	/*
	 * METHODS
	 */
	@Override
	public SpotAnalyzer<T> getAnalyzer( final ImgPlus< T > img, final int frame, final int channel )
	{
		final ImgPlus< T > imgTC = hyperSlice( img, channel, frame );
		return new MySpotRadiusEstimator< T >( imgTC );
	}

	private ImgPlus<T> hyperSlice(ImgPlus<T> img, int channel, int frame) {
		
		final int timeDim = img.dimensionIndex( Axes.TIME );
		final ImgPlus< T > imgT = timeDim < 0 ? img : ImgPlusViews.hyperSlice( img, timeDim, frame );

		final int channelDim = imgT.dimensionIndex( Axes.CHANNEL );
		final ImgPlus< T > imgTC = channelDim < 0 ? imgT : ImgPlusViews.hyperSlice( imgT, channelDim, channel );

		// Squeeze Z dimension if its size is 1.
		final int zDim = imgTC.dimensionIndex( Axes.Z );
		final ImgPlus< T > imgTCZ;
		if ( zDim >= 0 && imgTC.dimension( zDim ) <= 1 )
			imgTCZ = ImgPlusViews.hyperSlice( imgTC, zDim, imgTC.min( zDim ) );
		else
			imgTCZ = imgTC;

		return imgTCZ;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return FEATURE_NAMES;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return FEATURE_DIMENSIONS;
	}

	@Override
	public String getInfoText()
	{
		return null;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return KEY;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}

}
