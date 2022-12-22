package net.chicoronny.trackmate.lineartracker;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.TEXTFIELD_DIMENSION;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_INITIAL_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_MAX_COST;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_STICK_RADIUS;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_SUCCEEDING_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_ESTIMATE_RADIUS;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;

/**
 * The Class linearTrackerSettingsPanel.
 */
public class LinearTrackerSettingsPanel extends ConfigurationPanel {
    
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;
    
    /** The tracker name. */
    private final String trackerName;
    
    /** The space units. */
    private final String spaceUnits;
    
    /** The label tracker. */
    private JLabel labelTracker;
    
    /** The label tracker description. */
    private JLabel labelTrackerDescription;
    
    /** The info text. */
    private final String infoText;
    
    /** The label units. */
    private JLabel labelUnits;
    
    /** The init dist field. */
    private JFormattedTextField  initDistField;
    
    /** The init succ field. */
    private JFormattedTextField  initSuccField;
    
    /** The label unit2. */
    private JLabel labelUnit2;

    /** The init stick field. */
    private JFormattedTextField  initStickField;

    /** The label unit3. */
    private JLabel labelUnit3;

    /** The max cost field. */
    private JFormattedTextField  maxCostField;
    private JCheckBox chckbxEstimateRadius;

    /**
     * Instantiates a new linear tracker settings panel.
     * 
     * @param name
     *            the name
     * @param infoText
     *            the info text
     * @param spaceUnits
     *            the space units
     */
    public LinearTrackerSettingsPanel(final String name, final String infoText,
	    final String spaceUnits) {
	this.trackerName = name;
	this.spaceUnits = spaceUnits;
	this.infoText = infoText;
	initGUI();
    }

    /**
     * Inits the GUI.
     */
    private void initGUI() {
	this.setPreferredSize(new java.awt.Dimension(300, 500));
	this.setLayout(null);

	final JLabel lblSettingsForTracker = new JLabel("Settings for tracker:");
	lblSettingsForTracker.setBounds(10, 11, 280, 20);
	lblSettingsForTracker.setFont(FONT);
	add(lblSettingsForTracker);

	labelTracker = new JLabel(trackerName);
	labelTracker.setFont(BIG_FONT);
	labelTracker.setHorizontalAlignment(SwingConstants.CENTER);
	labelTracker.setBounds(10, 42, 280, 20);
	add(labelTracker);

	labelTrackerDescription = new JLabel("<tracker description>");
	labelTrackerDescription.setFont(FONT.deriveFont(Font.ITALIC));
	labelTrackerDescription.setBounds(10, 67, 280, 225);
	labelTrackerDescription.setText(infoText.replace("<br>", "")
		.replace("<p>", "<p align=\"justify\">")
		.replace("<html>", "<html><p align=\"justify\">"));
	add(labelTrackerDescription);

	final JLabel lblInitDistance = new JLabel("Initial distance: ");
	lblInitDistance.setFont(FONT);
	lblInitDistance.setBounds(10, 314, 164, 20);
	add(lblInitDistance);

	initDistField = new JFormattedTextField ();
	initDistField.setFont(FONT);
	initDistField.setBounds(184, 316, 62, 20);
	initDistField.setSize(TEXTFIELD_DIMENSION);
	add(initDistField);

	final JLabel lblSuccDistance = new JLabel("Succeeding distance: ");
	lblSuccDistance.setFont(FONT);
	lblSuccDistance.setBounds(10, 342, 164, 20);
	add(lblSuccDistance);

	initSuccField = new JFormattedTextField ();
	initSuccField.setFont(FONT);
	initSuccField.setBounds(184, 344, 62, 20);
	initSuccField.setSize(TEXTFIELD_DIMENSION);
	add(initSuccField);
	
	final JLabel lblStickDistance = new JLabel("Stick radius: ");
	lblStickDistance.setFont(FONT);
	lblStickDistance.setBounds(10, 370, 164, 20);
	add(lblStickDistance);
	
	initStickField = new JFormattedTextField ();
	initStickField.setFont(FONT);
	initStickField.setBounds(184, 372, 62, 20);
	initStickField.setSize(TEXTFIELD_DIMENSION);
	add(initStickField);
	
	final JLabel lblMaxCost = new JLabel("Maximal Cost: ");
	lblMaxCost.setFont(FONT);
	lblMaxCost.setBounds(10, 396, 164, 20);
	add(lblMaxCost);
	
	maxCostField = new JFormattedTextField ();
	maxCostField.setFont(FONT);
	maxCostField.setBounds(184, 398, 62, 20);
	maxCostField.setSize(TEXTFIELD_DIMENSION);
	add(maxCostField);

	labelUnits = new JLabel(spaceUnits);
	labelUnits.setFont(FONT);
	labelUnits.setBounds(236, 314, 34, 20);
	add(labelUnits);

	labelUnit2 = new JLabel(spaceUnits);
	labelUnit2.setFont(FONT);
	labelUnit2.setBounds(236, 342, 34, 20);
	add(labelUnit2);
	
	labelUnit3 = new JLabel(spaceUnits);
	labelUnit3.setFont(FONT);
	labelUnit3.setBounds(236, 368, 34, 20);
	add(labelUnit3);
	
	chckbxEstimateRadius = new JCheckBox("Estimate Radius");
	chckbxEstimateRadius.setFont(FONT);
	chckbxEstimateRadius.setBounds(10, 428, 128, 23);
	add(chckbxEstimateRadius);
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fiji.plugin.trackmate.gui.ConfigurationPanel#setSettings(java.util.Map)
	 */
	@Override
	public void setSettings(final Map<String, Object> settings) {
		initDistField.setText(String.format("%.1f", (Double) settings.get(KEY_INITIAL_DISTANCE)));
		initSuccField.setText(String.format("%.1f", (Double) settings.get(KEY_SUCCEEDING_DISTANCE)));
		initStickField.setText(String.format("%.1f", (Double) settings.get(KEY_STICK_RADIUS)));
		maxCostField.setText(String.format("%.1f", (Double) settings.get(KEY_MAX_COST)));
		chckbxEstimateRadius.setSelected((Boolean) settings.get(KEY_ESTIMATE_RADIUS));
	}

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.gui.ConfigurationPanel#getSettings()
     */
    @Override
    public Map<String, Object> getSettings() {
	final Map<String, Object> settings = new HashMap<String, Object>();
	settings.put(KEY_INITIAL_DISTANCE, initDistField.getValue());
	settings.put(KEY_SUCCEEDING_DISTANCE, initSuccField.getValue());
	settings.put(KEY_STICK_RADIUS, initStickField.getValue());
	settings.put(KEY_MAX_COST, maxCostField.getValue());
	settings.put(KEY_ESTIMATE_RADIUS, chckbxEstimateRadius.isSelected());
	return settings;
    }

	@Override
	public void clean() {
		
	}
}
