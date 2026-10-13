

4.1 

    - Run duration depends on hardware. On my laptop one round takes around 30ish min, so hour and a half for 3 (repeat 3)
    to run, use: MCP_URL=http://127.0.0.1:8080/mcp npx promptfoo eval -c queries.yaml --no-cache --repeat 3 -o results/queries-3x.json

    - to view results use npm run view from /eval folder 

## 5.1 Starting the app in mock mode

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
```

## 6.1 Starting Keycloak

```bash
docker compose -f keycloak/compose.yaml up -d
```

To log out of Keycloak, open <http://localhost:8180/realms/petclinic/protocol/openid-connect/logout> and confirm.

Keycloak runs at <http://localhost:8180> with the realm `petclinic`. To connect the MCP Inspector to `http://localhost:8080/mcp`:

1. In the server's OAuth settings, set the Client ID to `mcp-inspector` and leave the Client Secret empty. Without a client ID, the Inspector tries to register itself, and Keycloak refuses.
2. Uncheck "Request refresh token", next to Client ID and Client Secret. While it is checked, the Inspector asks for `offline_access`, and Keycloak refuses the sign-in with `invalid_scope`.
3. Connect, and sign in as one of two users:

| User | Password | Gets |
| --- | --- | --- |
| `read` | `read` | `mcp:tools`: the read tools |
| `write` | `write` | `mcp:tools` and `petclinic:write`: the read and the write tools |

To connect MCPJam, in the server's settings:

1. Set Authentication to OAuth.
2. Under Advanced Settings, set Registration Strategy to "Preregistration (Client Credentials)".
3. Set the Client ID to `mcp-inspector`, leave the Client Secret empty, and connect.

Keycloak reads `keycloak/petclinic-realm.json` at the first start only. After a change to that file, recreate the container with `docker compose -f keycloak/compose.yaml down` and start it again.

8.1 Interesting questions
    The results below are what mock mode answers. Live answers change with the day. In mock mode a question gets
    its recorded answer when the agent passes the recorded arguments, and a catch-all recording otherwise.

    # one service

    Which Denver dog parks have drinking water?
        - Result: Green Valley Ranch East, Greenway Park, Parkfield, Railyard Dog Park, Sanderson Gulch and Willow Bark Dog Park.

    How many barking dog complaints did Denver 311 get in September 2026?
        - Result: 114. Adoption and hold inquiries were the most common animal case, with 286.

    Which Colorado counties had the most rabid animals in 2025? (SOAP)
        - Result: Jefferson 11, Larimer 8, Boulder 7. 24 counties had at least one.

    Which pets tested positive for a disease in Colorado in 2017? (SOAP)
        - Result: 6 cats with plague, 1 cat with tularemia and 2 dogs with rabies, in Weld and Yuma.

    Did Colorado shelters adopt out more adult dogs in 2025 than in 2024?
        - Result: 32,978 in 2025 against 33,341 in 2024.

    Which ingredients are reported most with vomiting in dogs?
        - Result: Spinosad 45,251, Milbemycin Oxime 37,435, Ivermectin 13,328 (openFDA).

    Which wild mammals do people see most in Denver County?
        - Result: the Eastern Fox Squirrel (1,343), then humans (332) and the Black-tailed Prairie Dog (315). The Domestic Dog has 124.

    What do people mistake coyotes for in Colorado?
        - Result: the Red Fox (11), the Mountain Lion (10) and the Domestic Dog (9).

    Are there weather alerts in Colorado right now?
        - Result: a Flood Advisory for Mesa County, a Special Weather Statement for Yuma, Kit Carson and Cheyenne counties, and a Hydrologic Outlook for southwestern Colorado.

    How much water is flowing in the South Platte River in Denver right now?
        - Result: 73.2 cfs at gauge PLADENCO, at 05:45 MDT on 12 October 2026.

    How much snow was on the ground at Berthoud Summit on 1 April 2026?
        - Result: 21 inches, holding 7.6 inches of water.

    # the clinic's data plus a service

    Which dog parks are closest to Dev2 Next, and do they have water?
        - Result: Skyline Park (1,184 m, water not recorded), Zeckendorf Place (1,373 m, no water), Lawson (Sonny) Park (1,604 m, no water).

    Is our vet Helen Leary licensed in Colorado?
        - Result: no Helen Leary in the register. Two other people named Leary hold or held a license: a vet tech in Pueblo and a vet whose license expired.

    Is rabies a risk for Dev2 Next's dog Woofy in Denver?
        - Result: Denver County had 2 rabid animals in 2023, 6 in 2024 and 5 in 2025 (0.68 per 100,000 people).

    Can Dev2 Next take Woofy on a trail in Rocky Mountain National Park?
        - Result: pets are not allowed on any trail. They are allowed on a leash in picnic areas, campgrounds and along roads, such as Trail Ridge Road.

    # chains across services

    What is the weather forecast at Dev2 Next's address?
        - owners, then censusGeocodeAddress, then nwsPoint (office BOU, gridpoint 63,61), then nwsGridpointForecast.
        - Result: Columbus Day sunny, high near 81. Tonight mostly clear, low around 49. Tuesday mostly sunny, high near 66.

    Are there rattlesnakes or mountain lions where Dev2 Next lives?
        - owners, then censusGeocodeAddress, then cpwWildlifeAt.
        - Result: the point lies in 58 mapped areas, among them the prairie rattlesnake range. It is outside the mountain lion and black bear conflict areas.

    Were there earthquakes within 100 km of Dev2 Next's home since 2024?
        - owners, then censusGeocodeAddress, then usgsEarthquakes.
        - Result: 3, the strongest an M 3.0 near Black Forest on 17 October 2025.

    # questions that show the failure modes

    Where does George Franklin live on a map?
        - Result: the Census geocoder places 110 W. Liberty St., Madison in Madison, Connecticut. The clinic does not store a state, so the
          geocoder picks one.

    What does the National Weather Service glossary say? (live only)
        - Result: live, the answer is 75,588 characters with only the terms selected, past GATool's limit of 60,000 characters,
          so GATool holds the result back and tells the agent why. The mock recording is trimmed to 29 terms and fits.

    Which DWR gauges are in Denver, and how much water do they carry?
        - DWR documents its API only in HTML help pages, so these fields were generated from scraped forms. Its date filters
          each take a different format (01/2025 for months, 2025 for years).
