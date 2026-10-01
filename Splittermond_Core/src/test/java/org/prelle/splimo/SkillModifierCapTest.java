package org.prelle.splimo;

import static org.junit.Assert.*;
import org.junit.Test;
import org.prelle.splimo.modifications.*;

public class SkillModifierCapTest {
    private final Skill skill = new Skill("cap-test", Skill.SkillType.NORMAL, Attribute.MIND, Attribute.INTUITION);
    private SkillModification mod(int amount, ModificationSource source) {
        SkillModification mod = new SkillModification(skill, amount);
        mod.setModificationSource(source);
        return mod;
    }

    @Test public void equipmentAndMagicAreCappedBeforeTheirPenalties() {
        SkillValue value = new SkillValue(skill, 0);
        value.setModifierCap(3);
        for (ModificationSource source : new ModificationSource[]{ModificationSource.EQUIPMENT, ModificationSource.MAGICAL}) {
            value.addModification(mod(4, source));
            value.addModification(mod(-2, source));
        }
        assertEquals(2, value.getModifier());
        value.setModifierCap(6); assertEquals(4, value.getModifier());
        value.setModifierCap(0); assertEquals(4, value.getModifier());
    }

    @Test public void conditionsAndUnrestrictedBonusesRemainSeparate() {
        SkillValue value = new SkillValue(skill, 0);
        value.setModifierCap(3);
        SkillModification conditional = mod(-10, ModificationSource.EQUIPMENT);
        conditional.setConditional(true);
        value.addModification(conditional);
        value.addModification(mod(4, ModificationSource.EQUIPMENT));
        value.addModification(new SkillModification(skill, 6));
        assertEquals(9, value.getModifier());
    }

    @Test public void allPenaltiesRemainEffectiveWithoutPositiveBonuses() {
        SkillValue value = new SkillValue(skill, 0);
        value.setModifierCap(3);
        value.addModification(mod(-5, ModificationSource.EQUIPMENT));
        value.addModification(mod(-4, ModificationSource.MAGICAL));
        assertEquals(-9, value.getModifier());
    }
}
