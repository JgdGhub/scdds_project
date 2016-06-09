package uk.co.octatec.scdds.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by Jeromy Drake on 09/06/2016.
 */
public class DirCheck {

    private static final Logger log = LoggerFactory.getLogger(DirCheck.class);

    public static void check(String directory) {
        // older versions of intellij run the tests in the project directory, where there will be no 'test' directory
        File dir = new File(directory);
        if( !dir.exists() ) {
            log.info("creating dir [{}], current dir is [{}]", directory, System.getProperty("user.dir"));
            dir.mkdir();
        }
        else if( !dir.isDirectory()) {
            log.error("file [{}] exists but is NOT a directory, current dir is [{}]", directory, System.getProperty("user.dir"));
        }
    }
}
