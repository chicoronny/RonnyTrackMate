package net.chicoronny.trackmate.lineartracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import fiji.plugin.trackmate.tracking.kdtree.FlagNode;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.Spot;
import net.imglib2.RealLocalizable;
import net.imglib2.Sampler;
import net.imglib2.KDTree;
import net.imglib2.KDTreeNode;
import net.imglib2.neighborsearch.RadiusNeighborSearch;
import net.imglib2.util.ValuePair;

/**
 * The Class RadiusNeighborFlagSearchOnKDTree.
 * 
 * This class is inherited by RadiusNeighborSearch with FlagNode of Spot as type.
 */
public class RadiusNeighborFlagSearchOnKDTree implements RadiusNeighborSearch<FlagNode<Spot>> {

    /** The KD tree. */
    protected KDTree<FlagNode<Spot>> tree;
    
    /** The number of dimensions. */
    protected final int n;
    
    /** The position. */
    protected final double[] pos;
    
    /** The result points. */
    protected ArrayList<ValuePair<KDTreeNode<FlagNode<Spot>>, Double>> resultPoints;

    /**
     * Instantiates a new radius neighbor flag search on the KD tree.
     * 
     * @param tree
     *            the KD tree
     */
    public RadiusNeighborFlagSearchOnKDTree(KDTree<FlagNode<Spot>> tree) {
    	n = tree.numDimensions();
    	pos = new double[n];
    	this.tree = tree;
    	this.resultPoints = new ArrayList<ValuePair<KDTreeNode<FlagNode<Spot>>, Double>>();
    }

    /**
     * Find and order spots by a cost function
     * <p>
     * The cost function is calculated as follow:
     * <code>cost = D² + (1 + 1.5*R)² + A</code>
     * 
     * <p>
     * where D is the distance between 2 spots, R is the difference in estimated radii and A the enclosed angle (in °) between the estimated and the actual vector.
     * The angle is calculated as <code>acos(|a x b|)</code> to give the angle of 90° the maximum value.
     * 
     * @param current
     *            the current node that holds the spot
     * @param squRadius
     *            the square radius
     * @param spotRadius
     *            the estimated spot radius
     * @param maxCost 
     * 		  set a maximum cost for linking
     */
    protected void searchNode(final KDTreeNode<FlagNode<Spot>> current,
	    final double squRadius, float spotRadius, float quality, double[] oldCoords, double maxCost) {
	// consider the current node
	final double squDistance = current.squDistanceTo(pos);
	boolean visited = current.get().isVisited();
	final Spot currentSpot = current.get().getValue();
	// get coordinates of current position
	final double[] currentPos = new double[3];
	TMUtils.localize(currentSpot, currentPos);
	
	if (squDistance <= squRadius && !visited) {
		// calculate reference vector from estimated search position to the old found position from the frame before
		double[] longVector = LTUtils.Subtract(pos, oldCoords); 
		//
	    final double qualityDiff =  Math.abs(currentSpot.getFeature(Spot.QUALITY).floatValue() - quality);
	    // same factor as in LAP tracker
	    final double spotRadiusDiff = 1 + Math.abs(currentSpot.getFeature(Spot.RADIUS).floatValue() - spotRadius) * 3d; 
	    // include angle into cost function with calculation of actual vector from the current position to the old found position
	    final double angle = LTUtils.angleFromVectors(longVector, LTUtils.Subtract(currentPos, oldCoords)); 
	    // set score
	    final double cost = squDistance/8 + spotRadiusDiff + qualityDiff/4 + angle;
	    // set maximal cost
	    if (cost < maxCost) 
	    	resultPoints.add(new ValuePair<KDTreeNode<FlagNode<Spot>>, Double>(current, cost));
	}

	final double axisDiff = pos[current.getSplitDimension()] - current.getSplitCoordinate();
	final double axisSquDistance = axisDiff * axisDiff;
	final boolean leftIsNearBranch = axisDiff < 0;

	// search the near branch
	final KDTreeNode<FlagNode<Spot>> nearChild = leftIsNearBranch ? current.left : current.right;
	final KDTreeNode<FlagNode<Spot>> awayChild = leftIsNearBranch ? current.right : current.left;
	if (nearChild != null)
	    searchNode(nearChild, squRadius, spotRadius, quality, oldCoords, maxCost);

	// search the away branch - maybe
	if ((axisSquDistance <= squRadius) && (awayChild != null))
	    searchNode(awayChild, squRadius, spotRadius, quality, oldCoords, maxCost);
    }

    /**
     * Gets the results.
     * 
     * @return the results
     */
    public ArrayList<ValuePair<KDTreeNode<FlagNode<Spot>>, Double>> getResults() {
	return this.resultPoints;
    }

    /* (non-Javadoc)
     * @see net.imglib2.EuclideanSpace#numDimensions()
     */
    @Override
    public int numDimensions() {
	return n;
    }

    /* (non-Javadoc)
     * @see net.imglib2.neighborsearch.RadiusNeighborSearch#getDistance(int)
     */
    @Override
    public double getDistance(int i) {
	return Math.sqrt(resultPoints.get(i).b);
    }

    /* (non-Javadoc)
     * @see net.imglib2.neighborsearch.RadiusNeighborSearch#getPosition(int)
     */
    @Override
    public RealLocalizable getPosition(int i) {
	return resultPoints.get(i).a;
    }

    /* (non-Javadoc)
     * @see net.imglib2.neighborsearch.RadiusNeighborSearch#getSampler(int)
     */
    @Override
    public Sampler<FlagNode<Spot>> getSampler(int i) {
	return resultPoints.get(i).a;
    }

    /* (non-Javadoc)
     * @see net.imglib2.neighborsearch.RadiusNeighborSearch#getSquareDistance(int)
     */
    @Override
    public double getSquareDistance(int i) {
	return resultPoints.get(i).b;
    }

    /* (non-Javadoc)
     * @see net.imglib2.neighborsearch.RadiusNeighborSearch#numNeighbors()
     */
    @Override
    public int numNeighbors() {
	return resultPoints.size();
    }

    /**
     * Search function returning results sorted  by cost
     * 
     * @param reference
     *            the reference spot
     * @param radius
     *            the radius
     * @param oldCoords 
     * 		  old spot coordinates for angle calculation
     * @param maxCost 
     * 		  set a maximum cost for linking
     * @param sortResults
     *            sorting results
     */
    public void search(final Spot reference, final double radius, double[] oldCoords, double maxCost, final boolean sortResults) {
	TMUtils.localize(reference, pos);
	final float sourceSpotRadius = reference.getFeature(Spot.RADIUS).floatValue();
	final float quality = reference.getFeature(Spot.QUALITY).floatValue();
	resultPoints.clear();
	searchNode(tree.getRoot(), radius * radius, sourceSpotRadius, quality, oldCoords, maxCost);
	if (sortResults) {
	    Collections.sort(resultPoints,
			    new Comparator<ValuePair<KDTreeNode<FlagNode<Spot>>, Double>>() {
				@Override
				public int compare(
					final ValuePair<KDTreeNode<FlagNode<Spot>>, Double> o1,
					final ValuePair<KDTreeNode<FlagNode<Spot>>, Double> o2) {
				    return Double.compare(o1.b, o2.b);
				}
		    });
		}
    }

    // more are less obsolete
    /* (non-Javadoc)
     * @see net.imglib2.neighborsearch.RadiusNeighborSearch#search(net.imglib2.RealLocalizable, double, boolean)
     */
    @Override
    public void search(final RealLocalizable reference, final double radius, final boolean sortResults) {
	assert radius >= 0;
	reference.localize(pos);
	resultPoints.clear();
	searchNode(tree.getRoot(), radius * radius, 3f, 255f, new double[3],100000d);
	if (sortResults) {
	    Collections.sort(resultPoints,
			    new Comparator<ValuePair<KDTreeNode<FlagNode<Spot>>, Double>>() {
				@Override
				public int compare(final ValuePair<KDTreeNode<FlagNode<Spot>>, Double> o1, final ValuePair<KDTreeNode<FlagNode<Spot>>, Double> o2) {
					return Double.compare(o1.b, o2.b);
				}
			    });
	}
    }
    
    public void search(final RealLocalizable reference, final double radius, float spotRadius, double maxCost, final boolean sortResults) {
    	assert radius >= 0;
    	reference.localize(pos);
    	resultPoints.clear();
    	searchNode(tree.getRoot(), radius * radius, spotRadius, 255f, new double[3], maxCost);
    	if (sortResults) {
    	    Collections.sort(resultPoints,
    			    new Comparator<ValuePair<KDTreeNode<FlagNode<Spot>>, Double>>() {
    				@Override
    				public int compare(final ValuePair<KDTreeNode<FlagNode<Spot>>, Double> o1, final ValuePair<KDTreeNode<FlagNode<Spot>>, Double> o2) {
    					return Double.compare(o1.b, o2.b);
    				}
    			    });
    	}
    }

}