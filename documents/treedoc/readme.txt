= Introduction
    Pulsar is a music sequencer program which allows users to write music as
    Lisp Scheme programs.  In this system, musical notes and other informations
    are written as Scheme's association lists.  The musical notes can be
    dynamically generated association lists on-the-fly; a users can also
    interact with their programs to affect where the music go to.

    With this system, you can learn music and Lisp Scheme at same time.

    Pulsar is written in Java8 and Kawa, and processes MIDI data via Jack
    Audio Connection Kit.  Pulsar has been developed on the Ubuntu 16.04.
    Pulsar is expected to run on any Unix-like systems and versions of Windows.

    This manual is for @value{NAME} version @value{VERSION}.

    = What's New
        = Mon, 26 Nov 2018 21:23:29 +0900
            The Pulsar Music Sequencer has just been released; this application
            is very new and has not been fully tested yet. This application is
            not suitable to run production level systems. 

    = Feature
        - Enables you to write pieces of music as Lisp Scheme program.
        - Built with Kawa a very powerful Lisp Scheme implementation.
        - Works with JACK Audio Connection Kit and can connect to any
          synthesizer applications support JACK.
        - Includes Kawapad; Kawapad is an editor to edit Scheme program 
            - Kawapad can prettify Lisp code.
            - Execute a block of code on-the-fly.
            - Kawapad is extensible by Kawa-Scheme.

    = System Requirements
        Any operating systems that can run the following systems :

        - Java 8
        - JNA Java Native Access
        - Jack Audio Connection Kit

        Pulsar has been developed and tested in Ubuntu 16.04. A cursory
        experiment to run Pulsar in Windows 10 with Windows JACK was succeeded.
        It is still unknown if Pulsar can run in OS X and further experiments
        are needed.

        Pulser uses following libraries :

        - JNA-4.5.0
        - JNAJACK-1.3.0
        - KAWA-3.0

        These are statically linked to the main file `pulser.jar`.

    = Community 
        Currently There is no community which is related to Pulser. Currently
        Pulsar is developed by only one developer (me) and there is no user at
        all but the developer. The developer (me) strongly needs your help to
        improve the Pulsar.

= Getting Started
    = Installation
        Pulsar requires Java which version is 8 or later; install Java 8 at
        first.  Pulsar itself requires no installation required. Just place the
        main file `pulsar.jar` to any location where it is convenient.

    = Execution
        Pulsar is compiled into an executable JAR file; before executing Pulsar,
        please make sure `java` is placed on a directory in `PATH`, and then
        execute 

            `java -jar (directory where you placed jar)/pulsar.jar`

        may start Pulsar. (TODO make a script file to execute Pulsar and an
        installer for it.)

        If java is properly configured, just double clicking the jar file can also
        start Pulsar.

        Currently Pulsar outputs its error messages into `stterr` it is necessary
        to execute it in a terminal screen to read its error messages.

        = Command Line Arguments
            Currently Pulsar accepts only one parameter which is a path to a
            file to open. (TODO add useful parameters to Pulsar.)

    = Compilation
        Pulsar is developed by Eclipse and its repository contains entire
        Eclipse's workspace directory.

        = Compilation by Eclipse

            In most case, opening the workspace directory by Eclipse should compile
            the projects inside automatically. In case the project dependency could
            not be restored properly, reconfigure it.

            Currently there are four projects under Pulsar's workspace; each project
            depends on following projects :

            `lib`

            `kawapad` depends
            : -> lib

            `metro` depends
            : -> lib

            `pulsar` depends
            : -> lib
            : -> metro
            : -> kawapad

        = Compilation by Ant
            [workspace]/pulsar/build.xml is a ANT build file which was
            generated by Eclipse. This build file can also be used to
            build Pulsar.

= Basic
    = Start Up Pulsar
    

= Pulsar 
    = Scheme Notation

    = Application Model
        = Pulsar's Project File Model
            - Main file
            - Init file

    = Note-alist

    = API
        = Note Builder API
        = Pulser MIDI Controller API
        = Pulser GUI Controller API
            = gui-add
                If the specified constraint is "hidden" the object will not be
                actually added to the container. The added object could still be
                accessed by gui-get function. This method could be used for
                storing user data. (Sun, 14 Jul 2019 10:13:54 +0900)
                |
                |     hello
                |     world
                |     foo
                |     bar 
                |
                TEST

    = Files

= Kawapad
    = Overview

    = API
        = Kawapad GUI Controller API

= Metro
    = Overview


; vim: expandtab sw=4 textwidth=80 foldmethod=expr foldexpr=getline(v\:lnum)!\~'^[\\t\ ]\*\=\ \.\*\$' :
