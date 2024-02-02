package com.borderhopper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.borderhopper.GeographicalUnit.GeographicalType;

import java.text.Normalizer;
import java.util.*;

@Service
public class GeographicalService {

	@Autowired
    private GeographicalRepository geographicalRepository;
	
	private class GraphManager {
		public List<String> unitList = new ArrayList<>();
		public Set<String> unitSet;
		public Map<String, Set<String>> graph = new HashMap<>();
		public Map<String, Integer> component = new HashMap<>();
	
	    public void init(GeographicalType type) {
	        // Initialize the graph
	        List<GeographicalUnit> units = geographicalRepository.findByType(type);
	        for (GeographicalUnit unit : units) {
	            graph.put(unit.getUnitName(), new HashSet<>(unit.getConnectedUnitNames()));
	            unitList.add(unit.getUnitName());
	        }
	        unitSet = new HashSet<>(unitList);
	        int componentId = 0;
	        for (GeographicalUnit unit : units) {
	        	if (component.containsKey(unit.getUnitName())) {
	        		continue;
	        	}
	        	Queue<String> queue = new LinkedList<String>();
	        	queue.add(unit.getUnitName());
	        	component.put(unit.getUnitName(), componentId);
	        	while (!queue.isEmpty()) {
	        		String front = queue.poll();
	        		for (String neighbour : graph.get(front)) {
	        			if (component.containsKey(neighbour)) {
	        				continue;
	        			}
	        			component.put(neighbour, componentId);
	        			queue.add(neighbour);
	        		}
	        	}
	        	componentId++;
	        }
	    }
	}
	private Map<GeographicalType, GraphManager> graphs;
	public void init() {
		graphs = new HashMap<>();
		for (GeographicalType type : GeographicalType.values()) {
			graphs.put(type, new GraphManager());
			graphs.get(type).init(type);
		}
	}

    public Pair<String, String> getRandomConnectedUnits(GeographicalType type) {
        Random random = new Random();
    	GraphManager graph = graphs.get(type);
        
        while (true) {
            String startUnit = graph.unitList.get(random.nextInt(graph.unitList.size()));
            String targetUnit = graph.unitList.get(random.nextInt(graph.unitList.size()));

            if (!startUnit.equals(targetUnit) && graph.component.get(startUnit).equals(graph.component.get(targetUnit)) && !graph.graph.get(startUnit).contains(targetUnit)) {
                return Pair.of(startUnit, targetUnit);
            }
        }
    }
    
    public String getNextUnit(GeographicalType type, String start, String end, Set<String> unitsGuessed) {
    	GraphManager graph = graphs.get(type);
    	Map<String, String> parent = new HashMap<>();
    	Deque<String> queue = new LinkedList<String>();
    	queue.add(end);
    	parent.put(end, end);
    	boolean foundStart = false;
    	while (!queue.isEmpty() && !foundStart) {
    		String front = queue.poll();
    		for (String neighbour : graph.graph.get(front)) {
    			if (parent.containsKey(neighbour)) {
    				continue;
    			}
    			parent.put(neighbour, front);
    			if (unitsGuessed.contains(neighbour)) {
    				queue.addFirst(neighbour);
    			}
    			else {
    				queue.addLast(neighbour);
    			}
    			queue.add(neighbour);
    			if (neighbour.equals(start)) {
    				foundStart = true;
    				break;
    			}
    		}
    	}
    	if (!foundStart) {
    		return null;
    	}
    	String current = parent.get(start);
    	while (current != end) {
    		if (!unitsGuessed.contains(current)) {
    			return current;
    		}
    		current = parent.get(current);
    	}
    	return null;
    }
    
    public Integer distanceRemaining(GeographicalType type, String start, String end, Set<String> unitsGuessed) {
    	GraphManager graph = graphs.get(type);
    	Map<String, Integer> distance = new HashMap<>();
    	Deque<String> queue = new LinkedList<String>();
    	queue.add(end);
    	distance.put(end, 0);
    	boolean foundStart = false;
    	while (!queue.isEmpty() && !foundStart) {
    		String front = queue.poll();
    		int frontDist = distance.get(front);
    		for (String neighbour : graph.graph.get(front)) {
    			if (distance.containsKey(neighbour)) {
    				continue;
    			}
    			if (unitsGuessed.contains(neighbour)) {
        			distance.put(neighbour, frontDist);
    				queue.addFirst(neighbour);
    			}
    			else {
        			distance.put(neighbour, frontDist + 1);
    				queue.addLast(neighbour);
    			}
    			queue.add(neighbour);
    			if (neighbour.equals(start)) {
    				foundStart = true;
    				break;
    			}
    		}
    	}
    	if (!foundStart) {
    		return -1;
    	}
    	return distance.get(start) - 1; // Don't count start into distance
    }
    
    public Set<String> getConnected(GeographicalType type, String start, String end, Set<String> unitsGuessed) {
    	GraphManager graph = graphs.get(type);
    	Set<String> connected = new HashSet<>();
    	Queue<String> queue = new LinkedList<String>();
    	queue.add(end);
    	queue.add(start);
    	connected.add(start);
    	connected.add(end);
    	while (!queue.isEmpty()) {
    		String front = queue.poll();
    		for (String neighbour : graph.graph.get(front)) {
    			if (connected.contains(neighbour)) {
    				continue;
    			}
    			if (unitsGuessed.contains(neighbour)) {
    				connected.add(neighbour);
    				queue.add(neighbour);
    			}
    		}
    	}
    	connected.remove(start);
    	connected.remove(end);
    	return connected;
    }
    
    public Boolean unitExists(GeographicalType type, String unit) {
    	GraphManager graph = graphs.get(type);
    	return graph.unitSet.contains(unit);
    }
    
    public String getUnitGeometry(GeographicalType type, String unitName) {
        return geographicalRepository.findById(new GeographicalUnitId(unitName, type))
                .map(GeographicalUnit::getGeojson)
                .orElse(null);
    }

    public List<Pair<String,String>> getAllGeometries(GeographicalType type) {
        return geographicalRepository.findByType(type).stream()
                .map(u -> Pair.of(u.getUnitName(), u.getGeojson())).toList();
    }

    public List<String> similarNames(GeographicalType type, String searchString, int topN) {
    	GraphManager graph = graphs.get(type);
        List<Pair<String, Integer>> results = new ArrayList<>();
        searchString = searchString.toLowerCase();
        searchString = Normalizer.normalize(searchString, Normalizer.Form.NFD);

        for (String unit : graph.unitList) {
            // Calculate the distance between the prefix and the item
        	String unitName = unit;
        	if (type == GeographicalType.CountryUnfiltered) {
        		unitName = unitName.replace("Unfiltered", "");
        	}
        	String normalizedUnitName = Normalizer.normalize(unitName.toLowerCase(), Normalizer.Form.NFD);
        	Integer distance = calculateDistance(searchString, normalizedUnitName);
        	if (distance != -1) {
        		results.add(Pair.of(unitName, distance));
        	}
        }

        // Sort the results by distance (ascending)
        results.sort(Comparator.comparing(Pair::getSecond));

        // Take the top N results (if available)
        if (results.size() > topN) {
            results = results.subList(0, topN);
        }

        return results.stream().map(x -> x.getFirst()).toList();
    }

    private static int calculateDistance(String search, String unitName) {
    	int penaltyFront = 100;
    	int penaltyGap = 1000;
    	int penaltySkipLetter = 10000;
    	int distanceCutoff = 20100; // Score for which we will not consider this a candidate
        int[][] dp = new int[search.length() + 1][unitName.length() + 1];

        for (int j = 0; j <= unitName.length(); j++) {
            dp[search.length()][j] = unitName.length() - j; // Passed all unitName letters
        }

        for (int i = 0; i < search.length(); i++) {
            dp[i][unitName.length()] = (search.length() - i) * penaltySkipLetter;
        }

        for (int i = search.length() - 1; i >= 0; i--) {
            for (int j = unitName.length() - 1; j >= 0; j--) {
            	dp[i][j] = distanceCutoff;
                if (search.charAt(i) == unitName.charAt(j)) { // Match!
                	dp[i][j] = Math.min(dp[i][j], dp[i + 1][j + 1]);
                }
                dp[i][j] = Math.min(dp[i][j], dp[i + 1][j] + penaltySkipLetter);
                int penaltyMoveJ = i == 0 ? penaltyFront : penaltyGap;
                dp[i][j] = Math.min(dp[i][j], dp[i][j + 1] + penaltyMoveJ);
            }
        }
        if (dp[0][0] >= distanceCutoff) {
        	return -1;
        }
        return dp[0][0];
    }
}
