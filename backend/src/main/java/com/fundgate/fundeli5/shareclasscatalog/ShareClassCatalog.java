package com.fundgate.fundeli5.shareclasscatalog;

import com.fundgate.fundeli5.shareclasses.ShareClass;
import java.util.List;

/** Maps a fund or class mention to Share Classes; a fund named without a class gives all its classes. */
public interface ShareClassCatalog {

    List<ShareClass> resolve(String mention);
}
