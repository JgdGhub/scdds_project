# scdds_project
SC/DDS - simple cached data distribution service

This is a Java implementation of a simple cached data distribution service. The scdds_samples 'module' contains sample code to get started.
Basically a publisher puts objects into its local cache and clients subscribe to the 'published' cache 'by name' and get a local copy of
the cache and update notifications. A central 'registry' runs to keep track of cache-names and locations so that all a client needs to
know to subscribe to a cache is its name (both client and server need to know the location of the registry). Multiple registries can run
to provide resilience. Multiple servers can provide the same cache-name and the Registry provides a load balancing function to
subscribing clients.

SC/DDS is very easy to use and its only dependent jar is slf4j-api (it also requires junit for the tests).


