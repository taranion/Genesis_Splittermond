package org.prelle.rpgframework.splittermond.data;

import static org.junit.Assert.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.prelle.simplepersist.Persister;
import org.prelle.splimo.*;
import org.prelle.splimo.items.*;
import org.prelle.splimo.modifications.*;

/** Technical fixtures: no dependency on the new book catalogue. */
public class EquipmentSpellBonusTest {
    private final List<ItemTemplate> fixtures = new ArrayList<>();
    @BeforeClass public static void load() { new SplittermondDataPlugin().init(p -> {}); }
    @After public void clearFixtures() { SplitterMondCore.getItems().removeAll(fixtures); }

    private SpellValue spell(String school, SpellType... types) {
        Spell spell = new Spell("fixture-spell");
        for (SpellType type : types) spell.getTypes().add(type);
        return new SpellValue(spell, SplitterMondCore.getSkill(school));
    }

    private ItemTemplate template(String id, String group) throws Exception {
        String grouping = group==null ? "" : " group=\""+group+"\"";
        String xml = "<equipment-bonuses><bonus item=\""+id+"\" skill=\"deathmagic\" value=\"2\""+grouping+">"
                +"<type>CONJURATION</type><type>EXORCISE</type></bonus></equipment-bonuses>";
        ItemTemplate item = new ItemTemplate(id);
        item.setResourceBundle(new PropertyResourceBundle(new StringReader("item."+id+"=Fixture book\n")));
        item.addItemType(ItemType.OTHER);
        item.setSpellBonuses(new Persister().read(ItemSpellBonusList.class, new StringReader(xml)));
        SplitterMondCore.getItems().add(item); fixtures.add(item);
        return item;
    }

    private CarriedItem equip(SpliMoCharacter character, ItemTemplate template) {
        CarriedItem item = new CarriedItem(template);
        character.addItem(item); EquipmentTools.equip(character, item);
        return item;
    }

    private void modify(SpliMoCharacter character, SpellValue spell, int amount, ModificationSource source) {
        SkillModification mod = new SkillModification(spell.getSkill(), amount);
        mod.setModificationSource(source);
        character.getSkillValue(spell.getSkill()).addModification(mod);
    }

    @Test public void matchingSchoolAndAnyMatchingTypeGiveOneBonus() throws Exception {
        SpliMoCharacter character = new SpliMoCharacter();
        SpellValue spell = spell("deathmagic", SpellType.CONJURATION, SpellType.EXORCISE);
        int before = character.getSpellValueFor(spell);
        int school = character.getSkillValue(spell.getSkill()).getModifiedValue();
        equip(character, template("fixture-book", null));
        assertEquals(before+2, character.getSpellValueFor(spell));
        assertEquals(school, character.getSkillValue(spell.getSkill()).getModifiedValue());
        assertEquals(0, EquipmentTools.getSpellBonus(character, spell("illusionmagic", SpellType.CONJURATION)));
        assertEquals(0, EquipmentTools.getSpellBonus(character, spell("deathmagic", SpellType.SUMMON)));
    }

    @Test public void quantityAndDuplicateCopiesDoNotMultiplyTheBonus() throws Exception {
        SpliMoCharacter character = new SpliMoCharacter();
        ItemTemplate template = template("fixture-book", null);
        equip(character, template).setCount(5);
        equip(character, template);
        assertEquals(2, EquipmentTools.getSpellBonus(character, spell("deathmagic", SpellType.CONJURATION)));
    }

    @Test public void onlyPositiveCountItemsOnTheBodyApply() throws Exception {
        SpliMoCharacter character = new SpliMoCharacter();
        SpellValue spell = spell("deathmagic", SpellType.CONJURATION);
        CarriedItem item = equip(character, template("fixture-book", null));
        for (ItemLocationType location : ItemLocationType.values()) {
            item.setItemLocation(location);
            assertEquals(location.name(), location==ItemLocationType.BODY ? 2 : 0,
                    EquipmentTools.getSpellBonus(character, spell));
        }
        EquipmentTools.equip(character, item);
        item.setCount(0); assertEquals(0, EquipmentTools.getSpellBonus(character, spell));
        item.setCount(1); assertEquals(2, EquipmentTools.getSpellBonus(character, spell));
        EquipmentTools.unequip(character, item);
        assertEquals(0, EquipmentTools.getSpellBonus(character, spell));
        EquipmentTools.equip(character, item); character.removeItem(item);
        assertEquals(0, EquipmentTools.getSpellBonus(character, spell));
    }

    @Test public void distinctItemsShareEquipmentCapButPenaltiesDoNotFreeCapacity() throws Exception {
        SpliMoCharacter character = new SpliMoCharacter();
        SpellValue spell = spell("deathmagic", SpellType.CONJURATION);
        int base = character.getSpellValueFor(spell);
        character.getSkillValue(spell.getSkill()).setModifierCap(3);
        equip(character, template("fixture-first", null)); equip(character, template("fixture-second", null));
        assertEquals(base+3, character.getSpellValueFor(spell));
        modify(character, spell, 4, ModificationSource.EQUIPMENT);
        modify(character, spell, -2, ModificationSource.EQUIPMENT);
        assertEquals(base+1, character.getSpellValueFor(spell));
        character.getSkillValue(spell.getSkill()).setModifierCap(6);
        assertEquals(base+4, character.getSpellValueFor(spell));
    }

    @Test public void explicitGroupPreventsDifferentItemVariantsFromStacking() throws Exception {
        SpliMoCharacter character = new SpliMoCharacter();
        equip(character, template("fixture-first", "fixture-editions"));
        equip(character, template("fixture-second", "fixture-editions"));
        assertEquals(2, EquipmentTools.getSpellBonus(character, spell("deathmagic", SpellType.CONJURATION)));
    }

    @Test public void magicAndEquipmentUseSeparateCaps() throws Exception {
        SpliMoCharacter character = new SpliMoCharacter();
        SpellValue spell = spell("deathmagic", SpellType.CONJURATION);
        int base = character.getSpellValueFor(spell);
        character.getSkillValue(spell.getSkill()).setModifierCap(3);
        modify(character, spell, 4, ModificationSource.MAGICAL);
        modify(character, spell, -2, ModificationSource.MAGICAL);
        equip(character, template("fixture-book", null));
        assertEquals(base+3, character.getSpellValueFor(spell));
    }

    @Test public void spellValueIncludesExistingSkillModifiersWithoutEquipment() {
        SpliMoCharacter character = new SpliMoCharacter();
        SpellValue spell = spell("deathmagic", SpellType.CONJURATION);
        int base = character.getSpellValueFor(spell);
        modify(character, spell, 2, ModificationSource.EQUIPMENT);
        assertEquals(base+2, character.getSpellValueFor(spell));
        SkillModification conditional = new SkillModification(spell.getSkill(), 10);
        conditional.setConditional(true);
        character.getSkillValue(spell.getSkill()).addModification(conditional);
        assertEquals(base+2, character.getSpellValueFor(spell));
    }

    @Test public void savedCharacterRetainsItemsWithoutIncreasingLearnedSkill() throws Exception {
        SpliMoCharacter character = new SpliMoCharacter();
        SpellValue spell = spell("deathmagic", SpellType.CONJURATION);
        character.getSkillValue(spell.getSkill()).setValue(6);
        equip(character, template("fixture-book", null));
        StringWriter out = new StringWriter(); new Persister().write(character, out);
        SpliMoCharacter restored = new Persister().read(SpliMoCharacter.class, new StringReader(out.toString()));
        assertEquals(6, restored.getSkillValue(spell.getSkill()).getValue());
        assertEquals(2, EquipmentTools.getSpellBonus(restored, spell));
        assertEquals(character.getSpellValueFor(spell), restored.getSpellValueFor(spell));
    }

    @Test public void metadataDoesNotChangeLegacyTemplateXml() throws Exception {
        ItemTemplate template = template("fixture-book", null);
        Persister persister = new Persister();
        StringWriter with = new StringWriter(), without = new StringWriter();
        persister.write(template, with);
        template.setSpellBonuses(new ArrayList<>());
        persister.write(template, without);
        assertEquals(without.toString(), with.toString());
    }
}
