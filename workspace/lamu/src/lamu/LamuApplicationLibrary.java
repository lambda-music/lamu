package lamu;

import java.io.IOException;

import kawapad.Kawapad;
import kawapad.KawapadFrame;
import pulsar.Pulsar;
import pulsar.PulsarFrame;
import quartz.lib.scheme.SchemeEngine;
import quartz.lib.scheme.doc.DescriptiveHelp;
import quartz.lib.scheme.http.SchemeHttp;
import quartz.lib.scheme.http.SchemeHttp.UserAuthentication;

public class LamuApplicationLibrary {
    public static void initializeSchemeEngine( SchemeEngine schemeEngine ) {
        // pulsar gui
        PulsarFrame.registerGlobalSchemeInitializers( schemeEngine );
        Kawapad.registerSchemeInitializer( schemeEngine );
        
        // pulsar
        DescriptiveHelp.registerGlobalSchemeInitializer( schemeEngine );
        Pulsar.registerSchemeInitializers( schemeEngine );
    }

    public static SchemeEngine createSchemeEngine() {
        SchemeEngine schemeEngine = new SchemeEngine();
        initializeSchemeEngine( schemeEngine );
        return schemeEngine;
    }

    public static SchemeHttp createPulsarHttpServer(
            SchemeEngine schemeEngine, 
            int httpPort, 
            String path, 
            UserAuthentication userAuthentication ) throws IOException 
    {
        return new SchemeHttp( 
            httpPort,
            path,
            userAuthentication, 
            schemeEngine );
    }
    public static Pulsar createPulsar( SchemeEngine schemeEngine ) {
        return new Pulsar( schemeEngine );
    }

    public static PulsarFrame createPulsarGui( SchemeEngine schemeEngine  ) {
        return PulsarFrame.create( schemeEngine, true , null );
    }

    public static KawapadFrame createKawapad( SchemeEngine schemeEngine ) {
        return new KawapadFrame( schemeEngine, true, "Scheme Scratch Pad" );
    }


    
}