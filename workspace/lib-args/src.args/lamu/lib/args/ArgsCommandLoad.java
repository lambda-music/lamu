package lamu.lib.args;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArgsCommandLoad extends ArgsCommand {
    private String commandName = "load";
    public ArgsCommandLoad(String commandName) {
        super();
        this.commandName = commandName;
    }

    @Override
    protected String commandName() {
        return commandName;
    }
    
    static <T> void setCollection( Collection<T> from, Collection<T> to ) {
        to.clear();
        to.addAll(from);
    }

    @Override
    protected void execute( ArgsCommandState state, List<String> arguments, int recursiveCount ) {
        // Parse the arguments
        List<String> outSeqArgs = new ArrayList<>();
        Map<String, ArgsNamedArgument> outNamedArgs = new HashMap<>();
        Args.parseArguments( arguments, outSeqArgs, outNamedArgs);
        
        try {
            // Get the first argument as a filename and remove it.
            String uri = outSeqArgs.remove(0);
            
            // Read the file as a string value.
            String content = Utils.readAllAsString(uri);
            
            // Parse the string value into a list of string values. 
            List<String> scriptContent = ArgsQuotedStringSplitter.splitString(content); 
            
            // Execute the string list as a script program.
            Args.executeMacro( state, uri, scriptContent, arguments, outSeqArgs, outNamedArgs, recursiveCount  );

        } catch (IOException e) {
            throw new Error(e);
        }

        if ( arguments.isEmpty() ) {
            throw new Error( "no argument was specified" );
        }
    }
}