/**
 * 
 */
package org.prelle.splimo;

import java.util.ArrayList;
import java.util.List;

import org.prelle.simplepersist.AttribConvert;
import org.prelle.simplepersist.Attribute;
import org.prelle.simplepersist.ElementList;
import org.prelle.simplepersist.Root;
import org.prelle.splimo.modifications.ModificationSource;
import org.prelle.splimo.modifications.SkillModification;
import org.prelle.splimo.persist.SkillConverter;

import de.rpgframework.genericrpg.NumericalValue;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ModifyableImpl;

/**
 * @author prelle
 *
 */
@Root(name = "skillval")
public class SkillValue extends ModifyableImpl implements Comparable<SkillValue>, NumericalValue<Skill> {


	@Attribute(name="skill")
	@AttribConvert(SkillConverter.class)
	private Skill skill;
	/**
	 * The current value on the skill without the attributes
	 */
	@Attribute(name="val")
	private int value;
	@ElementList(entry="masterref",type=MastershipReference.class,inline=true)
	private List<MastershipReference> masterships;
	
	private transient int modifierCap;

	//-------------------------------------------------------------------
	public SkillValue() {
		masterships     = new ArrayList<MastershipReference>();
	}

	//-------------------------------------------------------------------
	public SkillValue(Skill skill, int val) {
		this.skill = skill;
		this.value = val;
		masterships = new ArrayList<MastershipReference>();
	}

	//-------------------------------------------------------------------
	public SkillValue(SkillValue toClone) {
		this.skill = toClone.getSkill();
		this.value = toClone.getValue();
		modifications.addAll(toClone.getModifications());
		masterships = new ArrayList<MastershipReference>(toClone.getMasterships());
	}

	//-------------------------------------------------------------------
	public String toString() {
		return String.format("%s = %s",
				String.valueOf(skill),
				value+"+"+getModifier()
				);
		//		return skill+"="+value+" ("+masterships+")";
	}

	//-------------------------------------------------------------------
	public String getName() {
		if (skill==null) return "?";
		return skill.getName();
	}

	//-------------------------------------------------------------------
	public boolean equals(Object o) {
		if (o instanceof SkillValue) {
			SkillValue other = (SkillValue)o;
			if (skill!=other.getSkill()) return false;
			if (value!=other.getValue()) return false;
			return masterships.equals(other.getMasterships());
		}
		return false;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the skill
	 */
	public Skill getSkill() {
		return skill;
	}

	//-------------------------------------------------------------------
	/**
	 * @param skill the skill to set
	 */
	public void setSkill(Skill skill) {
		this.skill = skill;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the value
	 */
	public int getValue() {
		return value;
	}

	//-------------------------------------------------------------------
	/**
	 * Returns the distributed points plus an equipment modifier
	 */
	public int getModifiedValue() {
		return value + getModifier();
	}

	//-------------------------------------------------------------------
	/**
	 * @param value the value to set
	 */
	public void setValue(int value) {
		this.value = value;
	}

	//--------------------------------------------------------------------
	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SkillValue other) {
		if (skill==null || other.getSkill()==null) return 0;
		return skill.compareTo(other.getSkill());
	}

	//-------------------------------------------------------------------
	/**
	 * @return the masterships
	 */
	public List<MastershipReference> getMasterships() {
		return masterships;
	}

	//-------------------------------------------------------------------
	public boolean hasMastership(Mastership master) {
		for (MastershipReference ref : masterships)
			if (ref.getMastership()==master)
				return true;
		return false;
	}

	//-------------------------------------------------------------------
	/**
	 * @param masterships the masterships to set
	 */
	public void addMastership(MastershipReference mastership) {
//		if (!masterships.contains(mastership))
			masterships.add(mastership);
	}

	//-------------------------------------------------------------------
	public void removeMastership(Mastership mastership) {
		for (MastershipReference ref : masterships)
			if (ref.getMastership()==mastership) {
				masterships.remove(ref);
				return;
			}
	}

	//-------------------------------------------------------------------
	public void removeSpecialization(SkillSpecialization special) {
		for (MastershipReference ref : masterships)
			if (ref.getSpecialization()!=null && ref.getSpecialization().getSpecial()==special) {
				masterships.remove(ref);
				return;
			}
	}

	//-------------------------------------------------------------------
	public int getSpecializationLevel(SkillSpecialization special) {
		if (special==null)
			throw new NullPointerException("SkillSpecialization is null");
		for (MastershipReference tmp : masterships) {
			if (tmp.getSpecialization()==null)
				continue;
			SkillSpecializationValue tmpSpec = tmp.getSpecialization();
			if (tmpSpec.getSpecial()==null)
				throw new NullPointerException("Missing specialization in "+tmpSpec);
//			switch (special.getType()) {
//			case NORMAL:
//			}
			if (tmpSpec.getSpecial().getId().equals(special.getId()))
				return tmpSpec.getLevel();
		}
		
		return 0;
	}

	//-------------------------------------------------------------------
	public MastershipReference setSpecializationLevel(SkillSpecialization special, int level) {
		// Search for old data
		for (MastershipReference tmp : masterships) {
			if (tmp.getSpecialization()==null)
				continue;
			SkillSpecializationValue tmpSpec = tmp.getSpecialization();
			if (tmpSpec.getSpecial().getId().equals(special.getId())) {
				tmpSpec.setLevel(level);
				return tmp;
			}
				
		}

		MastershipReference ref = new MastershipReference(special, level); 
		masterships.add(ref);
		return ref;
	}

	//-------------------------------------------------------------------
	public int getModifier() {
		int count = 0;
		int countEquip = 0;
		int countMagic = 0;
		int penalties = 0;
		for (Modification mod : modifications) {
			if (mod instanceof SkillModification) {
				SkillModification sMod = (SkillModification)mod;
				if (sMod.isConditional())
					continue;
				if (sMod.getSkill()==skill) {
					if (sMod.getValue()<0 && (sMod.getModificationSource()==ModificationSource.EQUIPMENT
							|| sMod.getModificationSource()==ModificationSource.MAGICAL))
						penalties += sMod.getValue();
					else if (sMod.getModificationSource()==ModificationSource.EQUIPMENT)
						countEquip += sMod.getValue();
					else if (sMod.getModificationSource()==ModificationSource.MAGICAL)
						countMagic += sMod.getValue();
					else
						count += sMod.getValue();
				}
			}
		}
		
		if (modifierCap>0) {
			count += Math.min(countMagic, modifierCap);
			count += Math.min(countEquip, modifierCap);
		} else {
			count += countMagic;
			count += countEquip;
		}

		return count + penalties;
	}

	/** Positive equipment bonuses already occupying the shared bonus cap. */
	public int getPositiveEquipmentModifier() {
		int positive = 0;
		for (Modification mod : modifications) {
			if (mod instanceof SkillModification) {
				SkillModification value = (SkillModification)mod;
				if (!value.isConditional() && value.getSkill()==skill && value.getValue()>0
						&& value.getModificationSource()==ModificationSource.EQUIPMENT)
					positive += value.getValue();
			}
		}
		return positive;
	}

	public int getModifierCap() { return modifierCap; }

	//-------------------------------------------------------------------
	public void setModifierCap(int modifierCap) {
		this.modifierCap = modifierCap;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.SelectedValue#getModifyable()
	 */
	@Override
	public org.prelle.splimo.Skill getModifyable() {
		return skill;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.NumericalValue#getPoints()
	 */
	@Override
	public int getPoints() {
		return getValue();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.NumericalValue#setPoints(int)
	 */
	@Override
	public void setPoints(int points) {
		setValue(points);
	}

}
