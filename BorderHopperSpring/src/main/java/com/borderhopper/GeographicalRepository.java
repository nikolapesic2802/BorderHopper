package com.borderhopper;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.borderhopper.GeographicalUnit.GeographicalType;

@Repository
public interface GeographicalRepository extends JpaRepository<GeographicalUnit, GeographicalUnitId> {
    List<GeographicalUnit> findByType(GeographicalType type);
}
