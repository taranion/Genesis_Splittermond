package org.prelle.splimo.items;

import java.util.ArrayList;
import org.prelle.simplepersist.ElementList;
import org.prelle.simplepersist.Root;

@Root(name="equipment-bonuses")
@ElementList(entry="bonus", type=ItemSpellBonus.class, inline=true)
public class ItemSpellBonusList extends ArrayList<ItemSpellBonus> {
    private static final long serialVersionUID = 1L;
}
