package com.aac.kpi.service;

import java.util.*;
import java.util.stream.Collectors;

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
        Random rnd = new Random();
        List<Organization> organizations = new ArrayList<>();
        for (int i = 0; i < ORGANIZATION_NAMES.size(); i++) {
            organizations.add(new Organization(
                    RandomDataUtil.uuid32().toUpperCase(),
                    ORGANIZATION_NAMES.get(i),
                    ORGANIZATION_TYPES.get(rnd.nextInt(ORGANIZATION_TYPES.size()))
            ));
        }

        // determine AAC counts per organization (start with 2 each then distribute extras)
        List<Integer> aacCounts = new ArrayList<>(Collections.nCopies(organizations.size(), 2));
        int extras = rnd.nextInt(6); // 0..5 extra AACs (total 10..15)
        for (int i = 0; i < extras; i++) {
            aacCounts.set(i % organizations.size(), aacCounts.get(i % organizations.size()) + 1);
        }

        List<AacCenter> centers = new ArrayList<>();
        Iterator<String> areaIter = new ArrayList<>(AREA_NAMES).iterator();
        for (int idx = 0; idx < organizations.size(); idx++) {
            Organization org = organizations.get(idx);
            for (int j = 0; j < aacCounts.get(idx); j++) {
                if (!areaIter.hasNext()) {
                    areaIter = new ArrayList<>(AREA_NAMES).iterator();
                }
                String id = String.format("AAC%03d", 110 + centers.size());
                String area = areaIter.next();
                centers.add(new AacCenter(id, "Active Ageing Centre – " + area, org.organizationId));
            }
        }

        List<Location> locations = new ArrayList<>();
        Set<String> usedPostals = new HashSet<>();
        int locCounter = 1;
        for (Organization org : organizations) {
            int count = 2 + rnd.nextInt(3); // 2..4
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

        // Map organization -> locations
        Map<String, List<Location>> locationsByOrg = locations.stream()
                .collect(Collectors.groupingBy(Location::organizationId));

        List<Volunteer> volunteers = new ArrayList<>();
        List<MasterRow> masterRows = new ArrayList<>();
        int volunteerCounter = 1;
        for (AacCenter center : centers) {
            Organization org = organizations.stream()
                    .filter(o -> o.organizationId.equals(center.organizationId))
                    .findFirst()
                    .orElse(organizations.get(0));
            List<Location> orgLocations = locationsByOrg.getOrDefault(org.organizationId, List.of());
            int volunteerCount = 3 + rnd.nextInt(4); // 3..6
            for (int i = 0; i < volunteerCount; i++) {
                String volId = RandomDataUtil.uuid32().toUpperCase();
                String volName = RandomDataUtil.randomVolunteerName();
                String role = VOLUNTEER_ROLES.get(rnd.nextInt(VOLUNTEER_ROLES.size()));
                Volunteer vol = new Volunteer(volId, volName, role, center.aacCenterId,
                        !orgLocations.isEmpty() ? orgLocations.get(i % orgLocations.size()).postalCode : "");
                volunteers.add(vol);
                Location location = !orgLocations.isEmpty() ? orgLocations.get(i % orgLocations.size()) : null;
                masterRows.add(new MasterRow(center, org, location, vol));
            }
        }

        return new MasterData(organizations, centers, locations, volunteers, masterRows, locationsByOrg);
    }

    public record Organization(String organizationId, String name, String organizationType) {}
    public record AacCenter(String aacCenterId, String aacCenterName, String organizationId) {}
    public record Location(String locationId, String locationName, String organizationId, String postalCode) {}
    public record Volunteer(String volunteerId, String volunteerName, String volunteerRole, String aacCenterId, String postalCode) {}
    public record MasterRow(AacCenter aacCenter, Organization organization, Location location, Volunteer volunteer) {}

    public static final class MasterData {
        private final List<Organization> organizations;
        private final List<AacCenter> aacCenters;
        private final List<Location> locations;
        private final List<Volunteer> volunteers;
        private final List<MasterRow> rows;
        private final Map<String, List<Location>> locationsByOrg;

        private MasterData(List<Organization> organizations,
                           List<AacCenter> aacCenters,
                           List<Location> locations,
                           List<Volunteer> volunteers,
                           List<MasterRow> rows,
                           Map<String, List<Location>> locationsByOrg) {
            this.organizations = List.copyOf(organizations);
            this.aacCenters = List.copyOf(aacCenters);
            this.locations = List.copyOf(locations);
            this.volunteers = List.copyOf(volunteers);
            this.rows = List.copyOf(rows);
            this.locationsByOrg = new HashMap<>(locationsByOrg);
        }

        public List<Organization> getOrganizations() { return organizations; }
        public List<AacCenter> getAacCenters() { return aacCenters; }
        public List<Location> getLocations() { return locations; }
        public List<Volunteer> getVolunteers() { return volunteers; }
        public List<MasterRow> getRows() { return rows; }
        public Map<String, List<Location>> getLocationsByOrg() { return Collections.unmodifiableMap(locationsByOrg); }
    }
}
