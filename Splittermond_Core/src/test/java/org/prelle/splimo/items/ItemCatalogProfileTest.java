package org.prelle.splimo.items;

import static org.junit.Assert.*;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.Test;
import org.prelle.simplepersist.Persister;
import org.prelle.splimo.items.Enhancement.EnhancementType;

public class ItemCatalogProfileTest {
    private CarriedItem item(Integer cost) {
        ItemTemplate template = new ItemTemplate("finished-test");
        template.addItemType(ItemType.POTION);
        if (cost!=null) template.setCatalogProfile(new ItemCatalogProfile(template.getId(), cost));
        return new CarriedItem(template);
    }

    private void enhance(CarriedItem item, int cost, EnhancementType type) {
        item.addEnhancement(new EnhancementReference(new Enhancement("test-"+type, cost, type)));
    }

    @Test public void catalogueCostIncludesExactlyOneFreePoint() {
        CarriedItem item = item(2);
        assertEquals(2, item.getQuality(EnhancementType.ALCHEMY));
        assertEquals(1, item.getItemQuality());
        assertEquals(1, item.getTotalQuality());
        enhance(item, 2, EnhancementType.ALCHEMY);
        assertEquals(4, item.getQuality(EnhancementType.ALCHEMY));
        assertEquals(3, item.getItemQuality());
        enhance(item, 1, EnhancementType.MAGIC);
        assertEquals(1, item.getArtifactQuality());
        assertEquals(4, item.getTotalQuality());
    }

    @Test public void finishedNonAlchemicalItemHasNeutralQuality() {
        CarriedItem item = item(0);
        assertEquals(0, item.getItemQuality());
        assertEquals(0, item.getTotalQuality());
        enhance(item, 2, EnhancementType.ALCHEMY);
        assertEquals(1, item.getItemQuality());
    }

    @Test public void existingGenericRecipeBudgetIsUnchanged() {
        CarriedItem item = item(null);
        assertEquals(-1, item.getItemQuality());
        enhance(item, 2, EnhancementType.ALCHEMY);
        assertEquals(1, item.getItemQuality());
        assertEquals(2, item.getQuality(EnhancementType.ALCHEMY));
    }

    @Test public void sidecarReadsWithoutChangingLegacyItemXml() throws Exception {
        Persister persist = new Persister();
        ItemCatalogProfileList list = persist.read(ItemCatalogProfileList.class,
                new StringReader("<equipment-profiles><profile item=\"sample\" alchemyCost=\"3\" /></equipment-profiles>"));
        assertEquals(2, list.get(0).getItemQuality());
        ItemTemplate template = item(2).getItem();
        StringWriter before = new StringWriter(), after = new StringWriter();
        persist.write(template, before);
        template.setCatalogProfile(null);
        persist.write(template, after);
        assertEquals(after.toString(), before.toString());
    }
}
