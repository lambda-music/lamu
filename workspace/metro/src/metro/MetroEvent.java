/*
 * Metro Musical Sequencing Framework written by Atsushi Oka 
 * Copyright 2018 Atsushi Oka
 *
 * This file is part of Metro Musical Sequencing Framework. 
 * 
 * Metro Musical Sequencing Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Metro Musical Sequencing Framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Metro Musical Sequencing Framework.  If not, see <https://www.gnu.org/licenses/>.
 */

package metro;

import java.util.Comparator;
import java.util.List;

/**
 * This interface provides common methods for objects that represents every
 * notes in a bar. Bars are usually generated by the tracks. This interface is
 * strongly related to {@link MetroTrack#progressCursor(int, List) } method;
 * refer the {@linkplain MetroTrack#progressCursor(int, List) method} for
 * further information.
 * <p>
 * These methods are called as a callback of JACKAudio processing; these method
 * should return as soon as possible. The heavy processing that blocks for
 * longer time than the current setting of JACK's frame rate causes JACK to
 * XRUN.
 * 
 * @author Ats Oka
 */
public interface MetroEvent {
    public static final Comparator<? super MetroEvent> BAR_OFFSET_COMPARATOR = new Comparator<MetroEvent>() {
        @Override
        public int compare( MetroEvent o1, MetroEvent o2) {
            int i;
            i = (int) Math.signum( o1.getBarOffset() - o2.getBarOffset() );
            if (i != 0 )
                return i;
            
            if ( o1 instanceof MetroMidiEvent &&  o2 instanceof MetroMidiEvent ) {
                MetroMidiEvent mo1 = (MetroMidiEvent) o1;
                MetroMidiEvent mo2 = (MetroMidiEvent) o2;

                // FIXME ??? 111100000? not 11110000? why five zeros?
                // FIXED BE CAREFUL (Tue, 29 Oct 2019 08:16:33 +0900)
                byte b1 = (byte) ( mo1.getMidiData()[0] & 0b11110000 );
                byte b2 = (byte) ( mo2.getMidiData()[0] & 0b11110000 );
                
                if ( b1 == b2 )
                    return 0;
                
                if ( b1 == 0b10010000 )
                    return 1;
                else 
                    return -1;
                        
            } else {
                return 0;
            }
        }
    };
    boolean isBetween(double from, double to);
    void setBarOffset(double barOffset);
    void calcBarOffset(int barLengthInFrames);
    double getBarOffset();
    
    /**
     * Check if the position of this event is inside the duration specified in the
     * parameter. See {@link MetroTrack#progressCursor(int, List) } for further
     * information.
     * 
     * This methods is called as a callback of JACKAudio processing; this method
     * should return as soon as possible. The heavy processing that blocks for
     * longer time than the current setting of JACK's frame rate causes JACK to
     * XRUN.
     * 
     * @param from
     *            Specifies the beginning point of the duration to check. The value
     *            is inclusive.
     * @param to
     *            Specifies the end point of the duration to check. The value is
     *            exclusive.
     * @return <code>true</code> if this event is inside the duration.
     */
    boolean isBetweenInFrames(int from, int to);
    void calcBarOffsetInFrames(int barLengthInFrames);
    int getBarOffsetInFrames();
    void setBarOffsetInFrames( int barOffsetInFrames );

    /*
     *  This method effectively converts MetroEvent into MetroMidiEvent.
     */
//    void      calcMidiOffset( int cursor );

    /**
     * Defines the procedure to execute when this event is activated. This method is
     * usually called when {@link #between(int, int)} returned <code>true</code>.
     * See {@link MetroTrack#progressCursor(int, List) } for further information.
     * 
     * This methods is called as a callback of JACKAudio processing; this method
     * should return as soon as possible. The heavy processing that blocks for
     * longer time than the current setting of JACK's frame rate causes JACK to
     * XRUN.
     * 
     * @param metro
     *            The Metro instance which is the owner of this event.
     * @param cursor TODO
     * @param from
     *            the value of <code>from</code> when {@link #between(int, int)}
     *            returns <code>true</code>.
     * @param to
     *            the value of <code>to</code> when {@link #between(int, int)}
     *            returns <code>true</code>.
     * @param nframes
     *            the current
     * @param eventList
     */
    MetroMidiEvent process(Metro metro, int cursor);
    
    
    /**
     * 
     * @param prefix
     * @return
     */
    public default String dump(String prefix) {
        StringBuilder sb = new StringBuilder();
        dumpProc(prefix, sb);
        return sb.toString();
    }

    void dumpProc(String prefix, StringBuilder sb);
}
