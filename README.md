# libs

The addon compiles against `vortexclient-api.jar` — the client itself.

The build workflow fetches it automatically from the client repository, so
nothing needs to be placed here for GitHub to build the addon.

**Building locally** does need it. Build the client once and copy the jar from
its `build/libs/` folder into this directory as `vortexclient-api.jar`.
