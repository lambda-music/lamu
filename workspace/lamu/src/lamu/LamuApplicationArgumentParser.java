package lamu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import kawapad.Kawapad;
import kawapad.KawapadFrame;
import lamu.lib.app.ApplicationVessel;
import lamu.lib.app.args.ArgsNamedArgument;
import lamu.lib.app.args.ArgsQuotedStringSplitter;
import lamu.lib.app.args.ArgumentParser;
import lamu.lib.app.args.ArgumentParserDefault;
import lamu.lib.app.args.ArgumentParserElement;
import lamu.lib.app.args.ArgumentParserElementFactory;
import lamu.lib.app.args.ArgumentParserStackKey;
import lamu.lib.doc.LamuAbstractDocument;
import lamu.lib.log.Logger;
import lamu.lib.scheme.AsyncThreadManager;
import lamu.lib.scheme.Evaluator;
import lamu.lib.scheme.MultiplexEvaluator;
import lamu.lib.scheme.RemoteEvaluator;
import lamu.lib.scheme.SchemeEvaluator;
import lamu.lib.scheme.SchemeUtils;
import lamu.lib.scheme.StreamEvaluator;
import lamu.lib.scheme.repl.ReplServer;
import lamu.lib.scheme.repl.SimpleReplService;
import lamu.lib.scheme.socket.SchemeHttp;
import lamu.lib.scheme.socket.SchemeHttp.UserAuthentication;
import lamu.lib.stream.LoggerStream;
import lamu.lib.stream.NullOutputStream;
import lamu.lib.stream.NullStream;
import lamu.lib.stream.SisoReceiver;
import lamu.lib.stream.StdioStream;
import lamu.lib.stream.Stream;
import pulsar.Pulsar;
import pulsar.PulsarFrame;

class LamuApplicationArgumentParser extends ArgumentParserDefault {
    static final Logger LOGGER = Logger.getLogger( MethodHandles.lookup().lookupClass().getName() );
    static void logError(String msg, Throwable e) { LOGGER.log(Level.SEVERE, msg, e); }
    static void logInfo(String msg)               { LOGGER.log(Level.INFO, msg);      } 
    static void logWarn(String msg)               { LOGGER.log(Level.WARNING, msg);   }

    static final String MSG_NO_STREAM_ERROR     = "no stream is available";
    static final String MSG_NO_LANG_ERROR  = "no evaluator was specified";
    static final String MSG_OUTPUT_HELP_ERROR   = "an error occured in output-help";
    static final String MSG_UNKNOWN_PARAM_ERROR = "unknown parameter : ";

    @Override
    protected void createValueStackMap() {
        getValueStack( EVALUATOR );
        getValueStack( PULSAR );
        getValueStack( KAWAPAD );
        getValueStack( REPL );
        getValueStack( HTTP );
        getValueStack( FRAME );
    }

    static final ArgumentParserStackKey<MultiplexEvaluator> EVALUATOR = new ArgumentParserStackKey<>();
    static final ArgumentParserStackKey<Pulsar> PULSAR = new ArgumentParserStackKey<>();
    static final ArgumentParserStackKey<Kawapad> KAWAPAD = new ArgumentParserStackKey<>();
    static final ArgumentParserStackKey<SisoReceiver> REPL = new ArgumentParserStackKey<>();
    static final ArgumentParserStackKey<KawapadFrame> FRAME = new ArgumentParserStackKey<>();
    static final ArgumentParserStackKey<SchemeHttp> HTTP = new ArgumentParserStackKey<>();

    static Stream vessel2stream( ApplicationVessel vessel ) {
        return (Stream)vessel.getComponents()
            .stream()
            .filter( item-> item instanceof Stream )
            .findAny()
            .orElse(null);
    }

    static final class AllAvailableReferenceArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                String outputFile = null;
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    if ( s.startsWith( "--" ) ) {
                        ArgsNamedArgument narg = new ArgsNamedArgument( s );
                        switch ( narg.getKey() ) {
                        case "output-file" :
                            outputFile = narg.getValue();
                            break;
                        default :
                            throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR + narg.getKey() );
                        }
                    } else {
                        throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR + s );
                    }
                    return this;
                }
                @Override
                public void notifyEnd( ArgumentParser parser ) {
                    if ( parser.getValueStack( EVALUATOR ).isEmpty() ) {
                        throw new RuntimeException( MSG_NO_LANG_ERROR );
                    }
                    LamuApplication.loadBasicClasses();
                    parser.getValueStack( RUNNABLE_INIT ).add( new Runnable() {
                        @Override
                        public void run() {
                            try {
                                LamuAbstractDocument.outputAvailableReferences( outputFile );
                            } catch (IOException e) {
                                logError( MSG_OUTPUT_HELP_ERROR, e ); 
                            }
                        }
                    });
                }
            };
        }
    }

    static final class OutputReferenceArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                String outputFile = null;
                String category   = null;
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    if ( s.startsWith( "--" ) ) {
                        ArgsNamedArgument narg = new ArgsNamedArgument( s );
                        switch ( narg.getKey() ) {
                        case "category" :
                            category = narg.getValue();
                            break;
                        case "output-file" :
                            outputFile = narg.getValue();
                            break;
                        default :
                            throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR  + narg.getKey() );
                        }
                    } else {
                        throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR  + s );
                    }
                    return this;
                }
                @Override
                public void notifyEnd(ArgumentParser parser) {
                    /*
                     * (Sat, 07 Mar 2020 20:23:21 +0900)
                     * ### SPECIAL ###  
                     * Check for special keyword(s) and process differently that cannot be processed
                     * by the `Descriptive` Documentation System. 
                     * 
                     */
                    switch ( this.category ) {
                    case "kawapad-keystrokes" :
                        /*
                         *  This is a special keyword to output keystroke reference which is 
                         *  not processed in `Desctiptive` help system.
                         */
                        procKeyStroke(parser);
                        break;
                        /*
                         * The others are processed by the documentation system. 
                         */
                    default :
                        procDocument(parser);
                        break;
                    }
                }
                void procKeyStroke(ArgumentParser parser) {
                    if ( parser.getValueStack( EVALUATOR ).isEmpty() ) {
                        throw new RuntimeException( MSG_NO_LANG_ERROR );
                    }
                    MultiplexEvaluator multiplexEvaluator = parser.getValueStack( EVALUATOR ).peek();

                    LamuApplication.loadBasicClasses();
                    parser.getValueStack( RUNNABLE_INIT ).add( new Runnable() {
                        @Override
                        public void run() {
                            Kawapad kawapad = new Kawapad(multiplexEvaluator);
                            String s = kawapad.outputKeyStrokeReference();
                            FileOutputStream o=null;
                            try {
                                o = new FileOutputStream( new File( outputFile ) );
                                o.write( s.getBytes(Charset.forName("utf-8")));
                                o.flush();
                            } catch (IOException e) {
                                logError( MSG_OUTPUT_HELP_ERROR, e ); 
                            } finally {
                                try {
                                    o.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });


                }
                void procDocument(ArgumentParser parser) {
                    if ( parser.getValueStack( EVALUATOR ).isEmpty() ) {
                        throw new RuntimeException( MSG_NO_LANG_ERROR );
                    }
                    LamuApplication.loadBasicClasses();
                    parser.getValueStack( RUNNABLE_INIT ).add( new Runnable() {
                        @Override
                        public void run() {
                            try {
                                LamuAbstractDocument.outputReference( category, outputFile ); 
                            } catch (IOException e) {
                                logError( MSG_OUTPUT_HELP_ERROR, e ); 
                            }
                        }
                    });
                }
            };
        }
    }

    static final class SimpleReplArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                //                int port = 0;
                @Override
                public ArgumentParserElement notifyArg( ArgumentParser parser, String s ) {
                    if ( false ) {
                    } else {
                        throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR + s );
                    }
                    return this;
                }
                @Override
                public void notifyEnd(ArgumentParser parser) {
                    if ( parser.getValueStack( STREAMABLES ).isEmpty() ) {
                        throw new RuntimeException( MSG_NO_STREAM_ERROR );
                    }
                    Stream stream = parser.getValueStack( STREAMABLES ).peek();
                    SisoReceiver sisoReceiver = new SisoReceiver( stream, new SimpleReplService() );
                    parser.getValueStack( REPL ).push( sisoReceiver );
                }
            };
        }
    }

    static final class SchemeHttpServerArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                private int serverPort = 8192;
                // TODO 0 variable accept path for HTTP 
                private String serverPath = "";
                UserAuthentication userAuthentication = UserAuthentication.ONLY_LOOPBACK;
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    if (s.startsWith( "--" ) ) {
                        ArgsNamedArgument a = new ArgsNamedArgument(s);
                        switch ( a.getKey() ) {
                        case "port" : 
                            this.serverPort = Integer.parseInt( a.getValue() );
                            break;
                        case "path" : 
                            this.serverPath = a.getValue();
                            break;
                        case "auth" : 
                            if ( "only-loopback".equals( a.getValue() )  ) {
                                this.userAuthentication = UserAuthentication.ONLY_LOOPBACK;
                                // } else if ( "only-loopback".equals( a.value )  ) {
                                // TODO
                            } else {
                                throw new RuntimeException( "unknown authentication type : " + a.getKey() );
                            }
                            break;
                        default :
                            throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR + a.getKey() );
                        }
                    } else {
                        throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR + s );
                    }
                    return this;
                }
                @Override
                public void notifyEnd(ArgumentParser parser) {
                    if ( parser.getValueStack( EVALUATOR ).isEmpty() ) {
                        throw new RuntimeException( MSG_NO_LANG_ERROR );
                    }
                    MultiplexEvaluator multiplexEvaluator = parser.getValueStack( EVALUATOR ).peek();
                    //                        if ( pulsarStack.isEmpty() ) {
                    //                            throw new RuntimeException( "no pulsar is defined." );
                    //                        }
                    //                        Pulsar pulsar = pulsarStack.peek();

                    try {
                        SchemeHttp schemeHttp = LamuApplicationLibrary.createPulsarHttpServer( 
                                multiplexEvaluator, serverPort, serverPath, userAuthentication );
                        parser.getValueStack( HTTP ).push( schemeHttp );
                    } catch (IOException e) {
                        throw new RuntimeException( e );
                    }
                }
            };
        }
    }

    static final class PulsarGuiArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                List<String> fileNameList = new ArrayList<>();
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    if ( s.startsWith( "--" ) ) {
                        ArgsNamedArgument a = new ArgsNamedArgument(s);
                        switch ( a.getKey() ) {
                        case "open" : 
                        case "load" : 
                            fileNameList.add( a.getValue() );
                            break;
                        default :
                            throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR + a.getKey() );
                        }
                    } else {
                        fileNameList.add(s);
                    }
                    return this;
                }
                @Override
                public void notifyEnd(ArgumentParser parser) {
                    if ( parser.getValueStack( EVALUATOR ).isEmpty() ) {
                        throw new RuntimeException( MSG_NO_LANG_ERROR );
                    }
                    MultiplexEvaluator multiplexEvaluator = parser.getValueStack( EVALUATOR ).peek();

                    PulsarFrame frame = LamuApplicationLibrary.createPulsarGui( multiplexEvaluator );
                    if (parser.getValueStack( FRAME ).size() == 0 )
                        frame.setShutdownWhenClose( true );
                    else
                        frame.setShutdownWhenClose( false );

                    parser.getValueStack( FRAME ).push( frame );
                    parser.getValueStack( KAWAPAD ).push( frame.getKawapad() );

                    parser.getValueStack( RUNNABLE_INIT ).push( new KawapadLoader( frame, fileNameList ));
                }
            };
        }
    }

    static final class KawapadGuiArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            List<String> fileNameList = new ArrayList<>();
            return new ArgumentParserElement() {
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    if ( s.startsWith( "--" ) ) {
                        ArgsNamedArgument a = new ArgsNamedArgument(s);
                        switch ( a.getKey() ) {
                        //                                case "port" : 
                        //                                    portNumberList.add(  Integer.parseInt( a.value ) );
                        //                                    break;
                        default :
                            throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR + a.getKey() );
                        }
                    } else {
                        fileNameList.add(s);
                    }
                    return this;
                }
                @Override
                public void notifyEnd(ArgumentParser parser) {
                    if ( parser.getValueStack( EVALUATOR ).isEmpty() ) {
                        throw new RuntimeException( MSG_NO_LANG_ERROR );
                    }
                    MultiplexEvaluator multiplexEvaluator = parser.getValueStack( EVALUATOR ).peek();
                    KawapadFrame frame = LamuApplicationLibrary.createKawapadGui( multiplexEvaluator );

                    if (parser.getValueStack( FRAME ).size() == 0 )
                        frame.setShutdownWhenClose( true );
                    else
                        frame.setShutdownWhenClose( false );

                    parser.getValueStack( FRAME ).push( frame );
                    parser.getValueStack( KAWAPAD ).push( frame.getKawapad() );
                    parser.getValueStack( RUNNABLE_INIT ).push( new KawapadLoader( frame, fileNameList ));
                    parser.getValueStack( RUNNABLE_INIT ).push( new Runnable() {
                        @Override
                        public void run() {
                            //                                pulsar.init();
                        }
                    });
                }
            };
        }
    }

    static final class PulsarArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR + s);
                }
                @Override
                public void notifyEnd(ArgumentParser parser) {
                    Pulsar pulsar = LamuApplicationLibrary.createPulsar();
                    parser.getValueStack( PULSAR ).push( pulsar );
                    parser.getValueStack( RUNNABLE_INIT ).push( new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
                }
            };
        }
    }
    static final class ReplArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                List<String> fileNameList = new ArrayList<>();
                List<String> scriptStringList = new ArrayList<>();
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    if ( s.startsWith( "--" ) ) {
                        ArgsNamedArgument a = new ArgsNamedArgument(s);
                        switch ( a.getKey() ) {
                        case "load" : 
                            fileNameList.add( a.getValue() );
                            break;
                        default :
                            throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR + a.getKey() );
                        }
                    } else {
                        fileNameList.add(s);
                    }
                    return this;
                }
                @Override
                public void notifyEnd(ArgumentParser parser) {
                    if ( parser.getValueStack( EVALUATOR ).isEmpty() ) {
                        throw new RuntimeException( MSG_NO_LANG_ERROR );
                    }
                    MultiplexEvaluator multiplexEvaluator = parser.getValueStack( EVALUATOR ).peek();

                    if ( parser.getValueStack( STREAMABLES ).isEmpty() ) {
                        throw new RuntimeException( MSG_NO_STREAM_ERROR );
                    }
                    Stream stream = parser.getValueStack( STREAMABLES ).peek();

                    
                    ReplServer replServer = new ReplServer( new AsyncThreadManager(), multiplexEvaluator );
                    SisoReceiver<ReplServer> receiver = new SisoReceiver<>( stream, replServer );
                    parser.getValueStack( REPL ).push( receiver );
                    parser.getValueStack( RUNNABLE_INIT ).push( new Runnable() {
                        boolean done = false;
                        @Override
                        public void run() {
                            if ( ! done ) {
                                done=true;
                                for ( String fileName : fileNameList ) {
                                    try {
                                        scriptStringList.add( "; " );
                                        scriptStringList.add( "; " + fileName );
                                        scriptStringList.add( "; " );
                                        scriptStringList.addAll( ArgsQuotedStringSplitter.splitLines( SchemeUtils.readAllAsString( fileName ) ) );
                                        scriptStringList.add( " " );
                                    } catch (IOException e) {
                                        throw new Error(e);
                                    }
                                }
                                scriptStringList.add( replServer.getPrefix() + "exec" );
                            } else {
                                throw new Error( "duplicate calling" );
                            }
                        }
                    });
                    parser.getValueStack( RUNNABLE_START ).push( new Runnable() {
                        @Override
                        public void run() {
                            receiver.process( scriptStringList );
                        }
                    });
                }
            };
        }
    }
    static final class SchemeArgumentParserElementFactory implements ArgumentParserElementFactory {
        static final String DEFAULT_REMOTE_URL = "http://localhost:";
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                MultiplexEvaluator multiplexEvaluator = MultiplexEvaluator.createEmpty();
                
                Evaluator createRemoteHttp( String url ) {
                    return new RemoteEvaluator( url );
                }
                Evaluator createRemoteStream( Stream streamable ) {
                    return new StreamEvaluator( streamable );
                }
                
                List<Evaluator> evaluatorList = new ArrayList<>();
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    if ( s.startsWith( "--"  ) ) {
                        ArgsNamedArgument a = new ArgsNamedArgument( s );
                        if ( false ) {
                            //
                        } else if ( "local".equals( a.getKey() ) ) {
                            evaluatorList.add( new SchemeEvaluator() );
                        } else if ( "server-url".equals( a.getKey() ) ) {
                            // deprecated
                            evaluatorList.add( createRemoteHttp( a.getValue() ) );
                        } else if ( "server-port".equals( a.getKey() ) ) {
                            // deprecated
                            evaluatorList.add( createRemoteHttp( DEFAULT_REMOTE_URL + a.getValue() ) );
                        } else if ( "http".equals( a.getKey() ) ) {
                            evaluatorList.add( createRemoteHttp( a.getValue() ) );
                        } else if ( "stream".equals( a.getKey() ) ) {
                            evaluatorList.add( createRemoteStream( parser.getValueStack( STREAMABLES ).peek()));
                        } else {
                            throw new RuntimeException( "unknown argument " + s );
                        }
                    } else {
                        throw new RuntimeException( "unknown command " + s );
                    }
                    return this;
                }
                @Override
                public void notifyEnd(ArgumentParser parser) {
                    if (this.evaluatorList.isEmpty() ) {
                        this.evaluatorList.add( new SchemeEvaluator() );
                    }
                    this.multiplexEvaluator.addAllEvaluators( this.evaluatorList );
                    parser.getValueStack( EVALUATOR ).push( this.multiplexEvaluator );

                    //                        runnableStack.push( new Runnable() {
                    //                            @Override
                    //                            public void run() {
                    //                                schemeSecretary.newScheme();
                    //                            }
                    //                        });
                }
            };
        }
    }

    static final class KawapadLoader implements Runnable {
        private final KawapadFrame frame;
        private List<String> fileNameList;
        KawapadLoader(KawapadFrame pulsarFrame, List<String> fileNameList) {
            this.frame = pulsarFrame;
            this.fileNameList = fileNameList;
        }

        @Override
        public void run() {
            frame.processInit();
            boolean first=true;
            for ( String s : fileNameList ) {
                try {
                    if ( first ) { 
                        frame.getKawapad().openFile( new File(s) );
                    } else {
                        frame.getKawapad().createKawapadFrame( new File(s) );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                first=false;
            }
            if ( first ) {
                frame.getKawapad().openIntro();
            }
        }
    }
    
    static final class LoggerArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                private String outFile;
                private String errFile;
                private String inFile;
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    if ( s.startsWith( "--" ) ) {
                        ArgsNamedArgument narg = new ArgsNamedArgument( s );
                        switch ( narg.getKey() ) {
                        case "out" :
                            outFile = narg.getValue();
                            break;
                        case "err" :
                            errFile = narg.getValue();
                            break;
                        case "in" :
                            inFile = narg.getValue();
                            break;
                        default :
                            throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR + narg.getKey() );
                        }
                    } else {
                        throw new RuntimeException( MSG_UNKNOWN_PARAM_ERROR + s );
                    }
                    return this;
                }
                @Override
                public void notifyEnd( ArgumentParser parser ) {
                    if ( parser.getValueStack( STREAMABLES ).isEmpty() ) {
                        throw new RuntimeException( MSG_NO_STREAM_ERROR );
                    }
                    Stream streamable = parser.getValueStack( STREAMABLES ).peek();
                    
                    try {
                        parser.getValueStack( STREAMABLES ).push(
                            new LoggerStream( streamable,
                                ( inFile  == null ? new NullOutputStream() : new FileOutputStream( inFile  )),
                                ( outFile == null ? new NullOutputStream() : new FileOutputStream( outFile )),
                                ( errFile == null ? new NullOutputStream() : new FileOutputStream( errFile ))));
                    } catch ( FileNotFoundException e ) {
                        throw new Error(e);
                    } 
                }
            };
        }

    }
    static final class StdioArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    return this;
                }
                @Override
                public void notifyEnd( ArgumentParser parser ) {
                    initStream(parser);
                }

                void initStream(ArgumentParser parser) {
                    parser.getValueStack( STREAMABLES ).push( StdioStream.INSTANCE );
                }

//                /**
//                 * Initialize the default stream. See {@link lamu.lib.app.args.ArgsState#initStream() }
//                 */
//                void initStream(ArgumentParser parser) {
//                    if ( ConsoleChecker.consoleExists() ) {
//                        parser.getValueStack( STREAMABLES ).push( StdioStream.INSTANCE );
//                    } else {
//                        logWarn(
//                            "=== WARNING ===\n" +
//                                "The user requested a STDIO stream to be instantiated but it is not available;\n" +
//                            "therefore, a dummy stream is created instead of the STDIO.\n" );
//                        parser.getValueStack( STREAMABLES ).push( NullStream.INSTANCE );
//                    }
//                }
            };
        }
    }
    static final class NullStreamArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    return this;
                }
                @Override
                public void notifyEnd( ArgumentParser parser ) {
                    parser.getValueStack( STREAMABLES ).push( NullStream.INSTANCE );
                }
            };
        }
    }
    static final class ForkedArgumentParserElementFactory implements ArgumentParserElementFactory {
        @Override
        public ArgumentParserElement create() {
            return new ArgumentParserElement() {
                @Override
                public ArgumentParserElement notifyArg(ArgumentParser parser, String s) {
                    return this;
                }
                @Override
                public void notifyEnd( ArgumentParser parser ) {
                    ApplicationVessel vessel = parser.getValueStack( VESSELS ).peek();
                    if ( vessel == null ) {
                        throw new Error( "no vessel was found" );
                    }
                    Stream stream = vessel2stream(vessel);
                    if ( stream == null ) {
                        throw new Error( "no stream was found" );
                    }
                    parser.getValueStack( STREAMABLES ).push( stream );
                }
            };
        }
    }


    /**
     * Sets up the commands for the command-line parser. Please read the code of the 
     * {@link LamuApplicationArgumentParser#initializeParser()} 
     * <pre>{@code
     * registerFactory("scheme",           new SchemeArgumentParserElementFactory());
     * registerFactory("kawapad",          new KawapadGuiArgumentParserElementFactory());
     * registerFactory("pulsar",           new PulsarArgumentParserElementFactory());
     * registerFactory("gui",              new PulsarGuiArgumentParserElementFactory());
     * registerFactory("http",            new SchemeServerArgumentParserElementFactory());
     * registerFactory("output-help",      new OutputReferenceArgumentParserElementFactory());
     * registerFactory("output-help-list", new AllAvailableReferenceArgumentParserElementFactory());
     * }</pre>
     */
    protected void initializeParser() {
        registerFactory( "scheme",           new SchemeArgumentParserElementFactory());
        registerFactory( "kawapad",          new KawapadGuiArgumentParserElementFactory());
        registerFactory( "pulsar",           new PulsarArgumentParserElementFactory());
        registerFactory( "repl",             new ReplArgumentParserElementFactory());
        registerFactory( "gui",              new PulsarGuiArgumentParserElementFactory());
        registerFactory( "http",             new SchemeHttpServerArgumentParserElementFactory());
        registerFactory( "simple-repl",      new SimpleReplArgumentParserElementFactory());
        registerFactory( "logger-stream",    new LoggerArgumentParserElementFactory());
        registerFactory( "stdio-stream",     new StdioArgumentParserElementFactory());
        registerFactory( "null-stream",      new NullStreamArgumentParserElementFactory());
        registerFactory( "forked-stream",    new ForkedArgumentParserElementFactory());
        
        registerFactory( "reference",        new OutputReferenceArgumentParserElementFactory());
        registerFactory( "reference-list",   new AllAvailableReferenceArgumentParserElementFactory());
    }
    {
        initializeParser();
    }
}
