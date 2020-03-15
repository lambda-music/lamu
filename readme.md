
Lambda Programmable Music Sequencer 
===================================

# Introduction #
Please, read [Overview of Lambda Programmable Music Sequencer][LNK_LAMBDA_MUSIC].

# Documentation #

## User's Documentation ##
- [Getting Started Installation and Execution](./docs/getting-started.md)
- [Lamu Command-line Parameter Specification](./docs/arguments.md)
- [Lamu Configuration File Specification](./docs/configuration.md)
- [Lamu API Reference](./workspace/build/docs/lamu-api-reference.md)
- [Lamu GUI Extension API Reference](./workspace/build/docs/lamu-gui-extension-api-reference.md)
- [Lamu Notation Reference](./workspace/build/docs/lamu-notation-reference.md)
- [KawaPad API Reference](./workspace/build/docs/kawapad-api-reference.md)
- [KawaPad Keystroke Reference](./workspace/build/docs/kawapad-keystroke-reference.md)

[# kawapad-api]: ./workspace/kawapad/docs.src/manual-kawapad-api.md
[# kawapad-keystroke]: ./workspace/kawapad/docs.src/manual-kawapad-keystroke.md

## Developer's Documentation ##
- [Architecture Overview](./architecture.md)
- [JavaDoc of Lambda Programmable Music Sequencer](./workspace/build/javadoc/index.html)
- [Pulsar](./workspace/pulsar/readme.md)
- [Metro](./workspace/metro/readme.md)
- [KawaPad](./workspace/kawapad/readme.md)
- [Lamu](./workspace/lamu/readme.md)


### Compilation ###
Lamu is developed by Eclipse. The `workspace` directory in the repository is an  
workspace directory of Eclipse. In most time, opening the workspace directory 
by Eclipse should compile the entire project automatically and can be executed 
on Eclipse. It is suffice for writing code and testing the code.

The JAR file for released version should be built by ANT build file on `build` 
directory.

Currently there are four projects under Lamu's workspace. The following list is
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

#### Compilation by Ant ####
`[workspace]/lamu/build.xml` is a ANT build file which was generated by
Eclipse. This file builds `Lamu.jar`.


[LNK_LAMBDA_MUSIC]:https://lambda-music.github.io/
[LNK_EDITOR_MOVIE]:./imgs/corresponding-parenthesis-movement.gif

[vim-modeline]: # ( vim: set spell expandtab fo+=awlt : )
