package ats.pulsar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ats.metro.Metro;
import ats.metro.MetroInvokable;
import ats.metro.MetroLogic;
import ats.metro.MetroMidiEvent;
import ats.metro.MetroNoteEventBuffer;
import ats.metro.MetroNoteEventBufferSequence;
import ats.metro.MetroPlayer;
import gnu.lists.AbstractSequence;
import gnu.lists.EmptyList;
import gnu.lists.ImmutablePair;
import gnu.lists.LList;
import gnu.lists.Pair;
import gnu.mapping.ProcedureN;
import gnu.mapping.Symbol;
import kawa.standard.Scheme;

public class SchemePulsarLogic extends MetroLogic {
	static final Logger LOGGER = Logger.getLogger(SchemePulsarLogic.class.getName());
//
//	static final String ID_TYPE      = "type";
//	static final String ID_ENABLED   = "enab";
//	static final String ID_CHANNEL   = "chan";
//	static final String ID_PORT_NO   = "port";
//	static final String ID_PROCEDURE = "proc";
//	static final String ID_LENGTH    = "len";
//	static final String ID_VELOCITY  = "velo";
//	static final String ID_NOTE      = "note";
//	static final String ID_OFFSET    = "pos";
//	static final String ID_KEY       = "key";
//	static final String ID_MIN       = "min";
//	static final String ID_MAX       = "max";
//	static final String ID_VALUE     = "val";
//	private static final Object ID_HUMANIZE  = "huma";
	
	/*
	 * (list
	 *     (cons 1.0  (lambda() ... ))
	 *     (cons 2.0  (lambda() ... ))
	 *     (cons 2.5  (lambda() ... )))
	 */
	static ArrayList<Pair> parseListOfPairs( Pair pair ) {
		ArrayList<Pair> pairs = new ArrayList<Pair>();
		for ( Object pp : pair ) {
			pairs.add( (Pair)pp ); 
		}
		return pairs;
	}

	/*
	 * Note (XXX_SYNC_01):
	 * Basically this class is not Scheme dependent. However only exec() method
	 * needs to refer scheme object to synchronize with the scheme execution in
	 * Pulse class. In the current policy, it is preferable to avoid referring the
	 * scheme object by wrapping invocation. But in exec() method-wise, referring
	 * to the scheme object here is not avoidable.
	 */
	final Scheme scheme;
	
	final MetroInvokable procedure;
	public SchemePulsarLogic ( Scheme scheme, MetroInvokable procedure ) {
		this.scheme = scheme;
		this.procedure = procedure;
	}
	
	static void logError( String msg, Throwable e ) {
        LOGGER.log(Level.SEVERE, msg, e);
	}
	static void logInfo( String msg ) {
//        Logger.getLogger(SchemePulsarLogic.class.getName()).log(Level.INFO, msg);
		System.err.println( msg );
	}

	@Override
	public void processInputMidiBuffer(Metro metro, List<MetroMidiEvent> in, List<MetroMidiEvent> out) {
		// out.addAll( in ); TODO ******************************
		System.err.println( "in.size()" + in.size());
		System.err.println( "out.size()" + out.size());
	}


	@Override
	public boolean processOutputNoteBuffer( Metro metro, MetroNoteEventBufferSequence sequence, MetroNoteEventBuffer buf ) {
		// System.out.println("Metro.logic.new MetroLogic() {...}.initBuffer()" );
//		buf.humanize( 0.0d, 3 );
		boolean result = false;
		try {
			result = scheme2buf(metro, sequence, scheme, procedure, buf);
		} catch ( RuntimeException e ) {
			LOGGER.log(Level.SEVERE, "", e);
		}
		return result;
	}

	public static boolean scheme2buf( Metro metro, MetroNoteEventBufferSequence sequence, Scheme scheme, MetroInvokable procedure, MetroNoteEventBuffer buf) {
		AbstractSequence<Object> pattern = (AbstractSequence<Object>)procedure.invoke();

		/*
		class Processor {
		}
		class NoteProcessor extends Processor {
		}
		class State {
			Processor processor = null;
		}
		State state = new State();
		*/
	
		return SchemeNoteParser.parse(scheme, pattern, buf, true );
	}

	LList asociationList = createPairs( this );
	public LList getAsociationList() {
		return asociationList;
	}


	public static LList createPairs( MetroPlayer player ) {
		Pair[] pairs = {
				new ImmutablePair() {
					@Override
					public Object getCar() {
						return SchemeUtils.schemeSymbol("name");
					}
					@Override
					public Object getCdr() {
						return SchemeUtils.toSchemeSymbol( player.getPlayerName() );
					}
				},
				new ImmutablePair() {
					@Override
					public Object getCar() {
						return SchemeUtils.schemeSymbol("tags");
					}
					@Override
					public Object getCdr() {
						return LList.makeList((List)
								SchemeUtils.<String,Symbol>convertList(
										player.getPlayerTags() , (v)->SchemeUtils.toSchemeSymbol(v) ) );
					}
				},
				new ImmutablePair() {
					@Override
					public Object getCar() {
						return SchemeUtils.schemeSymbol("playing");
					}
					@Override
					public Object getCdr() {
						return new ProcedureN() {
							public Object applyN(Object[] args) throws Throwable {
								if ( 0 < args.length ) {
									player.setPlayerEnabled((Boolean)args[0]);
								}
								return player.isPlayerEnabled();
							};
						};
					}
				},
				new ImmutablePair() {
					@Override
					public Object getCar() {
						return SchemeUtils.schemeSymbol( "remove" );
					}
					@Override
					public Object getCdr() {
						return new ProcedureN() {
							public Object applyN(Object[] args) throws Throwable {
								player.playerRemove( SchemeUtils.toBoolean( args[0]) );
								return EmptyList.emptyList;
							};
						};
					}
				},
				new ImmutablePair() {
					@Override
					public Object getCar() {
						return SchemeUtils.schemeSymbol( "position" );
					}
					@Override
					public Object getCdr() {
						return new ProcedureN() {
							public Object applyN(Object[] args) throws Throwable {
								return SchemeUtils.toSchemeNumber( player.getPosition() );
							};
						};
					}
				},
		};
		return LList.makeList( Arrays.asList( pairs ) );
	}
}