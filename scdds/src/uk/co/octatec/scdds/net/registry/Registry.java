package uk.co.octatec.scdds.net.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public class Registry {

    private final static Logger log = LoggerFactory.getLogger(Registry.class);

    final static String ANONYMOUS_GROUP = "#";

    public static class Entry implements RegistryEntryValidator.Validatable{
        static final int INVALID_FLAG = -1;
        String cacheName;
        String host;
        String port;
        String group;
        int connectionsCount;

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public int getPort() {
            return Integer.parseInt(port);
        }

        @Override
        public void setConnectionCount(int count) {
            connectionsCount = count;
        }

        @Override
        public int getConnectionCount() {
            return connectionsCount;
        }

        @Override
        public String getCacheName() {
            return cacheName;
        }

        @Override
        public String getGroup() {
            return group;
        }


        public int getConnectionsCount() {
            return connectionsCount;
        }

        public static Entry fromShortString(String s) {
            String[] ss = s.split("[;]") ;
            if( ss.length != 5 ) {
                log.error("wrong format of shot-string [{}], need 5 fields", s) ;
                return null;
            }
            Entry entry = new Entry(ss[0], ss[1], ss[2], ss[3]);
            entry.connectionsCount = Integer.parseInt(ss[4]);
            return entry;
        }

        public Entry(String cacheName, String host, String port, String group) {
            this.cacheName = cacheName;
            this.host = host;
            this.port = port;
            this.group = group;
        }

        public void setInvalid() {
            connectionsCount = INVALID_FLAG;
        }

        public boolean isValid() {
            return connectionsCount != INVALID_FLAG;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (!cacheName.equals(entry.cacheName)) return false;
            if (!host.equals(entry.host)) return false;
            if (!port.equals(entry.port)) return false;
            return group.equals(entry.group);

        }

        @Override
        public int hashCode() {
            int result = cacheName.hashCode();
            result = 31 * result + host.hashCode();
            result = 31 * result + port.hashCode();
            result = 31 * result + group.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "cacheName='" + cacheName + '\'' +
                    ", host='" + host + '\'' +
                    ", port='" + port + '\'' +
                    ", group='" + group + '\'' +
                    ", connectionsCount=" + connectionsCount +
                    '}';
        }
        public String toShortString() {
            return cacheName+";"+host+";"+port+";"+group+";"+ connectionsCount;
        }

    }

    private Map<String, List<Entry>> registryMap = new HashMap<>();

    private RegistryEntryValidator registryEntryValidator;

    public Registry() {
        this(new RegistryEntryValidatorImpl());
    }

    public Registry(RegistryEntryValidator registryEntryValidator) {
        this.registryEntryValidator = registryEntryValidator;
    }

    public void clear() {
        registryMap.clear();
    }

    public int getEntryCount()  {
        int count = 0;
        for(List<Entry> entryList : registryMap.values()) {
            count +=  entryList.size();
        }
        return count;
    }

    public Entry find(String name) {

        log.info("finding registry entry for [{}]", name);

        synchronized (registryMap) {
            List<Entry> entryList = registryMap.get(name);
            if( entryList==null ) {
                log.info("no entrys for [{}], list is null", name);
                return null;
            }

            while( entryList.size() > 0 ) {

                Entry lowestLoadInstance = null;
                for (Entry entry : entryList) {
                    if (lowestLoadInstance == null && entry.isValid()) {
                        lowestLoadInstance = entry;
                    } else {
                        if (entry.connectionsCount < lowestLoadInstance.connectionsCount && entry.isValid()) {
                            lowestLoadInstance = entry;
                        }
                    }
                }

                log.info("selected entry [{}], check its valid", lowestLoadInstance);

                registryEntryValidator.validate(lowestLoadInstance);

                if (lowestLoadInstance.isValid()) {
                    ++lowestLoadInstance.connectionsCount;
                    return lowestLoadInstance;
                }
                else {
                    log.info("entry is not valid, remove from list [{}]", lowestLoadInstance);
                    entryList.remove(lowestLoadInstance);
                }
            }

            log.info("no entry for [{}], list is empty", name);

            return null;
        }
    }

    public String dump() {
        log.info("registry-dump: starting");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        synchronized(registryMap) {
            for( List<Entry> entryList : registryMap.values()  ) {
                for(Entry entry : entryList ) {
                    sb.append(entry.toShortString());
                    sb.append("\n");
                    ++count;
                }
            }
        }
        log.info("registry-dump: record-count=[{}] string-length=[{}]", count, sb.length());
        return sb.toString();
    }

    public void add(String name, String host, String port, String group) {

        log.info("registartion request: [{}] [{}} [{}] [{}]", name, host, port, group);
        Entry newInstance = new Entry(name, host, port, group);
        addEntry(newInstance);
    }

    void addEntry(Entry newInstance) {
        synchronized (registryMap) {
            List<Entry> entryList = registryMap.get(newInstance.cacheName);
            if( entryList == null ) {
                entryList = new ArrayList<>();
                registryMap.put(newInstance.cacheName, entryList);
            }
            boolean alreadyExists = false;
            for(Entry entry : entryList ) {
                if( entry.equals(newInstance)) {
                    // the entry is already registered, just reset its number of
                    // connectionsCount since its re-registering, its probably just re-started
                    // (number of connectionsCount is used for load balancing)
                    log.info("updating existing entry [{}] with [{}]", entry, newInstance);
                    entry.connectionsCount = newInstance.connectionsCount;
                    alreadyExists = true;
                }
            }
            if( !alreadyExists ) {
                log.info("adding new entry [{}]", newInstance);
                entryList.add(newInstance);
            }
        }
    }

    Map<String, List<Entry>> getRegistryMap() {
        return registryMap;
    }

}
