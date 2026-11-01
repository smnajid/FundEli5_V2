package com.fundgate.fundeli5.shareclasses;

import com.fundgate.fundeli5.bankdata.FundDataUnavailable;
import java.util.List;

/** Out port (ADR 0008): the Share Class catalog. */
public interface ShareClasses {

    /**
     * Every Share Class the bank distributes. There is no lookup by ISIN: an ISIN outside this list
     * is a fund the bank does not distribute.
     *
     * @throws FundDataUnavailable when the fund master data cannot be read
     */
    List<ShareClass> all();
}
