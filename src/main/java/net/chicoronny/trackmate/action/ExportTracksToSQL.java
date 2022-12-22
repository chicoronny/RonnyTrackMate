package net.chicoronny.trackmate.action;

import ij.io.SaveDialog;

import java.awt.Frame;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class ExportTracksToSQL extends AbstractTMAction {

    public static final String INFO_TEXT = "<html>" +
		"Export the tracks in the current model content to a SQL database " +
		"<p> " +
		"The file will have one element per track, and each track " +
		"contains several spot elements. These spots are " +
		"sorted by frame number"+
		"<p>" +
		"As such, this format <u>cannot</u> handle track merging and " +
		"splitting properly, and is suited only for non-branching tracks." +
		"</html>";
    public static final String NAME= "Export tracks to SQLITE database";
    public static final String KEY = "EXPORT_TRACKS_TO_SQLITE";;
    
    public ExportTracksToSQL()
    {
    }    
    
    /**
     * Export.
     * 
     * @param model
     *            the model
     * @param settings
     *            the settings
     * @param file
     *            the file
     */
    public void export(final Model model, final Settings settings, final File file) {
	Logger logger = Logger.IJ_LOGGER;
	file.delete();
	Statement statement = null;
	final long start = System.currentTimeMillis();
	try {
	    statement = createDatabase();
	    marshall(model, settings, statement);
	    statement.executeUpdate("backup to '"+ file.getAbsolutePath() + "'");
	} catch (SQLException e) {
	    logger.log(e.getMessage());
	    e.printStackTrace();
	} finally {
	    if (statement != null)
		try {
		    statement.close();
		} catch (SQLException e) {
		    logger.log(e.getMessage());
		}
	}
	final long end = System.currentTimeMillis();
	logger.log("Exported to SQLite " + file.getName() + " in " + (end-start) + " ms.");
    }
    
    
    @Override
    public void execute(final TrackMate trackmate, SelectionModel selectionModel, DisplaySettings displaySettings, Frame parent) {
	logger.log("Exporting tracks to SQLite database.\n");
	final long start = System.currentTimeMillis();
	final Model model = trackmate.getModel();
	final int ntracks = model.getTrackModel().nTracks(true);
	if (ntracks == 0) {
	    logger.log("No visible track found. Aborting.\n");
	    return;
	}

	File folder;
	try {
	    folder = new File(trackmate.getSettings().imp.getOriginalFileInfo().directory);
	} catch (final NullPointerException npe) {
	    folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
	}

	String filename;
	try {
	    filename = trackmate.getSettings().imageFileName;
	    filename = filename.substring(0, filename.indexOf("."));
	    filename = folder.getPath() + File.separator + filename  + "_Tracks.db";
	} catch (final NullPointerException npe) {
	    filename = folder.getPath() + File.separator + "Tracks.db";
	}
	final SaveDialog sd = new SaveDialog("Save Database File", filename, ".db");
	
	String fileName = sd.getFileName();
	
	if (fileName == null || fileName.isEmpty()) return;
	
	final File file = new File(sd.getDirectory() + fileName); 
	
	if (file.delete())
		logger.log("File will be overwritten!\n");

	Statement statement = null;
	try {
	    statement = createDatabase();
	    marshall(model, trackmate.getSettings(), statement);
	    statement.executeUpdate("backup to '"+ file.getAbsolutePath() + "'");
	} catch (SQLException e) {
	    logger.log("SQL Error:" + e.getMessage());
	} finally {
	    if (statement != null)
		try {
		    statement.close();
		} catch (SQLException e) {
		    logger.log("SQL Error:" + e.getMessage());;
		}
	}
	final long end = System.currentTimeMillis();
	logger.log("Done in " + (end-start) + " ms.");
    }
    
    
    private static void marshall(final Model model, final Settings settings, Statement statement) throws SQLException{
	final FeatureModel fm = model.getFeatureModel();
	final TrackModel tm = model.getTrackModel();
	final Set<Integer> trackIDs = tm.trackIDs(true);
	final Collection< String > spotFeatures = fm.getSpotFeatures();
	final Collection< String > edgeFeatures = fm.getEdgeFeatures();
	final Collection< String > trackFeatures = fm.getTrackFeatures();
	final Map<String,Boolean> intMapSpot = fm.getSpotFeatureIsInt();
	final Map<String,Boolean> intMapEdge = fm.getEdgeFeatureIsInt();
	final Map<String,Boolean> intMapTrack = fm.getTrackFeatureIsInt();
	String CREATETSPOTQUERY = "CREATE TABLE spots (id INTEGER PRIMARY KEY, track_id INTEGER, ";
	String CREATEEDGEQUERY = "CREATE TABLE edges (id INTEGER PRIMARY KEY, track_id INTEGER, ";
	String CREATETRACKQUERY = "CREATE TABLE tracks (id INTEGER PRIMARY KEY, ";
	//particle INTEGER, t INTEGER, x FLOAT (5,8), y FLOAT (5,8), z FLOAT (5,8), quality FLOAT (5,8) ,file CHAR(100))";
	
	for ( final  String feature : trackFeatures ){
	    if (intMapTrack.get(feature))
		CREATETRACKQUERY += feature.toLowerCase() + " INTEGER, ";
	    else
		CREATETRACKQUERY += feature.toLowerCase() + " FLOAT (5,8), ";
	}
	CREATETRACKQUERY = CREATETRACKQUERY.substring(0, CREATETRACKQUERY.length()-2);
	CREATETRACKQUERY += ")";
	statement.executeUpdate(CREATETRACKQUERY);
	
	for ( final String feature : spotFeatures ){
	    if (intMapSpot.get(feature))
		CREATETSPOTQUERY += feature.toLowerCase() + " INTEGER, ";
	    else
		CREATETSPOTQUERY += feature.toLowerCase() + " FLOAT (5,8), ";
	}
	CREATETSPOTQUERY = CREATETSPOTQUERY.substring(0, CREATETSPOTQUERY.length()-2);
	CREATETSPOTQUERY += ", FOREIGN KEY (track_id) REFERENCES tracks(id))";
	statement.executeUpdate(CREATETSPOTQUERY);
	
	for ( final  String feature : edgeFeatures ){
	    if (intMapEdge.get(feature))
		CREATEEDGEQUERY += feature.toLowerCase() + " INTEGER, ";
	    else
		CREATEEDGEQUERY += feature.toLowerCase() + " FLOAT (5,8), ";
	}
	CREATEEDGEQUERY = CREATEEDGEQUERY.substring(0, CREATEEDGEQUERY.length()-2);
	CREATEEDGEQUERY += ", FOREIGN KEY (track_id) REFERENCES tracks(id))";
	statement.executeUpdate(CREATEEDGEQUERY);
			
	for (final Integer trackID : trackIDs) {
	    final Set<Spot> trackSpots = tm.trackSpots(trackID);
	    final Set<DefaultWeightedEdge> trackEdges = tm.trackEdges( trackID );
	    
	    // Sort them by time
	    final TreeSet<Spot> sortedTrack = new TreeSet<Spot>(Spot.timeComparator);
	    sortedTrack.addAll(trackSpots);
	    
	    String Track_String = "insert into tracks (id, "; 
	    for ( final String feature : trackFeatures )
		    Track_String += feature.toLowerCase() + ", ";
	    Track_String = Track_String.substring(0, Track_String.length()-2);
	    Track_String += ") values (";
	    Track_String += trackID.intValue() + ", ";
	    
	    for ( final String feature : trackFeatures ){
		Double val = fm.getTrackFeature( trackID, feature );
		if (val==null || Double.isNaN(val) || Double.isInfinite(val))
	    	val=Double.valueOf(0d);
		if (intMapTrack.get(feature))
		    Track_String += String.format("%d, ", val.intValue());
		else
		    Track_String += String.format(Locale.US,"%.8f, ", val.floatValue());
	    }
	    Track_String = Track_String.substring(0, Track_String.length()-2);
	    Track_String += ")";
	    
	    statement.executeUpdate(Track_String);
	    
	    for (final Spot spot : sortedTrack) {				
			String SpotString = "insert into spots (id, track_id, ";
			for ( final String feature : spotFeatures )
				SpotString += feature.toLowerCase() + ", ";
			SpotString = SpotString.substring(0, SpotString.length()-2);
			SpotString += ") values ("; 
			SpotString += spot.ID() + ", ";
			SpotString += trackID.intValue() + ", ";
						
			for ( final String feature : spotFeatures ){
			    Double val = spot.getFeature( feature );
			    if (val==null || Double.isNaN(val) || Double.isInfinite(val))
			    	val=Double.valueOf(0d);
			    if (intMapSpot.get(feature))
			    	SpotString += String.format("%d, ", val.intValue());
			    else
			    	SpotString += String.format(Locale.US, "%.8f, ", val.floatValue());
			}
			SpotString = SpotString.substring(0, SpotString.length()-2);
			SpotString += ")";
			
			statement.executeUpdate(SpotString);
	    }
	    
	    for ( final DefaultWeightedEdge edge : trackEdges ){
		
			String EdgeString = "insert into edges (track_id, ";
			for ( final String feature : edgeFeatures )
				EdgeString += feature.toLowerCase() + ", ";
			EdgeString = EdgeString.substring(0, EdgeString.length()-2);
			EdgeString += ") values (";
			EdgeString += trackID.intValue() + ", ";
			
			for ( final String feature : edgeFeatures )
			{
			    Double val = fm.getEdgeFeature( edge, feature );
			    if (val==null || Double.isNaN(val) || Double.isInfinite(val))
			    	val=Double.valueOf(0d);
			    if (intMapEdge.get(feature))
			    	EdgeString += String.format("%d, ", val.intValue());
			    else
			    	EdgeString += String.format(Locale.US, "%.8f, ", val.floatValue());
			}
			EdgeString = EdgeString.substring(0, EdgeString.length()-2);
			EdgeString += ")";
			
			statement.executeUpdate(EdgeString);
	    }
		}
    }
    

    private static Statement createDatabase() throws SQLException {
	try {
		Class.forName("org.sqlite.JDBC");
	} catch (ClassNotFoundException e) {
		System.err.println(e.getMessage());
	}
	Connection connection = null;
	connection = DriverManager.getConnection("jdbc:sqlite::memory:");
	Statement statement = connection.createStatement();
	statement.setQueryTimeout(30);
	statement.executeUpdate("PRAGMA foreign_keys = ON");
	return statement;
    }


    @Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new ExportTracksToSQL();
		}

		@Override
		public ImageIcon getIcon()
		{
			return Icons.APPLY_ICON;
		}
	}

}