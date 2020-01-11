package pulsar.lib.scheme;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.logging.Level;

import kawa.standard.Scheme;
import pulsar.lib.log.PulsarLogger;

public class SchemeEvaluatorGenericUtils {
    static final PulsarLogger LOGGER = PulsarLogger.getLogger( MethodHandles.lookup().lookupClass().getName() );
    static void logError(String msg, Throwable e) { LOGGER.log(Level.SEVERE, msg, e); }
    static void logInfo(String msg)               { LOGGER.log(Level.INFO, msg);      } 
    static void logWarn(String msg)               { LOGGER.log(Level.WARNING, msg);   }
    public static void executeExternalFile(Scheme scheme, Runnable threadInitializer, String fileType, File scriptFile) {
        // Read user's configuration file. If any problem is occurred, print its
        // stacktrace in the stderr, and then continue the process.
        try {
            logInfo( "Loading " + scriptFile.getName() );
            if ( scriptFile.exists() || scriptFile.isFile() ) {
                SchemeEvaluator evaluator = new SchemeEvaluator( scheme );
                evaluator.evaluate( threadInitializer, scriptFile ).throwIfError();
            } else {
                logInfo( "The " + fileType + " file \"" + scriptFile.getPath() + "\" does not exist. Ignored." );
            }
        } catch (Throwable e) {
            logError( "Ignored an error : ", e);
        }
    }
}
