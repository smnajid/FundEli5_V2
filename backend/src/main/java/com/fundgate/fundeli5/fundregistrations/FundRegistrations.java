package com.fundgate.fundeli5.fundregistrations;

import com.fundgate.fundeli5.bankdata.Country;
import com.fundgate.fundeli5.bankdata.FundDataUnavailable;
import com.fundgate.fundeli5.bankdata.Isin;

/** Out port (ADR 0008): the bank's Fund Registration record. */
public interface FundRegistrations {

    /**
     * The bank's record for one Share Class in one country.
     *
     * @throws FundDataUnavailable when the fund master data cannot be read
     */
    Registration registration(Isin isin, Country country);
}
