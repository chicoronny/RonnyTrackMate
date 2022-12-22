package net.chicoronny.trackmate.lineartracker;

import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_INITIAL_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_STICK_RADIUS;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_SUCCEEDING_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_MAX_COST;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class LinearTrackerTestDrive {

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
		settings.trackerFactory = tp.getFactory( LinearTrackerFactory.TRACKER_KEY );
	final Map<String, Object> ts = settings.trackerFactory.getDefaultSettings();
	ts.put(KEY_INITIAL_DISTANCE, 2.5d);
	ts.put(KEY_SUCCEEDING_DISTANCE, 3.0d);
	ts.put(KEY_STICK_RADIUS, 0.9d);
	ts.put(KEY_MAX_COST, 90d);
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
	
	File ofile = null;
	try {
	    ofile = new File("samples/CRTD14a.xml");
	} catch (final NullPointerException e){
	    System.err.println(e.getMessage());
	    return;
	}
	
	final TmXmlWriter writer = new TmXmlWriter( ofile );

	//writer.appendLog( logPanel.getTextContent() );
	writer.appendModel( trackmate.getModel() );
	writer.appendSettings( trackmate.getSettings() );
	//writer.appendGUIState( controller.getGuimodel() );
	
	try
	{
		writer.writeToFile();
		System.out.println( "Data saved to: " + ofile.toString() + '\n' );
	}
	catch ( final FileNotFoundException e )
	{
		System.out.println( "File not found:\n" + e.getMessage() + '\n' );
		return;
	}
	catch ( final IOException e )
	{
		System.out.println( "Input/Output error:\n" + e.getMessage() + '\n' );
		return;
	}
	
	final DisplaySettings displaySettings = DisplaySettings.defaultStyle().copy();
	
	final ExportTracksToSQL ex = new ExportTracksToSQL();
	final SelectionModel sm = new SelectionModel( model );
	
	ex.execute(trackmate, sm, displaySettings, null);
	
	final HyperStackDisplayer view = new HyperStackDisplayer( model, sm, displaySettings );
	view.render();
	Locale.setDefault(curlocale);
    }

}