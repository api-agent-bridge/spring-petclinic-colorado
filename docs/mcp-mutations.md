# Questions that change data through MCP

These seven questions ask an agent to change the Petclinic data through the GraphQL API over MCP. Most of them need a lookup before the write, and several show what happens when the agent retries a write. The questions that only read data are in [mcp-queries.md](mcp-queries.md).

Each question names the tools it needs. The tools come from the operation files in `src/main/resources/gatool/mcp/`, and each write returns the id of what it changed. The answers assume a freshly started app with the sample data of the H2 database. Most questions here change that data, so restart the app before you try them again.

## One write

1. **Register a new owner: Jan Peeters, 100 W 14th Ave Pkwy, Denver, telephone 7205551234.**

   Uses `addOwner`. Answer: the id of the new owner, 11 on a freshly started app. Every call of `addOwner` registers one more owner, so a retry after a timeout registers a second Jan Peeters.

## A lookup, then one write

2. **Harold Davis moved to 1701 Wynkoop St in Denver. Update the address.**

   Uses `findOwners` with the first name Harold and the last name Davis, then `getOwnerDetails`, then `updateOwner`. Answer: owner 4 now lives at 1701 Wynkoop St, Denver. `updateOwner` replaces every field, so the agent first reads the telephone (6085553198) with `getOwnerDetails` and sends it back as it is. Calling it again with the same input leaves the owner unchanged.

3. **Register a dog called Bobbie, born on 1 May 2020, for Jean Coleman.**

   Uses `findOwners`, `listPetTypes` and `addPet`. Answer: the id of the new pet, 14 on a freshly started app. The agent looks up the id of the owner (6) and the id of the type dog (2) before it can call `addPet`. The same request a second time returns a `BAD_REQUEST` error for the name, because an owner cannot have two pets with the same name.

4. **Book a check-up for Leo next Monday.**

   Uses `findPets` with the name Leo, then `addVisit`. Answer: a visit for pet 1 on the date of next Monday. The agent has to turn "next Monday" into a date. Every call books one more visit, so a retry books Leo twice.

## A question back to the user before the write

5. **Book a vaccination for Lucky tomorrow.**

   Uses `findPets` with the name Lucky, then `addVisit` once the user has chosen a pet. Answer: the agent asks which Lucky is meant, the bird of Jeff Black or the dog of Carlos Estaban, and books the visit after the user answers. `addVisit` books a visit without a date for tomorrow, so the agent can leave the date out.

## Reads, a plan, then several writes

6. **Every pet that had a rabies shot needs a booster. List those pets with their owners, and after I confirm, book a "rabies booster" visit for each of them next Monday.**

   Uses `findOwners` without arguments, then `getOwnerDetails` for every owner, then `addVisit` once for each pet. Answer: Samantha (rabies shot on 2013-01-01) and Max (rabies shot on 2013-01-02), both owned by Jean Coleman. After the confirmation the agent books two visits, for pets 7 and 8. The agent has to read the visits of every owner, wait for the confirmation, and make two writes that each book a second visit if they are retried.

## A request the tools cannot carry out

7. **Delete the owner George Franklin with all their pets.**

   Answer: the agent says that it cannot delete owners or pets, because the tools only add and change them. A call of any write tool is a mistake, such as `updateOwner` with the details overwritten so that the owner looks deleted.
