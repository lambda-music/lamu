package lamu;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import gnu.kawa.io.Path;
import gnu.mapping.Environment;
import gnu.mapping.Values;
import kawa.Shell;
import lamu.lib.doc.LamuDocument;
import lamu.lib.evaluators.SchemeUtils;
import lamu.lib.log.Logger;
import lamu.lib.procs.PulsarProcessWrapper;
import lamu.lib.scheme.proc.MultipleNamedProcedure1;
import lamu.lib.scheme.proc.MultipleNamedProcedureN;

public class procs implements Runnable {
    static final Logger LOGGER = Logger.getLogger( MethodHandles.lookup().lookupClass().getName() );
    static void logError(String msg, Throwable e) { LOGGER.log(Level.SEVERE,   msg, e   ); }
    static void logInfo (String msg             ) { LOGGER.log(Level.INFO,     msg      ); }
    static void logWarn (String msg             ) { LOGGER.log(Level.WARNING,  msg      ); }

    public static final LamuDocument createProcessBean = null;

    public static final CreateProcessProc createProcessProc = new CreateProcessProc(new String[] { "create-process", "newp" });
    public static final class CreateProcessProc extends MultipleNamedProcedureN {
        public CreateProcessProc(String[] names) {
            super(names);
        }

        @Override
        public Object applyN(Object[] args) throws Throwable {
   //                List l =  new ArrayList( Arrays.asList( args ) );
   //                l.add( 0, Keyword.make( "directory" ) );
   //                l.add( 1, new File( System.getProperty( "user.dir" ) ) );
   //                args = l.toArray();
            
            List<String> list = SchemeUtils.anySchemeValueListToStringList( Arrays.asList(args) );
            ProcessBuilder sb = new ProcessBuilder( list );
            
            // XXX ??? (Tue, 24 Mar 2020 06:09:27 +0900) <<< This should be integrated.
            sb.directory( ((Path) Shell.currentLoadPath.get()).toFile() );
            
            // TODO the IO should be able to controlled. this is bogus.
            // REMOVED (Tue, 24 Mar 2020 05:20:12 +0900) >>>
            // sb.inheritIO();
            // REMOVED (Tue, 24 Mar 2020 05:20:12 +0900) <<<
            return new PulsarProcessWrapper( sb.start(), new ArrayList( list ) );
        }
    }

    public static final LamuDocument destroyProcessBean = null;

    public static final DestroyProcessProc destroyProcessProc = new DestroyProcessProc(new String[] { "destroy-process", "kilp" });
    public static final class DestroyProcessProc extends MultipleNamedProcedureN {
        public DestroyProcessProc(String[] names) {
            super(names);
        }

        @Override
        public Object applyN(Object[] args) throws Throwable {
            for ( int i=0; i<args.length; i++ ) {
                if ( args[i] instanceof Process ) {
                    ((Process)args[i]).destroy();
                } else if ( args[i] instanceof PulsarProcessWrapper ) {
                        ((PulsarProcessWrapper)args[i]).destroy();
                } else {
                    logWarn( "warning : the value of the arguments no " + i + " is not a process object." );
                }
            }
            return Values.empty;
        }
    }

    public static final LamuDocument killProcessBean = null;

    public static final KillProcessProc killProcessProc = new KillProcessProc(new String[] { "kill-process", "fkilp" });
    public static final class KillProcessProc extends MultipleNamedProcedureN {
        public KillProcessProc(String[] names) {
            super(names);
        }

        @Override
        public Object applyN(Object[] args) throws Throwable {
            for ( int i=0; i<args.length; i++ ) {
                if ( args[i] instanceof Process ) {
                    ((Process)args[i]).destroyForcibly();
                } else if ( args[i] instanceof PulsarProcessWrapper ) {
                    ((PulsarProcessWrapper)args[i]).destroyForcibly();
                } else {
                    logWarn( "warning : the value of the arguments no " + i + " is not a process object." );
                }
            }
            return Values.empty;
        }
    }

    public static final LamuDocument sleepBean = null;

    public static final SleepProc sleepProc = new SleepProc(new String[] { "sleep" });
    public static final class SleepProc extends MultipleNamedProcedure1 {
        public SleepProc(String[] names) {
            super(names);
        }

        @Override
        public Object apply1(Object arg1) throws Throwable {
            Thread.sleep( SchemeUtils.toInteger( arg1 ));
            return Values.empty;
        }
    }

    
    
    /**
     * Initializes an environment of scheme engine and defines API for the scripts.
     * 
     * @param scheme
     *            the scheme instance to initialize.
     */
    public static void initScheme( Environment env ) {
        SchemeUtils.defineLambda( env, createProcessProc);
        SchemeUtils.defineLambda( env, destroyProcessProc); 
        SchemeUtils.defineLambda( env, killProcessProc);
        SchemeUtils.defineLambda( env, sleepProc );
    }

    @Override
    public void run() {
        initScheme( Environment.getCurrent() );
    }

    
}