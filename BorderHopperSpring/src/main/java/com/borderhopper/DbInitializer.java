package com.borderhopper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.borderhopper.GeographicalUnit.GeographicalType;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

@Component
public class DbInitializer {

    @Autowired
    private GeographicalRepository geographicalRepository;

    public void initDb() {
    	try {
            // Define the UTM and WGS84 coordinate reference systems (for Serbian data)
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:32634");
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326", true);

            // Find a conversion between the coordinate reference systems
            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, false);

	    	if (geographicalRepository.findByType(GeographicalType.Country).size() > 0) {
	    		// Table already has data, so skip the initialization
	            System.out.println("Countries already present in the database. Skipping initialization.");
	    	}
	    	else {
		    	System.out.println("Fetching countries to process...");
		        try (InputStream inputStream = getClass().getResourceAsStream("/countries.geojson")) {
		            JSONTokener tokener = new JSONTokener(inputStream);
		            JSONObject object = new JSONObject(tokener);
		            JSONArray features = object.getJSONArray("features");
		
		            List<GeographicalUnit> units = new ArrayList<>();
		            Map<String, Geometry> geometries = new HashMap<>();
		
		            GeoJsonReader reader = new GeoJsonReader();
		            GeoJsonWriter writer = new GeoJsonWriter();
		            double tolerance = 0.02; // Approx 50 meters for simplication and connected detections
					double areaThreshold = 1;
					int pointsThreshold = 100;
		
		            // Parse each country and add to the list
		            for (int i = 0; i < features.length(); i++) {
		                JSONObject feature = features.getJSONObject(i);
		                JSONObject properties = feature.getJSONObject("properties");
		                String admin = properties.getString("ADMIN");
		                String geoJson = feature.getJSONObject("geometry").toString();
		
		                Geometry geometry = reader.read(geoJson);
		                String simplifiedGeoJson;
		                if (geometry.getArea() > areaThreshold && geometry.getNumPoints() > pointsThreshold) { // So that small countries do not disappear
		                    Geometry simplifiedGeometry = DouglasPeuckerSimplifier.simplify(geometry, tolerance);
		                    simplifiedGeoJson = writer.write(simplifiedGeometry);
		                } else {
		                    simplifiedGeoJson = writer.write(geometry);
		                }
		
		                geometries.put(admin, geometry.buffer(tolerance)); // Store the original geometry for graph creation
		
		                GeographicalUnit unit = new GeographicalUnit();
		                unit.setUnitName(admin);
		                unit.setGeojson(simplifiedGeoJson); // Use simplified GeoJSON
		                unit.setConnectedUnitNames(new HashSet<>());
		                unit.setType(GeographicalType.Country);
		                units.add(unit);
		            }
		
		            // Save all countries to the database
		            geographicalRepository.saveAll(units);
		
		            // Pairs of countries that technically share a border, but make no sense to be connected (there are probably others that I didn't filter).
		            Set<String> forbiddenPairs = new HashSet<>();
		            forbiddenPairs.add("France-Brazil");
		            forbiddenPairs.add("Brazil-France");
		            forbiddenPairs.add("France-Suriname");
		            forbiddenPairs.add("Suriname-France");
		            forbiddenPairs.add("Spain-Morocco");
		            forbiddenPairs.add("Morocco-Spain");
		            forbiddenPairs.add("Russia-Poland");
		            forbiddenPairs.add("Poland-Russia");
		            forbiddenPairs.add("Russia-Lithuania");
		            forbiddenPairs.add("Lithuania-Russia");
		            forbiddenPairs.add("Azerbaijan-Turkey");
		            forbiddenPairs.add("Turkey-Azerbaijan");
		            // Find and save connections
		            int countProcessed = 0;
		            for (GeographicalUnit unit : units) {
		                Geometry thisGeometry = geometries.get(unit.getUnitName());
		
		                for (GeographicalUnit otherUnit : units) {
		                    if (!unit.getUnitName().equals(otherUnit.getUnitName())) {
		                        String pair = unit.getUnitName() + "-" + otherUnit.getUnitName();
		                        if (!forbiddenPairs.contains(pair)) {
			                        Geometry otherGeometry = geometries.get(otherUnit.getUnitName());
			                        if (thisGeometry.getEnvelopeInternal().intersects(otherGeometry.getEnvelopeInternal()) &&
			                        	thisGeometry.intersects(otherGeometry)) {
			                            unit.getConnectedUnitNames().add(otherUnit.getUnitName());
			                        }
		                        }
		                    }
		                }
		                geographicalRepository.save(unit);
		                countProcessed++;
		                System.out.println("Processed " + countProcessed + " out of " + units.size() + " countries");
		            }
		            System.out.println("Finished processing " + countProcessed + " countries into the DB");
		        } catch (Exception e) {
		            e.printStackTrace();
		        }
	    	}
	    	
	    	if (geographicalRepository.findByType(GeographicalType.CountryUnfiltered).size() > 0) {
	    		// Table already has data, so skip the initialization
	            System.out.println("Countries unfiltered  already present in the database. Skipping initialization.");
	    	}
	    	else {
		    	System.out.println("Fetching countries unfiltered to process...");
		        try (InputStream inputStream = getClass().getResourceAsStream("/countries.geojson")) {
		            JSONTokener tokener = new JSONTokener(inputStream);
		            JSONObject object = new JSONObject(tokener);
		            JSONArray features = object.getJSONArray("features");
		
		            List<GeographicalUnit> units = new ArrayList<>();
		            Map<String, Geometry> geometries = new HashMap<>();
		
		            GeoJsonReader reader = new GeoJsonReader();
		            GeoJsonWriter writer = new GeoJsonWriter();
		            double tolerance = 0.02; // Approx 2km for simplication and connected detections
					double areaThreshold = 1;
					int pointsThreshold = 100;
		
		            // Parse each country and add to the list
		            for (int i = 0; i < features.length(); i++) {
		                JSONObject feature = features.getJSONObject(i);
		                JSONObject properties = feature.getJSONObject("properties");
		                String admin = properties.getString("ADMIN");
		                String geoJson = feature.getJSONObject("geometry").toString();

		                Geometry geometry = reader.read(geoJson);
		                String simplifiedGeoJson;
		                if (geometry.getArea() > areaThreshold && geometry.getNumPoints() > pointsThreshold) { // So that small countries do not disappear
		                    Geometry simplifiedGeometry = TopologyPreservingSimplifier.simplify(geometry, tolerance);
		                    simplifiedGeoJson = writer.write(simplifiedGeometry);
		                } else {
		                    simplifiedGeoJson = writer.write(geometry);
		                }

		                geometries.put(admin, geometry.buffer(tolerance)); // Store the original geometry for graph creation
		
		                GeographicalUnit unit = new GeographicalUnit();
		                unit.setUnitName(admin);
		                unit.setGeojson(simplifiedGeoJson); // Use simplified GeoJSON
		                unit.setConnectedUnitNames(new HashSet<>());
		                unit.setType(GeographicalType.CountryUnfiltered);
		                units.add(unit);
		            }
		
		            // Save all countries to the database
		            geographicalRepository.saveAll(units);
		            // Find and save connections
		            int countProcessed = 0;
		            for (GeographicalUnit unit : units) {
		                Geometry thisGeometry = geometries.get(unit.getUnitName());
		
		                for (GeographicalUnit otherUnit : units) {
		                    if (!unit.getUnitName().equals(otherUnit.getUnitName())) {
		                        Geometry otherGeometry = geometries.get(otherUnit.getUnitName());
		                        if (thisGeometry.getEnvelopeInternal().intersects(otherGeometry.getEnvelopeInternal()) &&
		                        	thisGeometry.intersects(otherGeometry)) {
		                            unit.getConnectedUnitNames().add(otherUnit.getUnitName());
		                        }
		                    }
		                }
		                geographicalRepository.save(unit);
		                countProcessed++;
		                System.out.println("Processed " + countProcessed + " out of " + units.size() + " countries unfiltered");
		            }
		            System.out.println("Finished processing " + countProcessed + " countries unfiltered into the DB");
		        } catch (Exception e) {
		            e.printStackTrace();
		        }
	    	}
	    	
	    	if (geographicalRepository.findByType(GeographicalType.Okrug).size() > 0) {
	    		// Table already has data, so skip the initialization
	            System.out.println("Serbian 'okruzi' already present in the database. Skipping initialization.");
	    	}
	    	else {
	    	    System.out.println("Fetching Serbian 'okruzi' to process...");
	    	    try (InputStream inputStream = getClass().getResourceAsStream("/okruzi.geojson")) {
	    	        JSONTokener tokener = new JSONTokener(inputStream);
	    	        JSONObject object = new JSONObject(tokener);
	    	        JSONArray features = object.getJSONArray("features");
	
	    	        List<GeographicalUnit> units = new ArrayList<>();
	    	        Map<String, Geometry> geometries = new HashMap<>();
	
	    	        GeoJsonReader reader = new GeoJsonReader();
	    	        GeoJsonWriter writer = new GeoJsonWriter();
	    	        double tolerance = 0.0005;  // Approx 50 meters for simplication and connected detections
	
	    	        // Parse each 'okrug' and add to the list
	    	        for (int i = 0; i < features.length(); i++) {
	    	            JSONObject feature = features.getJSONObject(i);
	    	            JSONObject properties = feature.getJSONObject("properties");
	    	            String okrugName = properties.getString("okrug_imel");
	    	            String processedOkrugName = processOkrugName(okrugName);
	    	            System.out.println(okrugName + " -> " + processedOkrugName);
	    	            String geoJson = feature.getJSONObject("geometry").toString();
	
	    	            Geometry utmGeometry = reader.read(geoJson); // Parse the UTM geometry
	    	            Geometry wgs84Geometry = JTS.transform(utmGeometry, transform); // Convert UTM to WGS84
	    	            Geometry simplifiedGeometry = TopologyPreservingSimplifier.simplify(wgs84Geometry, tolerance); // Simplify the geometry
		                String simplifiedGeoJson = writer.write(simplifiedGeometry); // Convert simplified geometry back to GeoJSON
	
	    	            geometries.put(processedOkrugName, wgs84Geometry.buffer(tolerance)); // Store the original geometry for graph creation
	
	    	            GeographicalUnit unit = new GeographicalUnit();
	    	            unit.setUnitName(processedOkrugName);
	    	            unit.setGeojson(simplifiedGeoJson); // Use WGS84 GeoJSON
	    	            unit.setConnectedUnitNames(new HashSet<>());
	    	            unit.setType(GeographicalType.Okrug);
	    	            units.add(unit);
	    	        }
	
	    	        // Save all 'okruzi' to the database
	    	        geographicalRepository.saveAll(units);
	
	    	        int countProcessed = 0;
		            for (GeographicalUnit unit : units) {
		                Geometry thisGeometry = geometries.get(unit.getUnitName());
		
		                for (GeographicalUnit otherUnit : units) {
		                    if (!unit.getUnitName().equals(otherUnit.getUnitName())) {
		                        Geometry otherGeometry = geometries.get(otherUnit.getUnitName());
		                        if (thisGeometry.getEnvelopeInternal().intersects(otherGeometry.getEnvelopeInternal()) &&
		                        	thisGeometry.intersects(otherGeometry)) {
		                            unit.getConnectedUnitNames().add(otherUnit.getUnitName());
		                        }
		                    }
		                }
		                geographicalRepository.save(unit);
		                countProcessed++;
		                System.out.println("Processed " + countProcessed + " out of " + units.size() + " 'okruga'");
		            }
	    	        System.out.println("Finished processing Serbian 'okruzi' into the DB");
	    	    } catch (Exception e) {
	    	        e.printStackTrace();
	    	    }
	    	}
	    	
	    	if (geographicalRepository.findByType(GeographicalType.Opstina).size() > 0) {
	    		// Table already has data, so skip the initialization
	            System.out.println("Serbian 'opstine' already present in the database. Skipping initialization.");
	    	}
	    	else {
	    	    System.out.println("Fetching Serbian 'opstine' to process...");
	    	    try (InputStream inputStream = getClass().getResourceAsStream("/opstine.geojson")) {
	    	        JSONTokener tokener = new JSONTokener(inputStream);
	    	        JSONObject object = new JSONObject(tokener);
	    	        JSONArray features = object.getJSONArray("features");
	
	    	        List<GeographicalUnit> units = new ArrayList<>();
	    	        Map<String, Geometry> geometries = new HashMap<>();
	
	    	        GeoJsonReader reader = new GeoJsonReader();
	    	        GeoJsonWriter writer = new GeoJsonWriter();
	    	        double tolerance = 0.0005; // Approx 50 meters for simplication and connected detections
	
	    	        // Parse each 'opstinu' and add to the list
	    	        for (int i = 0; i < features.length(); i++) {
	    	            JSONObject feature = features.getJSONObject(i);
	    	            JSONObject properties = feature.getJSONObject("properties");
	    	            String opstinaName = properties.getString("opstina_imel");
	    	            String processedOpstinaName = processOpstinaName(opstinaName);
	    	            System.out.println(opstinaName + " -> " + processedOpstinaName);
	    	            String geoJson = feature.getJSONObject("geometry").toString();
	
	    	            Geometry utmGeometry = reader.read(geoJson); // Parse the UTM geometry
	    	            Geometry wgs84Geometry = JTS.transform(utmGeometry, transform); // Convert UTM to WGS84
	    	            Geometry simplifiedGeometry = TopologyPreservingSimplifier.simplify(wgs84Geometry, tolerance); // Simplify the geometry
		                String simplifiedGeoJson = writer.write(simplifiedGeometry); // Convert simplified geometry back to GeoJSON
	
	    	            geometries.put(processedOpstinaName, wgs84Geometry.buffer(tolerance)); // Store the original geometry for graph creation
	
	    	            GeographicalUnit unit = new GeographicalUnit();
	    	            unit.setUnitName(processedOpstinaName);
	    	            unit.setGeojson(simplifiedGeoJson);
	    	            unit.setConnectedUnitNames(new HashSet<>());
	    	            unit.setType(GeographicalType.Opstina);
	    	            units.add(unit);
	    	        }
	
	    	        // Save all 'opstine' to the database
	    	        geographicalRepository.saveAll(units);
	
	    	        int countProcessed = 0;
		            for (GeographicalUnit unit : units) {
		                Geometry thisGeometry = geometries.get(unit.getUnitName());
		
		                for (GeographicalUnit otherUnit : units) {
		                    if (!unit.getUnitName().equals(otherUnit.getUnitName())) {
		                        Geometry otherGeometry = geometries.get(otherUnit.getUnitName());
		                        if (thisGeometry.getEnvelopeInternal().intersects(otherGeometry.getEnvelopeInternal()) &&
		                        	thisGeometry.intersects(otherGeometry)) {
		                            unit.getConnectedUnitNames().add(otherUnit.getUnitName());
		                        }
		                    }
		                }
		                geographicalRepository.save(unit);
		                countProcessed++;
		                System.out.println("Processed " + countProcessed + " out of " + units.size() + " 'opstina'");
		            }
	    	        System.out.println("Finished processing Serbian 'opstine' into the DB");
	    	    } catch (Exception e) {
	    	        e.printStackTrace();
	    	    }
	    	}
	    	
	    	if (geographicalRepository.findByType(GeographicalType.Tablica).size() > 0) {
	    		// Table already has data, so skip the initialization
	            System.out.println("Serbian 'tablice' already present in the database. Skipping initialization.");
	    	}
	    	else {
	    	    System.out.println("Fetching Serbian 'tablice' to process...");
	    	    try (InputStream inputStream = getClass().getResourceAsStream("/tablice.tsv");
    	    		 InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
    	             BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
    	    		 InputStream inputStreamOpstine = getClass().getResourceAsStream("/opstine.geojson")) {
	    	    	JSONTokener tokener = new JSONTokener(inputStreamOpstine);
	    	        JSONObject object = new JSONObject(tokener);
	    	        JSONArray features = object.getJSONArray("features");
	
		    	    Map<String, Geometry> opstineMap = new HashMap<>();

	    	        GeoJsonReader reader = new GeoJsonReader();
	    	        GeoJsonWriter writer = new GeoJsonWriter();
	    	        double tolerance = 0.0005; // Approx 50 meters for simplication and connected detections
	    	        // Parse each 'opstinu' and add to the map
	    	        for (int i = 0; i < features.length(); i++) {
	    	            JSONObject feature = features.getJSONObject(i);
	    	            JSONObject properties = feature.getJSONObject("properties");
	    	            String opstinaName = properties.getString("opstina_imel");
	    	            String processedOpstinaName = processOpstinaName(opstinaName);
	    	            String geoJson = feature.getJSONObject("geometry").toString();
	
	    	            Geometry utmGeometry = reader.read(geoJson); // Parse the UTM geometry
	    	            Geometry wgs84Geometry = JTS.transform(utmGeometry, transform); // Convert UTM to WGS84
	
		                opstineMap.put(processedOpstinaName, wgs84Geometry);
	    	        }
		    	    
	    	        List<GeographicalUnit> units = new ArrayList<>();
	    	        Map<String, Geometry> geometries = new HashMap<>();
	    	        String line;
	                while ((line = bufferedReader.readLine()) != null) {
	                    String[] a = line.split("\t");
	                    String ime = a[0] + " (" + a[1] + ")";
	                    String[] opstineObuhvacene = a[2].split(", ");
	                    Geometry geometrija = null;
	                    for (String opstina : opstineObuhvacene) {
	                    	if (!opstineMap.containsKey(opstina)) {
	                    		System.out.println("Error, opstina not contained!! " + opstina);
	                    		return;
	                    	}
	                    	Geometry geo = opstineMap.get(opstina);
	                    	opstineMap.remove(opstina);
	                    	if (geometrija == null) {
	                    		geometrija = geo;
	                    	}
	                    	else {
	                    		geometrija = geometrija.union(geo);
	                    	}
	                    }
	                    geometrija = geometrija.buffer(2.5 * tolerance).buffer(-2.5 * tolerance); // Remove inner points and areas from incorrect union
	                    Geometry simplifiedGeometry = TopologyPreservingSimplifier.simplify(geometrija, tolerance); // Simplify the geometry
		                String simplifiedGeoJson = writer.write(simplifiedGeometry); // Convert simplified geometry back to GeoJSON
	
	    	            geometries.put(ime, geometrija.buffer(tolerance)); // Store the original geometry for graph creation
	                    GeographicalUnit unit = new GeographicalUnit();
	    	            unit.setUnitName(ime);
	    	            unit.setGeojson(simplifiedGeoJson);
	    	            unit.setConnectedUnitNames(new HashSet<>());
	    	            unit.setType(GeographicalType.Tablica);
	    	            units.add(unit);
	                }
	                
	                for (String key : opstineMap.keySet()) {
	                	System.out.println(key + " opstina nije ni u jednom regionu!!");
	                }
	
	    	        // Save all 'tablice' to the database
	    	        geographicalRepository.saveAll(units);
	
	    	        int countProcessed = 0;
		            for (GeographicalUnit unit : units) {
		                Geometry thisGeometry = geometries.get(unit.getUnitName());
		
		                for (GeographicalUnit otherUnit : units) {
		                    if (!unit.getUnitName().equals(otherUnit.getUnitName())) {
		                        Geometry otherGeometry = geometries.get(otherUnit.getUnitName());
		                        if (thisGeometry.getEnvelopeInternal().intersects(otherGeometry.getEnvelopeInternal()) &&
		                        	thisGeometry.intersects(otherGeometry)) {
		                            unit.getConnectedUnitNames().add(otherUnit.getUnitName());
		                        }
		                    }
		                }
		                geographicalRepository.save(unit);
		                countProcessed++;
		                System.out.println("Processed " + countProcessed + " out of " + units.size() + " 'tablica'");
		            }
	    	        System.out.println("Finished processing Serbian 'tablice' into the DB");
	    	    } catch (Exception e) {
	    	        e.printStackTrace();
	    	    }
	    	}
    	} catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String processOkrugName(String name) {
        name = name.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        // Remove the 'UPRAVNI OKRUG', 'UPR. OKRUG', and 'OKRUG'
        name = name.replaceAll("(?i)UPRAVNI\\sOKRUG", "");
        name = name.replaceAll("(?i)UPR\\.\\sOKRUG", "");
        name = name.replaceAll("(?i)OKRUG", "");

        // Remove any additional whitespace
        name = name.trim();

        // Capitalize the first letter of each word
        StringBuilder capitalized = new StringBuilder();
        String[] words = name.split("\\s+");
        for (String word : words) {
            if (word.length() > 0) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase())
                            .append(" ");
            }
        }
        return capitalized.toString().trim();
    }
    public static String processOpstinaName(String name) {
        // Remove any additional whitespace
        name = name.trim();

        // Capitalize the first letter of each word
        StringBuilder capitalized = new StringBuilder();
        String[] words = name.split("\\s+");
        for (String word : words) {
            if (word.length() > 0) {
            	if (word.charAt(0) == '(') {
                    capitalized.append('(').append(Character.toUpperCase(word.charAt(1)))
                                .append(word.substring(2).toLowerCase())
                                .append(" ");
            	}
            	else {
	                capitalized.append(Character.toUpperCase(word.charAt(0)))
	                            .append(word.substring(1).toLowerCase())
	                            .append(" ");
            	}
            }
        }
        return capitalized.toString().trim();
    }
}
