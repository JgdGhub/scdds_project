package uk.co.octatec.scdds.net.registry;
/*
  SC/DDS - simple cached data distribution service

  Copyright 2016 by Jeromy Drake

  This program is free software; you may redistribute and/or modify it under
  the terms of the GNU General Public License Version 2 as published by the
  Free Software Foundation.

  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, without even the implied warranty of MERCHANTABILITY
  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  for complete details.
*/
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.mock.MockClientConnector;
import uk.co.octatec.scdds.mock.MockRegistryEntryValidator;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public class ResgistryTest {

    private final Logger log = LoggerFactory.getLogger(ResgistryTest.class);


    @BeforeClass
    public static void setup() {

        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
    }

    @Test
    public void RandomTest() {
        CacheRegistrarImpl cr1 = new CacheRegistrarImpl(null);
        CacheRegistrarImpl cr2 = new CacheRegistrarImpl(null);
        int r11 = cr1.nextRandom(1000);
        int r12 = cr1.nextRandom(1000);
        int r21 = cr2.nextRandom(1000);
        int r22 = cr2.nextRandom(1000);
        HashSet<Integer> check = new HashSet<>();
        check.add(r11);
        check.add(r12);
        check.add(r21);
        check.add(r22);
        log.info("random numbers [{}]", check);
        Assert.assertEquals("random number check", check.size(), 4); // at least all the numbers are different
        for(int i : check ) {
            Assert.assertTrue("random number in range", i >= 0 && i <= 1000 );
        }
    }

    @Test
    public void registerAndFindInstance() {

        log.info("## registerAndFindInstance");

        MockRegistryEntryValidator mockRegistryEntryValidator = new MockRegistryEntryValidator();

        Registry registry = new Registry(mockRegistryEntryValidator);
        Assert.assertTrue("registry is empty", registry.getRegistryMap().size()==0);

        log.info("# find when nothing is registered");

        Registry.Entry instance = registry.find("MyCache1");
        Assert.assertTrue("MyCache1 is not found", instance == null);

        log.info("# test registration");

        registry.add("MyCache1", "localhost", "9991",  "#");
        registry.add("MyCache2", "serverA", "9992",  "#");
        registry.add("MyCache2", "serverB", "9993",  "#");
        Map<String, List<Registry.Entry>> map = registry.getRegistryMap();
        Assert.assertTrue("registry is not empty", registry.getRegistryMap().size()==2);
        List<Registry.Entry> list1 = map.get("MyCache1");
        List<Registry.Entry> list2 = map.get("MyCache2");
        Assert.assertTrue("instance list-1 is correct", list1.size()==1);
        Assert.assertTrue("instance list-2 is correct", list2.size()==2);

        log.info("# test find");

        log.info("# find MyCache1");

        instance = registry.find("MyCache1");
        Assert.assertTrue("MyCache1 is found", instance != null);

        log.info("# find MyCache2 (1)");
        instance = registry.find("MyCache2");
        Assert.assertTrue("MyCache2 is found (1)", instance != null);
        Assert.assertTrue("MyCache2 is correct instance (1)", instance.host.equals("serverA")); // the first matching entry as all loads are same

        log.info("# find MyCache2 (2)");
        instance.connectionsCount = 5; // increase 'load' on serverA
        instance = registry.find("MyCache2");
        Assert.assertTrue("MyCache2 is found (2)", instance != null);
        Assert.assertTrue("MyCache2 is correct instance (2)", instance.host.equals("serverB")); // this has the lowest load

        log.info("# find MyCache2 (3)");
        instance.connectionsCount = 6; // increase 'load' on serverB
        instance = registry.find("MyCache2");
        Assert.assertTrue("MyCache2 is found (3)", instance != null);
        Assert.assertTrue("MyCache2 is correct instance (3)", instance.host.equals("serverA")); // now has the lowest load again

        log.info("# find MyCache2 (4)");
        mockRegistryEntryValidator.invalidServer = "serverA"; // make "ServerA" invalide
        instance = registry.find("MyCache2");
        Assert.assertTrue("MyCache2 is found (4)", instance != null);
        Assert.assertTrue("MyCache2 is correct instance (4)", instance.host.equals("serverB")); // this is the only valid server for MyCache22

        list2 = map.get("MyCache2"); // should only have 1 entry now, the invalid one should be removed
        Assert.assertTrue("instance list-2 is correct (2)", list2.size()==1);

        log.info("# find MyCache2 (5)");
        mockRegistryEntryValidator.invalidServer = "serverB"; // make "ServerA" invalide
        instance = registry.find("MyCache2");
        Assert.assertTrue("MyCache2 is not found (5)", instance == null);

        list2 = map.get("MyCache2"); // should  have no entries now, the invalid one should be removed
        Assert.assertTrue("instance list-2 is correct (3)", list2.size()==0);

        log.info("# find MyCache1 (2)");

        instance = registry.find("MyCache1");  // should still be there
        Assert.assertTrue("MyCache1 is found (2)", instance != null);

        log.info("# find MyCache1 (3)");

        mockRegistryEntryValidator.invalidServer = "*";  // all servers are invalid
        instance = registry.find("MyCache1");  // should notbe there
        Assert.assertTrue("MyCache1 is not found (3)", instance == null);

        list1 = map.get("MyCache1");
        Assert.assertTrue("instance list-1 is correct (2)", list1.size()==0);

    }

    @Test
    public void synchronizationTest() {

        log.info("## synchronizationTest");

        MockRegistryEntryValidator vaidator1 = new MockRegistryEntryValidator();
        Registry registry1 = new Registry(vaidator1);

        log.info("register some entries");

        registry1.add("MyCache0", "localhost", "9991",  "#");
        registry1.add("MyCache1", "localhost", "9991",  "#");
        registry1.add("MyCache2", "serverA", "9992",  "#");
        registry1.add("MyCache2", "serverB", "9993",  "#");

        {
            Map<String, List<Registry.Entry>> map = registry1.getRegistryMap();
            Assert.assertTrue("registry1 is not empty", registry1.getRegistryMap().size()==3);
            List<Registry.Entry> list0 = map.get("MyCache0");
            List<Registry.Entry> list1 = map.get("MyCache1");
            List<Registry.Entry> list2 = map.get("MyCache2");
            Assert.assertTrue("instance 1 list-0 is correct", list0.size()==1);
            Assert.assertTrue("instance 1 list-1 is correct", list1.size()==1);
            Assert.assertTrue("instance 1 list-2 is correct", list2.size()==2);

            Assert.assertEquals("entry count (2)", registry1.getEntryCount(), 4);
        }

        String registryDump = registry1.dump();
        log.info("registry-dump [{}]", registryDump);
        Assert.assertNotNull("resgistry dump not null",registryDump);
        String[] ss = registryDump.split("[\n]");
        log.info("registry-dump line-count=[{}]", ss.length);
        Assert.assertEquals("resgistry dump has all entries",ss.length, 4);

        // sync registry-1 to registry-1

        MockRegistryEntryValidator vaidator2 = new MockRegistryEntryValidator();
        Registry registry2 = new Registry();
        Assert.assertTrue("registry2 is empty", registry2.getRegistryMap().size()==0);

        RegistrySynchronizer rsync = new  RegistrySynchronizer(null, 0);
        rsync.applyRegistryDump(registry2, registryDump);
        {
            Map<String, List<Registry.Entry>> map2 = registry2.getRegistryMap();
            Assert.assertTrue("registry2 is not empty", registry1.getRegistryMap().size()==3);
            List<Registry.Entry> list0 = map2.get("MyCache0");
            List<Registry.Entry> list1 = map2.get("MyCache1");
            List<Registry.Entry> list2 = map2.get("MyCache2");
            Assert.assertTrue("instance 2 list-0 is correct", list0.size()==1);
            Assert.assertTrue("instance 2 list-1 is correct", list1.size()==1);
            Assert.assertTrue("instance 2 list-2 is correct", list2.size()==2);

            Assert.assertEquals("entry count (2)", registry2.getEntryCount(), 4);
        }
    }

    @Test
    public void createRegistryEntryTestGoodValidation() throws Exception {
        log.info("##createRegistryEntryTestGoodValidation");
        doCreateRegistryEntryTest(true/*validation ok*/);
    }

    @Test
    public void createRegistryEntryTestBadValidation() throws Exception {
        log.info("##createRegistryEntryTestBadValidation");
        doCreateRegistryEntryTest(false/*bad validaton*/);
    }

    private void doCreateRegistryEntryTest(boolean validationOk) throws Exception {

        log.info("doCreateRegistryEntryTest validationOk={}", validationOk);

        long timeStart = System.currentTimeMillis();

        MockClientConnector mockClientConnector = new  MockClientConnector();

        mockClientConnector.mockSession.initialStringRead = validationOk ?
                "request=load-check:cache-name=MyCache1:load=2:timestamp=1": // set up the response text the validator will receive
                "request=load-check:error=no-such-cache";

        RegistryEntryValidator vaidator = new RegistryEntryValidatorImpl(mockClientConnector);

        log.info("## createRegistryEntryTest ");
        RegistryServer registryServer1 = RegistryServer.startInThread(vaidator);
        int port = RegistryServer.awaitPortAllocation(registryServer1);
        log.info("registry port allocated [{}] time=[{}]", port,  System.currentTimeMillis() - timeStart);

        Assert.assertTrue("registry port allocated", port != 0);

        ArrayList<InetSocketAddress> registryServers = new ArrayList<>();
        InetSocketAddress addr = new InetSocketAddress("localhost", registryServer1.getPort());
        registryServers.add(addr);

        log.info("# registration tests time=[{}]", System.currentTimeMillis() - timeStart);

        CacheRegistrar reg = new CacheRegistrarImpl(registryServers);

        reg.registerCache("MyCache1", "serverA", 9999, 1);
        log.info("registry contact count [{}] during registration time=[{}]", reg.getRegistriesContactedCount(), System.currentTimeMillis() - timeStart);
        Assert.assertTrue("registry contact count during registration", reg.getRegistriesContactedCount() == 1);

        Map<String, List<Registry.Entry>> map = registryServer1.getRegistryMap();
        Assert.assertTrue("registry has entries", map.size() == 1);
        List<Registry.Entry> rlist = map.get("MyCache1");
        Assert.assertTrue("registry has entry for MyCache1", rlist.size() == 1);
        Registry.Entry instance = rlist.get(0);
        log.info("actual registry entry [{}]", instance);
        Assert.assertTrue("registry has correct name entry", instance.cacheName.equals("MyCache1"));
        Assert.assertTrue("registry has correct host entry", instance.host.equals("serverA"));
        Assert.assertTrue("registry has correct port entry", instance.port.equals("9999"));

        log.info("# location tests");

        CacheLocator locator = new CacheLocatorImpl(registryServers);

        log.info("locating MyCache1... time=[{}]", System.currentTimeMillis() - timeStart);


        InetSocketAddress cacheAddr = locator.locate("MyCache1", 1);
        log.info("registry contact count [{}] during location (1)", locator.getRegistriesContactedCount());
        Assert.assertTrue("registry contact count during location (1)", locator.getRegistriesContactedCount() == 1);

        mockClientConnector.mockSession.awaitLastStringWrite();
        log.info("validator message sent to server: [{}]", mockClientConnector.mockSession.lastStringWrite);

        //Assert.assertTrue("validator was called", vaidator1.validateCalled);

        log.info("MyCache1 located at [{}} time=[{}]", cacheAddr, System.currentTimeMillis() - timeStart);

        if( !validationOk ) {
            Assert.assertNull("MyCache1 shound not be found", cacheAddr);
            return;
        }
        else {
            Assert.assertNotNull("MyCache1 found", cacheAddr);
        }

        log.info("locating MyCacheX (should not be found)... time=[{}]", System.currentTimeMillis() - timeStart);

        cacheAddr = locator.locate("MyCacheX", 1);
        log.info("MyCacheX located at [{}] time=[{}]", cacheAddr, System.currentTimeMillis() - timeStart);

        log.info("registry contact count [{}] during location (2)", locator.getRegistriesContactedCount());
        Assert.assertTrue("registry contact count during location (2)", locator.getRegistriesContactedCount() == 1);

        Assert.assertNull("MyCacheX not found", cacheAddr);
    }

    @Test
    public void createRegistryEntryOnMultipleRegistriesTest() throws Exception{

        log.info("## createMultipleRegistryEntryTest");

        // REGISTRY 1

        MockRegistryEntryValidator vaidator1 = new MockRegistryEntryValidator();

        log.info("create registry-1");
        RegistryServer registryServer1 = RegistryServer.startInThread(vaidator1);
        int port1 = RegistryServer.awaitPortAllocation(registryServer1);
        log.info("registry-1 port allocated [{}]", port1);

        Assert.assertTrue("registry-1 port allocated", port1 != 0 );

        // REGISTRY 2

        MockRegistryEntryValidator vaidator2 = new MockRegistryEntryValidator();

        log.info("create registry-2");
        RegistryServer registryServer2 = RegistryServer.startInThread(vaidator2);
        int port2 = RegistryServer.awaitPortAllocation(registryServer2);
        log.info("registry-1 port allocated [{}]", port2);

        ArrayList<InetSocketAddress> registryServers = new ArrayList<>();
        InetSocketAddress addr1 = new InetSocketAddress("localhost", port1);
        registryServers.add(addr1);
        InetSocketAddress addr2 = new InetSocketAddress("localhost", port2);
        registryServers.add(addr2);

        CacheRegistrar reg = new CacheRegistrarImpl(registryServers);

        reg.registerCache("MyCache1", "serverX", 9991, 1);
        log.info("registry contact count [{}] during registration",reg.getRegistriesContactedCount());
        Assert.assertTrue("registry contact count during registration (expect 2)", reg.getRegistriesContactedCount()==2);

        // both registries should be the same...

        {
            Map<String, List<Registry.Entry>> map1 = registryServer1.getRegistryMap();
            Assert.assertTrue("registry-1 has entries", map1.size()==1);
            List<Registry.Entry> rlist1 = map1.get("MyCache1");
            Assert.assertTrue("registry-1 has entry for MyCache1", rlist1.size()==1);
            Registry.Entry instance1 = rlist1.get(0);
            log.info("actual registry entry [{}]", instance1);
            Assert.assertTrue("registry-1 has correct name entry", instance1.cacheName.equals("MyCache1"));
            Assert.assertTrue("registry-1 has correct host entry", instance1.host.equals("serverX"));
            Assert.assertTrue("registry-1 has correct port entry", instance1.port.equals("9991"));
        }

        {
            Map<String, List<Registry.Entry>> map2 = registryServer1.getRegistryMap();
            Assert.assertTrue("registry-2 has entries", map2.size()==1);
            List<Registry.Entry> rlist2 = map2.get("MyCache1");
            Assert.assertTrue("registry-2 has entry for MyCache1", rlist2.size()==1);
            Registry.Entry instance2 = rlist2.get(0);
            log.info("actual registry entry [{}]", instance2);
            Assert.assertTrue("registry-2 has correct name entry", instance2.cacheName.equals("MyCache1"));
            Assert.assertTrue("registry-2 has correct host entry", instance2.host.equals("serverX"));
            Assert.assertTrue("registry-2 has correct port entry", instance2.port.equals("9991"));
        }

        log.info("# location tests (multiple registries count={})", registryServers.size());

        CacheLocator locator = new CacheLocatorImpl(registryServers);

        log.info("locating MyCache1..." );

        InetSocketAddress cacheAddr = locator.locate("MyCache1", 1);
        log.info("registry contact count [{}] during location (multiple,1)",locator.getRegistriesContactedCount());
        Assert.assertTrue("registry contact count during location (multiple,1)", locator.getRegistriesContactedCount()==1);
                            // there are 2 registry servers, but we only contact the first if it can full-fill our request

        vaidator1.awaitValidateCalled();
        Assert.assertTrue("validator1 was called", vaidator1.validateCalled);
        vaidator2.awaitValidateCalled();
        Assert.assertFalse("validator2 was not called", vaidator2.validateCalled);
                    // the first successful valid find halts the location process, so
                    // we never need to validate the 2nd registry

        log.info("MyCache1 located at [{}}");
        Assert.assertNotNull("MyCache1 found", cacheAddr);

        log.info("locating MyCacheX (should not be found)...");

        cacheAddr = locator.locate("MyCacheX", 1);
        log.info("MyCacheX located at [{}]");

        log.info("registry contact count [{}] during location (multiple,2)",locator.getRegistriesContactedCount());
        Assert.assertTrue("registry contact count during location (multiple,2)", locator.getRegistriesContactedCount()==2);
                    // since MyCacheX is not known to any registries, both will be contacted

        Assert.assertNull("MyCacheX not found", cacheAddr);
    }

    @Test
    public void runtimeSynchronizationTest() throws Exception{

        log.info("## runtimeSynchronizationTest");

        // REGISTRY 1

        MockRegistryEntryValidator vaidator1 = new MockRegistryEntryValidator();

        log.info("create registry-1");
        RegistryServer registryServer1 = RegistryServer.startInThread(vaidator1);
        int portReadyCount = 0;
        while( registryServer1.getPort() == 0 ) {
            Thread.sleep(10);
            if( portReadyCount == 50 ) {
                log.warn("no port set in server-1 after waiting {}ms", 50*10);
                break;
            }
            ++portReadyCount;
        }
        log.info("registry-1 port alloced [{}] after portReadyCount=[{}]", registryServer1.getPort(), portReadyCount);

        Assert.assertTrue("registry-1 port allocated", registryServer1.getPort() != 0 );

        ArrayList<InetSocketAddress> registryServers = new ArrayList<>();
        InetSocketAddress addr1 = new InetSocketAddress("localhost", registryServer1.getPort());
        registryServers.add(addr1);

        CacheRegistrar reg = new CacheRegistrarImpl(registryServers);

        reg.registerCache("MyCache1", "serverX_1", 9991, 1);
        reg.registerCache("MyCache1", "serverX_2", 9991, 1);
        reg.registerCache("MyCache1", "serverX_3", 9991, 1);
        reg.registerCache("MyCache2", "serverX_1", 9992, 1);
        reg.registerCache("MyCacheA", "serverA", 9993, 1);
        reg.registerCache("MyCacheB", "serverB", 9994, 1);

        {
            Map<String, List<Registry.Entry>> map1 = registryServer1.getRegistryMap();
            Assert.assertTrue("registry-1 has map entries", map1.size()==4);
            Assert.assertTrue("registry-1 has total entries", registryServer1.getEntryCount()==6);
        }

        // REGISTRY 2

        MockRegistryEntryValidator vaidator2 = new MockRegistryEntryValidator();

        log.info("create registry-2");
        RegistryServer registryServer2 = new RegistryServer(0,vaidator2);

        Map<String, List<Registry.Entry>> map2 = registryServer2.getRegistryMap();
        Assert.assertTrue("registry-2 has map entries", map2.size()==0);
        Assert.assertTrue("registry-2 has total entries", registryServer2.getEntryCount()==0);

        log.info("registry-2 start sync from [{}]", addr1);
        registryServer2.syncRegistry(addr1.getHostName(), addr1.getPort());
        log.info("registry-2 start sync from [{}] done", addr1);

        Assert.assertTrue("after sync registry-2 has map entries", map2.size()==4);
        Assert.assertTrue("after sync registry-2 has total entries", registryServer2.getEntryCount()==6);

      }
}
