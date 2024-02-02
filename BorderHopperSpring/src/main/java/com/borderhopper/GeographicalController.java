package com.borderhopper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.borderhopper.GeographicalUnit.GeographicalType;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class GeographicalController {

    @Autowired
    private GeographicalService geographicalService;
    @Autowired
    private DbInitializer dbInitializer;
    

    @PostConstruct
    private void init() {
    	dbInitializer.initDb();
    	geographicalService.init();
    }

    @GetMapping("/randomConnected")
    public ResponseEntity<Pair<String, String>> getRandomConnectedUnits(@RequestParam GeographicalType type) {
        Pair<String, String> units = geographicalService.getRandomConnectedUnits(type);
        if (units == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        return ResponseEntity.ok(units);
    }

    @GetMapping("/nextUnit")
    public ResponseEntity<String> getNextUnit(@RequestParam GeographicalType type, @RequestParam String start, @RequestParam String end, @RequestParam Set<String> unitsGuessed) {
        if (!geographicalService.unitExists(type, start)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unit not found: " + start);
        }if (!geographicalService.unitExists(type, end)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unit not found: " + start);
        }
        for (String unit : unitsGuessed) {
        	if (!geographicalService.unitExists(type, unit)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unit not found: " + unit);
        	}
        }
        return ResponseEntity.of(Optional.ofNullable(geographicalService.getNextUnit(type, start, end, unitsGuessed)));
    }

    @GetMapping("/distanceRemaining")
    public ResponseEntity<?> getDistanceRemaining(@RequestParam GeographicalType type, @RequestParam String start, @RequestParam String end, @RequestParam Set<String> unitsGuessed) {
    	if (!geographicalService.unitExists(type, start)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unit not found: " + start);
        }if (!geographicalService.unitExists(type, end)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unit not found: " + start);
        }
        for (String unit : unitsGuessed) {
        	if (!geographicalService.unitExists(type, unit)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unit not found: " + unit);
        	}
        }
        return ResponseEntity.ok(geographicalService.distanceRemaining(type, start, end, unitsGuessed));
    }

    @GetMapping("/getConnected")
    public ResponseEntity<?> getConnected(@RequestParam GeographicalType type, @RequestParam String start, @RequestParam String end, @RequestParam Set<String> unitsGuessed) {
        if (!geographicalService.unitExists(type, start)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unit not found: " + start);
        }if (!geographicalService.unitExists(type, end)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unit not found: " + start);
        }
        for (String unit : unitsGuessed) {
        	if (!geographicalService.unitExists(type, unit)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unit not found: " + unit);
        	}
        }
        return ResponseEntity.ok(geographicalService.getConnected(type, start, end, unitsGuessed));
    }

    @GetMapping("/geometry/{unitName}")
    public ResponseEntity<String> getUnitGeometry(@RequestParam GeographicalType type, @PathVariable String unitName) {
        return ResponseEntity.of(Optional.ofNullable(geographicalService.getUnitGeometry(type, unitName)));
    }

    @GetMapping("/geometries")
    public ResponseEntity<List<Pair<String, String>>> getAllGeometries(@RequestParam GeographicalType type) {
        return ResponseEntity.ok(geographicalService.getAllGeometries(type));
    }

    @GetMapping("/suggestUnits")
    public ResponseEntity<List<String>> suggestUnits(@RequestParam GeographicalType type, @RequestParam String searchString, @RequestParam Integer topN) {
        return ResponseEntity.ok(geographicalService.similarNames(type, searchString, topN));
    }

    @PutMapping("/initDb")
    public ResponseEntity<?> initDb() {
    	dbInitializer.initDb();
        return ResponseEntity.ok(null);
    }
}
