package de.tum.bgu.msm.data.dwelling;

import org.locationtech.jts.geom.Coordinate;

public class DwellingMstm implements Dwelling {

    private final DwellingImpl delegate;

    /**
     * 0: no restriction, negative value: rent-controlled, positive value:
     * rent-controlled and maximum income of renter
     */
    private float restriction;
    private int yearBuilt;

    public DwellingMstm(int id, int zoneId, Coordinate coordinate, int hhId,
                        DwellingType type, int bedrooms,
                        int quality, int price) {
        this.delegate = new DwellingImpl(id, zoneId, coordinate, hhId,
                type, bedrooms, quality, price);
    }

    @Override
    public int getQuality() {
        return delegate.getQuality();
    }

    @Override
    public int getResidentId() {
        return delegate.getResidentId();
    }

    @Override
    public int getPrice() {
        return delegate.getPrice();
    }

    @Override
    public DwellingType getType() {
        return delegate.getType();
    }

    @Override
    public int getBedrooms() {
        return delegate.getBedrooms();
    }

    @Override
    public void setResidentID(int residentID) {
        delegate.setResidentID(residentID);
    }

    @Override
    public void setQuality(int quality) {
        delegate.setQuality(quality);
    }

    @Override
    public void setPrice(int price) {
        delegate.setPrice(price);
    }

    @Override
    public void setFloorSpace(int floorSpace) {
        delegate.setFloorSpace(floorSpace);
    }

    @Override
    public int getFloorSpace() {
        return delegate.getFloorSpace();
    }

    @Override
    public void setCoordinate(Coordinate coordinate) {
        delegate.setCoordinate(coordinate);
    }

    @Override
    public void setUsage(DwellingUsage usage) {
        delegate.setUsage(usage);
    }

    @Override
    public DwellingUsage getUsage() {
        return delegate.getUsage();
    }

    @Override
    public int getId() {
        return delegate.getId();
    }

    @Override
    public int getZoneId() {
        return delegate.getZoneId();
    }

    /**
     * 0: no restriction, negative value: rent-controlled, positive value:
     * rent-controlled and maximum income of renter
     */
    public void setRestriction(float restriction) {
        this.restriction = restriction;
    }

    /**
     * 0: no restriction, negative value: rent-controlled, positive value:
     * rent-controlled and maximum income of renter
     */
    public float getRestriction() {
        return restriction;
    }

    public int getYearBuilt() {
        return yearBuilt;
    }

    public void setYearBuilt(int year) {
        this.yearBuilt = year;
    }
}