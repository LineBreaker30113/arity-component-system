package org.halim.acs.arity.catalog;

/**
 * Splits a long identity into a chunk and an int offset inside it, which is the only thing standing
 * between a long identity space and Java's int-indexed arrays. A catalog holds a list of blocks and
 * asks this where a value sits; it never spells the arithmetic out itself.
 * <p>
 * The reason this is an interface rather than two constants is that the split has to agree with
 * itself. A chunk index taken at one width and an offset masked at another put two distinct
 * identities on one seat, silently - the failure is a wrong read, not a crash. Written once behind
 * {@link #chunkOf} and {@link #offsetOf}, that disagreement has nowhere to live.
 * <p>
 * An implementation must satisfy, for every identity the catalog will address:
 * <ul>
 *   <li>{@link #identityOf}{@code (chunkOf(i), offsetOf(i)) == i} - the split round-trips;</li>
 *   <li>{@code offsetOf(i)} is in {@code [0, fullLength())} - an offset addresses its own chunk;</li>
 *   <li>{@code chunkOf} is non-decreasing in the identity, so ascending identities visit chunks in
 *       order and a catalog can walk them without sorting.</li>
 * </ul>
 * Stateless and immutable, so one instance is shared by every catalog that chunks the same way.
 * */
public interface LongChunkInt {

/** Which chunk the identity lives in. */
int chunkOf(long identity);

/** Where inside that chunk it sits. */
int offsetOf(long identity);

/** The inverse of the split. */
long identityOf(int chunk, int offset);

/** The length of a chunk that is not the last one. Every chunk below the tail is this long. */
int fullLength();

/** How many chunks it takes to address {@code lastIdentity}. Zero for an empty catalog (-1). */
default int chunkCount(long lastIdentity) {
	return lastIdentity < 0 ? 0 : chunkOf(lastIdentity) + 1;
}

/**
 * How long chunk {@code chunk} has to be for the catalog to address every identity up to
 * {@code lastIdentity}. Full for every chunk before the tail, and exactly as far as it reaches for
 * the tail itself - which is what keeps a small catalog from paying for a whole chunk.
 * */
default int lengthOf(int chunk, long lastIdentity) {
	if(lastIdentity < 0) { return 0; }
	int last = chunkOf(lastIdentity);
	if(chunk > last) { return 0; }
	return chunk < last ? fullLength() : offsetOf(lastIdentity) + 1;
}


/**
 * The split as a shift and a mask, which is the only reason to prefer a power of two here: no
 * division on a path that runs once per element read.
 * <p>
 * {@code bits} is the chunk length as a power of two. The catalogs default to 22, which is 4Mi
 * elements - 32 MiB of doubles, 16 MiB of floats - a block a machine can hand over without thinking
 * about it, and an identity space of 2^53, since the chunk index has to stay an int and that is what
 * bounds the reach. Raising it trades that easy allocation for reach: 30 carries 2^61 identities, at
 * 8 GiB the time a chunk fills.
 * */
class LongChunkIntPowerOfTwo implements LongChunkInt {
	
	public final int bits;
	public final int size;
	public final long mask;
	
	public LongChunkIntPowerOfTwo(int bits) {
		if(bits < 1 || bits > 30) { throw new IllegalArgumentException("chunk bits out of range: " + bits); }
		this.bits = bits;
		this.size = 1 << bits;
		this.mask = size - 1;
	}
	
	@Override
	public int chunkOf(long identity) {
		return Math.toIntExact(identity >>> bits);
	}
	
	@Override
	public int offsetOf(long identity) {
		return (int) (identity & mask);
	}
	
	@Override
	public long identityOf(int chunk, int offset) {
		return ((long) chunk << bits) | offset;
	}
	
	@Override
	public int fullLength() {
		return size;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[1 << " + bits + "]";
	}
}


/**
 * The split by division, for a chunk length that is not a power of two - a page size a store hands
 * down, or a length picked to fit a buffer rather than a shift.
 * <p>
 * Costs a division and a multiply per access where {@link LongChunkIntPowerOfTwo} costs a shift and
 * a mask. Worth it only when the length is genuinely not yours to choose.
 * */
class LongChunkIntUniform implements LongChunkInt {
	
	public final int size;
	
	public LongChunkIntUniform(int size) {
		if(size < 1) { throw new IllegalArgumentException("chunk size out of range: " + size); }
		this.size = size;
	}
	
	@Override
	public int chunkOf(long identity) {
		return Math.toIntExact(identity / size);
	}
	
	@Override
	public int offsetOf(long identity) {
		return (int) (identity % size);
	}
	
	@Override
	public long identityOf(int chunk, int offset) {
		return (long) chunk * size + offset;
	}
	
	@Override
	public int fullLength() {
		return size;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + size + "]";
	}
}


}
