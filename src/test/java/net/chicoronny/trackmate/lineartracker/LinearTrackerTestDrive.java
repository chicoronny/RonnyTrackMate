package net.chicoronny.trackmate.lineartracker;

import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_INITIAL_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_STICK_RADIUS;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_SUCCEEDING_DISTANCE;

import java.io.File;
import java.util.Locale;
import java.util.Map;

import net.chicoronny.trackmate.action.ExportTracksToSQL;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.edges.EdgeAngleAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackLinkingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class LinearTrackerTestDrive {

    public static void main(final String[] args) {
	final Locale curlocale = Locale.getDefault();
	final Locale usLocale = new Locale("en", "US"); // setting us locale
    	Locale.setDefault(usLocale);
    	
    	File file = null;
	try {
	    file = new File("samples/FakeTracks.xml");
	} catch (final NullPointerException e){
	    System.err.println(e.getMessage());
	    return;
	}
	System.out.println("Opening file: " + file.getAbsolutePath());
	final TmXmlReader reader = new TmXmlReader(file);
	final Model model = reader.getModel();
	final Settings settings = new Settings();
	reader.readSettings(settings, new DetectorProvider(), new TrackerProvider(), null, null, null);

	final SpotCollection spots = model.getSpots();
	System.out.println("Spots: " + spots);
	System.out.println();
	
	final long start = System.currentTimeMillis();
	
	final TrackerProvider tp = new TrackerProvider(); 
		settings.trackerFactory = tp.getFactory( LinearTrackerFactory.TRACKER_KEY );
	final Map<String, Object> ts = settings.trackerFactory.getDefaultSettings();
	ts.put(KEY_INITIAL_DISTANCE, 10d);
	ts.put(KEY_SUCCEEDING_DISTANCE, 8d);
	ts.put(KEY_STICK_RADIUS, 2d);
	settings.trackerSettings = ts;
	
	settings.addEdgeAnalyzer(new EdgeAngleAnalyzer());
	settings.addEdgeAnalyzer(new EdgeTargetAnalyzer());
	settings.addTrackAnalyzer(new TrackDurationAnalyzer());
	settings.addTrackAnalyzer(new TrackSpeedStatisticsAnalyzer());
	settings.addTrackAnalyzer(new TrackLinkingAnalyzer());
	
	final FeatureFilter filterb = new FeatureFilter("TRACK_MEAN_LINK_COST", 1d, true);
	//FeatureFilter filtera = new FeatureFilter("LINK_COST", 0.5d, true);
	final FeatureFilter filterc = new FeatureFilter("TRACK_DURATION", 5d, true);
	settings.addTrackFilter(filterb);
	settings.addTrackFilter(filterc);
		
	System.out.println("Tracker settings:");
	System.out.println(settings);
	
	final LinearTracker lap = new LinearTracker(spots, ts);
	lap.setLogger(Logger.DEFAULT_LOGGER);

	if (!lap.checkInput())
	    System.err.println("Error checking input: " + lap.getErrorMessage());
	if (!lap.process())
	    System.err.println("Error in process: " + lap.getErrorMessage());

	// Pass the new graph
	model.setSpots(spots, false);
	model.setTracks(lap.getResult(), false);
	
	final TrackMate trackmate = new TrackMate(model, settings);
	
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
	ex.execute(trackmate);
	
	Locale.setDefault(curlocale);
    }

}