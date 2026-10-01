package org.prelle.splimo.items;

import java.util.ArrayList;
import java.util.List;
import org.prelle.simplepersist.AttribConvert;
import org.prelle.simplepersist.Attribute;
import org.prelle.simplepersist.ElementList;
import org.prelle.splimo.Skill;
import org.prelle.splimo.SpellType;
import org.prelle.splimo.SpellValue;
import org.prelle.splimo.persist.SkillConverter;

/** Optional catalogue metadata, kept outside the legacy item XML format. */
public class ItemSpellBonus {
    @Attribute private String item;
    @Attribute @AttribConvert(SkillConverter.class) private Skill skill;
    @Attribute private int value;
    @Attribute(required=false) private String group;
    @ElementList(entry="type", type=SpellType.class, inline=true)
    private List<SpellType> types = new ArrayList<>();

    public String getItemId() { return item; }
    public Skill getSkill() { return skill; }
    public int getValue() { return value; }
    public String getGroup() { return group; }
    public List<SpellType> getTypes() { return java.util.Collections.unmodifiableList(types); }

    public boolean matches(SpellValue spell) {
        return spell!=null && skill!=null && skill.equals(spell.getSkill())
                && types.stream().anyMatch(spell.getSpell().getTypes()::contains);
    }
}
