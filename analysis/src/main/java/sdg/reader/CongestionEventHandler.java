package sdg.reader;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import sdg.data.AnalyzedPerson;

import java.util.HashMap;
import java.util.Map;

public class CongestionEventHandler implements ActivityEndEventHandler, LinkEnterEventHandler,
        LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler {

    private final Network network;

    public Map< Id<Person>, AnalyzedPerson> getPersons() {
        return persons;
    }

    private Map< Id<Person>, AnalyzedPerson> persons = new HashMap<>();

    public CongestionEventHandler(Network network) {
        this.network = network;
    }


    public void handleEvent(ActivityEndEvent event) {
        Id<Person> personId = Id.createPersonId(event.getPersonId().toString());
        persons.putIfAbsent(personId, new AnalyzedPerson(personId));


    }


    @Override
    public void handleEvent(LinkEnterEvent event) {
        double time = event.getTime();
        Id<Person> personId = Id.createPersonId(event.getVehicleId().toString());
        Id<Link> linkId = event.getLinkId();
        if (persons.keySet().contains(personId)){
            AnalyzedPerson person = persons.get(personId);
            person.enterThisLink(linkId, time);
        } else {
            throw new RuntimeException("This person appeared suddenly?");
        }



    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        double time = event.getTime();
        Id<Vehicle> personId = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();

        if (persons.keySet().contains(personId)){
            AnalyzedPerson person = persons.get(personId);
            Link link = network.getLinks().get(linkId);
            person.leaveThisLink(linkId, time, link);
        } else {
            throw new RuntimeException("This person appeared suddenly?");
        }
    }

    @Override
    public void reset(int iteration) {
    }


    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        Id<Vehicle> personId = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();
        if (persons.keySet().contains(personId)){
            AnalyzedPerson person = persons.get(personId);
            person.leaveTraffic();

        } else {
            throw new RuntimeException("This person appeared suddenly?");
        }

    }

}
