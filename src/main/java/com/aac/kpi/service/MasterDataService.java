package com.aac.kpi.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.aac.kpi.service.AppState;

public final class MasterDataService {
    private static final List<String> ORGANIZATION_NAMES = List.of(
            "SilverCare Foundation",
            "Evergreen Community",
            "RiverLife Society",
            "Harmony Support Network",
            "United Ageing Partners"
    );

    private static final List<String> ORGANIZATION_TYPES = List.of("VWO", "Community Partner", "Private Operator", "Health Agency");
    private static final List<String> AREA_NAMES = List.of("Bedok North", "Pasir Ris", "Clementi", "Yishun", "Tampines", "Jurong", "Woodlands", "Ang Mo Kio", "Bukit Merah", "Marine Parade");
    private static final List<String> LOCATION_NAMES = List.of("Community Centre", "Neighbourhood Hub", "Wellness Studio", "Activity Hall", "Learning Annex");
    private static final List<String> VOLUNTEER_ROLES = List.of(
            "Event Organizer",
            "Volunteer Coordinator",
            "Admin Staff",
            "Social Worker",
            "Healthcare Assistant",
            "Programme Coordinator"
    );

    private MasterDataService() {}

    public static MasterData generate() {
        return generate(AppState.getVolunteersPerCenter());
    }

    public static MasterData generate(int volunteersPerCenter) {
        Random rnd = new Random();

        int totalCenters = 10 + rnd.nextInt(6); // between 10 and 15 AAC centers
        List<AacCenter> centers = new ArrayList<>();
        List<Organization> organizations = new ArrayList<>();
        Iterator<String> areaIter = new ArrayList<>(AREA_NAMES).iterator();
        Set<String> usedOrgIds = new HashSet<>();

        for (int i = 0; i < totalCenters; i++) {
            if (!areaIter.hasNext()) {
                areaIter = new ArrayList<>(AREA_NAMES).iterator();
            }
            String aacId = String.format("AAC%03d", 110 + i);
            String area = areaIter.next();
            String orgId;
            do {
                orgId = RandomDataUtil.uuid32().toUpperCase();
            } while (!usedOrgIds.add(orgId));
            Organization org = new Organization(
                    orgId,
                    ORGANIZATION_NAMES.get(i % ORGANIZATION_NAMES.size()) + " Hub " + (i + 1),
                    ORGANIZATION_TYPES.get(rnd.nextInt(ORGANIZATION_TYPES.size()))
            );
            organizations.add(org);
            centers.add(new AacCenter(aacId, "Active Ageing Centre – " + area, org.organizationId));
        }

        List<Location> locations = new ArrayList<>();
        Set<String> usedPostals = new HashSet<>();
        int locCounter = 1;
        for (Organization org : organizations) {
            int count = 1 + rnd.nextInt(3); // 1..3 locations per organization
            for (int i = 0; i < count; i++) {
                String postal;
                do {
                    postal = RandomDataUtil.randomPostal6();
                } while (usedPostals.contains(postal));
                usedPostals.add(postal);
                String locName = LOCATION_NAMES.get(rnd.nextInt(LOCATION_NAMES.size())) + " " + (i + 1);
                locations.add(new Location(
                        String.format("LOC%03d", locCounter++),
                        locName,
                        org.organizationId(),
                        postal
                ));
            }
        }

        Map<String, List<Location>> locationsByOrg = locations.stream()
                .collect(Collectors.groupingBy(Location::organizationId));

        List<Volunteer> volunteers = new ArrayList<>();
        Map<String, List<Volunteer>> volunteersByCenter = new HashMap<>();
        int countPerCenter = Math.max(0, volunteersPerCenter);
        for (AacCenter center : centers) {
            List<Location> orgLocations = locationsByOrg.getOrDefault(center.organizationId, List.of());
            List<Volunteer> centerVolunteers = new ArrayList<>();
            for (int i = 0; i < countPerCenter; i++) {
                String volId = RandomDataUtil.uuid32().toUpperCase();
                String volName = RandomDataUtil.randomVolunteerName();
                String role = VOLUNTEER_ROLES.get(rnd.nextInt(VOLUNTEER_ROLES.size()));
                String postal = !orgLocations.isEmpty() ? orgLocations.get(i % orgLocations.size()).postalCode : "";
                Volunteer vol = new Volunteer(volId, volName, role, center.aacCenterId, postal);
                centerVolunteers.add(vol);
                volunteers.add(vol);
            }
            volunteersByCenter.put(center.aacCenterId, centerVolunteers);
        }

        return new MasterData(organizations, centers, locations, volunteers, volunteersByCenter, locationsByOrg);
    }

    public record Organization(String organizationId, String name, String organizationType) {}
    public record AacCenter(String aacCenterId, String aacCenterName, String organizationId) {}
    public record Location(String locationId, String locationName, String organizationId, String postalCode) {}
    public record Volunteer(String volunteerId, String volunteerName, String volunteerRole, String aacCenterId, String postalCode) {}
    public static final class MasterData {
        private final List<Organization> organizations;
        private final List<AacCenter> aacCenters;
        private final List<Location> locations;
        private final List<Volunteer> volunteers;
        private final Map<String, List<Volunteer>> volunteersByCenter;
        private final Map<String, List<Location>> locationsByOrg;
        private final Map<String, Organization> organizationById;

        private MasterData(List<Organization> organizations,
                           List<AacCenter> aacCenters,
                           List<Location> locations,
                           List<Volunteer> volunteers,
                           Map<String, List<Volunteer>> volunteersByCenter,
                           Map<String, List<Location>> locationsByOrg) {
            this.organizations = List.copyOf(organizations);
            this.aacCenters = List.copyOf(aacCenters);
            this.locations = List.copyOf(locations);
            this.volunteers = List.copyOf(volunteers);
            this.volunteersByCenter = new HashMap<>();
            volunteersByCenter.forEach((key, value) -> this.volunteersByCenter.put(key, List.copyOf(value)));
            this.locationsByOrg = new HashMap<>(locationsByOrg);
            this.organizationById = organizations.stream()
                    .collect(Collectors.toMap(Organization::organizationId, Function.identity()));
        }

        public List<Organization> getOrganizations() { return organizations; }
        public List<AacCenter> getAacCenters() { return aacCenters; }
        public List<Location> getLocations() { return locations; }
        public List<Volunteer> getVolunteers() { return volunteers; }
        public Map<String, List<Location>> getLocationsByOrg() { return Collections.unmodifiableMap(locationsByOrg); }

        public Organization getOrganization(String organizationId) {
            return organizationById.get(organizationId);
        }

        public Location getPrimaryLocation(String organizationId) {
            List<Location> locations = locationsByOrg.get(organizationId);
            return locations == null || locations.isEmpty() ? null : locations.get(0);
        }

        public List<Volunteer> getVolunteersByCenter(String aacCenterId) {
            return volunteersByCenter.getOrDefault(aacCenterId, List.of());
        }

        public List<String> getVolunteerIds(String aacCenterId) {
            return getVolunteersByCenter(aacCenterId).stream()
                    .map(Volunteer::volunteerId)
                    .filter(id -> id != null && !id.isBlank())
                    .toList();
        }

        public List<String> getVolunteerNames(String aacCenterId) {
            return getVolunteersByCenter(aacCenterId).stream()
                    .map(Volunteer::volunteerName)
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
        }

        public int getVolunteerCount(String aacCenterId) {
            return getVolunteersByCenter(aacCenterId).size();
        }
    }
}
