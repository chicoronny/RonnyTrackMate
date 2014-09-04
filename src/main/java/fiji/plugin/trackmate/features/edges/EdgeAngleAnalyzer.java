package fiji.plugin.trackmate.features.edges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@Plugin( type = EdgeAnalyzer.class )
public class EdgeAngleAnalyzer implements EdgeAnalyzer {
    
    private static final String KEY = "Edge angle";
    private static final String EDGE_ANGLE = "EDGE_ANGLE";
    private String INFO_TEXT = "angle between adjacent spots and the x-axis";
    private long processingTime;
	private int numThreads;
    private static final List< String > FEATURES = new ArrayList< String >( 1 );
    private static final Map< String, Boolean > IS_INT = new HashMap< String, Boolean >( 1 );
    public static final Map< String, String > FEATURE_NAMES = new HashMap< String, String >( 1 );
    public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >( 1 );
    public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 1 );
    
    static{
	FEATURES.add( EDGE_ANGLE );
	IS_INT.put( EDGE_ANGLE, false );
	FEATURE_NAMES.put( EDGE_ANGLE, "Link angle" );
	FEATURE_SHORT_NAMES.put( EDGE_ANGLE, "Angle" );
	FEATURE_DIMENSIONS.put( EDGE_ANGLE, Dimension.ANGLE );
    }

    @Override
    public long getProcessingTime() {
	return processingTime;
    }

    @Override
    public List<String> getFeatures() {
	return FEATURES;
    }

    @Override
    public Map<String, String> getFeatureShortNames() {
	return FEATURE_SHORT_NAMES;
    }

    @Override
    public Map<String, String> getFeatureNames() {
	return FEATURE_NAMES;
    }

    @Override
    public Map<String, Dimension> getFeatureDimensions() {
	return FEATURE_DIMENSIONS;
    }

    @Override
    public Map<String, Boolean> getIsIntFeature() {
	return Collections.unmodifiableMap( IS_INT );
    }

    @Override
    public boolean isManualFeature() {
	return false;
    }

    @Override
    public String getInfoText() {
	return INFO_TEXT;
    }

    @Override
    public ImageIcon getIcon() {
	return null;
    }

    @Override
    public String getKey() {
	return KEY;
    }

    @Override
    public String getName() {
	return KEY;
    }

    @Override
    public int getNumThreads() {
    return numThreads;
    }

    @Override
    public void setNumThreads() {
    this.numThreads = Runtime.getRuntime().availableProcessors();
    }

    @Override
    public void setNumThreads(int numThreads) {
    this.numThreads = numThreads;
    }

    @Override
    public void process(Collection<DefaultWeightedEdge> edges, Model model) {
	final FeatureModel fm = model.getFeatureModel();
        for ( final DefaultWeightedEdge edge : edges )
        {
            final Spot source = model.getTrackModel().getEdgeSource( edge );
            final Spot target = model.getTrackModel().getEdgeTarget( edge );
 
            final double x1 = source.getDoublePosition( 0 );
            final double y1 = source.getDoublePosition( 1 );
            final double x2 = target.getDoublePosition( 0 );
            final double y2 = target.getDoublePosition( 1 );
 
            final double angle = Math.atan2( y2 - y1, x2 - x1 );
            fm.putEdgeFeature( edge, EDGE_ANGLE, Double.valueOf( angle ) );
        }
    }

    @Override
    public boolean isLocal() {
	return true;
    }

}