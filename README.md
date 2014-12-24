housenumberclient
=================

client for Evaluation of addresses in OpenStreetMap against official housenumber lists

The existing housenumber evaluation on regio-osm.de/hausnummerauswertung is limited to work on a single server and not easy to install.
It needs a lot of components and this limits the contributed evaluation of housenumber evaluations. The existing version is stored in a private github repository.

This project continue the evaluation, but will be coded completely new to support contributed and decentralized evaluations.

The client in this repository will be work in future in this way:
- get an official housenumber list from the server
- get the osm data (first, via an overpass-api call)
- evaluate the official housenumbers against the one existing in osm
- create a simple result page
- upload the result with details to server

The server code is not in this repository. It will be accessible in regio-osm/housenumberserver, but it's still in planning phase (as of 2014-12-24).

