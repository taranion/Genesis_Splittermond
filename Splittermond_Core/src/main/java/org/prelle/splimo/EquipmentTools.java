/**
 * 
 */
package org.prelle.splimo;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prelle.splimo.Skill.SkillType;
import org.prelle.splimo.SkillSpecialization.SkillSpecializationType;
import org.prelle.splimo.items.Armor;
import org.prelle.splimo.items.CarriedItem;
import org.prelle.splimo.items.EnhancementReference;
import org.prelle.splimo.items.ItemAttribute;
import org.prelle.splimo.items.ItemLocationType;
import org.prelle.splimo.items.ItemTemplate;
import org.prelle.splimo.items.ItemType;
import org.prelle.splimo.items.PersonalizationReference;
import org.prelle.splimo.items.Shield;
import org.prelle.splimo.modifications.AttributeModification;
import org.prelle.splimo.modifications.FeatureModification;
import org.prelle.splimo.modifications.ItemModification;
import org.prelle.splimo.modifications.MastershipModification;
import org.prelle.splimo.modifications.ModificationImpl;
import org.prelle.splimo.modifications.ModificationSource;
import org.prelle.splimo.modifications.SkillModification;
import org.prelle.splimo.requirements.AttributeRequirement;
import org.prelle.splimo.requirements.Requirement;

import de.rpgframework.genericrpg.modification.Modification;

/**
 * @author prelle
 *
 */
public class EquipmentTools {

	private final static Logger logger = LogManager.getLogger("splittermond.items");
	
	public final static String TOTAL_HANDICAP = "TOTAL_HANDICAP";

	//-------------------------------------------------------------------
	/**
	 * Check if character meets the requirements of the item. If not attach item modifications
	 * to the item.
	 */
	private static void processShieldAndArmorRequirements(SpliMoCharacter model, CarriedItem item, List<Requirement> reqs) {
		for (Requirement requires :reqs) {
			if (SplitterTools.isRequirementMet(model, requires))
				continue;
			if (requires instanceof AttributeRequirement) {
				AttributeRequirement req = (AttributeRequirement)requires;
				int missing = req.getValue() - model.getAttribute(req.getAttribute()).getValue();
				logger.info("  Missing "+missing+" points in "+req.getAttribute()+" to satisfy "+item.getName());
				ItemModification itemMod1 = new ItemModification(ItemAttribute.TICK_MALUS, missing);
				ItemModification itemMod2 = new ItemModification(ItemAttribute.HANDICAP  , missing);
				itemMod1.setSource(req.getAttribute());
				itemMod2.setSource(req.getAttribute());
				item.addItemModification(itemMod1);
				item.addItemModification(itemMod2);
				logger.debug("    Add to item: "+itemMod1);
				logger.debug("    Add to item: "+itemMod2);
			} else
				logger.warn("I don't know how to check for "+requires.getClass());
			
		}
	}

	//-------------------------------------------------------------------
	/**
	 * Check if character meets the requirements of the item. If not attach item modifications
	 * to the item.
	 */
	private static List<Modification> processWeaponRequirements(SpliMoCharacter model, CarriedItem item, List<Requirement> reqs) {
		List<Modification> ret = new ArrayList<>();

		int max = 0;
		for (Requirement requires :reqs) {
			if (SplitterTools.isRequirementMet(model, requires))
				continue;
			if (requires instanceof AttributeRequirement) {
				AttributeRequirement req = (AttributeRequirement)requires;
				int missing = req.getValue() - model.getAttribute(req.getAttribute()).getValue();
				max = Math.max(max, missing);
			} else
				logger.warn("I don't know how to check for "+requires.getClass());
			
		}
		if (max>0) {
			logger.info("    Missing "+max+" points in "+reqs+" to satisfy "+item.getName());
			Skill skill = item.getItem().getSkill();
			
			SkillModification skillMod1 = new SkillModification(skill, -max);
			ItemModification itemMod2 = new ItemModification(ItemAttribute.SPEED  , max);
			skillMod1.setSource(item);
			itemMod2.setSource(item);
			ret.add(skillMod1);
			logger.debug("    Add to item: "+itemMod2);
			item.addItemModification(itemMod2);
		}
		
		return ret;
	}

	//-------------------------------------------------------------------
	public static void determineEnhancementReferenceModifications(CarriedItem item, EnhancementReference ref) {
		if (ref.getModifications().size()>0)
			return;
		
		logger.debug("  determineEnhancementReferenceModifications "+ref+" from "+item);
		logger.debug("   * Enhancement SkillSpecial = "+ref.getSkillSpecialization());
		logger.debug("   * Enhancement SpellValue   = "+ref.getSpellValue());
		logger.debug("   * Enhancement Power        = "+ref.getPower());
		logger.debug("   * Item Skill               = "+item.getItem().getSkill());
		
		/*
		 * Assign skill to skill specialization
		 */
		if (ref.getSkillSpecialization()!=null && ref.getSkillSpecialization().getSkill()==null) {
			SkillSpecialization spec = ref.getSkillSpecialization();
			String search  = spec.getId();
			String skillID = null;
			if (search.contains("/")) {
				skillID= search.substring(0,  search.indexOf("/"));
				search = search.substring(search.indexOf("/")+1);
			}
//			logger.debug("   * search  = "+search);
//			logger.debug("   * skillID = "+skillID);
			outer:
			for (Skill tmp : SplitterMondCore.getSkills()) {
				if (skillID!=null && !tmp.getId().equals(skillID))
					continue outer;
				for (SkillSpecialization tmp2 : tmp.getSpecializations()) {
					if (tmp2.getId().equals(search)) {
						logger.warn("    Assume skill "+tmp+" for enhancement "+ref);
						tmp2.setSkill(tmp);
						ref.getSkillSpecialization().setSkill(tmp);
						logger.warn("    Spec now "+tmp2+" for enhancement "+ref);
						break outer;
					}
				}
			}
			if (spec.getSkill()==null) {
				logger.error("Cannot detect skill for specialization "+spec.getId()+" in enhancement "+ref.getID());
			} 
		}	
		
		for (Modification mod: ref.getEnhancement().getModifications()) {
			mod.setSource(item);

			if (mod instanceof MastershipModification) {
				MastershipModification mmod = (MastershipModification)mod;
//				logger.debug("Master = "+mmod.getMastership());
//				logger.debug("Skill  = "+mmod.getSkill());
//				logger.debug("Special= "+mmod.getSpecialization());
				if (mmod.getMastership()!=null || mmod.getSpecialization()!=null) {
					logger.debug("    found mastership modification "+mod);
					item.addCharacterModification(mod);
				} else if (mmod.getSkill()==null && mmod.getSpecialization()==null) {
					logger.debug("    found mastership modification without spec or master");
					if (ref.getSkillSpecialization()!=null) {
						SkillSpecialization spec = ref.getSkillSpecialization();
						Skill skill = spec.getSkill();
						logger.debug("    apply specialization "+spec);
						
						// Check again
						if (skill==null) {
							logger.error("Cannot deduct skill for specialization "+spec+" in enhancement "+ref+" of "+item);
						} else {
							MastershipModification mod2 = new MastershipModification(ref.getSkillSpecialization(), 1);
							ref.getModifications().add(mod2);

						}
						
					} else {
						logger.warn("Don't know how to deal with mastership modification("+mod+") for this enhancement");
					}
					
				} else {
					logger.debug("    ignore unspecified mastership modification - assume it is a skill specialization  ("+mod+")");
				}
			} else
				ref.getModifications().add(mod);
		}
		
		logger.debug("   changed "+ref.getEnhancement().getModifications()+" to "+ref.getModifications());
	}
	
	//-------------------------------------------------------------------
	public static void determineEnhancementReferenceModifications(CarriedItem item) {
		logger.debug("  determineEnhancementReferenceModifications of "+item.getName());
		for (EnhancementReference ref : item.getEnhancements()) {
			determineEnhancementReferenceModifications(item, ref);
		}
	}
		

	//-------------------------------------------------------------------
	/**
	 * Iterate through enhancements. Get their modifications. Apply those that
	 * refer to the item itself. Return those that needs to be applied to the
	 * character
	 * 
	 * @return Modifications to apply to character
	 */
	public static void applyEnhancement(CarriedItem item, EnhancementReference ref) {
		logger.debug("  Apply enhancement "+ref+" to "+item);
//		logger.debug("   * Enhancement SkillSpecial = "+ref.getSkillSpecialization());
//		logger.debug("   * Enhancement SpellValue   = "+ref.getSpellValue());
//		logger.debug("   * Enhancement Power        = "+ref.getPower());
//		logger.debug("   * Item Skill               = "+item.getItem().getSkill());
//		
//		/*
//		 * Assign skill to skill specialization
//		 */
//		if (ref.getSkillSpecialization()!=null && ref.getSkillSpecialization().getSkill()==null) {
//			SkillSpecialization spec = ref.getSkillSpecialization();
//			outer:
//			for (Skill tmp : SplitterMondCore.getSkills()) {
//				for (SkillSpecialization tmp2 : tmp.getSpecializations()) {
//					if (tmp2.getId().equals(spec.getId())) {
//						logger.warn("    Assume skill "+tmp+" for enhancement "+ref);
//						tmp2.setSkill(tmp);
//						ref.getSkillSpecialization().setSkill(tmp);
//						logger.warn("    Spec now "+tmp2+" for enhancement "+ref);
//						break outer;
//					}
//				}
//			}
//			if (spec.getSkill()==null) {
//				logger.error("Cannot detect skill for specialization "+spec.getId()+" in enhancement "+ref.getID());
//			} 
//		}		
		
		int count=0;
		for (Modification mod: ref.getEnhancement().getModifications()) {
			count++;
			mod.setSource(item);

			if (mod instanceof AttributeModification) {
				AttributeModification cloned = ((AttributeModification)mod).clone();
				cloned.setUniqueID(ref.getID()+"-"+ref.getUniqueID()+"-"+count);
				cloned.setModificationSource(((AttributeModification)mod).getModificationSource());
				item.addCharacterModification(cloned);
			} else if (mod instanceof FeatureModification) {
				logger.debug("    found feature modification "+mod);
				item.addItemModification(mod);
			} else if (mod instanceof ItemModification) {
				logger.debug("    found item modification "+mod);
				item.addItemModification(mod);
			} else if (mod instanceof MastershipModification) {
				MastershipModification mmod = (MastershipModification)mod;
				logger.debug("Master = "+mmod.getMastership());
				logger.debug("Skill  = "+mmod.getSkill());
				logger.debug("Special= "+mmod.getSpecialization());
				if (mmod.getMastership()!=null || mmod.getSpecialization()!=null) {
					logger.debug("    found mastership modification "+mod);
					item.addCharacterModification(mod);
				} else if (mmod.getSkill()==null && mmod.getSpecialization()==null) {
					logger.debug("    found mastership modification without spec or master");
					if (ref.getSkillSpecialization()!=null) {
						SkillSpecialization spec = ref.getSkillSpecialization();
						Skill skill = spec.getSkill();
						logger.debug("    apply specialization "+spec);
						
						// Check again
						if (skill==null) {
							logger.error("Cannot deduct skill for specialization "+spec+" in enhancement "+ref+" of "+item);
						} else {
							MastershipModification mod2 = new MastershipModification(ref.getSkillSpecialization(), 1);
							logger.debug("  would add "+mod2);
							item.addItemModification(mod2);
//							Enhancement enhance2 = new Enhancement(id, size, type)
//							ref.getEnhancement().getModifications().remove(mod);
//							ref.getEnhancement().getModifications().add(mod2);
						}
						
					} else {
						logger.warn("Don't know how to deal with mastership modification("+mod+") for this enhancement");
					}
					
				} else {
					logger.debug("    ignore unspecified mastership modification - assume it is a skill specialization  ("+mod+")");
				}
			} else
				logger.warn("    Don't know what to do with "+mod.getClass());
		}
		
		if (ref.getSkillSpecialization()!=null) {
			SkillSpecialization spec = ref.getSkillSpecialization();
			if (spec.getSkill()==null) {
				if (item.isType(ItemType.WEAPON))
					spec.setSkill(item.getSkill(ItemType.WEAPON));
				else if (item.isType(ItemType.LONG_RANGE_WEAPON))
					spec.setSkill(item.getSkill(ItemType.LONG_RANGE_WEAPON));
				else
					spec.setSkill(item.getSkill(ItemType.OTHER));
			}
			logger.info("    add skill specialization "+spec);
			MastershipModification mmod = new MastershipModification(spec, 1);
			mmod.setSource(ref);
			item.addCharacterModification(mmod);
			logger.info("    added as mastership mod "+mmod);
		}
		
		if (ref.getSpellValue()!=null) {
			logger.warn("   TODO: implement adding "+ref.getSpellValue());
		}
	}

	//-------------------------------------------------------------------
	/**
	 * Iterate through enhancements. Get their modifications. Apply those that
	 * refer to the item itself. Return those that needs to be applied to the
	 * character
	 * 
	 * @return Modifications to apply to character
	 */
	public static void undoEnhancement(CarriedItem item, EnhancementReference ref) {
		logger.debug("  Undo enhancement "+ref+" to "+item);
		for (Modification mod: ref.getEnhancement().getModifications()) {
			mod.setSource(item);

			if (mod instanceof AttributeModification) {
				logger.debug("    remove character modification "+mod);
				item.removeCharacterModification(mod);
			} else if (mod instanceof FeatureModification) {
				logger.debug("    remove feature modification "+mod);
				item.removeItemModification(mod);
			} else if (mod instanceof ItemModification) {
				logger.debug("    remove item modification "+mod);
				item.removeItemModification(mod);
			} else
				logger.warn("    Don't know what to do with "+mod.getClass());
		}
	}

	//-------------------------------------------------------------------
	/**
	 * Iterate through enhancements. Get their modifications. Apply those that
	 * refer to the item itself. Return those that needs to be applied to the
	 * character
	 * 
	 * @return Modifications to apply to character, based on requirements
	 */
	public static void applyEnhancements(SpliMoCharacter model, CarriedItem item) {
		logger.debug("  applyEnhancements of "+item.getName());
		item.getRequirementModifications().clear();
		item.getItemModifications().clear();
		item.getCharacterModifications().clear();

		if (!item.getEnhancements().isEmpty())
			logger.debug("  Item "+item.getName()+" has enhancements");
		int count=0;
		for (EnhancementReference ref : item.getEnhancements()) {
			ref.setUniqueID(String.valueOf(++count));
			applyEnhancement(item, ref);
		}
		
		/*
		 * Personalizations
		 */
		for (PersonalizationReference perso : item.getPersonalizations()) {
			for (Modification mod : perso.getModifications()) {
				if (mod instanceof ItemModification) {
					item.addItemModification(mod);
				} else if (mod instanceof MastershipModification) {
					// Ignore
				} else
					logger.warn("Don't know what to do with "+mod.getClass());
			}
		}
		
		/*
		 * Check requirements
		 */
		ItemTemplate real = item.getItem();
		if (real.isType(ItemType.ARMOR)) {
			processShieldAndArmorRequirements(model, item, item.getRequirements(ItemType.ARMOR));
		} else if (real.isType(ItemType.SHIELD)) {
			processShieldAndArmorRequirements(model, item, item.getRequirements(ItemType.SHIELD));
		} else if (real.isType(ItemType.WEAPON)) {
			item.getCharacterModifications().addAll(processWeaponRequirements(model, item, item.getRequirements(ItemType.WEAPON)));
		} else if (real.isType(ItemType.LONG_RANGE_WEAPON)) {
			item.getCharacterModifications().addAll(processWeaponRequirements(model, item, item.getRequirements(ItemType.LONG_RANGE_WEAPON)));
		}
		
		logger.debug("  Item "+item.getName()+": Character mods from enhancements = "+item.getCharacterModifications());
	}

	//-------------------------------------------------------------------
	/**
	 * Return the handicap the item has when worn by the character
	 */
	private static int getEquippedHandicap(SpliMoCharacter model, CarriedItem item) {
		/*
		 * Apply armor
		 */
		Armor armor = item.getItem().getType(Armor.class);
		if (armor!=null) {
			// Handicap
			logger.trace("  Unmodified handicap without requirements is "+armor.getHandicap());
			int hc = item.getHandicap(ItemType.ARMOR);
//			// Personalizations
//			for (PersonalizationReference ref : item.getPersonalizations()) {
//				for (Modification mod : ref.getModifications()) {
//					if (mod instanceof ItemModification) {
//						ItemModification itemMod = (ItemModification)mod;
//						if (itemMod.getAttribute()==ItemAttribute.HANDICAP) {
//							logger.debug("  personalization "+ref.getName()+" changes handicap "+itemMod.getValue());
//							hc += itemMod.getValue();
//						}
//					}
//				}
//			}
			
			// Reduce handicap by masterships in toughness
			if (model.hasMastership(SplitterMondCore.getSkill("endurance").getMastership("armour1"))) {
				hc = (hc>0)?(--hc):0;
			}
			if (hc<0)
				hc=0;
			logger.debug("  equipped handicap of "+item.getName()+" is "+hc);
			return hc;
		}
		
		/*
		 * Apply shield
		 */
		Shield shield = item.getItem().getType(Shield.class);
		if (shield!=null) {
			// Handicap
			logger.trace("  Unmodified handicap without requirements is "+shield.getHandicap());
			int hc = item.getHandicap(ItemType.SHIELD);
			// Reduce handicap by masterships in toughness
			if (model.hasMastership(SplitterMondCore.getSkill("endurance").getMastership("shield1"))) {
				hc = (hc>0)?(--hc):0;
			}
			logger.debug("  equipped handicap of "+item.getName()+" is "+hc);
			return hc;
		}
		
		return 0;
	}

	//-------------------------------------------------------------------
	/**
	 * Return a list of modifications that result from the stats of an item -
	 * like defense
	 * 
	 * @return Modifications made
	 */
	private static List<Modification> getModificationsFromItem(SpliMoCharacter model, CarriedItem item) {
		List<Modification> ret = new ArrayList<>();
		
		/*
		 * Apply armor
		 */
		Armor armor = item.getItem().getType(Armor.class);
		if (armor!=null) {
			// Defense
			ret.add(new AttributeModification(Attribute.DEFENSE, item.getDefense(ItemType.ARMOR), ModificationSource.EQUIPMENT));
			// Handicap
			int hc = getEquippedHandicap(model, item);
			if (hc>0) {
				for (SkillValue skillVal : model.getSkills(SkillType.NORMAL)) {
					Skill skill = skillVal.getSkill();
					if (skill.getAttribute1()==Attribute.AGILITY || skill.getAttribute2()==Attribute.AGILITY) {
						ret.add(new SkillModification(skill, -hc));
						logger.debug("  Added handicap "+hc+" to "+skill.getName());
					}
				}
			}
		}
		
		/*
		 * Apply shield
		 */
		Shield shield = item.getItem().getType(Shield.class);
		if (shield!=null) {
			// Defense
			if (item.getDefense(ItemType.SHIELD)!=0) {
				ret.add(new AttributeModification(Attribute.DEFENSE, item.getDefense(ItemType.SHIELD), ModificationSource.EQUIPMENT));
				logger.debug("Added shield defense +"+item.getDefense(ItemType.SHIELD));
			}
			
			// Handicap
			int hc = getEquippedHandicap(model, item);
			if (hc>0) {
				for (SkillValue skillVal : model.getSkills(SkillType.NORMAL)) {
					Skill skill = skillVal.getSkill();
					if (skill.getAttribute1()==Attribute.AGILITY || skill.getAttribute2()==Attribute.AGILITY) {
						ret.add(new SkillModification(skill, -hc));
						logger.debug("  Added handicap "+hc+" to "+skill.getName());
					}
				}
			}
		}
		
		
		// Assign source
		for (Modification mod : ret)
			((ModificationImpl)mod).setSource(item);
		
		/*
		 * Apply personalizations
		 * This must happen later, so that the source is different from the item.
		 * (Necessary to detect modifications made by personalizations - e.g. for evade)
		 */
		for (PersonalizationReference ref : item.getPersonalizations()) {
			for (Modification mod : ref.getModifications()) {
				mod.setSource(ref);
				item.addCharacterModification(mod);
//				ret.add(mod);
			}
		}
		
		/*
		 * Check material
		 */
		if (item.getMaterial()!=null) {
			for (Modification mod : item.getMaterial().getModifications()) {
				mod.setSource(item.getMaterial());
				if (mod instanceof AttributeModification) {
					((AttributeModification) mod).setModificationSource(ModificationSource.EQUIPMENT);
					item.addCharacterModification(mod);
				} else if (mod instanceof FeatureModification) {
					item.addItemModification(mod);
				} else if (mod instanceof ItemModification) {
					item.addItemModification(mod);
				} else if (mod instanceof SkillModification) {
					item.addCharacterModification(mod);
				} else
					logger.warn("Material: Don't know how to deal with "+mod.getClass()+": "+mod);
			}
		}
		
		logger.debug("  getModificationsFromItem("+item.getName()+": Character mods from items = "+ret);
		return ret;
	}

	//-------------------------------------------------------------------
	public static void equip(SpliMoCharacter model, CarriedItem item) {
		logger.info("equip "+item);

		// Ensure enhancements are applied to item
		applyEnhancements(model, item);

		/*
		 *  Build a list of modifications to apply to the character
		 *  based on modifications granted by the item and those inflicted
		 *  upon not fulfilling requirements.
		 *  Add those given by item stats
		 */
		List<Modification> mods = new ArrayList<>();
		mods.addAll(item.getRequirementModifications());
		mods.addAll(getModificationsFromItem(model, item));
		logger.info("Item character mods are "+item.getCharacterModifications());
		mods.addAll(item.getCharacterModifications());
		for (Modification mod : mods) {
			logger.debug("...."+mod);
			mod.setSource(item);
		}
		
		// Skill for item
		Skill skill = null;
		SkillSpecialization spec = null;
		if (item.isType(ItemType.LONG_RANGE_WEAPON)) {
			skill = item.getSkill(ItemType.LONG_RANGE_WEAPON);
			spec  = new SkillSpecialization(skill, SkillSpecializationType.WEAPON, item.getItem().getID());
		} else if (item.isType(ItemType.WEAPON)) {
			skill = item.getSkill(ItemType.WEAPON);
			spec  = new SkillSpecialization(skill, SkillSpecializationType.WEAPON, item.getItem().getID());
		} else {
			skill = item.getItem().getSkill();
			spec  = new SkillSpecialization(skill, SkillSpecializationType.NORMAL, item.getItem().getID());
		}
		try {
			SplitterTools.applyToCharacter(model, mods, skill, spec);
		} catch (Exception e) {
			logger.error("Error equipping "+item,e);
		}
		
		item.setItemLocation(ItemLocationType.BODY);
		
		updateTotalHandicap(model);
	}


	//-------------------------------------------------------------------
	private static List<Modification> unloadEquipmentModifications(SpliMoCharacter model, CarriedItem item) {
		logger.debug("Remove modifications from "+item);
		ArrayList<Modification> ret = new ArrayList<>();
		
		if (item.getLocation()!=ItemLocationType.BODY)
			return ret;

		// Defense
		AttributeValue aVal = model.getAttribute(Attribute.DEFENSE);
		for (Modification mod : new ArrayList<Modification>(aVal.getModifications())) {
			if (mod.getSource()==item) {
				ret.add(mod);
				logger.debug("Remove defense +"+mod);
			}
		}
		// Skills
		for (SkillValue skillVal : model.getSkills(SkillType.NORMAL)) {
			for (Modification mod : new ArrayList<Modification>(skillVal.getModifications())) {
				ModificationImpl modI = (ModificationImpl)mod;
				if (modI.getSource()==item) {
					ret.add(mod);
					aVal.removeModification(mod);
					logger.debug("Removed "+mod);
				}
			}
		}
		
		return ret;
	}

	//-------------------------------------------------------------------
	public static void unequip(SpliMoCharacter model, CarriedItem item) {
		logger.info("unequip "+item);

		/*
		 *  Build a list of modifications to apply to the character
		 *  based on modifications granted by the item and those inflicted
		 *  upon not fulfilling requirements
		 */
		List<Modification> mods = new ArrayList<>();
		mods.addAll(item.getRequirementModifications());
		mods.addAll(item.getCharacterModifications());
		mods.addAll(unloadEquipmentModifications(model, item));
		
		SplitterTools.unapplyToCharacter(model, mods);
		
		item.setItemLocation(ItemLocationType.CONTAINER);
		
		updateTotalHandicap(model);
	}

	//-------------------------------------------------------------------
	public static void updateTotalHandicap(SpliMoCharacter model) {
		/*
		 * Calculate the old total handicap
		 */
		int totalHC = 0;
		for (CarriedItem previous : model.getItems()) {
			if (previous.getLocation()==ItemLocationType.BODY) {
				totalHC+= getEquippedHandicap(model, previous);
			}
		}
		if (totalHC<0)
			totalHC=0;
		
		
		/*
		 * Calculate the new total handicap
		 */
		// Total handicap changed
		logger.info("Total handicap is now "+totalHC);
		int newSpeedMod = (int) Math.round(totalHC/2.0);
		AttributeValue aVal = model.getAttribute(Attribute.SPEED);
		AttributeModification mod = null;
		for (Modification anyMod : aVal.getModifications()) {
			if (anyMod.getSource()==TOTAL_HANDICAP) {
				// Found a previous attribute modification to use
				logger.debug("Found previous "+anyMod);
				mod = (AttributeModification)anyMod;
				mod.setValue(-newSpeedMod);
				break;
			}
		}
		if (mod==null) {
			mod = new AttributeModification(Attribute.SPEED, -newSpeedMod, ModificationSource.EQUIPMENT);
			mod.setSource(TOTAL_HANDICAP);
			aVal.addModification(mod);
		}
	}

	//-------------------------------------------------------------------
	public static boolean updateAllItems(SpliMoCharacter model){
		boolean mightHaveChanged = false;
		for (CarriedItem item : model.getItems()) {
			ItemTemplate itemTemplate = item.getItem();
			if (item.getItemModifications().size() > 0 && (
					itemTemplate.isType(ItemType.WEAPON) ||
					itemTemplate.isType(ItemType.LONG_RANGE_WEAPON) ||
					itemTemplate.isType(ItemType.ARMOR) ||
					itemTemplate.isType(ItemType.SHIELD))) {
				mightHaveChanged= true;
				if (item.getLocation() != ItemLocationType.BODY) {
					EquipmentTools.applyEnhancements(model, item);
				} else {
					EquipmentTools.unequip(model, item);
					EquipmentTools.equip(model, item);
				}
			}
		}
		return mightHaveChanged;
	}


	/** Optional school/type bonuses from equipped catalogue items. */
	public static int getSpellBonus(SpliMoCharacter model, SpellValue spell) {
		java.util.Map<String, Integer> groups = new java.util.HashMap<>();
		for (CarriedItem item : model.getItems(ItemLocationType.BODY)) {
			if (item.getCount()<=0)
				continue;
			for (org.prelle.splimo.items.ItemSpellBonus bonus : item.getItem().getSpellBonuses()) {
				if (!bonus.matches(spell))
					continue;
				String group = bonus.getGroup()==null ? item.getItem().getId() : bonus.getGroup();
				groups.merge(group, bonus.getValue(), Math::max);
			}
		}
		int additional = groups.values().stream().mapToInt(Integer::intValue).sum();
		SkillValue skill = model.getSkillValue(spell.getSkill());
		// Penalties are applied after the positive-bonus cap; they do not free capacity.
		int positive = skill.getPositiveEquipmentModifier();
		return skill.getModifierCap()>0
				? Math.min(additional, Math.max(0, skill.getModifierCap()-positive)) : additional;
	}

}
