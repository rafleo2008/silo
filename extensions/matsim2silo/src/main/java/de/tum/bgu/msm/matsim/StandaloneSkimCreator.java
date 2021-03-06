package de.tum.bgu.msm.matsim;


import de.tum.bgu.msm.data.Id;
import de.tum.bgu.msm.data.travelTimes.SkimTravelTimes;
import de.tum.bgu.msm.io.output.OmxMatrixWriter;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StandaloneSkimCreator {

    public static void main(String[] args) {
        createSkims(Integer.parseInt(args[0]), args[1], args[2], args[3], args[4]);
    }

    public static void createSkims(int threads, String shapePath, String networkPath,
                                   String schedulePath, String outputPath) {

        Config config = ConfigUtils.createConfig();

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkPath);

        Scenario scenario = ScenarioUtils.createScenario(config);
        new TransitScheduleReader(scenario).readFile(schedulePath);
        TransitSchedule schedule = scenario.getTransitSchedule();

        final Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapePath);
        final Map<Id, List<SimpleFeature>> zonesById = features.stream()
                .collect(Collectors.groupingBy(f -> () -> Integer.parseInt(String.valueOf(f.getAttribute("id")))));

        ZoneConnectorManager zoneConnectorManager = zoneId -> zonesById.get(zoneId).stream()
                .map( f -> CoordUtils.createCoord(((Geometry) f.getDefaultGeometry()).getCentroid().getCoordinate())).collect(Collectors.toList());
        MatsimData data = new MatsimData(config, threads, network, schedule, zoneConnectorManager);
        FreespeedTravelTimeAndDisutility freespeed = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
        data.update(freespeed, freespeed);
        final MatsimSkimCreator matsimSkimCreator = new MatsimSkimCreator(data);

        final IndexedDoubleMatrix2D carSkim = matsimSkimCreator.createCarSkim(zonesById.keySet());
        final IndexedDoubleMatrix2D ptSkim = matsimSkimCreator.createPtSkim(zonesById.keySet());

        final SkimTravelTimes skimTravelTimes = new SkimTravelTimes();
        skimTravelTimes.updateSkimMatrix(carSkim, TransportMode.car);
        skimTravelTimes.updateSkimMatrix(ptSkim, TransportMode.pt);

        int dimension = zonesById.size();
        OmxMatrixWriter.createOmxFile(outputPath, dimension);
        skimTravelTimes.printOutCarSkim(TransportMode.car, outputPath, "carTravelTimes");
        skimTravelTimes.printOutCarSkim(TransportMode.pt, outputPath, "ptTravelTimes");
    }
}
