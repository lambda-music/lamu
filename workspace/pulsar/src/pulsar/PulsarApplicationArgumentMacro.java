package pulsar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PulsarApplicationArgumentMacro {
    List<String> input;
    List<String> output= new ArrayList<>();
    public PulsarApplicationArgumentMacro( List<String> input ) {
        this.input = input;
    }
    public List<String> getInput() {
        return input;
    }
    public List<String> getOutput() {
        return output;
    }
    public String[] getOutputAsArray() {
        return output.toArray( new String[ output.size() ] );
    }
    static abstract class MacroElement {
        final String name;
        public MacroElement( String name ) {
            this.name = name;
        }
        public String getMacroName() {
            return this.name;
        };
        abstract List<String> execute( Map<String,PulsarApplicationNamedArgument> namedArgs );
    }
    static final HashMap<String,MacroElement> map = new HashMap<>();
    static void register( MacroElement e ){
        map.put( e.getMacroName(), e );
    }
    static {
        register( new MacroElement( "local" ) {
            @Override
            List<String> execute(Map<String,PulsarApplicationNamedArgument> namedArgs ) {
                List<String> result = new ArrayList<>();
                result.add("exec");
                result.add("scheme");
                result.add("+");
                result.add("pulsar");
                result.add("+");
                result.add("pulsar-gui");
                if ( namedArgs.containsKey( "open" ) ) {
                    result.add( namedArgs.get( "open" ).getValue() );
                }
                result.add("+");
                return result;
            }
        });
    }
    
    void exec() {
        String s=null;
        for ( Iterator<String> i = input.iterator();;) {
            if ( s != null ) {
                if ( map.containsKey( s ) ) {
                    MacroElement e = map.get( s );
                    HashMap<String,PulsarApplicationNamedArgument> nargs = new HashMap<>();
                    while ( i.hasNext() ) {
                        s=i.next();
                        if ( s.startsWith( "--" ) ) {
                            PulsarApplicationNamedArgument na = new PulsarApplicationNamedArgument( s );
                            nargs.put( na.getKey(), na );
                        } else {
                            break;
                        }
                    }
                    
                    output.addAll( e.execute( nargs ) );
                } else {
                    output.add( s );
                }
            }
            
            if ( i.hasNext() ) {
                s = i.next();
            } else {
                break;
            }
        }
    }
}