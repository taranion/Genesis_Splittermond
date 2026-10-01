package org.prelle.rpgframework.splittermond.data;

import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.*;
import org.prelle.simplepersist.Persister;
import org.prelle.splimo.*;
import org.prelle.splimo.items.*;

/** Independent catalogue inventory from HdS pp. 36-44; also runs on legacy Core. */
public class SchleierCompletenessTest {
    private static final String ROOT = "/org/prelle/rpgframework/splittermond/data/schleier/";
    private static final String[] MASTERS = {
        "arcanelore/spiritlore1/1", "arcanelore/spiritlore2/2", "diplomacy/spiritdiplomat/1",
        "empathy/spiritempathy1/1", "empathy/spiritempathy2/2", "empathy/spiritworldsense1/1", "empathy/spiritworldsense2/2",
        "determination/steadfastsoul1/1", "determination/steadfastsoul2/3", "determination/undaunted/1",
        "determination/willform/1", "determination/willweapon/2", "determination/willarmor/3",
        "perception/apartbutunited/2", "combatmagic/spiritfoe_dead/2", "combatmagic/spiritfoe_nature/2",
        "combatmagic/spiritfoe_civilization/2", "deathmagic/powerofwill1/1", "deathmagic/powerofwill2/2",
        "deathmagic/powerofwill3/3", "deathmagic/medium1/1", "deathmagic/medium2/2", "deathmagic/onewithbeyond/4"
    };
    private static final String[] SPELLS = {"forced_return", "sense_foreign_control", "sense_spirit_anchor",
        "spirit_mimicry", "spirit_wall", "message_from_beyond", "otherworld_flame", "sever_spirit_bond", "invisible_to_spirits"};
    private static final String[] CONSUMABLES = {"nurghons_breath", "huularu_mango", "veil_moss", "spirit_lily",
        "sedative", "strong_sedative", "dream_resin", "pale_lily_extract", "spirit_incense", "soul_wax"};

    @BeforeClass public static void load() throws Exception {
        new SplittermondDataPlugin().init(p -> {});
        // Data-only contribution: production loader wiring is a separate change.
        // Exercise the loaders only when production has not already loaded these catalogues.
        PluginSkeleton plugin = new PluginSkeleton("Schleier", "Hinter dem Schleier");
        try (InputStream powers = SchleierCompletenessTest.class.getResourceAsStream(ROOT+"data/powers-schleier.xml");
                InputStream spells = SchleierCompletenessTest.class.getResourceAsStream(ROOT+"data/spells-schleier.xml")) {
            assertNotNull("Missing powers catalogue", powers);
            assertNotNull("Missing spells catalogue", spells);
            if (SplitterMondCore.getPower("oldsoul")==null)
                SplitterMondCore.loadPowers(plugin, powers, plugin.getResources(), plugin.getHelpResources());
            if (SplitterMondCore.getSpell("forced_return")==null)
                SplitterMondCore.loadSpells(plugin, spells, plugin.getResources(), plugin.getHelpResources());
        }
    }

    private Document xml(String file) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(file.equals("equipment-bonuses.xml") ? "/org/prelle/rpgframework/splittermond/data/equipment-bonuses-schleier.xml" : ROOT+"data/"+file)) {
            assertNotNull(file,in);
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        }
    }
    private Mastership master(String ref) {
        String[] parts=ref.split("/");
        Skill skill=SplitterMondCore.getSkill(parts[0]);
        assertNotNull(ref,skill);
        Mastership m=skill.getMastership(parts[1]);
        assertNotNull(ref,m);
        return m;
    }
    private void text(String kind,String id,int page) throws Exception {
        Properties labels=new Properties(),help=new Properties();
        try (Reader r=new InputStreamReader(getClass().getResourceAsStream(ROOT+"i18n/schleier.properties"),"UTF-8")) { labels.load(r); }
        try (Reader r=new InputStreamReader(getClass().getResourceAsStream(ROOT+"i18n/schleier-help.properties"),"UTF-8")) { help.load(r); }
        assertNotNull(kind+" "+id,labels.getProperty(kind+"."+id));
        assertEquals(String.valueOf(page),labels.getProperty(kind+"."+id+".page"));
        assertTrue(id,help.getProperty(kind+"."+id+("spell".equals(kind)?".descr":".desc"),"").length()>20);
    }

    @Test public void allMasteryOptionsHaveCorrectLevelsSourcesAndNoPermanentModifiers() throws Exception {
        assertEquals(23,xml("masterships-schleier.xml").getElementsByTagName("mastership").getLength());
        for (String ref : MASTERS) {
            Mastership m=master(ref);
            assertEquals(ref,Integer.parseInt(ref.split("/")[2]),m.getLevel());
            assertEquals("Schleier",m.getPlugin().getID());
            assertTrue(ref,m.getModifications().isEmpty());
            assertFalse(m.getName().startsWith("mastership."));
            assertTrue(m.getShortDescription().length()>10);
            assertTrue(m.getHelpResourceBundle().containsKey(m.getHelpI18NKey()));
        }
    }

    @Test public void allPrerequisitesResolveAndCrossSkillPrerequisitesAreRetained() throws Exception {
        Document doc=xml("masterships-schleier.xml");
        NodeList reqs=doc.getElementsByTagName("masterreq");
        assertEquals(14,reqs.getLength());
        for (int i=0;i<reqs.getLength();i++) master(((Element)reqs.item(i)).getAttribute("ref"));
        assertEquals(2,master("deathmagic/onewithbeyond").getPrerequisites().size());
        for (String kind : new String[]{"dead","nature","civilization"})
            assertEquals(1,master("combatmagic/spiritfoe_"+kind).getPrerequisites().size());
    }

    @Test public void oldSoulCostsTwoAndHasNoPermanentBonus() throws Exception {
        Power power=SplitterMondCore.getPower("oldsoul");
        assertNotNull(power);
        assertEquals(2,power.getCost());
        assertTrue(power.getModifications().isEmpty());
        assertEquals(Power.SelectionType.ALWAYS,power.getSelectable());
        assertTrue(power.getDescription().contains("Splitterpunkteinsatz"));
        text("power","oldsoul",36);
    }

    @Test public void allNewSpellsAreUniqueAndHaveValidSchoolReferences() throws Exception {
        assertEquals(9,xml("spells-schleier.xml").getElementsByTagName("spell").getLength());
        for (String id : SPELLS) {
            Spell spell=SplitterMondCore.getSpell(id);
            assertNotNull(id,spell);
            assertEquals("Schleier",spell.getPlugin().getID());
            assertEquals(1,SplitterMondCore.getSpells().stream().filter(s -> id.equals(s.getId())).count());
            for (SpellSchoolEntry school : spell.getSchools()) {
                assertNotNull(id,school.getSchool());
                assertTrue(school.getLevel()>=2 && school.getLevel()<=4);
            }
            text("spell",id,spell.getPage());
        }
    }

    @Test public void spellNumbersAndTypesMatchTheBook() throws Exception {
        int[] difficulty={24,21,21,27,24,21,21,27,24};
        String[] cost={"12V3","8V2","K8V2","K16V4","K12V3","8V2","8V2","16V4","K12V3"};
        int[] duration={20,8,12,14,12,-1,9,-1,12};
        int[] range={-1,5,0,0,5,5000,10,-1,0};
        for (int i=0;i<SPELLS.length;i++) {
            Spell s=SplitterMondCore.getSpell(SPELLS[i]);
            assertEquals(SPELLS[i],difficulty[i],s.getDifficulty());
            assertEquals(SPELLS[i],cost[i],new org.prelle.splimo.persist.SpellCostConverter().write(s.getCost()));
            assertEquals(SPELLS[i],duration[i],s.getCastDurationTicks());
            assertEquals(SPELLS[i],range[i],s.getCastRange());
        }
        assertEquals(Arrays.asList(SpellType.OTHERWORLD,SpellType.MESSAGE),SplitterMondCore.getSpell("message_from_beyond").getTypes());
        assertEquals(Arrays.asList(SpellType.WEAKEN,SpellType.EXORCISE),SplitterMondCore.getSpell("sever_spirit_bond").getTypes());
        assertEquals(3,SplitterMondCore.getSpell("sense_spirit_anchor").getSchools().size());
    }

    @Test public void consumablesHaveRequestedCategoryAndBookPrices() throws Exception {
        int[] prices={200,4,1,400,1000,1500,1500,2500,3,500};
        for (int i=0;i<CONSUMABLES.length;i++) {
            ItemTemplate item=SplitterMondCore.getItem(CONSUMABLES[i]);
            assertNotNull(CONSUMABLES[i],item);
            assertTrue(item.isType(ItemType.POTION));
            assertEquals(prices[i],item.getPrice());
            text("item",item.getId(),42);
        }
    }

    @Test public void remainingBooksHavePricesAndBonusesWhileCompendiaAreDeferred() throws Exception {
        String[] ids={"book_spirit_travel","book_spirit_summon_banish","book_spirit_almanac","book_zhelom_travels"};
        int[] prices={3000,4000,2000,15000};
        for (int i=0;i<ids.length;i++) assertBook(ids[i],prices[i]);
        int books=0;
        NodeList items=xml("equipment-schleier.xml").getElementsByTagName("item");
        assertEquals(27,items.getLength());
        for (int i=0;i<items.getLength();i++) {
            String id=((Element)items.item(i)).getAttribute("id");
            assertFalse(id,id.startsWith("book_death_compendium_"));
            if (id.startsWith("book_")) { books++; assertTrue(id,Arrays.asList(ids).contains(id)); }
        }
        assertEquals(4,books);
        for (SpellType type : SpellType.values())
            assertNull(SplitterMondCore.getItem("book_death_compendium_"+type.name().toLowerCase(Locale.ROOT)));
        NodeList bonuses=xml("equipment-bonuses.xml").getElementsByTagName("bonus");
        assertEquals(2,bonuses.getLength());
        for (int i=0;i<bonuses.getLength();i++) {
            Element bonus=(Element)bonuses.item(i);
            assertNotNull(SplitterMondCore.getItem(bonus.getAttribute("item")));
            assertNotNull(SplitterMondCore.getSkill(bonus.getAttribute("skill")));
            NodeList types=bonus.getElementsByTagName("type");
            for (int j=0;j<types.getLength();j++) assertNotNull(SpellType.valueOf(types.item(j).getTextContent()));
        }
    }
    private void assertBook(String id,int price) throws Exception {
        ItemTemplate item=SplitterMondCore.getItem(id);
        assertNotNull(id,item);
        assertEquals(price,item.getPrice()); assertEquals(2,item.getLoad()); assertEquals(1,item.getRigidity());
        assertTrue(item.isType(ItemType.OTHER)); assertNotNull(item.getSkill()); text("item",id,41);
    }

    @Test public void artifactVariantsHaveDistinctPrices() {
        String[] ids={"portable_spirit_circle","portable_spirit_circle_improved","portable_spirit_circle_charged",
            "spirit_salt_circle","spirit_salt_circle_improved","spirit_salt_shield","spirit_salt_shield_improved",
            "spirit_salt_close","spirit_salt_close_improved"};
        int[] prices={9000,15000,21000,1800,3600,1200,2400,1800,3600};
        for (int i=0;i<ids.length;i++) assertEquals(ids[i],prices[i],SplitterMondCore.getItem(ids[i]).getPrice());
    }

    @Test public void allNewSpellAndItemReferencesSurviveSaving() throws Exception {
        Persister p=new Persister();
        for (String id : SPELLS) {
            Spell spell=SplitterMondCore.getSpell(id);
            SpellValue original=new SpellValue(spell,spell.getSchools().get(0).getSchool());
            StringWriter out=new StringWriter(); p.write(original,out);
            SpellValue restored=p.read(SpellValue.class,new StringReader(out.toString()));
            assertSame(spell,restored.getSpell()); assertSame(original.getSkill(),restored.getSkill());
        }
        NodeList items=xml("equipment-schleier.xml").getElementsByTagName("item");
        for (int i=0;i<items.getLength();i++) {
            String id=((Element)items.item(i)).getAttribute("id");
            CarriedItem original=new CarriedItem(SplitterMondCore.getItem(id));
            StringWriter out=new StringWriter(); p.write(original,out);
            CarriedItem restored=p.read(CarriedItem.class,new StringReader(out.toString()));
            assertSame(id,original.getItem(),restored.getItem());
        }
    }

    @Test public void ghostSilkHasMinimumQualityFive() {
        assertEquals(5, SplitterMondCore.getMaterial("ghostsilk").getQuality());
    }

    @Test public void optionalCatalogueProfilesHaveUniqueValidItemsAndDeclaredCosts() throws Exception {
        String file = "/org/prelle/rpgframework/splittermond/data/equipment-profiles-schleier.xml";
        try (InputStream in = getClass().getResourceAsStream(file)) {
            assertNotNull(file, in);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            NodeList profiles = doc.getElementsByTagName("profile");
            assertEquals(16, profiles.getLength());
            Set<String> seen = new HashSet<>();
            Map<String,Integer> costs = new HashMap<>();
            costs.put("sedative", 2); costs.put("soul_wax", 2);
            costs.put("strong_sedative", 3); costs.put("dream_resin", 3);
            costs.put("pale_lily_extract", 3);
            for (int i=0; i<profiles.getLength(); i++) {
                Element profile = (Element)profiles.item(i);
                String id = profile.getAttribute("item");
                assertTrue("Duplicate catalogue profile: "+id, seen.add(id));
                assertNotNull("Unknown item: "+id, SplitterMondCore.getItem(id));
                int cost = profile.hasAttribute("alchemyCost") ? Integer.parseInt(profile.getAttribute("alchemyCost")) : 0;
                assertEquals(id, costs.getOrDefault(id, 0).intValue(), cost);
            }
            assertTrue(seen.containsAll(costs.keySet()));
        }
    }
}
