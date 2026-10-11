package org.halim.ontology.incubation.acse_beta.catalogs.calculated;

import org.halim.ontology.incubation.acse_beta.ACSE_ArityCatalogRowBase;

/**
 * A file path per identity, calculated from its eight hex digits {@code abcdefgh} as
 * {@code <path>ceg/abdfh<extension>}, so nothing is stored and nothing grows. Every other digit of the
 * last six names the folder, so consecutive identities spread over 4096 folders of at most 16^5 files
 * each. The path and the extension are fixed for the row's life: changing either moves every file,
 * which is replacing the row, not editing it.
 * */
public class FilePathCatalogRow implements ACSE_ArityCatalogRowBase {

public final String path;
public final String extension;

public FilePathCatalogRow(String path, String extension) {
	this.path = path;
	this.extension = extension;
}

@Override
public Object getObjectFor(long identity) {
	if(identity < 0 || identity > 0xFFFFFFFFL) { throw new ArithmeticException(identity + " does not fit 32 bits"); }
	String hex = String.format("%08x", identity);
	return path + hex.charAt(2) + hex.charAt(4) + hex.charAt(6) + '/'
		+ hex.charAt(0) + hex.charAt(1) + hex.charAt(3) + hex.charAt(5) + hex.charAt(7) + extension;
}

@Override
public double getEstimatedBytesPerIdentity() { return 0; }

@Override
public int getPreferredType() { return PREFERRED_OBJECT; }

@Override
public boolean ensureCapacity(long identity) { return false; }

@Override
public void truncate(long capacity) { }

}
