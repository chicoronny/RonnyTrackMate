package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.multithreading.SimpleMultiThreading;

@Plugin( type = TrackAnalyzer.class )
public class TrackLinkingAnalyzer implements TrackAnalyzer, Benchmark,	MultiThreaded {
    
    /*
     * 	FEATURE NAMES
     */
    public static final String KEY = "Track linking";
    
    public static final String TRACK_MEAN_LINK_COST = "TRACK_MEAN_LINK_COST";
    
    public static final String TRACK_LENGTH = "TRACK_LENGTH";
    
    private static final int numFeatures = 2;
    
    public static final List< String > FEATURES = new ArrayList< String >( numFeatures );
    
    public static final Map< String, String > FEATURE_NAMES = new HashMap< String, String >( numFeatures );
    
    public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >( numFeatures );
    
    public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >( numFeatures );
    
    public static final Map< String, Boolean > IS_INT = new HashMap< String, Boolean >( numFeatures );
    
    private int numThreads;

    private long processingTime;
    
    static{
	FEATURES.add( TRACK_MEAN_LINK_COST );
	FEATURES.add( TRACK_LENGTH );
	
	FEATURE_NAMES.put( TRACK_MEAN_LINK_COST, "Mean linking cost");
	FEATURE_NAMES.put( TRACK_LENGTH, "Track length");
	
	FEATURE_SHORT_NAMES.put( TRACK_MEAN_LINK_COST, "Mean linking cost");
	FEATURE_SHORT_NAMES.put( TRACK_LENGTH, "Track length");
	
	FEATURE_DIMENSIONS.put( TRACK_MEAN_LINK_COST, Dimension.NONE);
	FEATURE_DIMENSIONS.put( TRACK_LENGTH, Dimension.NONE);
	
	IS_INT.put( TRACK_MEAN_LINK_COST, Boolean.FALSE );
	IS_INT.put( TRACK_LENGTH, Boolean.FALSE );
    }

    public TrackLinkingAnalyzer() {
	setNumThreads();
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
	return IS_INT;
    }

    @Override
    public boolean isManualFeature() {
	return false;
    }

    @Override
    public String getInfoText() {
	return null;
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
    public long getProcessingTime() {
	return processingTime;
    }

    @Override
    public void process(final Collection<Integer> trackIDs, final Model model) {
	if ( trackIDs.isEmpty() ) return; 
	
	final ArrayBlockingQueue< Integer > queue = new ArrayBlockingQueue< Integer >( trackIDs.size(), false, trackIDs );
	final FeatureModel fm = model.getFeatureModel();

	final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
	for ( int i = 0; i < threads.length; i++ ){
	    threads[ i ] = new Thread( "TrackLinkingAnalyzer thread " + i ){	
		
		@Override
		public void run(){
		    Integer trackID;
		    while ( ( trackID = queue.poll() ) != null ){
			final Set< DefaultWeightedEdge > track = model.getTrackModel().trackEdges( trackID );
			
			double mean = 0;
			double sum = 0;
			double length = 0;
			
			for ( final DefaultWeightedEdge edge : track )
			{
			    final Spot source = model.getTrackModel().getEdgeSource( edge );
			    final Spot target = model.getTrackModel().getEdgeTarget( edge );
			    
			    length += Math.sqrt(source.squareDistanceTo(target));
			    sum += model.getTrackModel().getEdgeWeight(edge);
			}
			mean = sum / track.size();
			
			fm.putTrackFeature( trackID, TRACK_MEAN_LINK_COST, mean );
			fm.putTrackFeature( trackID, TRACK_LENGTH, length );
		    }
		}
	    };
	}
	
	final long start = System.currentTimeMillis();
	SimpleMultiThreading.startAndJoin( threads );
	final long end = System.currentTimeMillis();
	processingTime = end - start;
    }

    @Override
    public boolean isLocal() {
	return true;
    }

}