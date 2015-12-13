package net.chicoronny.trackmate.lineartracker;

import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_INITIAL_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_MAX_COST;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_STICK_RADIUS;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_SUCCEEDING_DISTANCE;
import static net.chicoronny.trackmate.lineartracker.LinearTrackerKeys.KEY_ESTIMATE_RADIUS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.imglib2.RealCursor;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.KDTree;
import net.imglib2.KDTreeNode;
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
 * The Class LinearTracker.
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
public class LinearTracker implements SpotTracker, MultiThreaded
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

    private int numThreads;

    protected int MAX_GAP = 2;

    private double ANGLE_DIFF = 0.1745d;

    private double LOC_DIFF = 0.26d;

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
	graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
	setNumThreads();
	final Iterator<Spot> it = spots.iterator(true);
	while (it.hasNext()) 
		graph.addVertex(it.next());
	
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

	/*
	 * (non-Javadoc)
	 * 
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

		// final long start = System.currentTimeMillis();

		// Extract parameter values
		final double initR = (Double) settings.get(KEY_INITIAL_DISTANCE);
		final double succR = (Double) settings.get(KEY_SUCCEEDING_DISTANCE);
		final double stickR = (Double) settings.get(KEY_STICK_RADIUS);
		final double maxCost = (Double) settings.get(KEY_MAX_COST);
		final boolean estimRadius = (Boolean) settings.get(KEY_ESTIMATE_RADIUS);

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
				final Spot spot;
				if (estimRadius)
					spot = LTUtils.RadiusToEstimated(nextIt.next());
				else
					spot = nextIt.next();
				TMUtils.localize(spot, coords);
				nextCoords.add(new RealPoint(coords));
				nextNodes.add(new FlagNode<Spot>(spot));
			}
			if (!nextNodes.isEmpty() && !nextCoords.isEmpty()) treeList.add(new KDTree<FlagNode<Spot>>(nextNodes, nextCoords));
		}

		nFrames = treeList.size(); // for the case there are empty frames
		KDTree<FlagNode<Spot>> frameTree = treeList.get(0);

		int dd = 0;
		// Burn-out Sticking Particles
		final Map<Integer, ArrayList<DefaultWeightedEdge>> edgesMap = new HashMap<Integer, ArrayList<DefaultWeightedEdge>>();
		Integer edgesHash = 0;
		final RealCursor<FlagNode<Spot>> KDcursor = frameTree.cursor();
		while (KDcursor.hasNext()) {
			final FlagNode<Spot> source = KDcursor.next();

			int curFrame = 1;
			final List<FlagNode<Spot>> nodeList = new ArrayList<FlagNode<Spot>>();
			while (curFrame < nFrames) {
				final RadiusNeighborFlagSearchOnKDTree bsearch = new RadiusNeighborFlagSearchOnKDTree(treeList.get(curFrame));
				curFrame++;
				bsearch.search(source.getValue(), stickR, false);
				if (bsearch.numNeighbors() > 0) {
					final ArrayList<ValuePair<KDTreeNode<FlagNode<Spot>>, Double>> cur = bsearch.getResults();
					nodeList.add(cur.get(0).getA().get());
				}
			}

			final int burn = (int) Math.round(nFrames * 0.8d);
			if (nodeList.size() > burn) {
				final Iterator<FlagNode<Spot>> ii = nodeList.iterator();
				FlagNode<Spot> oldNode = source;
				oldNode.setVisited(true);
				dd++;
				while (ii.hasNext()) {
					final FlagNode<Spot> loopNode = ii.next();
					loopNode.setVisited(true);

					final Spot begin = oldNode.getValue();
					final Spot fin = loopNode.getValue();
					if (!graph.containsEdge(begin, fin)) {

						final DefaultWeightedEdge edge = graph.addEdge(begin, fin);
						graph.setEdgeWeight(edge, 0d);
					}

					oldNode = loopNode;
				}
			}
		}

		logger.log("Sticking:" + dd + "\n");

		frameTree = treeList.get(0);
		// Main Loop over all frames
		for (int Tree = 1; Tree < nFrames; Tree++) {
			// go to next frame for searching
			final RadiusNeighborFlagSearchOnKDTree rsearch = new RadiusNeighborFlagSearchOnKDTree(treeList.get(Tree));

			// retrieve spots from current frame
			final RealCursor<FlagNode<Spot>> spotIt = frameTree.cursor();
			while (spotIt.hasNext()) {

				final Spot source = spotIt.next().getValue();
				final double[] sourceCoords = new double[3];
				TMUtils.localize(source, sourceCoords);
				rsearch.search(source, initR, source.getFeature(Spot.RADIUS).floatValue(), maxCost,true); // use initial radius for searching spot in the next frame

				if (rsearch.numNeighbors() < 1) continue;

				final FlagNode<Spot> foundNode = rsearch.getSampler(0).get();
				// get the cost - we have to use the getSquareDistance function
				// here for compatibility
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
				int Run = 0;
				ArrayList<DefaultWeightedEdge> edges = new ArrayList<DefaultWeightedEdge>();

				while (succFrame < nFrames - 1) {
					final RadiusNeighborFlagSearchOnKDTree lsearch = new RadiusNeighborFlagSearchOnKDTree(treeList.get(succFrame));
					estim = LTUtils.Add(estim, preVector);
					final double[] estimMean = LTUtils.DivideScalar(estim, count); // calculate means
					final double[] estimCoords = LTUtils.Add(searchCoords, estimMean); // estimate search position										
					final double[] oldCoords = searchCoords.clone();
					final Spot estimSpot = new Spot(
						estimCoords[0], estimCoords[1], estimCoords[2], oldNode.getValue().getFeature(Spot.RADIUS), oldNode.getValue().getFeature(
							Spot.QUALITY));
					// use succeeding radius for searching spot in next frame
					lsearch.search(estimSpot, succR, oldCoords, maxCost, true); 
																			
					if (lsearch.numNeighbors() < 1) {
						if (Run < MAX_GAP) { // automatic gap handling
							// estim = LTUtils.Add(estim, preVector);
							Run++;
							succFrame++;
							count++;
							continue;
						}
						break;
					}
					Run = 0;
					// get first entry in the ordered result list
					final ValuePair<KDTreeNode<FlagNode<Spot>>, Double> cur = lsearch.getResults().get(0);

					final FlagNode<Spot> loopNode = cur.getA().get();
					cost = cur.getB();

					final Spot begin = oldNode.getValue();
					final Spot fin = loopNode.getValue();

					TMUtils.localize(fin, searchCoords);
					preVector = LTUtils.Subtract(searchCoords, oldCoords);

					// check & make the link
					if (!graph.containsEdge(begin, fin)) {
						final DefaultWeightedEdge edge = graph.addEdge(begin, fin);
						graph.setEdgeWeight(edge, cost);
						edges.add(edge);
						oldNode.setVisited(true);
						loopNode.setVisited(true);
					}

					oldNode = loopNode;
					count++;
					succFrame++;
				}
				if (!edges.isEmpty()) edgesMap.put(edgesHash++, edges);
			}
			frameTree = treeList.get(Tree);
			logger.setProgress(Tree / nFrames);
		}
		logger.setProgress(1d);
		logger.setStatus("");

		// second run to connect broken tracks
		int cc = 0;
		for (ArrayList<DefaultWeightedEdge> current : edgesMap.values()) {

			if (current.size() < 1) continue;
			Spot source = graph.getEdgeSource(current.get(current.size() - 1));
			Spot target = graph.getEdgeTarget(current.get(current.size() - 1));

			final double x1 = source.getDoublePosition(0);
			final double y1 = source.getDoublePosition(1);
			final double x2 = target.getDoublePosition(0);
			final double y2 = target.getDoublePosition(1);

			final double angle = Math.atan2(y2 - y1, x2 - x1);

			ArrayList<ValuePair<Spot, Double>> resultPoints = new ArrayList<ValuePair<Spot, Double>>();

			Map<Integer, ArrayList<DefaultWeightedEdge>> reducedSet = new HashMap<Integer, ArrayList<DefaultWeightedEdge>>(edgesMap);
			for (ArrayList<DefaultWeightedEdge> icurrent : reducedSet.values()) {
				Spot isource = graph.getEdgeSource(icurrent.get(0));
				Spot itarget = graph.getEdgeTarget(icurrent.get(0));

				final double ix1 = isource.getDoublePosition(0);
				final double iy1 = isource.getDoublePosition(1);
				final double ix2 = itarget.getDoublePosition(0);
				final double iy2 = itarget.getDoublePosition(1);

				final double iangle = Math.atan2(iy2 - iy1, ix2 - ix1);
				final double zangle = Math.atan2(iy1 - y2, ix1 - x2);
				final double diffa = Math.abs(iangle - angle);
				final double diffb = Math.abs(zangle - angle);
				// final double diffc = Math.abs(zangle - iangle);

				final double linkgap = Math.abs(isource.diffTo(target, Spot.FRAME));

				if (diffa < ANGLE_DIFF && diffb < LOC_DIFF && linkgap < MAX_GAP * 2) {

					final double spotRadiusDiff = 1 + Math.abs(isource.getFeature(Spot.RADIUS).floatValue()
						- target.getFeature(Spot.RADIUS).floatValue()) * 1.5d;
					final double angleSum = (diffa + diffb) * 180 / Math.PI;
					double cost = target.squareDistanceTo(isource) / 4 + spotRadiusDiff + angleSum / 10;
					if (cost < maxCost) resultPoints.add(new ValuePair<Spot, Double>(isource, cost));
				}
			}

			if (!resultPoints.isEmpty()) {
				Collections.sort(resultPoints, new Comparator<ValuePair<Spot, Double>>() {
					@Override
					public int compare(ValuePair<Spot, Double> o1, ValuePair<Spot, Double> o2) {
						return Double.compare(o1.b, o2.b);
					}
				});
				ValuePair<Spot, Double> res = resultPoints.get(0);
				if (!graph.containsEdge(target, res.getA())) {
					final DefaultWeightedEdge newEdge = graph.addEdge(target, res.getA());
					graph.setEdgeWeight(newEdge, res.getB());

					cc++;
				}
			}

		}
		logger.log("2nd run:" + cc + " added edges\n");

		// final long end = System.currentTimeMillis();
		return true;
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
	ok = ok	& checkParameter(settings, KEY_ESTIMATE_RADIUS, Boolean.class, errorHolder);
	final List<String> mandatoryKeys = new ArrayList<String>();
	mandatoryKeys.add(KEY_INITIAL_DISTANCE);
	mandatoryKeys.add(KEY_SUCCEEDING_DISTANCE);
	mandatoryKeys.add(KEY_STICK_RADIUS);
	mandatoryKeys.add(KEY_MAX_COST);
	mandatoryKeys.add(KEY_ESTIMATE_RADIUS);
	ok = ok & checkMapKeys(settings, mandatoryKeys, null, errorHolder);
	return ok;
    }
    

	/**
	 * Ignored.
	 */
	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	/**
	 * Ignored.
	 */
	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	/**
	 * Ignored.
	 */
	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

}