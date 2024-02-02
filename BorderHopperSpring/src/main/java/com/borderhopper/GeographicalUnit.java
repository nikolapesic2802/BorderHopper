package com.borderhopper;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "geographical_unit")
@IdClass(GeographicalUnitId.class)
public class GeographicalUnit {

    public enum GeographicalType {
        Country,
        CountryUnfiltered,
        Opstina,
        Okrug,
        Tablica
    }

    @Id
    @Column(name = "unit_name")
    private String unitName;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private GeographicalType type;

    @Column(name = "geojson", columnDefinition = "TEXT")
    private String geojson;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> connectedUnitNames;

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getGeojson() {
        return geojson;
    }

    public void setGeojson(String geojson) {
        this.geojson = geojson;
    }

    public Set<String> getConnectedUnitNames() {
        return connectedUnitNames;
    }

    public void setConnectedUnitNames(Set<String> connectedUnitNames) {
        this.connectedUnitNames = connectedUnitNames;
    }

    public GeographicalType getType() {
        return type;
    }

    public void setType(GeographicalType type) {
        this.type = type;
    }
}
