package net.chicoronny.trackmate.lineartracker;

import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.DEFAULT_INITIAL_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.DEFAULT_MAX_COST;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.DEFAULT_STICK_RADIUS;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.DEFAULT_SUCCEEDING_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.KEY_INITIAL_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.KEY_MAX_COST;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.KEY_STICK_RADIUS;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.KEY_SUCCEEDING_DISTANCE;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

/**
 * A factory for creating LinearTracker objects.
 */
@Plugin(type = SpotTrackerFactory.class)
public class linearTrackerFactory implements SpotTrackerFactory {

    /** The Constant TRACKER_KEY. */
    public static final String TRACKER_KEY = "LINEAR_TRACKER";
    
    /** The Constant NAME. */
    public static final String NAME = "Linear Tracker";
    
    /** The Constant INFO_TEXT. */
    public static final String INFO_TEXT = "<html>"
	    + "This tracker is linking neighbours by calculation of a cost function which calculates the differences and angles between current and expected position, as well as spot radii differences."
	    + "It uses a two-step algorithm which indentifies possible track starters in a different radius than succeeding track followers. It can close gaps of 1 frame. <br>"
	    + "</html>";
    
    /** The error message. */
    private String errorMessage;

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.TrackMateModule#getInfoText()
     */
    @Override
    public String getInfoText() {
	return INFO_TEXT;
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.TrackMateModule#getIcon()
     */
    @Override
    public ImageIcon getIcon() {
	return null;
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.TrackMateModule#getKey()
     */
    @Override
    public String getKey() {
	return TRACKER_KEY;
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.TrackMateModule#getName()
     */
    @Override
    public String getName() {
	return NAME;
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.tracking.SpotTrackerFactory#create(fiji.plugin.trackmate.SpotCollection, java.util.Map)
     */
    @Override
    public SpotTracker create(final SpotCollection spots, final Map<String, Object> settings) {
	return new LinearTracker(spots, settings);
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.tracking.SpotTrackerFactory#getTrackerConfigurationPanel(fiji.plugin.trackmate.Model)
     */
    @Override
    public ConfigurationPanel getTrackerConfigurationPanel(final Model model) {
	final String spaceUnits = model.getSpaceUnits();
	return new linearTrackerSettingsPanel(NAME, INFO_TEXT, spaceUnits);
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.tracking.SpotTrackerFactory#marshall(java.util.Map, org.jdom2.Element)
     */
    @Override
    public boolean marshall(final Map<String, Object> settings, final Element element) {
	final StringBuilder str = new StringBuilder();
	boolean ok = true;
	ok = ok	& writeAttribute(settings, element, KEY_INITIAL_DISTANCE, Double.class, str);
	ok = ok	& writeAttribute(settings, element, KEY_SUCCEEDING_DISTANCE, Double.class, str);
	ok = ok	& writeAttribute(settings, element, KEY_STICK_RADIUS, Double.class, str);
	ok = ok	& writeAttribute(settings, element, KEY_MAX_COST, Double.class, str);

	if (!ok) {
	    errorMessage = str.toString();
	}
	return ok;
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.tracking.SpotTrackerFactory#unmarshall(org.jdom2.Element, java.util.Map)
     */
    @Override
    public boolean unmarshall(final Element element, final Map<String, Object> settings) {
	boolean ok = true;
	final StringBuilder errorHolder = new StringBuilder();
	settings.clear();
	ok = ok	& readDoubleAttribute(element, settings, KEY_INITIAL_DISTANCE,	errorHolder);
	ok = ok	& readDoubleAttribute(element, settings, KEY_SUCCEEDING_DISTANCE, errorHolder);
	ok = ok	& readDoubleAttribute(element, settings, KEY_STICK_RADIUS, errorHolder);
	ok = ok	& readDoubleAttribute(element, settings, KEY_MAX_COST, errorHolder);

	if (!ok) {
	    errorMessage = errorHolder.toString();
	}
	return ok;
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.tracking.SpotTrackerFactory#toString(java.util.Map)
     */
    @Override
    public String toString(final Map<String, Object> sm) {
	if (!checkSettingsValidity(sm)) {
	    return errorMessage;
	}
	final StringBuilder str = new StringBuilder();
	str.append(String.format("Initial distance: %.1f\n", (Double) sm.get(KEY_INITIAL_DISTANCE)));
	str.append(String.format("Succeeding distance: %.1f\n",	(Double) sm.get(KEY_SUCCEEDING_DISTANCE)));
	str.append(String.format("Stick Radius: %.1f\n", (Double) sm.get(KEY_STICK_RADIUS)));
	str.append(String.format("Max Cost: %.1f\n", (Double) sm.get(KEY_MAX_COST)));

	return str.toString();
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.tracking.SpotTrackerFactory#getDefaultSettings()
     */
    @Override
    public Map<String, Object> getDefaultSettings() {
	final Map<String, Object> settings = new HashMap<String, Object>();
	settings.put(KEY_INITIAL_DISTANCE, DEFAULT_INITIAL_DISTANCE);
	settings.put(KEY_SUCCEEDING_DISTANCE, DEFAULT_SUCCEEDING_DISTANCE);
	settings.put(KEY_STICK_RADIUS, DEFAULT_STICK_RADIUS);
	settings.put(KEY_MAX_COST, DEFAULT_MAX_COST);

	return settings;
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.tracking.SpotTrackerFactory#checkSettingsValidity(java.util.Map)
     */
    @Override
    public boolean checkSettingsValidity(final Map<String, Object> settings) {
	final StringBuilder str = new StringBuilder();
	final boolean ok = LinearTracker.checkInput(settings, str);
	if (!ok) {
	    errorMessage = str.toString();
	}
	return ok;
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.tracking.SpotTrackerFactory#getErrorMessage()
     */
    @Override
    public String getErrorMessage() {
	return errorMessage;
    }

}