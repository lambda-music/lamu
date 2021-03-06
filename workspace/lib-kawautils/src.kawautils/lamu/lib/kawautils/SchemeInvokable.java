/*
 * Pulsar-Sequencer written by Atsushi Oka 
 * Copyright 2018 Atsushi Oka
 *
 * This file is part of Pulsar-Sequencer. 
 * 
 * Pulsar-Sequencer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Pulsar-Sequencer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Pulsar-Sequencer.  If not, see <https://www.gnu.org/licenses/>.
 */

package lamu.lib.kawautils;

import gnu.mapping.Procedure;
import lamu.lib.Invokable;
import lamu.lib.InvokablyRunnable;

public class SchemeInvokable implements Invokable {
    private final Procedure procedure;
    public SchemeInvokable( Procedure procedure ) {
        this.procedure = procedure;
    }
    @Override
    public Object invoke( Object... args ) {
        try {
            return procedure.applyN( args );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static Runnable createRunnable( Procedure procedure, Object... args) {
        return new InvokablyRunnable( create( procedure ), args );
    }
    public static Invokable create( Procedure procedure ) {
        return new SchemeInvokable( procedure );  
    }
}
