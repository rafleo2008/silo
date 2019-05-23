package de.tum.bgu.msm.data;


import de.tum.bgu.msm.data.geo.GeoData;
import de.tum.bgu.msm.data.travelTimes.SkimTravelTimes;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import de.tum.bgu.msm.models.ModelUpdateListener;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import de.tum.bgu.msm.utils.TravelTimeUtil;

public class TravelTimesWrapper implements TravelTimes, ModelUpdateListener {

    @Deprecated
    public TravelTimes getDelegate() {
        return delegate;
    }

    private final TravelTimes delegate;
    private final Properties properties;
    private final GeoData geoData;

    public TravelTimesWrapper(TravelTimes travelTimes, Properties properties, GeoData geoData){
        delegate = travelTimes;
        this.properties = properties;
        this.geoData = geoData;
    }


    @Override
    public double getTravelTime(Location origin, Location destination, double timeOfDay_s, String mode) {
        return delegate.getTravelTime(origin, destination, timeOfDay_s, mode);
    }

    @Override
    public double getTravelTimeToRegion(Location origin, Region destinationRegion, double timeOfDay_s, String mode) {
        return delegate.getTravelTimeToRegion(origin, destinationRegion, timeOfDay_s, mode);
    }

    @Override
    public IndexedDoubleMatrix2D getPeakSkim(String mode) {
        return delegate.getPeakSkim(mode);
    }

    @Override
    public TravelTimes duplicate() {
        throw new RuntimeException("Not implemented for wrapper");
    }

    @Override
    public void setup() {
        if (delegate instanceof SkimTravelTimes){
            updateSkims(properties.main.startYear);
        }
    }

    @Override
    public void prepareYear(int year) {

    }

    @Override
    public void endYear(int year) {
        if (properties.accessibility.skimYears.contains(year) && year != properties.main.startYear) {
            updateSkims(year);
        }
    }

    @Override
    public void endSimulation() {

    }

    private void updateSkims(int year) {
        TravelTimeUtil.updateCarSkim((SkimTravelTimes) delegate, year, properties);
        TravelTimeUtil.updateTransitSkim((SkimTravelTimes) delegate, year, properties);
        ((SkimTravelTimes) delegate).updateZoneToRegionTravelTimes(geoData.getZones().values(), geoData.getRegions().values());
    }
}
