package de.tum.bgu.msm.syntheticPopulationGenerator.munich.allocation;

import de.tum.bgu.msm.SiloUtil;
import de.tum.bgu.msm.container.SiloDataContainer;
import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.properties.PropertiesSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich.preparation.MicroDataManager;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static de.tum.bgu.msm.data.RealEstateDataManager.rentCategories;

public class GenerateVacantDwellings {

    private static final Logger logger = Logger.getLogger(GenerateVacantDwellings.class);

    private final DataSetSynPop dataSetSynPop;
    private final MicroDataManager microDataManager;
    private int previousHouseholds;
    private int previousPersons;
    private Map<Integer, Map<Integer, Float>> ddQuality;
    private int totalHouseholds;
    private float ddTypeProbOfSFAorSFD;
    private float ddTypeProbOfMF234orMF5plus;
    private Map<Integer, Float> probTAZ;
    private Map<Integer, Float> probMicroData;
    private Map<Integer, Float> probVacantBuildingSize;
    private Map<Integer, Float> probVacantFloor;
    private double[] probabilityId;
    private double sumProbabilities;
    private double[] probabilityTAZ;
    private double sumTAZs;
    private int[] ids;
    private int[] idTAZs;
    private int personCounter;
    private int householdCounter;
    private int highestDwellingIdInUse;
    private final SiloDataContainer dataContainer;
    private RealEstateDataManager realEstateDataManager;


    public GenerateVacantDwellings(SiloDataContainer dataContainer, DataSetSynPop dataSetSynPop){
        this.dataContainer = dataContainer;
        this.dataSetSynPop = dataSetSynPop;
        microDataManager = new MicroDataManager(dataSetSynPop);
    }

    public void run(){
        logger.info("   Running module: household, person and dwelling generation");
        previousHouseholds = 0;
        previousPersons = 0;
        initializeQualityAndIncomeDistributions();
        generateVacantDwellings();
    }


    private void initializeQualityAndIncomeDistributions(){

        previousHouseholds = 0;
        previousPersons = 0;

        realEstateDataManager = dataContainer.getRealEstateData();
        for (Dwelling dd: realEstateDataManager.getDwellings()){
            int municipality = (int) PropertiesSynPop.get().main.cellsMatrix.getIndexedValueAt(dd.getZone(),"ID_city");
            updateQualityMap(municipality, dd.getYearBuilt(), dd.getQuality());
        }
        highestDwellingIdInUse = 0;
        for (Dwelling dd: realEstateDataManager.getDwellings()) {
            highestDwellingIdInUse = Math.max(highestDwellingIdInUse, dd.getId());
        }

    }

    private void generateVacantDwellings(){

        for (int municipality : dataSetSynPop.getMunicipalities()){
            int vacantDwellings =(int) PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(municipality, "totalDwellingsVacant");
            initializeVacantDwellingData(municipality);
            int vacantCounter = 0;
            int[] tazSelection = selectMultipleTAZ(vacantDwellings);
            for (int draw = 0; draw < vacantDwellings; draw++){
                int tazSelected = tazSelection[draw];
                int newDdId = highestDwellingIdInUse++;
                int floorSpace = microDataManager.guessFloorSpace(SiloUtil.select(probVacantFloor));
                int buildingYearSize = SiloUtil.select(probVacantBuildingSize);
                int year = microDataManager.dwellingYearfromBracket(extractYear(buildingYearSize));
                DwellingType type = extractDwellingType(buildingYearSize, ddTypeProbOfSFAorSFD, ddTypeProbOfMF234orMF5plus);
                int bedRooms = microDataManager.guessBedrooms(floorSpace);
                int quality = selectQualityVacant(municipality, extractYear(buildingYearSize));
                int groundPrice = dataSetSynPop.getDwellingPriceByTypeAndZone().get(tazSelected).get(type);
                int price = microDataManager.guessPrice(groundPrice, quality, floorSpace, Dwelling.Usage.VACANT);
                Dwelling dwell = realEstateDataManager.createDwelling(newDdId, tazSelected, -1, DwellingType.MF234, bedRooms, quality, price, 0, year); //newDwellingId, raster cell, HH Id, ddType, bedRooms, quality, price, restriction, construction year
                dwell.setUsage(Dwelling.Usage.VACANT);
                dwell.setFloorSpace(floorSpace);
                dwell.setYearConstructionDE(year);
                vacantCounter++;
            }
            logger.info("Municipality " + municipality + ". Generated vacant dwellings: " + vacantCounter);
        }
    }


    private void initializeVacantDwellingData(int municipality){

        probVacantFloor = new HashMap<>();
        for (int floor : PropertiesSynPop.get().main.sizeBracketsDwelling) {
            probVacantFloor.put(floor, PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(municipality, "vacantDwellings" + floor));
        }
        probVacantBuildingSize = new HashMap<>();
        for (int year : PropertiesSynPop.get().main.yearBracketsDwelling){
            int sizeYear = year;
            String label = "vacantSmallDwellings" + year;
            probVacantBuildingSize.put(sizeYear, PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(municipality, label));
            sizeYear = year + 10;
            label = "vacantMediumDwellings" + year;
            probVacantBuildingSize.put(sizeYear, PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(municipality, label));

        }
        ddTypeProbOfSFAorSFD = PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(municipality,"ddProbSFAorSFD");
        ddTypeProbOfMF234orMF5plus = PropertiesSynPop.get().main.marginalsMunicipality.getIndexedValueAt(municipality,"ddProbMF234orMF5plus");
        probabilityTAZ = new double[dataSetSynPop.getProbabilityZone().get(municipality).keySet().size()];
        sumTAZs = 0;
        probabilityTAZ = dataSetSynPop.getProbabilityZone().get(municipality).values().stream().mapToDouble(Number::doubleValue).toArray();
        for (int i = 1; i < probabilityTAZ.length; i++){
            probabilityTAZ[i] = probabilityTAZ[i] + probabilityTAZ[i-1];
        }
        idTAZs = dataSetSynPop.getProbabilityZone().get(municipality).keySet().stream().mapToInt(Number::intValue).toArray();
    }


    private void updateQualityMap(int municipality, int year, int quality){

        int yearBracket = microDataManager.dwellingYearBracket(year);
        int key = yearBracket * 10000000 + municipality;
        if (ddQuality != null) {
            if (ddQuality.get(key) != null) {
                Map<Integer, Float> qualities = ddQuality.get(key);
                if (qualities.get(quality) != null) {
                    float prev = 1 + qualities.get(quality);
                    qualities.put(quality, prev);
                } else {
                    qualities.put(quality, 1f);
                }
                ddQuality.put(key, qualities);
            } else {
                Map<Integer, Float> qualities = new HashMap<>();
                qualities.put(quality, 1f);
                ddQuality.put(key, qualities);
            }
        } else {
            ddQuality = new HashMap<>();
            Map<Integer, Float> qualities = new HashMap<>();
            qualities.put(quality, 1f);
            ddQuality.put(key, qualities);
        }
    }


    private int[] selectMultipleTAZ(int selections){

        int[] selected;
        selected = new int[selections];
        int completed = 0;
        for (int iteration = 0; iteration < 100; iteration++){
            int m = selections - completed;
            //double[] randomChoice = new double[(int)(numberOfTrips*1.1) ];
            double[] randomChoices = new double[m];
            for (int k = 0; k < randomChoices.length; k++) {
                randomChoices[k] = SiloUtil.getRandomNumberAsDouble();
            }
            Arrays.sort(randomChoices);

            //look up for the n travellers
            int p = 0;
            double cumulative = probabilityTAZ[p];
            for (double randomNumber : randomChoices){
                while (randomNumber > cumulative && p < probabilityTAZ.length - 1) {
                    p++;
                    cumulative += probabilityTAZ[p];
                }
                if (probabilityTAZ[p] > 0) {
                    selected[completed] = idTAZs[p];
                    completed++;
                }
            }
        }
        return selected;
    }


    private int extractYear(int buildingYear){

        int year = 0;
        if (buildingYear < 10){
            year = buildingYear;
        } else {
            year = buildingYear - 10;
        }
        return year;
    }


    private DwellingType extractDwellingType (int buildingYear, float ddType1Prob, float ddType3Prob){

        DwellingType type = DwellingType.SFA;

        if (buildingYear < 10){
            if (SiloUtil.getRandomNumberAsFloat() < ddType1Prob){
                type = DwellingType.SFD;
            } else {
                type = DwellingType.SFA;
            }
        } else {
            if (SiloUtil.getRandomNumberAsFloat() < ddType3Prob){
                type = DwellingType.MF5plus;
            }
        }


        return type;
    }


    private int selectQualityVacant(int municipality, int year){
        int result = 0;
        if (ddQuality.get(year * 10000000 + municipality) == null) {
            HashMap<Integer, Float> qualities = new HashMap<>();
            for (int quality = 1; quality <= PropertiesSynPop.get().main.numberofQualityLevels; quality++){
                qualities.put(quality, 1f);
            }
            ddQuality.put(year * 10000000 + municipality, qualities);
        }
        result = SiloUtil.select(ddQuality.get(year * 10000000 + municipality));
        return result;
    }


}
