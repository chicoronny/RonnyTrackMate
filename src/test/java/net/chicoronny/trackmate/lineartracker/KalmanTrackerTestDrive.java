package net.chicoronny.trackmate.lineartracker;


import java.io.File;
import java.util.Locale;
import java.util.Map;

import net.chicoronny.trackmate.action.ExportTracksToSQL;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.edges.EdgeAngleAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.spot.MySpotRadiusEstimatorFactory;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackLinkingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.kalman.KalmanTracker;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import static fiji.plugin.trackmate.tracking.kalman.KalmanTrackerFactory.KEY_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;


public class KalmanTrackerTestDrive {

    @SuppressWarnings("rawtypes")
    public static void main(final String[] args) {
	final Locale curlocale = Locale.getDefault();
	final Locale usLocale = new Locale("en", "US"); // setting us locale
    Locale.setDefault(usLocale);
    	
    File file = null;
	try {
	    file = new File("samples/CRTD14.xml");
	} catch (final NullPointerException e){
	    System.err.println(e.getMessage());
	    return;
	}
	System.out.println("Opening file: " + file.getAbsolutePath());
	final TmXmlReader reader = new TmXmlReader(file);
	final Model model = reader.getModel();
	final Settings settings = reader.readSettings(null, new DetectorProvider(), new TrackerProvider(), null, null, null, null);

	final SpotCollection spots = model.getSpots();
	System.out.println("Spots: " + spots);
	System.out.println();
	
	final long start = System.currentTimeMillis();
	
	final TrackerProvider tp = new TrackerProvider(); 
		settings.trackerFactory = tp.getFactory( "KALMAN_TRACKER" );
	final Map<String, Object> ts = settings.trackerFactory.getDefaultSettings();
	ts.put(KEY_KALMAN_SEARCH_RADIUS, 2.0d);
	ts.put(KEY_LINKING_MAX_DISTANCE, 2.5d);
	ts.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, 2);
	settings.trackerSettings = ts;
	
	settings.addSpotAnalyzerFactory(new MySpotRadiusEstimatorFactory());
	settings.addEdgeAnalyzer(new EdgeAngleAnalyzer());
	settings.addEdgeAnalyzer(new EdgeTargetAnalyzer());
	settings.addTrackAnalyzer(new TrackDurationAnalyzer());
	settings.addTrackAnalyzer(new TrackSpeedStatisticsAnalyzer());
	settings.addTrackAnalyzer(new TrackLinkingAnalyzer());
	
	//final FeatureFilter filterb = new FeatureFilter("TRACK_MEAN_LINK_COST", 1d, true);
	final FeatureFilter filterc = new FeatureFilter("TRACK_DURATION", 2d, true);
	//settings.addTrackFilter(filterb);
	settings.addTrackFilter(filterc);
		
	System.out.println("Settings:");
	System.out.println(settings);
	
	final KalmanTracker lap = new KalmanTracker(spots, 2.0d, 3, 2.5d);
	lap.setLogger(Logger.DEFAULT_LOGGER);

	if (!lap.checkInput())
	    System.err.println("Error checking input: " + lap.getErrorMessage());
	if (!lap.process())
	    System.err.println("Error in process: " + lap.getErrorMessage());

	// Pass the new graph
	model.setSpots(spots, false);
	model.setTracks(lap.getResult(), false);
	
	final TrackMate trackmate = new TrackMate(model, settings);
	
	trackmate.computeSpotFeatures(true);
	trackmate.computeEdgeFeatures( true );
	trackmate.computeTrackFeatures( true );
	trackmate.execTrackFiltering(true);  
		
	final long end = System.currentTimeMillis();
	
	System.out.println();
	System.out.println("Found " + model.getTrackModel().nTracks(true)
		+ " filtered tracks from " + model.getTrackModel().nTracks(false) +" tracks");
	System.out.println("Whole tracking done in " + (end - start) + " ms.");
	System.out.println();
	
	//final String newName = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-4) + "_T.db";
	//final File _file = new File(newName);
	//ExportTracksToSQL.export(trackmate.getModel(), settings, _file);
	
	final ExportTracksToSQL ex = new ExportTracksToSQL();
	final SelectionModel sm = new SelectionModel( model );
	final DisplaySettings displaySettings = DisplaySettings.defaultStyle().copy();
	
	ex.execute(trackmate,sm, displaySettings, null);
	final HyperStackDisplayer view = new HyperStackDisplayer( model, sm, settings.imp, displaySettings );
	view.render();
	
	Locale.setDefault(curlocale);
    }

}