package net.chicoronny.trackmate;

import javax.swing.JFrame;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.gui.wizard.TrackMateWizardSequence;
import fiji.plugin.trackmate.gui.wizard.WizardSequence;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class TrackMatePlugIn_ implements PlugIn
{
	/**
	 * Runs the TrackMate GUI plugin.
	 *
	 * @param imagePath
	 *            a path to an image that can be read by ImageJ. If set, the
	 *            image will be opened and TrackMate will be started set to
	 *            operate on it. If <code>null</code> or 0-length, TrackMate
	 *            will be set to operate on the image currently opened in
	 *            ImageJ.
	 */
	@Override
	public void run( final String imagePath )
	{
		final ImagePlus imp;
		if ( imagePath != null && imagePath.length() > 0 )
		{
			imp = new ImagePlus( imagePath );
			if ( null == imp.getOriginalFileInfo() )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, "Could not load image with path " + imagePath + "." );
				return;
			}
		}
		else
		{
			imp = WindowManager.getCurrentImage();
			if ( null == imp )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, "Please open an image before running TrackMate." );
				return;
			}
		}
		if ( !imp.isVisible() )
		{
			imp.setOpenAsHyperStack( true );
			imp.show();
		}
		GuiUtils.userCheckImpDimensions( imp );

		final Settings settings = createSettings( imp );
		final Model model = createModel();
		final TrackMate trackmate = createTrackMate(model, settings);
		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings displaySettings = DisplaySettingsIO.readUserDefault().copy( "CurrentDisplaySettings" );

		/*
		 * Launch GUI.
		 */
		// Wizard.
		final WizardSequence sequence = new TrackMateWizardSequence( trackmate, selectionModel, displaySettings );
		final JFrame frame = sequence.run( "TrackMate on " + imp.getShortTitle() );
		frame.setIconImage( Icons.TRACKMATE_ICON.getImage() );
		GuiUtils.positionWindow( frame, imp.getWindow() );
				frame.setVisible( true );		
	}

	/*
	 * HOOKS
	 */

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Model} instance that will be used to store data in the
	 * {@link TrackMate} instance.
	 *
	 * @return a new {@link Model} instance.
	 */

	protected Model createModel()
	{
		return new Model();
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Settings} instance that will be used to tune the
	 * {@link TrackMate} instance. It is initialized by default with values
	 * taken from the current {@link ImagePlus}.
	 *
	 * @return a new {@link Settings} instance.
	 */
	protected Settings createSettings( final ImagePlus imp )
	{
		final Settings settings = new Settings(imp);
		return settings;
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the TrackMate instance that will be controlled in the GUI.
	 *
	 * @return a new {@link TrackMate} instance.
	 */
	protected TrackMate createTrackMate( final Model model, final Settings settings )
	{
		/*
		 * Since we are now sure that we will be working on this model with this
		 * settings, we need to pass to the model the units from the settings.
		 */
		final String spaceUnits = settings.imp.getCalibration().getXUnit();
		final String timeUnits = settings.imp.getCalibration().getTimeUnit();
		model.setPhysicalUnits( spaceUnits, timeUnits );

		final TrackMate trackmate = new TrackMate( model, settings );

		// Set the num of threads from IJ prefs.
		trackmate.setNumThreads( Prefs.getThreads() );

		return trackmate;
	}

	/*
	 * MAIN METHOD
	 */

	/**
	 * @param args
	 * 			program arguments
	 */
	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new TrackMatePlugIn_().run( "samples/FakeTracks.tif" );
	}

}
