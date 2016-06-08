package uk.co.octatec.scdds.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.cache.publish.CacheFilter;

/**
 * Created by Jeromy Drake on 24/05/2016.
 */
public class EvenSimpleDataFilter implements CacheFilter<String,SimpleData> {

    private final static Logger log = LoggerFactory.getLogger(EvenSimpleDataFilter.class);

    @Override
    public void init(String data) {
    }

    @Override
    public boolean accept(String key, SimpleData value) {
        if( key == null || value == null ) {
            log.error("*** NULL VALUES PASSED TO EvenSimpleDataFilter *** [{}] [[]]", key, value);
            throw new NullPointerException("NULL VALUES PASSED TO EvenSimpleDataFilter");
        }
        return value.data2%2==0;
    }
}
