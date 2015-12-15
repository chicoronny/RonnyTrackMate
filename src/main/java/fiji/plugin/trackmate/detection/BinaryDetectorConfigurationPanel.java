package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.detection.BinaryDetectorFactory.KEY_MAX;
import static fiji.plugin.trackmate.detection.BinaryDetectorFactory.KEY_MIN;
import static fiji.plugin.trackmate.detection.BinaryDetectorFactory.KEY_OPTIONS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.util.JLabelLogger;
import fiji.util.NumberParser;

import javax.swing.SpringLayout;
import javax.swing.JLabel;

import java.awt.Font;

import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class BinaryDetectorConfigurationPanel extends ConfigurationPanel {
	
	private ImagePlus imp;
	private Model model;
	private Logger localLogger;
	private int options;

	public BinaryDetectorConfigurationPanel(final ImagePlus imp, final String infoText, final String detectorName, final Model model) {
		this.imp = imp;
		this.model = model;
		SpringLayout springLayout = new SpringLayout();
		setLayout(springLayout);
		
		JLabel lblHelpText = new JLabel();
		springLayout.putConstraint(SpringLayout.NORTH, lblHelpText, 35, SpringLayout.NORTH, this);
		springLayout.putConstraint(SpringLayout.WEST, lblHelpText, 10, SpringLayout.WEST, this);
		lblHelpText.setFont(FONT.deriveFont( Font.ITALIC ));
		lblHelpText.setText( infoText.replace( "<br>", "" ).replace( "<p>", "<p align=\"justify\">" ).replace( "<html>", "<html><p align=\"justify\">" ) );
		add(lblHelpText);
		
		JLabel lblMaximumSize = new JLabel("Maximum Size");
		springLayout.putConstraint(SpringLayout.NORTH, lblMaximumSize, 79, SpringLayout.SOUTH, lblHelpText);
		springLayout.putConstraint(SpringLayout.WEST, lblMaximumSize, 0, SpringLayout.WEST, lblHelpText);
		lblMaximumSize.setFont(FONT);
		add(lblMaximumSize);
		
		JLabel lblSettings = new JLabel("Settings for: " + detectorName);
		springLayout.putConstraint(SpringLayout.WEST, lblSettings, 0, SpringLayout.WEST, lblHelpText);
		springLayout.putConstraint(SpringLayout.SOUTH, lblSettings, -8, SpringLayout.NORTH, lblHelpText);
		lblSettings.setFont(BIG_FONT);
		add(lblSettings);
		
		btnPreview = new JButton("Preview");
		springLayout.putConstraint(SpringLayout.WEST, btnPreview, 0, SpringLayout.WEST, lblHelpText);
		btnPreview.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				preview();
			}
		});
		add(btnPreview);
		
		JLabel lblSegmentInChannel = new JLabel( "Segment in channel:" );
		springLayout.putConstraint(SpringLayout.SOUTH, lblSegmentInChannel, -275, SpringLayout.SOUTH, this);
		springLayout.putConstraint(SpringLayout.NORTH, btnPreview, 22, SpringLayout.SOUTH, lblSegmentInChannel);
		springLayout.putConstraint(SpringLayout.WEST, lblSegmentInChannel, 0, SpringLayout.WEST, lblHelpText);
		springLayout.putConstraint( SpringLayout.EAST, lblSegmentInChannel, 116, SpringLayout.WEST, this );
		lblSegmentInChannel.setFont( SMALL_FONT );
		add( lblSegmentInChannel );
		
		final JLabel labelChannel= new JLabel( "1" );
		springLayout.putConstraint(SpringLayout.NORTH, labelChannel, 0, SpringLayout.NORTH, lblSegmentInChannel);
		springLayout.putConstraint(SpringLayout.EAST, labelChannel, -64, SpringLayout.EAST, this);
		labelChannel.setHorizontalAlignment( SwingConstants.CENTER );
		labelChannel.setFont( SMALL_FONT );
		add( labelChannel );
		
		JLabelLogger labelLogger = new JLabelLogger();
		springLayout.putConstraint(SpringLayout.NORTH, labelLogger, 167, SpringLayout.NORTH, this);
		springLayout.putConstraint(SpringLayout.WEST, labelLogger, 0, SpringLayout.WEST, lblHelpText);
		add(labelLogger);
		
		JLabel lblMinimumSize = new JLabel("Minimum Size");
		springLayout.putConstraint(SpringLayout.WEST, lblMinimumSize, 0, SpringLayout.WEST, lblHelpText);
		springLayout.putConstraint(SpringLayout.SOUTH, lblMinimumSize, -22, SpringLayout.NORTH, lblMaximumSize);
		lblMinimumSize.setFont(new Font("Arial", Font.PLAIN, 10));
		add(lblMinimumSize);
		
		textFieldMin = new JTextField();
		springLayout.putConstraint(SpringLayout.WEST, textFieldMin, 29, SpringLayout.EAST, lblMinimumSize);
		textFieldMin.setFont(FONT);
		textFieldMin.setHorizontalAlignment(SwingConstants.TRAILING);
		textFieldMin.setText("10");
		add(textFieldMin);
		textFieldMin.setColumns(10);
		
		textFieldMax = new JTextField();
		springLayout.putConstraint(SpringLayout.SOUTH, textFieldMin, -10, SpringLayout.NORTH, textFieldMax);
		springLayout.putConstraint(SpringLayout.EAST, textFieldMin, 0, SpringLayout.EAST, textFieldMax);
		springLayout.putConstraint(SpringLayout.EAST, textFieldMax, -132, SpringLayout.EAST, this);
		textFieldMax.setFont(FONT);
		textFieldMax.setHorizontalAlignment(SwingConstants.TRAILING);
		springLayout.putConstraint(SpringLayout.NORTH, textFieldMax, 91, SpringLayout.SOUTH, lblSettings);
		springLayout.putConstraint(SpringLayout.WEST, textFieldMax, 26, SpringLayout.EAST, lblMaximumSize);
		textFieldMax.setText("10000");
		add(textFieldMax);
		textFieldMax.setColumns(10);
		
		sliderChannel = new JSlider();
		sliderChannel.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				labelChannel.setText( "" + sliderChannel.getValue() );
			}
		});
		springLayout.putConstraint(SpringLayout.NORTH, sliderChannel, 21, SpringLayout.SOUTH, textFieldMax);
		springLayout.putConstraint(SpringLayout.WEST, labelChannel, 15, SpringLayout.EAST, sliderChannel);
		springLayout.putConstraint(SpringLayout.WEST, sliderChannel, 11, SpringLayout.EAST, lblSegmentInChannel);
		springLayout.putConstraint(SpringLayout.EAST, sliderChannel, 77, SpringLayout.EAST, lblSegmentInChannel);
		add(sliderChannel);
		
		// Deal with channels: the slider and channel labels are only
		// visible if we find more than one channel.
		final int n_channels = imp.getNChannels();

		if ( n_channels <= 1 ){
			labelChannel.setVisible( false );
			lblSegmentInChannel.setVisible( false );
			sliderChannel.setVisible( false );
		}else{
			labelChannel.setVisible( true );
			lblSegmentInChannel.setVisible( true );
			sliderChannel.setVisible( true );
		}
		localLogger = labelLogger.getLogger();
	}

	private static final long serialVersionUID = 1L;
	private JButton btnPreview;
	private JTextField textFieldMin;
	private JTextField textFieldMax;
	private JSlider sliderChannel;

	@Override
	public Map<String, Object> getSettings() {
		final HashMap< String, Object > settings = new HashMap< String, Object >( 4 );
		settings.put( KEY_OPTIONS, options);
		settings.put( KEY_MIN, NumberParser.parseInteger(textFieldMin.getText()));
		settings.put( KEY_MAX, NumberParser.parseInteger(textFieldMax.getText()));
		settings.put( KEY_TARGET_CHANNEL, sliderChannel.getValue());
		return settings;
	}

	@Override
	public void setSettings(Map<String, Object> settings) {
		textFieldMin.setText(String.valueOf(settings.get(KEY_MIN)));
		textFieldMax.setText(String.valueOf(settings.get(KEY_MAX)));
		options = (Integer) settings.get(KEY_OPTIONS);
		sliderChannel.setValue( ( Integer ) settings.get( KEY_TARGET_CHANNEL ) );
	}
	
	/**
	 * Returns a new instance of the {@link SpotDetectorFactory} that this
	 * configuration panels configures. The new instance will in turn be used
	 * for the preview mechanism. Therefore, classes extending this class are
	 * advised to return a suitable implementation of the factory.
	 * 
	 * @return a new {@link SpotDetectorFactory}.
	 */
	@SuppressWarnings( "rawtypes" )
	protected SpotDetectorFactory< ? > getDetectorFactory(){
		return new BinaryDetectorFactory();
	}
	
	/**
	 * Launch detection on the current frame.
	 */
	private void preview()
	{
		btnPreview.setEnabled( false );
		new Thread( "TrackMate preview detection thread" )
		{
			@Override
			public void run(){
				final Settings settings = new Settings();
				settings.setFrom( imp );
				final int frame = imp.getFrame() - 1;
				settings.tstart = frame;
				settings.tend = frame;

				settings.detectorFactory = getDetectorFactory();
				settings.detectorSettings = getSettings();

				final TrackMate trackmate = new TrackMate( settings );
				trackmate.getModel().setLogger( localLogger );

				final boolean detectionOk = trackmate.execDetection();
				if ( !detectionOk ){
					localLogger.error( trackmate.getErrorMessage() );
					return;
				}
				localLogger.log( "Found " + trackmate.getModel().getSpots().getNSpots( false ) + " spots." );

				// Wrap new spots in a list.
				final SpotCollection newspots = trackmate.getModel().getSpots();
				final Iterator< Spot > it = newspots.iterator( frame, false );
				final ArrayList< Spot > spotsToCopy = new ArrayList< Spot >( newspots.getNSpots( frame, false ) );
				while ( it.hasNext() ){
					spotsToCopy.add( it.next() );
				}
				// Pass new spot list to model.
				model.getSpots().put( frame, spotsToCopy );
				// Make them visible
				for ( final Spot spot : spotsToCopy ){
					spot.putFeature( SpotCollection.VISIBLITY, SpotCollection.ONE );
				}
				// Generate event for listener to reflect changes.
				model.setSpots( model.getSpots(), true );

				btnPreview.setEnabled( true );
			};
		}.start();
	}

	@Override
	public void clean() {		
	}
}
