package net.chicoronny.trackmate.lineartracker;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory;

public class LTUtils {
    
    /**
     * Subtract.
     * 
     * @param first
     *            the first
     * @param second
     *            the second
     * @return the double[]
     */
    public static final double[] Subtract(double[] first, double[] second) {
	assert (first.length == 3);
	assert (second.length == 3);
	final double[] res = new double[3];
	res[0] = first[0] - second[0];
	res[1] = first[1] - second[1];
	res[2] = first[2] - second[2];
	return res;
    }

    /**
     * Adds the.
     * 
     * @param first
     *            the first
     * @param second
     *            the second
     * @return the double[]
     */
    public static final double[] Add(double[] first, double[] second) {
	assert (first.length == 3);
	assert (second.length == 3);
	final double[] res = new double[3];
	res[0] = first[0] + second[0];
	res[1] = first[1] + second[1];
	res[2] = first[2] + second[2];
	return res;
    }

    /**
     * Divide scalar.
     * 
     * @param first
     *            the first
     * @param second
     *            the second
     * @return the double[]
     */
    public static final double[] DivideScalar(double[] first, double second) {
	assert (first.length == 3);
	assert (second != 0);
	final double[] res = new double[3];
	res[0] = first[0] / second;
	res[1] = first[1] / second;
	res[2] = first[2] / second;
	return res;
    }
   
     
    /**
     * Angle from vectors.
     * The angle is calculated as <code>acos(|a x b|)</code> to give the angle of 90° the maximum value.
     * 
     * @param first
     *            the first
     * @param second
     *            the second
     * @return the double
     */
    public static final double angleFromVectors(double[] first, double[] second){
	assert (first.length == 3);
	assert (second.length == 3);
	final double lenFirst = Math.sqrt( first[0] * first[0] + first[1] * first[1] + first[2] * first[2] );
	final double lenSecond = Math.sqrt( second[0] * second[0] + second[1] * second[1] + second[2] * second[2] );
	final double dotProduct = first[0] * second[0] + first[1] * second[1] + first[2] * second[2];
	if (lenFirst == 0d || lenSecond == 0d) 
	    return 0;
	return Math.acos(Math.abs(dotProduct / (lenFirst * lenSecond)))* 180 / Math.PI;
    }

    /**
     * Set Radius to estimated Radius.
     * 
     * @param spot
     *            the spot
     * @return the spot with estimated Radius
     */
    public static final Spot RadiusToEstimated(Spot spot) {
	final Double diameter = spot.getFeature(SpotRadiusEstimatorFactory.ESTIMATED_DIAMETER);
	if (null == diameter || diameter == 0) {
	    return spot;
	}
	spot.putFeature(Spot.RADIUS, diameter / 2);
	return spot;
    }
    
    /**
     * List files.
     * 
     * @param dir
     *            the directory
     * @param ext
     *            the extensions
     * @return the collection
     */
    public static Collection<File> listFiles(File dir, String[] ext) {
	if (!dir.isDirectory())
	    throw new IllegalArgumentException("Parameter 'dir' is not a directory");
	if (ext == null)
	    throw new NullPointerException("Parameter 'ext' is null");

	String[] suffixes = new String[ext.length];
	for (int i = 0; i < ext.length; i++) {
	    suffixes[i] = "." + ext[i];
	}

	class SuffixFileFilter implements FileFilter{

	    private String[] suffixes;

	    public  SuffixFileFilter(String[] suffixes){
		if (suffixes == null) 
		    throw new IllegalArgumentException("The array of suffixes must not be null");
	        this.suffixes = suffixes;
	    }
	    
	    // accept directories and files with the given suffix(es)
	    @Override
	    public boolean accept(File file) {
		if (file.isDirectory()) return true;
		String name = file.getName();
		for (int i = 0; i < this.suffixes.length; i++) {
		    if (name.endsWith(this.suffixes[i])) 
			return true;
		}
		return false;
	    }
	};
	
	FileFilter filter = new SuffixFileFilter(suffixes);

	Collection<File> files = new java.util.LinkedList<File>();
	innerListFiles(files, dir, filter);

	return files;
    }
    
    private static void innerListFiles(Collection<File> files, File dir, FileFilter filter) {
	File[] found = dir.listFiles(filter);
	if (found != null) {
	    for (int i = 0; i < found.length; i++) {
		if (found[i].isDirectory())
		    innerListFiles(files, found[i], filter);
		else
		    files.add(found[i]);

	    }
	}
    }
}
