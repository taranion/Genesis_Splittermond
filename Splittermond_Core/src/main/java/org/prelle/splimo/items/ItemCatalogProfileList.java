package org.prelle.splimo.items;

import java.util.ArrayList;
import org.prelle.simplepersist.ElementList;
import org.prelle.simplepersist.Root;

@Root(name="equipment-profiles")
@ElementList(entry="profile", type=ItemCatalogProfile.class, inline=true)
public class ItemCatalogProfileList extends ArrayList<ItemCatalogProfile> {
    private static final long serialVersionUID = 1L;
}
