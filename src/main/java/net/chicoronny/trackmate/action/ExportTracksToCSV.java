package net.chicoronny.trackmate.action;

import ij.io.SaveDialog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;

public class ExportTracksToCSV extends AbstractTMAction {
	
	public static final String INFO_TEXT = "<html>" +
		"Export the tracks in the current model content to CSV Files " +
		"<p> " +
		"The file will have one element per track, and each track " +
		"contains several spot elements. These spots are " +
		"sorted by frame number"+
		"<p>" +
		"As such, this format <u>cannot</u> handle track merging and " +
		"splitting properly, and is suited only for non-branching tracks." +
		"</html>";
    public static final String NAME= "Export tracks to CSV files";
    public static final String KEY = "EXPORT_TRACKS_TO_CSV";;
    public static final ImageIcon ICON  = new ImageIcon(TrackMateWizard.class.getResource("images/page_save.png"));
    
    public static final String TRACK_ID = "TRACK_ID";
	
	public static final String TRACK_MEAN_SPEED = "TRACK_MEAN_SPEED";

	public static final String TRACK_MAX_SPEED = "TRACK_MAX_SPEED";

	public static final String TRACK_MIN_SPEED = "TRACK_MIN_SPEED";

	public static final String TRACK_MEDIAN_SPEED = "TRACK_MEDIAN_SPEED";

	public static final String TRACK_STD_SPEED = "TRACK_STD_SPEED";
	
	public static final String TRACK_DURATION = "TRACK_DURATION";

	public static final String TRACK_START = "TRACK_START";

	public static final String TRACK_STOP = "TRACK_STOP";

	public static final String TRACK_DISPLACEMENT = "TRACK_DISPLACEMENT";
    
    public static final Collection< String > trackFeatures = new LinkedHashSet< String >(
		Arrays.asList(TRACK_ID, TRACK_MEAN_SPEED, TRACK_MAX_SPEED, TRACK_MIN_SPEED, TRACK_MEDIAN_SPEED, TRACK_STD_SPEED, 
				TRACK_DURATION, TRACK_START, TRACK_STOP, TRACK_DISPLACEMENT));
    
    public static final String CONTRAST = "CONTRAST";

	public static final String SNR = "SNR";
	
	public static final String MEAN_INTENSITY = "MEAN_INTENSITY";

	public static final String MEDIAN_INTENSITY = "MEDIAN_INTENSITY";

	public static final String MIN_INTENSITY = "MIN_INTENSITY";

	public static final String MAX_INTENSITY = "MAX_INTENSITY";

	public static final String TOTAL_INTENSITY = "TOTAL_INTENSITY";

	public static final String STANDARD_DEVIATION = "STANDARD_DEVIATION";
	
	public static final String AREA = "Area";
	
	public static final String CIRC = "Circ.";

	public static Collection<String> spotFeatures = new LinkedHashSet< String >(
		Arrays.asList(Spot.FRAME, Spot.POSITION_X, Spot.POSITION_Y, Spot.RADIUS, Spot.QUALITY,
			AREA, CIRC, CONTRAST, SNR, MEAN_INTENSITY, MEDIAN_INTENSITY, MIN_INTENSITY, MAX_INTENSITY, 
			TOTAL_INTENSITY, STANDARD_DEVIATION));

	public ExportTracksToCSV() {
	}

	@Override
	public void execute(TrackMate trackmate) {
		logger.log("Exporting tracks to CSV files.\n");
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
		    filename = folder.getPath() + File.separator + filename  + "_Tracks.csv";
		} catch (final NullPointerException npe) {
		    filename = folder.getPath() + File.separator + "Tracks.csv";
		}
		
		SaveDialog sd = new SaveDialog("Save CSV Files", filename, ".csv");
		String fileName = sd.getFileName();
		
		if (fileName.isEmpty()) return;
		
		File fileTracks = new File(sd.getDirectory() + fileName); 
		File fileSpots  = new File(sd.getDirectory() + fileName.substring(0, fileName.length()-4) + "_spots.csv");
		
		if (fileTracks.delete())
			logger.log("File will be overwritten!\n");
		
		try {
			final FileWriter writerTracks = new FileWriter(fileTracks);
			final FileWriter writerSpots = new FileWriter(fileSpots);
			
			final Set<Integer> trackIDs = model.getTrackModel().trackIDs(true);
			
			String Header_String = "";
			for (String feature : trackFeatures)
		    	Header_String += feature+ ", ";
			Header_String = Header_String.substring(0, Header_String.length()-2) + "\n";
			writerTracks.write(Header_String);
			
			Header_String = "TrackID, SpotID, ";
			for (String feature : spotFeatures)
		    	Header_String += feature+ ", ";
			Header_String = Header_String.substring(0, Header_String.length()-2) + "\n";
			writerSpots.write(Header_String);
			
			for (final Integer trackID : trackIDs) {
				final Set<Spot> trackSpots = model.getTrackModel().trackSpots(trackID);
			    
			    // Sort them by time
			    final TreeSet<Spot> sortedTrack = new TreeSet<Spot>(Spot.timeComparator);
			    sortedTrack.addAll(trackSpots);
			    
			    String Track_String = "";
			    Iterator<String> it = trackFeatures.iterator();
			    Double val = model.getFeatureModel().getTrackFeature( trackID, it.next() );
			    Track_String += String.format(Locale.US, "%d, ", val.intValue()); // first column is integer
			    
			    while ( it.hasNext() ){
					val = model.getFeatureModel().getTrackFeature( trackID, it.next() );
					if (val==null || Double.isNaN(val) || Double.isInfinite(val))
				    	val=Double.valueOf(0d);
					Track_String += String.format(Locale.US, "%.4f, ", val.floatValue());
			    }
			    Track_String = Track_String.substring(0, Track_String.length()-2) + "\n";
			    writerTracks.write(Track_String);
			     
			    for (final Spot spot : sortedTrack) {
			    	String Spot_String = "";
			    	Spot_String += String.format(Locale.US, "%d, ", trackID.intValue());
			    	Spot_String += String.format(Locale.US, "%d, ", spot.ID());
			    	for ( final String feature : spotFeatures ){
			    		Number value = spot.getFeature(feature);
			    		Spot_String += String.format(Locale.US, "%.4f, ", value.floatValue());
			    	}
			    	Spot_String = Spot_String.substring(0, Spot_String.length()-2) + "\n";
			    	writerSpots.write(Spot_String);
			    }    
			}
			writerTracks.close();
			writerSpots.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		final long end = System.currentTimeMillis();
		logger.log("Done in " + (end-start) + " ms.");
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
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new ExportTracksToCSV();
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}
	}

}
