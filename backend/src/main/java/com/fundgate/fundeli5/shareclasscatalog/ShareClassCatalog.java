package com.fundgate.fundeli5.shareclasscatalog;

import com.fundgate.fundeli5.shareclasses.ShareClass;
import java.util.List;

/** Maps a fund or class mention, or an ISIN, to Share Classes; a fund named without a class gives all its classes. */
public interface ShareClassCatalog {

    /**
     * The Share Classes a free-text mention names. Empty when the mention names nothing the bank
     * distributes (an ISIN outside the catalog is a fund the bank does not distribute).
     */
    List<ShareClass> resolve(String mention);

    /**
     * Narrows already computed rows, deterministically: case, accents and punctuation are ignored, every
     * typed word must appear in any order, and an ISIN matches exactly or by prefix. An empty query keeps
     * every row.
     */
    List<ShareClass> search(String query, List<ShareClass> rows);
}
