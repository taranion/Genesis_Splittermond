package org.prelle.splimo.items;

import org.prelle.simplepersist.Attribute;
import org.prelle.simplepersist.Root;

/** Included properties of a finished catalogue item, already covered by its price. */
@Root(name="profile")
public class ItemCatalogProfile {
    @Attribute(name="item", required=true)
    private String itemId;
    @Attribute(name="alchemyCost", required=false)
    private int alchemyCost;

    public ItemCatalogProfile() { }

    public ItemCatalogProfile(String itemId, int alchemyCost) {
        if (alchemyCost < 0) throw new IllegalArgumentException("Negative alchemy cost");
        this.itemId = itemId;
        this.alchemyCost = alchemyCost;
    }

    public String getItemId() { return itemId; }
    public int getAlchemyCost() { return alchemyCost; }

    /** One alchemical effect point is free; costs are not themselves item quality. */
    public int getItemQuality() { return Math.max(0, alchemyCost - 1); }
}
