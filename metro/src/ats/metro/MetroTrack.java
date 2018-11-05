package ats.metro;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPosition;

public class MetroTrack implements MetroTrackInfo, MetroLock {
	static final Logger LOGGER = Logger.getLogger(MetroTrack.class.getName());
	static void logError(String msg, Throwable e) {
		LOGGER.log(Level.SEVERE, msg, e);
	}
	static void logInfo(String msg) {
		// LOGGER.log(Level.INFO, msg);
		System.err.println(msg);
	}
	static void logWarn(String msg) {
		LOGGER.log(Level.WARNING, msg);
	}


	public enum SyncType {
		IMMEDIATE, PARALLEL, SERIAL,  
	}
	
	
	/**
	 * 
	 */
	private final Metro metro;
	/**
	 * Note that the String object which is stored in name field must be interned.  
	 */
	protected final String name;
	protected Set<String> tags;
	protected boolean enabled = true;

	private static final boolean DEBUG = false;

//	private static final int BUFFER_SIZE = 2;
	private static final double MARGIN_LENGTH = 2;   

	int id = (int) (Math.random()* Integer.MAX_VALUE);

	private transient SyncType syncType;
	private transient MetroTrack syncTrack;
	private transient double syncOffset=0.0d;
	private BlockingQueue<MetroEventBuffer> buffers = new LinkedBlockingQueue<>();
	protected transient int cursor = 0;
	protected transient int lastLengthInFrames = 0;
	protected transient int lastAccumulatedLength = 0;
	
	protected final MetroSequence sequence;
	transient boolean ending = false;
	transient double endingLength = 0;
	
	public MetroTrack( Metro metro, String name, Collection<String> tags, MetroSequence sequence, SyncType syncType, MetroTrack syncTrack, double syncOffset ) {
//		LOGGER.info( "Track(" + name + ") : " + tags + " : " + syncType + " : " + syncOffset );
		this.name = name.intern();
		if ( tags == null )
			this.tags = new HashSet<>();
		else
			this.tags = (new HashSet<>( tags ));
		this.metro = metro;
		this.sequence = sequence;
		this.syncType = syncType;
		this.syncTrack = syncTrack;
		this.syncOffset = syncOffset;
		
		sequence.setTrackInfo( this );
	}
	
	@Override
	public Object getMetroLock() {
		return this.metro.lock;
	}
	
	public MetroSequence getSequence() {
		return this.sequence;
	}
	@Override
	public String getTrackName() {
		return name;
	}
	@Override
	public Set<String> getTrackTags() {
		return tags;
	}
	
	@Override
	public boolean isTrackEnabled() {
		return enabled;
	}
	@Override
	public void setTrackEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	@Override
	public void removeTrack( boolean graceful ) {
		if ( graceful ) {
			this.ending = true;
		} else {
			metro.unregisterTrack( this );
		}
	}
	@Override
	public double getTrackPosition() {
		if ( lastLengthInFrames < 0 || cursor < 0) {
			return 0;
		} else {
			return (double)( cursor - lastAccumulatedLength + lastLengthInFrames )  / (double)lastLengthInFrames;
		}
	}

	
	
//	@Override
//	public boolean equals(Object obj) {
//		try {
//			return this.name == ((Track)obj).name;
//		} catch ( ClassCastException e ) {
//            Logger.getLogger( Metro.class.getName()).log(Level.WARNING, null, e );
//			return false;
//		}
//	}

// ???
//	public int getCursor() {
//		return cursor;
//	}
//	public void setCursor( int cursor ) {
//		this.cursor = cursor; 
//	}

	
	/*
	 * === About `found` flag ===
	 * There are three stages :
	 *
	 * 1. While between() function returns false,
	 * the current position should be before the area.
	 * 2. While between() function returns true,
	 * the current position should be in the area. 
	 * 3. When between() function return false 
	 * after between() function return true,
	 * the current position should be after the area.
	 * 
	 * If it entered to the stage-3, between() function will not
	 * return true again; therefore the searching process is
	 * not necessary to continue anymore.
	 * 
	 * * Asterisk denotes a Note Event Object. 
	 * 
	 * 
	 *                    | THE AREA   |
	 *  *                 |            |
	 *    *               |            |
	 *         *          |            |
	 *              *     |            |
	 *                    | *          |
	 *                    |     *      |
	 *                    |            |
	 *                    |          * |
	 *                    |            |*  
	 *                    |            |        *
	 *                    |            |           *
	 *                    |            |             *
	 */


	static int BUFFER_REMOVAL_STRATEGY = 3;

	/* 
	 * (Sat, 23 Dec 2017 23:16:25 +0900)
	 * 1. 
	 * This "if" statement should not be like :
	 *     buf.getLengthInFrames() <= nextCursor
	 * This should be :
	 *     buf.getLengthInFrames() <   nextCursor
	 *  
	 * Because there might be a note on the last position.
	 * This may often be a note off event.
	 * 
	 * 2.
	 *    this.cursor= this.cursor - buf.getLengthInFrames();
	 *    
	 * In this statement `this.cursor` could be lower than zero.
	 * This value is used when when we call JackMidi.eventWrite() afterwards 
	 * as offset value of the current frame.  
	 * 
	 */
	protected void progressCursor( int nframes, List<MetroAbstractMidiEvent> result ) throws JackException {
		synchronized ( this.buffers ) {
			this.metro.clearAllPorts();

			int currentCursor = this.cursor;
			int nextCursor = currentCursor + nframes;
			
			// This keeps negative offset value for the current cursor position. 
			int cursorOffset = 0;

			for ( Iterator<MetroEventBuffer> ibuf = this.buffers.iterator(); ibuf.hasNext(); ) {
				MetroEventBuffer buf = ibuf.next();

				int actualCursor     = currentCursor - cursorOffset;
				int actualNextCursor = nextCursor    - cursorOffset;
				
				boolean found= false;
				for ( Iterator<MetroEvent> ie = buf.iterator(); ie.hasNext();  ) {
					MetroEvent e = ie.next();
					
					if ( e.between( actualCursor, actualNextCursor ) ) {
						found = true;
						e.process( metro, actualCursor, actualNextCursor, nframes, result );
					} else {
						if ( found )
							break;
					}
				}

				cursorOffset = cursorOffset + buf.getLengthInFrames();
			}
			
			{
				int accumulatedLength = 0;
				int pollCount = 0;
				int lengthInFrame = -1;
				for (Iterator<MetroEventBuffer> it = this.buffers.iterator();it.hasNext(); ) {
					MetroEventBuffer b=it.next();
					accumulatedLength += b.getLengthInFrames();
					
					if (  accumulatedLength < ( currentCursor - (int)(b.getBarLengthInFrames() * MARGIN_LENGTH ) ) ) {
						pollCount ++;
					}
					if ( currentCursor < accumulatedLength ) {
						lengthInFrame = b.getLengthInFrames();
						break;
					}
				}
				
				int polledCount = 0;
				for (Iterator<MetroEventBuffer> it = this.buffers.iterator();it.hasNext(); ) {
					MetroEventBuffer b = it.next();
					if ( polledCount < pollCount  ) {
						polledCount ++;
						int currentLengthInFrames = b.getLengthInFrames();
						currentCursor -= currentLengthInFrames;
						nextCursor -= currentLengthInFrames;
						it.remove();
//						this.buffers.poll();
						if (DEBUG)
							logInfo( String.format( "currentLengthInFrames:%d currentCursor:%d ", currentLengthInFrames , currentCursor ) );

					} else {
						break;
					}
				}
				
				if ( 0< pollCount )
					metro.notifyCheckBuffer();

				this.cursor = nextCursor;
				this.lastLengthInFrames = lengthInFrame;
				this.lastAccumulatedLength = accumulatedLength;
			}
			
			if ( Metro.DEBUG && false)
				logInfo( currentCursor + "/" + (this.buffers.isEmpty() ? "empty" : this.buffers.peek().getLengthInFrames()  ));
		}
	}

	protected void prepare( int barInFrames ) throws JackException {
		int offset = (int) (-1.0d * this.syncOffset * barInFrames);

		switch ( this.syncType ) {
			case IMMEDIATE :
			{
				this.cursor = offset;
				logInfo( "prepare(immediate):" + this.cursor );
				if ( this.syncTrack != null ) {
					Logger.getLogger( Metro.class.getName()).log(Level.WARNING, "syncTrack was passed but ignored by the process because syncType was `immediate`." );
				}
			}
			break;
			case PARALLEL :
			{
				if ( this.syncTrack == null ) {
					this.cursor = offset;
					Logger.getLogger( Metro.class.getName()).log(Level.WARNING, "`parallel` was specified but syncTrack was not passed." );
				} else {
					this.cursor = this.syncTrack.cursor + offset ;
				}

			}
			break;
			case SERIAL :
				if ( this.syncTrack == null ) {
					this.cursor = offset;
		            Logger.getLogger( Metro.class.getName()).log(Level.WARNING, "`serial` was specified but syncTrack was not passed." );
				} else {
					synchronized ( this.syncTrack.buffers ) {
						this.cursor =
								this.syncTrack.cursor - 
								this.syncTrack.buffers.peek().getLengthInFrames() + 
								offset;
					}
				}
				break;
			default :
				throw new RuntimeException( "Internal Error"); // this won't occur.
		}

	}

	protected  void reprepare( Metro metro, JackClient client, JackPosition position, 
			double prevBeatsPerMinute, double beatsPerMinute ) throws JackException 
	{
		synchronized ( this.buffers ) {
			int prevLengthInFrame = -1;
			int lengthInFrame = -1;
			{
				MetroEventBuffer headBuffer = this.buffers.peek();
				if ( headBuffer != null )
					prevLengthInFrame = headBuffer.getLengthInFrames();
			}
				
			// double ratio = magnifyCursorPosition( prevBeatsPerMinute, beatsPerMinute );
			for ( MetroEventBuffer buffer : this.buffers ) {
				buffer.prepare(metro, client, position, false);
			}
			
			{
				MetroEventBuffer headBuffer = this.buffers.peek();
				if ( headBuffer != null )
					lengthInFrame = headBuffer.getLengthInFrames();
			}
			
			double ratio = (double)lengthInFrame / (double)prevLengthInFrame; 
			if ( 0< ratio && 1.0d!= ratio ) {
				// System.out.println( "ratio: " + ratio );
				// System.out.println( "prev cursor: " + cursor );
				this.cursor = (int) Math.round( ((double)this.cursor)            * ratio );
				// System.out.println( "after cursor: " + cursor );
				// System.out.println( "lengthInFrame    : " + lengthInFrame );
				// System.out.println( "prevLengthInFrame: " + prevLengthInFrame );
				// this.lastLengthInFrame = (int) Math.round( ((double)this.lastLengthInFrame) * ratio );
			}
		}

	}

	private double getAccumulatedLength() {
		double accumulatedLength = 0;
		for ( MetroEventBuffer b : this.buffers ) {
			accumulatedLength += b.getLength();
		}
//		logInfo( "accumulatedLength" + accumulatedLength );

		return accumulatedLength;
	}
	protected  void checkBuffer( Metro metro, JackClient client, JackPosition position ) throws JackException {
		synchronized ( this.buffers ) { // << ADDED synchronided (Sun, 30 Sep 2018 11:45:13 +0900)
//			if ( this.buffers.size() < BUFFER_SIZE ) {
//				this.offerNewBuffer( metro, client, position );
//			}
			while ( getAccumulatedLength() < MARGIN_LENGTH * MARGIN_LENGTH ) {
				this.offerNewBuffer( metro, client, position );
			}
		}
	}
	
	protected  void clearBuffer() {
		synchronized ( this.buffers ) {
			this.buffers.clear();
			this.cursor =0;
		}
	}
//	public void resetBuffer() {
//		synchronized ( this.buffers ) {
//			this.buffers.clear();
//			this.cursor =0;
//		}
//	}

	private void offerNewBuffer( Metro metro, JackClient client, JackPosition position ) throws JackException {
		synchronized ( this.buffers ) {
			if ( this.ending ) {
				MetroEventBuffer buf = new MetroEventBuffer();
				buf.exec( this.endingLength , new Runnable() {
					@Override
					public void run() {
						System.err.println( "UNREGISTER THIS" );
						metro.unregisterTrack( MetroTrack.this );
					}
				});
				buf.setLength( this.endingLength );
				buf.prepare( metro, client, position, true );
				this.buffers.offer( buf );
				
			} else {
				MetroEventBuffer buf = new MetroEventBuffer();
				boolean result = this.sequence.processBuffered( metro, this, buf );
				buf.prepare( metro, client, position, true );

				if ( DEBUG && ( buf.size() >0 ) )
					buf.dump();
				
				this.buffers.offer( buf );
				
				if ( result ) {
				} else {
					this.ending = true;
					this.endingLength = buf.getActualLength();
					if ( this.endingLength < 1 )
						this.endingLength = 1;
				}
				// buf.dump();
			}
		}
	}
	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof MetroTrack ) {
			return ((MetroTrack)obj).name == this.name;
		} else {
			return false;
		}
	}
	@Override
	public int hashCode() {
		return this.name.hashCode() * 2;
	}
} 