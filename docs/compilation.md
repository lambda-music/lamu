# How to Compile Lambda Music Sequencer #
Lambda Music Sequencer (hereinafter abbreviated as Lamu) is developed by 
Eclipse. The `workspace` directory in the repository is an  workspace directory 
of Eclipse.  In most time, opening the workspace directory by Eclipse should 
compile the entire project automatically and can be executed on Eclipse.  It is 
suffice for writing code and testing the code.

The executable JAR file can  be built by ANT build file on `build` directory.

Currently there are six projects under Lamu's workspace. The following list is
the relation of dependency among the projects in Lamu.

```memo
- lib
- kawapad
    +>lib
- metro 
    +-> lib
- pulsar 
    +-> lib
    +-> metro
    +-> kawapad
- lamu
    +-> lib
    +-> kawapad
    +-> pulsar
    +-> lamu
```

# Compilation by Ant #
`[workspace]/build/build.xml` is a ANT build file which was generated by
Eclipse. This builds `lamu.jar`.

# Main Class #
The main-class is `lamu.LamuApplication`.


