package com.borderhopper;

import java.io.Serializable;
import java.util.Objects;

import com.borderhopper.GeographicalUnit.GeographicalType;

public class GeographicalUnitId implements Serializable {
    private String unitName;
    private GeographicalUnit.GeographicalType type;

    public GeographicalUnitId() {
		super();
	}

	public GeographicalUnitId(String unitName, GeographicalType type) {
		super();
		this.unitName = unitName;
		this.type = type;
	}

	public String getUnitName() {
		return unitName;
	}

	public void setUnitName(String unitName) {
		this.unitName = unitName;
	}

	public GeographicalUnit.GeographicalType getType() {
		return type;
	}

	public void setType(GeographicalUnit.GeographicalType type) {
		this.type = type;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeographicalUnitId that = (GeographicalUnitId) o;
        return Objects.equals(unitName, that.unitName) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(unitName, type);
    }
}
