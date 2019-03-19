package de.tum.bgu.msm.run;

import de.tum.bgu.msm.io.GeoDataReaderMstm;
import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.container.DefaultDataContainer;
import de.tum.bgu.msm.data.household.HouseholdFactoryMstm;
import de.tum.bgu.msm.data.person.PersonfactoryMstm;
import de.tum.bgu.msm.data.accessibility.Accessibility;
import de.tum.bgu.msm.data.dwelling.*;
import de.tum.bgu.msm.data.geo.GeoDataMstm;
import de.tum.bgu.msm.data.household.HouseholdData;
import de.tum.bgu.msm.data.household.HouseholdDataImpl;
import de.tum.bgu.msm.data.household.HouseholdDataManager;
import de.tum.bgu.msm.data.household.HouseholdDataManagerImpl;
import de.tum.bgu.msm.data.job.*;
import de.tum.bgu.msm.data.travelTimes.SkimTravelTimes;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import de.tum.bgu.msm.io.PersonReaderMstm;
import de.tum.bgu.msm.io.input.*;
import de.tum.bgu.msm.models.transportModel.matsim.MatsimTravelTimes;
import de.tum.bgu.msm.properties.Properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static de.tum.bgu.msm.properties.modules.TransportModelPropertiesModule.TransportModelIdentifier.MATSIM;

public final class DataBuilder {

    private DataBuilder() {
    }

    public static DataContainer buildDataContainer(Properties properties) {

        GeoDataMstm geoData = new GeoDataMstm(properties);

        List<DwellingType> dwellingTypeList = new ArrayList<>();
        Collections.addAll(dwellingTypeList, DefaultDwellingTypeImpl.values());

        DwellingData dwellingData = new DwellingDataImpl();
        HouseholdData householdData = new HouseholdDataImpl();
        JobData jobData = new JobDataImpl();

        TravelTimes travelTimes;
        if (properties.transportModel.transportModelIdentifier == MATSIM) {
            travelTimes = new MatsimTravelTimes();
        } else {
            travelTimes = new SkimTravelTimes();
        }

        Accessibility accessibility = new Accessibility(geoData, travelTimes, properties, dwellingData, householdData);

        //TODO: revise this!
        new JobType(properties.jobData.jobTypes);

        RealEstateDataManager realEstateManager = new RealEstateDataManagerImpl(
                dwellingTypeList, dwellingData,
                householdData, geoData,
                new DwellingFactoryImpl(),
                properties);

        JobDataManager jobManager = new JobDataManagerImpl(
                properties, new JobFactoryImpl(),
                jobData, geoData,
                travelTimes, accessibility);

        final HouseholdFactoryMstm hhFactory = new HouseholdFactoryMstm();
        final PersonfactoryMstm ppFactory = new PersonfactoryMstm();

        HouseholdDataManager householdManager = new HouseholdDataManagerImpl(
                householdData, dwellingData, geoData,
                ppFactory, hhFactory,
                properties, realEstateManager);

        DataContainer dataContainer = new DefaultDataContainer(
                geoData, realEstateManager,
                jobManager, householdManager,
                travelTimes, accessibility, properties);
        return dataContainer;
    }

    public static void readInput(Properties properties, DataContainer dataContainer) {
        final GeoDataReaderMstm geoDataReaderMstm = new GeoDataReaderMstm((GeoDataMstm) dataContainer.getGeoData());

        String fileName = properties.main.baseDirectory + properties.geo.zonalDataFile;
        String pathShp = properties.main.baseDirectory + properties.geo.zoneShapeFile;
        geoDataReaderMstm.readZoneCsv(fileName);
        geoDataReaderMstm.readZoneShapefile(pathShp);
        geoDataReaderMstm.readCrimeData(Properties.get().main.baseDirectory + Properties.get().geo.countyCrimeFile);

        int year = properties.main.startYear;

        readHouseholds(properties, dataContainer.getHouseholdDataManager(),
                (HouseholdFactoryMstm) dataContainer.getHouseholdDataManager().getHouseholdFactory(), year);
        readPersons(properties, dataContainer.getHouseholdDataManager(), (PersonfactoryMstm) dataContainer.getHouseholdDataManager().getPersonFactory(), year);
        readDwellings(properties, dataContainer.getRealEstateDataManager(), year);

        JobReader jjReader = new DefaultJobReader(dataContainer.getJobDataManager());
        String jobsFile = properties.main.baseDirectory + properties.jobData.jobsFileName + "_" + year + ".csv";
        jjReader.readData(jobsFile);
    }

    private static void readDwellings(Properties properties, RealEstateDataManager realEstateManager, int year) {
        DwellingReader ddReader = new DefaultDwellingReader(realEstateManager);
        String dwellingsFile = properties.main.baseDirectory + properties.realEstate.dwellingsFileName + "_" + year + ".csv";
        ddReader.readData(dwellingsFile);
    }

    private static void readHouseholds(Properties properties, HouseholdDataManager householdData, HouseholdFactoryMstm hhFactory, int year) {
        String householdFile = properties.main.baseDirectory + properties.householdData.householdFileName;
        householdFile += "_" + year + ".csv";
        DefaultHouseholdReader hhReader = new DefaultHouseholdReader(householdData, hhFactory);
        hhReader.readData(householdFile);
    }

    private static void readPersons(Properties properties, HouseholdDataManager householdData, PersonfactoryMstm ppFactory, int year) {
        String personFile = properties.main.baseDirectory + properties.householdData.personFileName;
        personFile += "_" + year + ".csv";
        PersonReaderMstm personReader = new PersonReaderMstm(householdData, ppFactory);
        personReader.readData(personFile);
    }
}