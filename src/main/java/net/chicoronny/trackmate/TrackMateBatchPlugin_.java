package net.chicoronny.trackmate;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.kalman.KalmanTrackerFactory.KEY_KALMAN_SEARCH_RADIUS;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_INITIAL_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_MAX_COST;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_STICK_RADIUS;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_SUCCEEDING_DISTANCE;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import net.chicoronny.trackmate.action.ExportTracksToSQL;
import net.chicoronny.trackmate.lineartracker.LTUtils;
import net.chicoronny.trackmate.lineartracker.LinearTrackerFactory;
import net.imglib2.util.ValuePair;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DogDetectorFactory;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.oldlap.FastLAPTrackerFactory;
import fiji.plugin.trackmate.tracking.oldlap.LAPTrackerFactory;
import fiji.plugin.trackmate.tracking.oldlap.SimpleFastLAPTrackerFactory;
import fiji.plugin.trackmate.tracking.oldlap.SimpleLAPTrackerFactory;
import fiji.plugin.trackmate.tracking.sparselap.SparseLAPTrackerFactory;

/**
 * The Class TrackMateBatchPlugin_.
 * 
 * This Plug-in processes files within a directory. Most parameters can be set
 * in a parameter file which has to placed in the parent directory.
 */
public class TrackMateBatchPlugin_ implements PlugIn
{

	/** The Constant KEY. */
	private static final String KEY = "BatchPlugin";

	/** The initial distance. */
	private double INITIAL_DISTANCE;

	/** The succeeding distance. */
	private double SUCCEEDING_DISTANCE;

	/** The stick radius. */
	private double STICK_RADIUS;

	/** The quality. */
	private double PRE_QUALITY;

	/** The default one. */
	private final String DEFAULT_ONE = "1";

	/** The default false. */
	private final String DEFAULT_FALSE = "FALSE";

	/** The radius. */
	private double RADIUS;

	/** The threshold. */
	private double THRESHOLD;

	/** The detector. */
	private String DETECTOR;

	/** The median filtering. */
	private Boolean MEDIAN_FILTERING;

	/** The track filter. */
	private String[] TRACK_FILTER;

	/** The spot filter. */
	private String[] SPOT_FILTER;

	/** The track filters. */
	private ArrayList< ValuePair< String, Double >> trackfilters;

	/** The spot filters. */
	private ArrayList< ValuePair< String, Double >> spotfilters;

	/** The extensions. */
	private String[] EXTENSIONS;

	/** The tracker. */
	private String TRACKER;

	/** The allow track merging. */
	private boolean ALLOW_TRACK_MERGING;

	/** The allow track splitting. */
	private boolean ALLOW_TRACK_SPLITTING;

	/** The gap closing max distance. */
	private double GAP_CLOSING_MAX_DISTANCE;

	/** The gap closing max frame gap. */
	private int GAP_CLOSING_MAX_FRAME_GAP;

	/** The linking max distance. */
	private double LINKING_MAX_DISTANCE;

	/** The merging max distance. */
	private double MERGING_MAX_DISTANCE;

	/** The splitting max distance. */
	private double SPLITTING_MAX_DISTANCE;

	/** The allow gap closing. */
	private boolean ALLOW_GAP_CLOSING;

	/** The spot analyzer. */
	private String[] SPOTANALYZER;

	/** The track analyzer. */
	private String[] TRACKANALYZER;

	/** The edge analyzer. */
	private String[] EDGEANALYZER;

	/** The maximum cost. */
	private Object MAX_COST;

	private File file;
	
	/** The kalman search radius. */
	private double KALMAN_SEARCH_RADIUS;

	/**
	 * Instantiates a new track mate batch plugin_.
	 */
	public TrackMateBatchPlugin_()
	{
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run( final String imagePath )
	{
		final Locale curLocale = Locale.getDefault();
		final Locale usLocale = new Locale( "en", "US" ); // setting us locale
		Locale.setDefault( usLocale );

		DirectoryChooser.setDefaultDirectory( "/media/data/users/Sofia/tiffs" );
		final DirectoryChooser dc = new DirectoryChooser( "Choose a folder to process" );
		final String dname = dc.getDirectory();
		if ( null == dname )
			return;

		final File folder = new File( dname );
		final File parent = folder.getParentFile();
		final Logger logger = Logger.IJ_LOGGER;

		try
		{
			file = new File( parent, "TrackMate.properties" );
		}
		catch ( final NullPointerException e )
		{
			logger.log( e.getMessage() );
			return;
		}
		logger.log( "Using " + file.getAbsolutePath() );

		try
		{
			final FileReader reader = new FileReader( file );
			final Properties props = new Properties();
			props.load( reader );
			EXTENSIONS = props.getProperty( "EXTENSIONS" ).split( "," );
			INITIAL_DISTANCE = Double.parseDouble( props.getProperty( KEY_INITIAL_DISTANCE, DEFAULT_ONE ) );
			SUCCEEDING_DISTANCE = Double.parseDouble( props.getProperty( KEY_SUCCEEDING_DISTANCE, DEFAULT_ONE ) );
			STICK_RADIUS = Double.parseDouble( props.getProperty( KEY_STICK_RADIUS, DEFAULT_ONE ) );
			MAX_COST = Double.parseDouble( props.getProperty( KEY_MAX_COST, DEFAULT_ONE ) );
			PRE_QUALITY = Double.parseDouble( props.getProperty( "PRE_QUALITY", DEFAULT_ONE ) );
			RADIUS = Double.parseDouble( props.getProperty( "RADIUS", DEFAULT_ONE ) );
			THRESHOLD = Double.parseDouble( props.getProperty( "THRESHOLD", DEFAULT_ONE ) );
			MEDIAN_FILTERING = Boolean.parseBoolean( props.getProperty( "MEDIAN_FILTERING", DEFAULT_FALSE ) );
			DETECTOR = props.getProperty( "DETECTOR", "DOG_DETECTOR" );
			TRACKER = props.getProperty( "TRACKER", "LINEAR_TRACKER" );
			TRACK_FILTER = props.getProperty( "TRACK_FILTER", "" ).split( "\n" );
			SPOT_FILTER = props.getProperty( "SPOT_FILTER", "" ).split( "\n" );
			ALLOW_TRACK_MERGING = Boolean.parseBoolean( props.getProperty( "ALLOW_TRACK_MERGING", DEFAULT_FALSE ) );
			ALLOW_TRACK_SPLITTING = Boolean.parseBoolean( props.getProperty( "ALLOW_TRACK_SPLITTING", DEFAULT_FALSE ) );
			ALLOW_GAP_CLOSING = Boolean.parseBoolean( props.getProperty( "ALLOW_GAP_CLOSING", DEFAULT_ONE ) );
			GAP_CLOSING_MAX_DISTANCE = Double.parseDouble( props.getProperty( "GAP_CLOSING_MAX_DISTANCE", DEFAULT_ONE ) );
			GAP_CLOSING_MAX_FRAME_GAP = Integer.parseInt( props.getProperty( "GAP_CLOSING_MAX_FRAME_GAP", DEFAULT_ONE ) );
			LINKING_MAX_DISTANCE = Double.parseDouble( props.getProperty( "LINKING_MAX_DISTANCE", DEFAULT_ONE ) );
			MERGING_MAX_DISTANCE = Double.parseDouble( props.getProperty( "MERGING_MAX_DISTANCE", DEFAULT_ONE ) );
			SPLITTING_MAX_DISTANCE = Double.parseDouble( props.getProperty( "SPLITTING_MAX_DISTANCE", DEFAULT_ONE ) );
			SPOTANALYZER = props.getProperty( "SPOTANALYZER", "" ).split( "[,\n]" );
			TRACKANALYZER = props.getProperty( "TRACKANALYZER", "" ).split( "[,\n]" );
			EDGEANALYZER = props.getProperty( "EDGEANALYZER", "" ).split( "[,\n]" );
			KALMAN_SEARCH_RADIUS = Double.parseDouble( props.getProperty( "KALMAN_SEARCH_RADIUS", DEFAULT_ONE ) );
			reader.close();
		}
		catch ( final FileNotFoundException e1 )
		{
			logger.log( e1.getMessage() );
			return;
		}
		catch ( final IOException e1 )
		{
			logger.log( e1.getMessage() );
		}

		trackfilters = new ArrayList< ValuePair< String, Double >>();
		for ( final String f : TRACK_FILTER )
		{
			final String[] splitted = f.split( "," );
			if ( splitted.length == 2 )
			{
				trackfilters.add( new ValuePair< String, Double >( splitted[ 0 ], Double.parseDouble( splitted[ 1 ] ) ) );
			}
		}

		spotfilters = new ArrayList< ValuePair< String, Double >>();
		for ( final String f : SPOT_FILTER )
		{
			final String[] splitted = f.split( "," );
			if ( splitted.length == 2 )
			{
				spotfilters.add( new ValuePair< String, Double >( splitted[ 0 ], Double.parseDouble( splitted[ 1 ] ) ) );
			}
		}

		try
		{
			process( folder );
		}
		catch ( final IOException e2 )
		{
			logger.log( e2.getMessage() );
		}
		Locale.setDefault( curLocale );
	}

	/**
	 * Process.
	 * 
	 * @param folder
	 *            the name of the directory to process
	 * @throws FileNotFoundException
	 *             the file not found exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void process( final File folder ) throws IOException
	{
	// get the directory to work on

	final long start = System.currentTimeMillis();
	final Logger logger = Logger.IJ_LOGGER; 
	
	final Collection<File> fList = LTUtils.listFiles(folder,EXTENSIONS);
	
	for (final File file : fList) {
	    logger.log("Processing " + file.getName());
	    final ImagePlus imp = new ImagePlus(file.getAbsolutePath());
	    final Settings settings = new Settings();
//	    settings.setFromWithoutROI(imp);
			imp.killRoi();
			settings.setFrom( imp );

	    // Detection
	    final DetectorProvider provider = new DetectorProvider();
	    if (DETECTOR.equalsIgnoreCase("DOG_DETECTOR"))
		settings.detectorFactory = provider.getFactory(DogDetectorFactory.DETECTOR_KEY);
	    if (DETECTOR.equalsIgnoreCase("LOG_DETECTOR"))
		settings.detectorFactory = provider.getFactory(LogDetectorFactory.DETECTOR_KEY);
	    if (settings.detectorFactory == null) { 
		logger.log("No Detector provided!"); return;
	    }
	    
	    final Map<String, Object> dmap = settings.detectorFactory.getDefaultSettings();
	    dmap.put(KEY_RADIUS, RADIUS);
	    dmap.put(KEY_THRESHOLD, THRESHOLD);
	    dmap.put(KEY_DO_MEDIAN_FILTERING, MEDIAN_FILTERING);
	    settings.detectorSettings = dmap;
	    settings.initialSpotFilterValue = PRE_QUALITY;

	 // Tracking
	    final TrackerProvider tp = new TrackerProvider(); 
	    if(TRACKER.startsWith("LINEAR_TRACKER")){
	       	    settings.trackerFactory = tp.getFactory( LinearTrackerFactory.TRACKER_KEY );
	    	    final Map<String, Object> ts = settings.trackerFactory.getDefaultSettings();
	    	    ts.put(KEY_INITIAL_DISTANCE, INITIAL_DISTANCE);
	    	    ts.put(KEY_SUCCEEDING_DISTANCE, SUCCEEDING_DISTANCE);
	    	    ts.put(KEY_STICK_RADIUS, STICK_RADIUS);
	    	    ts.put(KEY_MAX_COST, MAX_COST);
	    	    settings.trackerSettings = ts; }
		else if(TRACKER.startsWith("FAST_LAP_TRACKER")) {
	    	    settings.trackerFactory = tp.getFactory( FastLAPTrackerFactory.TRACKER_KEY );
	    	    final Map<String, Object> fl = settings.trackerFactory.getDefaultSettings();
	    	    fl.put(KEY_ALLOW_GAP_CLOSING, ALLOW_GAP_CLOSING);
	    	    fl.put(KEY_ALLOW_TRACK_MERGING, ALLOW_TRACK_MERGING);
	    	    fl.put(KEY_ALLOW_TRACK_SPLITTING, ALLOW_TRACK_SPLITTING);
	    	    fl.put(KEY_GAP_CLOSING_MAX_DISTANCE, GAP_CLOSING_MAX_DISTANCE);
	    	    fl.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, GAP_CLOSING_MAX_FRAME_GAP );
	    	    fl.put(KEY_LINKING_MAX_DISTANCE, LINKING_MAX_DISTANCE);
	    	    fl.put(KEY_MERGING_MAX_DISTANCE, MERGING_MAX_DISTANCE);
	    	    fl.put(KEY_SPLITTING_MAX_DISTANCE, SPLITTING_MAX_DISTANCE);
	    	    settings.trackerSettings = fl; }
		else if(TRACKER.startsWith("SIMPLE_FAST_LAP_TRACKER")) {    
	    	    settings.trackerFactory = tp.getFactory( SimpleFastLAPTrackerFactory.TRACKER_KEY );
	    	    final Map<String, Object> sfl = settings.trackerFactory.getDefaultSettings();
	    	    sfl.put(KEY_ALLOW_GAP_CLOSING, ALLOW_GAP_CLOSING);
	    	    sfl.put(KEY_GAP_CLOSING_MAX_DISTANCE, GAP_CLOSING_MAX_DISTANCE);
	    	    sfl.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, GAP_CLOSING_MAX_FRAME_GAP );
	    	    sfl.put(KEY_LINKING_MAX_DISTANCE, LINKING_MAX_DISTANCE);
	    	    settings.trackerSettings = sfl; }
		else if (TRACKER.startsWith("LAP_TRACKER")){
	    	    settings.trackerFactory = tp.getFactory( LAPTrackerFactory.TRACKER_KEY );
	    	    final Map<String, Object> l = settings.trackerFactory.getDefaultSettings();
	    	    l.put(KEY_ALLOW_GAP_CLOSING, ALLOW_GAP_CLOSING);
	    	    l.put(KEY_ALLOW_TRACK_MERGING, ALLOW_TRACK_MERGING);
	    	    l.put(KEY_ALLOW_TRACK_SPLITTING, ALLOW_TRACK_SPLITTING);
	    	    l.put(KEY_GAP_CLOSING_MAX_DISTANCE, GAP_CLOSING_MAX_DISTANCE);
	    	    l.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, GAP_CLOSING_MAX_FRAME_GAP );
	    	    l.put(KEY_LINKING_MAX_DISTANCE, LINKING_MAX_DISTANCE);
	    	    l.put(KEY_MERGING_MAX_DISTANCE, MERGING_MAX_DISTANCE);
	    	    l.put(KEY_SPLITTING_MAX_DISTANCE, SPLITTING_MAX_DISTANCE);
	    	    settings.trackerSettings = l; }
		else if (TRACKER.startsWith("SPARSE_LAP_TRACKER")) {
	    	    settings.trackerFactory = tp.getFactory( SparseLAPTrackerFactory.TRACKER_KEY );
	    	    final Map<String, Object> slp = settings.trackerFactory.getDefaultSettings();
	    	    slp.put(KEY_ALLOW_GAP_CLOSING, ALLOW_GAP_CLOSING);
	    	    slp.put(KEY_ALLOW_TRACK_MERGING, ALLOW_TRACK_MERGING);
	    	    slp.put(KEY_ALLOW_TRACK_SPLITTING, ALLOW_TRACK_SPLITTING);
	    	    slp.put(KEY_GAP_CLOSING_MAX_DISTANCE, GAP_CLOSING_MAX_DISTANCE);
	    	    slp.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, GAP_CLOSING_MAX_FRAME_GAP );
	    	    slp.put(KEY_LINKING_MAX_DISTANCE, LINKING_MAX_DISTANCE);
	    	    slp.put(KEY_MERGING_MAX_DISTANCE, MERGING_MAX_DISTANCE);
	    	    slp.put(KEY_SPLITTING_MAX_DISTANCE, SPLITTING_MAX_DISTANCE);
	    	    settings.trackerSettings = slp; }
		else if(TRACKER.startsWith("SIMPLE_LAP_TRACKER")) {
	    	    settings.trackerFactory = tp.getFactory( SimpleLAPTrackerFactory.TRACKER_KEY );
	    	    final Map<String, Object> sl = settings.trackerFactory.getDefaultSettings();
	    	    sl.put(KEY_ALLOW_GAP_CLOSING, ALLOW_GAP_CLOSING);
	    	    sl.put(KEY_GAP_CLOSING_MAX_DISTANCE, GAP_CLOSING_MAX_DISTANCE);
	    	    sl.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, GAP_CLOSING_MAX_FRAME_GAP );
	    	    sl.put(KEY_LINKING_MAX_DISTANCE, LINKING_MAX_DISTANCE);
	    	    settings.trackerSettings = sl; }
		else if(TRACKER.startsWith("KALMAN_TRACKER")) {
    	    settings.trackerFactory = tp.getFactory( "KALMAN_TRACKER" );
    	    final Map<String, Object> ka = settings.trackerFactory.getDefaultSettings();
    	    ka.put(KEY_KALMAN_SEARCH_RADIUS, KALMAN_SEARCH_RADIUS);
    	    ka.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, GAP_CLOSING_MAX_FRAME_GAP );
    	    ka.put(KEY_LINKING_MAX_DISTANCE, LINKING_MAX_DISTANCE);
    	    settings.trackerSettings = ka; }
	    
		else {
	    	    logger.log("No Tracker found in TrackMate.properties");
	    	    return;} 
	    
	 // Analyzer
	    ClassLoader cl = ClassLoader.getSystemClassLoader();
	    
	    for (String a:SPOTANALYZER){
		try {
		    Class<?> c = cl.loadClass("fiji.plugin.trackmate.features.spot."+a.trim());
		    @SuppressWarnings("rawtypes")
		    Class<? extends SpotAnalyzerFactory> ac = c.asSubclass(SpotAnalyzerFactory.class);
		    settings.addSpotAnalyzerFactory(ac.newInstance());
		} catch (ClassNotFoundException e) {
		    logger.log(e.getMessage());
		    return;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	    }
	    
	    for (String a:TRACKANALYZER){
		try {
		    Class<?> c = cl.loadClass("fiji.plugin.trackmate.features.track."+a.trim());
		    Class<? extends TrackAnalyzer> ac = c.asSubclass(TrackAnalyzer.class);
		    settings.addTrackAnalyzer(ac.newInstance());
		} catch (ClassNotFoundException e) {
		    logger.log(e.getMessage());
		    return;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	    }
	    
	    for (String a:EDGEANALYZER){
		try {
		    Class<?> c = cl.loadClass("fiji.plugin.trackmate.features.edges."+a.trim());
		    Class<? extends EdgeAnalyzer> ac = c.asSubclass(EdgeAnalyzer.class);
		    settings.addEdgeAnalyzer(ac.newInstance());
		} catch (ClassNotFoundException e) {
		    logger.log(e.getMessage());
		    return;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	    }	    

	    // Filter with check of availability
	    List<SpotAnalyzerFactory<?>> spotFactories = settings.getSpotAnalyzerFactories();
	    for (ValuePair<String, Double> f:spotfilters){
		boolean isAbove = f.getB() >= 0; 
		final FeatureFilter filter = new FeatureFilter(f.getA(), Math.abs(f.getB()), isAbove);
		Iterator<SpotAnalyzerFactory<?>> fIt = spotFactories.iterator();
		while (fIt.hasNext()){
		    List<String> curAnalyzer = fIt.next().getFeatures();
		    if (curAnalyzer.contains(f.getA()))
			settings.getSpotFilters().add(filter);
		}
		Collection<String> spotFeatures = Spot.FEATURES;
		if(spotFeatures.contains(f.getA()))
		    settings.getSpotFilters().add(filter);
	    }
	    
	    
	    List<TrackAnalyzer> trackAnalyzers = settings.getTrackAnalyzers();
	    for (ValuePair<String, Double> f:trackfilters){
		boolean isAbove = f.getB() >= 0; 
		final FeatureFilter filter = new FeatureFilter(f.getA(), Math.abs(f.getB()), isAbove);
		Iterator<TrackAnalyzer> tIt = trackAnalyzers.iterator();
		while (tIt.hasNext()){
		    List<String> curFeatures = tIt.next().getFeatures();
		    if (curFeatures.contains(f.getA()))
			settings.getTrackFilters().add(filter);
		}
	    }
  
	    logger.log(settings.toString());

	    // Process
	    final Model model = new Model();
	    model.setLogger(Logger.IJ_LOGGER);

	    final TrackMate trackmate = new TrackMate(model, settings);
	    boolean ok = trackmate.checkInput();
	    if (!ok) {
		logger.log(trackmate.getErrorMessage());
		return;
	    }

	    ok = ok & trackmate.process();
	    if (!ok) {
		logger.log(trackmate.getErrorMessage());
		continue;
	    }

	    // Export
	    final String newName = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-4) + "_B.db";
	    final File _file = new File(newName);
	    
	    ExportTracksToSQL ex = new  ExportTracksToSQL();
	    ex.export(trackmate.getModel(), trackmate.getSettings(), _file); // SQLITE
	}
	final long end = System.currentTimeMillis();
	logger.log("All Done in " + (end - start)/1000 + "s.");
    }

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main( final String[] args )
	{
		ImageJ.main( args );
		//IJ.run("Enhance Contrast...","saturated=0.1 normalize process_all");
		final TrackMateBatchPlugin_ plugIn = new TrackMateBatchPlugin_();
		plugIn.run( "TrackMate.properties" );
		return;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fiji.plugin.trackmate.gui.descriptors.WizardPanelDescriptor#getKey()
	 */
	public String getKey()
	{
		return KEY;
	}

}