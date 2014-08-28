package net.chicoronny.trackmate.lineartracker;

import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.KEY_INITIAL_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.KEY_MAX_COST;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.KEY_STICK_RADIUS;
import static net.chicoronny.trackmate.lineartracker.linearTrackerKeys.KEY_SUCCEEDING_DISTANCE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.imglib2.RealPoint;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.collection.KDTree;
import net.imglib2.collection.KDTreeNode;
import net.imglib2.util.ValuePair;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.kdtree.FlagNode;
import fiji.plugin.trackmate.util.TMUtils;


/**
 * The Class linearTracker.
 * 
 * 1. Link and set a flag for all objects that are sticking more than 90% of the time
 * lapse movie, i.e not moving within a preset radius (Stick radius)
 * 
 * 2. Establish a first possible link from an object from the first frame with an object
 * in the second frame within an initial radius
 * 
 * 3. Estimate the position of the object in the next frame (3rd) with the obtained vector
 * 
 * 4. Link to an object near to this estimated position within a succeeding radius
 * 
 * 5. Go on to the next frame until the last is reached
 * 
 * @author Ronny Sczech
 */
public class LinearTracker implements SpotTracker, Benchmark
{

    /** The logger. */
    private Logger logger = Logger.VOID_LOGGER;
    
    /** The graph. */
    private SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph;
    
    /** The spots. */
    private final SpotCollection spots;
    
    /** The settings. */
    private final Map<String, Object> settings;
    
    /** The error message. */
    private String errorMessage;

    private long processingTime;

    /**
     * Instantiates a new tracker.
     * 
     * @param spots
     *            the spots
     * @param settings
     *            the settings
     */
    public LinearTracker(final SpotCollection spots, final Map<String, Object> settings) {
	this.spots = spots;
	this.settings = settings;
    }

    /* (non-Javadoc)
     * @see net.imglib2.algorithm.OutputAlgorithm#getResult()
     */
    @Override
    public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getResult() {
	return graph;
    }

    /* (non-Javadoc)
     * @see net.imglib2.algorithm.Algorithm#checkInput()
     */
    @Override
    public boolean checkInput() {
	final StringBuilder errrorHolder = new StringBuilder();
	final boolean ok = checkInput(settings, errrorHolder);
	if (!ok) {
	    errorMessage = errrorHolder.toString();
	}
	return ok;
    }

    /* (non-Javadoc)
     * @see net.imglib2.algorithm.Algorithm#getErrorMessage()
     */
    @Override
    public String getErrorMessage() {
	return errorMessage;
    }

    /* (non-Javadoc)
     * @see net.imglib2.algorithm.Algorithm#process()
     */
    @Override
    public boolean process() {
	// Check that the objects list itself isn't null
	if (null == spots) {
	    errorMessage = "The spot collection is null.";
	    return false;
	}

	// Check that the objects list contains inner collections.
	if (spots.keySet().isEmpty()) {
	    errorMessage = "The spot collection is empty.";
	    return false;
	}

	// Check that at least one inner collection contains an object.
	boolean empty = true;
	for (final int frame : spots.keySet()) {
	    if (spots.getNSpots(frame, true) > 0) {
		empty = false;
		break;
	    }
	}
	if (empty) {
	    errorMessage = "The spot collection is empty.";
	    return false;
	}
	
	final long start = System.currentTimeMillis();

	reset();

	// Extract parameter values
	final double initR = (Double) settings.get(KEY_INITIAL_DISTANCE);
	final double succR = (Double) settings.get(KEY_SUCCEEDING_DISTANCE);
	final double stickR = (Double) settings.get(KEY_STICK_RADIUS);
	final double maxCost = (Double) settings.get(KEY_MAX_COST);

	// Make List of KD-Trees
	int nFrames = spots.keySet().size();
	final List<KDTree<FlagNode<Spot>>> treeList = new ArrayList<KDTree<FlagNode<Spot>>>(nFrames);
	final Iterator<Integer> frameIt = spots.keySet().iterator();

	while (frameIt.hasNext()) {
	    final int curFrame = frameIt.next();
	    final int nNextSpots = spots.getNSpots(curFrame, true);

	    final List<RealPoint> nextCoords = new ArrayList<RealPoint>(nNextSpots);
	    final List<FlagNode<Spot>> nextNodes = new ArrayList<FlagNode<Spot>>(nNextSpots);
	    final Iterator<Spot> nextIt = spots.iterator(curFrame, true);
	    while (nextIt.hasNext()) {
		final double[] coords = new double[3];
		// use estimated radius
		final Spot spot = LTUtils.RadiusToEstimated(nextIt.next());
		TMUtils.localize(spot, coords);
		nextCoords.add(new RealPoint(coords));
		nextNodes.add(new FlagNode<Spot>(spot));
	    }
	    if (!nextNodes.isEmpty() && !nextCoords.isEmpty())
		treeList.add(new KDTree<FlagNode<Spot>>(nextNodes, nextCoords));
	}
	
	nFrames = treeList.size();

	KDTree<FlagNode<Spot>> frameTree = treeList.get(0);
	
	//Burn-out Sticking Particles
	
	final Iterator<FlagNode<Spot>> iterator = frameTree.iterator();
	while (iterator.hasNext()) {
	    final FlagNode<Spot> source = iterator.next();
	    
	    int curFrame = 1;
	    final List<FlagNode<Spot>> nodeList = new ArrayList<FlagNode<Spot>>();
	    while (curFrame < nFrames) {
		final RadiusNeighborFlagSearchOnKDTree bsearch = new RadiusNeighborFlagSearchOnKDTree(treeList.get(curFrame));
		curFrame++;
		bsearch.search(source.getValue(), stickR, true);
		if (bsearch.numNeighbors() > 0){
		    final ArrayList<ValuePair<KDTreeNode<FlagNode<Spot>>, Double>> cur = bsearch.getResults();
		    nodeList.add(cur.get(0).getA().get());
		}
	    }
	    
	    final int  burn = (int) Math.round(nFrames*0.9d);
	    if (nodeList.size() > burn){
		final Iterator<FlagNode<Spot>> ii = nodeList.iterator();
		FlagNode<Spot> oldNode = source;
		oldNode.setVisited(true);
		
		while (ii.hasNext()){
		    final FlagNode<Spot> loopNode = ii.next();
		    loopNode.setVisited(true);
		    
		    final Spot begin = oldNode.getValue();
		    final Spot fin = loopNode.getValue(); 
		    graph.addVertex(begin);
		    graph.addVertex(fin);
		    final DefaultWeightedEdge edge = graph.addEdge(begin, fin);
		    graph.setEdgeWeight(edge, 0d);
		 
		    oldNode = loopNode;
		}
	    }
	}

	frameTree = treeList.get(0);
	// Main Loop over all frames
	for (int Tree = 1; Tree < nFrames; Tree++) {
	    // go to next frame for searching
	    final RadiusNeighborFlagSearchOnKDTree rsearch = new RadiusNeighborFlagSearchOnKDTree(treeList.get(Tree));

	    // retrieve spots from current frame
	    final Iterator<FlagNode<Spot>> spotIt = frameTree.iterator();
	    while (spotIt.hasNext()) {

		final Spot source = spotIt.next().getValue();
		final double[] sourceCoords = new double[3];
		TMUtils.localize(source, sourceCoords);
		rsearch.search(source, initR, true); // use initial radius for searching spot in the next frame

		if (rsearch.numNeighbors() < 1)
		    continue;

		final FlagNode<Spot> foundNode = rsearch.getSampler(0).get();
		// get the cost - we have to use the getSquareDistance function here for compatibility
		double cost = rsearch.getSquareDistance(0); 
		final double[] searchCoords = new double[3];
		TMUtils.localize(foundNode.getValue(), searchCoords);
		double[] preVector = LTUtils.Subtract(searchCoords, sourceCoords);
		double[] estim = new double[3];
		estim[0] = 0;
		estim[1] = 0;
		estim[2] = 0;
		int count = 1;
		FlagNode<Spot> oldNode = foundNode;
		int succFrame = Tree + 1;
		boolean firstRun = true;

		while (succFrame < nFrames - 1) { 
		    final RadiusNeighborFlagSearchOnKDTree lsearch = new 
			    RadiusNeighborFlagSearchOnKDTree(treeList.get(succFrame));
		    estim = LTUtils.Add(estim, preVector);
		    final double[] estimMean = LTUtils.DivideScalar(estim, count); // calculate means
		    final double[] estimCoords = LTUtils.Add(searchCoords, estimMean); // estimate search position
		    final double[] oldCoords = searchCoords.clone();
		    final Spot estimSpot = new Spot(estimCoords[0], estimCoords[1], estimCoords[2], 
			    oldNode.getValue().getFeature( Spot.RADIUS), oldNode.getValue().getFeature(Spot.QUALITY));

		    lsearch.search(estimSpot, succR, oldCoords, maxCost, true); // use succeeding radius for searching spot in next frame
		    
		    if (lsearch.numNeighbors() < 1) {
			if (firstRun) {  // automatic gap handling -- just one frame
			    firstRun = false;
			    //estim = LTUtils.Add(estim, preVector);
			    succFrame++;
			    count++;
			    continue;
			} else
			    break;
		    }
		    // get first entry in the ordered result list
		    final ValuePair<KDTreeNode<FlagNode<Spot>>, Double> cur = lsearch.getResults().get(0); 

		    final FlagNode<Spot> loopNode = cur.getA().get();
		    cost = cur.getB();

		    oldNode.setVisited(true);
		    loopNode.setVisited(true);
		    
		    final Spot begin = oldNode.getValue(); 
		    final Spot fin = loopNode.getValue();

		    TMUtils.localize(fin, searchCoords);
		    preVector = LTUtils.Subtract(searchCoords, oldCoords);
		    
		    // make the link
		    graph.addVertex(begin);
		    graph.addVertex(fin);
		    final DefaultWeightedEdge edge = graph.addEdge(begin, fin);
		    graph.setEdgeWeight(edge, cost);
		    
		    oldNode = loopNode;
		    count++;
		    succFrame++;
		}
	    }
	    frameTree = treeList.get(Tree);
	    logger.setProgress(Tree / nFrames);
	}
	logger.setProgress(1d);
	logger.setStatus("");
	final long end = System.currentTimeMillis();
	processingTime = end - start;
	return true;
    }

    /**
     * Reset any link created in the graph result in this tracker, effectively
     * creating a new graph, containing the spots but no edge.
     */
    private void reset() {
	graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(
		DefaultWeightedEdge.class);
	for (final Spot spot : spots.iterable(true)) {
	    graph.addVertex(spot);
	}
    }

    /* (non-Javadoc)
     * @see fiji.plugin.trackmate.tracking.SpotTracker#setLogger(fiji.plugin.trackmate.Logger)
     */
    @Override
    public void setLogger(final Logger logger) {
	this.logger = logger;
    }

    /**
     * Check input.
     * 
     * @param settings
     *            the settings
     * @param errorHolder
     *            the error holder
     * @return true, if successful
     */
    public static boolean checkInput(final Map<String, Object> settings, final StringBuilder errorHolder) {
	boolean ok = true;
	ok = ok	& checkParameter(settings, KEY_INITIAL_DISTANCE, Double.class,	errorHolder);
	ok = ok	& checkParameter(settings, KEY_SUCCEEDING_DISTANCE, Double.class, errorHolder);
	ok = ok	& checkParameter(settings, KEY_STICK_RADIUS, Double.class, errorHolder);
	ok = ok	& checkParameter(settings, KEY_MAX_COST, Double.class, errorHolder);
	final List<String> mandatoryKeys = new ArrayList<String>();
	mandatoryKeys.add(KEY_INITIAL_DISTANCE);
	mandatoryKeys.add(KEY_SUCCEEDING_DISTANCE);
	mandatoryKeys.add(KEY_STICK_RADIUS);
	mandatoryKeys.add(KEY_MAX_COST);
	ok = ok & checkMapKeys(settings, mandatoryKeys, null, errorHolder);
	return ok;
    }

    @Override
    public long getProcessingTime() {
	return processingTime;
    }

}