package org.prelle.splimo.equip;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.prelle.rpgframework.splittermond.data.SplittermondDataPlugin;
import org.prelle.splimo.chargen.event.GenerationEventDispatcher;
import org.prelle.splimo.items.*;
import org.prelle.splimo.items.Enhancement.EnhancementType;

public class ItemCatalogPriceTest {
    @BeforeClass public static void load() { new SplittermondDataPlugin().init(p -> {}); }
    @After public void clearListeners() { GenerationEventDispatcher.clear(); }

    private CarriedItem item(int cost) {
        ItemTemplate template = new ItemTemplate("fixture-potion");
        template.addItemType(ItemType.POTION);
        template.setPrice(1000);
        template.setCatalogProfile(new ItemCatalogProfile(template.getId(), cost));
        return new CarriedItem(template);
    }

    @Test public void finishedCatalogueQualityIsAlreadyIncludedInThePrice() {
        for (int cost : new int[]{0, 1, 2, 3}) {
            CarriedItem item = item(cost);
            assertEquals(Math.max(0, cost-1), item.getItemQuality());
            assertEquals(1000, new ItemLevellerAndGenerator(item, 6).getPrice());
        }
    }

    @Test public void addedEnhancementsAreChargedSeparately() {
        for (EnhancementType type : new EnhancementType[]{EnhancementType.NORMAL, EnhancementType.MAGIC, EnhancementType.ALCHEMY}) {
            CarriedItem item = item(2);
            item.addEnhancement(new EnhancementReference(new Enhancement("fixture-extra", 1, type)));
            assertEquals(type.name(), 2500, new ItemLevellerAndGenerator(item, 6).getPrice());
        }
    }

    @Test public void ordinaryEquipmentPriceIsUnaffected() {
        ItemTemplate template = new ItemTemplate("fixture-ordinary"); template.setPrice(1250);
        CarriedItem item = new CarriedItem(template);
        assertEquals(1250, new ItemLevellerAndGenerator(item, 6).getPrice());
        item.addEnhancement(new EnhancementReference(new Enhancement("fixture-extra", 1, EnhancementType.NORMAL)));
        assertEquals(2750, new ItemLevellerAndGenerator(item, 6).getPrice());
    }
}
