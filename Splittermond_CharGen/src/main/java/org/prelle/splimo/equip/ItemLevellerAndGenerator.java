/**
 * 
 */
package org.prelle.splimo.equip;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prelle.splimo.Attribute;
import org.prelle.splimo.DeityType;
import org.prelle.splimo.HolyPower;
import org.prelle.splimo.Skill;
import org.prelle.splimo.SkillSpecialization;
import org.prelle.splimo.SkillSpecialization.SkillSpecializationType;
import org.prelle.splimo.SpellValue;
import org.prelle.splimo.SplitterMondCore;
import org.prelle.splimo.charctrl.NewItemController;
import org.prelle.splimo.charctrl4.SpliMoCharGenConstants;
import org.prelle.splimo.chargen.event.GenerationEvent;
import org.prelle.splimo.chargen.event.GenerationEventDispatcher;
import org.prelle.splimo.chargen.event.GenerationEventType;
import org.prelle.splimo.items.CarriedItem;
import org.prelle.splimo.items.Enhancement;
import org.prelle.splimo.items.Enhancement.EnhancementType;
import org.prelle.splimo.items.EnhancementReference;
import org.prelle.splimo.items.ItemType;
import org.prelle.splimo.items.LongRangeWeapon;
import org.prelle.splimo.items.Material;
import org.prelle.splimo.items.Personalization;
import org.prelle.splimo.items.PersonalizationReference;
import org.prelle.splimo.items.Shield;
import org.prelle.splimo.items.Weapon;
import org.prelle.splimo.modifications.AttributeChangeModification;
import org.prelle.splimo.modifications.MastershipModification;
import org.prelle.splimo.requirements.AttributeRequirement;
import org.prelle.splimo.requirements.Requirement;

import de.rpgframework.genericrpg.modification.Modification;

/**
 * @author Stefan
 *
 */
public class ItemLevellerAndGenerator implements NewItemController {
	
	private static Logger logger = LogManager.getLogger("splittermond.chargen");

	private final static ResourceBundle RES = SpliMoCharGenConstants.RES;
	
	private CarriedItem model;
	private int maximumQuality;
	
	private List<EnhancementReference> nonRemovable;
	
	private List<Enhancement> allEnhancements;
	private List<Enhancement> availEnhancements;
	private List<Material>    availMaterials;
	private List<PersonalizationReference> allPersonal;
	private List<PersonalizationReference> availPersonal;

	//-------------------------------------------------------------------
	/**
	 * @param model    Item to level
	 * @param maxQual  Maximum quality allowed
	 * @param free	   Free points that can be invested without accessing EXP
	 */
	public ItemLevellerAndGenerator(CarriedItem model, int maxQual) {
		if (model==null)
			throw new NullPointerException("CarriedItem may not be null");
		if (model.getItem()==null)
			throw new NullPointerException("Item within CarriedItem may not be null");
		allEnhancements   = SplitterMondCore.getEnhancements();
		availEnhancements = new ArrayList<>();
		availMaterials    = new ArrayList<>();
		allPersonal       = new ArrayList<>();
		availPersonal     = new ArrayList<>();
		nonRemovable      = new ArrayList<>(model.getEnhancements());
		
		this.model = model;
		this.maximumQuality = maxQual;
		if (model.getMaterial()==null && !SplitterMondCore.getMaterials(model.getItem().getMaterialType()).isEmpty()) {
			logger.debug("Set to default material "+SplitterMondCore.getMaterials(model.getItem().getMaterialType()).get(0));
			model.setMaterial(SplitterMondCore.getMaterials(model.getItem().getMaterialType()).get(0));
		}
		
		calculateAvailable();
		calculateAvailableMaterials();
		calculateAllPersonalizations();
		calculateAvailablePersonalizations();
		fireItemChange();
	}

	//--------------------------------------------------------------------
	private void fireItemChange() {
		logger.debug("fireItemChange");
		GenerationEventDispatcher.fireEvent(
				new GenerationEvent(GenerationEventType.ITEM_CHANGED, model)
				);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.Generator#getPointsLeft()
	 */
	@Override
	public int getPointsLeft() {
		if (model.isRelic())
			return maximumQuality - getNewlyInvestedPoints();
		return 0;
	}
	
	//-------------------------------------------------------------------
	/**
	 * Count those points invested during this levelling session.
	 */
	private int getNewlyInvestedPoints() {
		int count = 0;
		for (EnhancementReference ref : model.getEnhancements()) {
			if (nonRemovable.contains(ref))
				continue;
			count += ref.getEnhancement().getSize();
		}
		
		return count;
	}
	
	//-------------------------------------------------------------------
	/**
	 * Check if the enhancement can be used for the given item type
	 */
	private boolean isSuitable(Enhancement toAdd) {
		// Check if enhancement is limited to specific types
		if (!toAdd.getItemTypeLimitations().isEmpty()) {
			boolean notFound = true;
			for (ItemType type : toAdd.getItemTypeLimitations()) {
				if (model.isType(type)) {
					notFound = false;
					break;
				}
			}
			if (notFound) {
				logger.trace("Enhancement '"+toAdd+"' ("+toAdd.getItemTypeLimitations()+") does not match item types of "+model);
				return false;
			}
		}

		switch (toAdd.getType()) {
		case RELIC:
		case HIGHARTIFACT:
			if (!model.isRelic()) {
				logger.trace("Enhancement "+toAdd+" may only used by relics, but "+model.getName()+" is none");
				return false;
			}
			break;
		default:
			break;
		}
		
		return true;
	}
	
	//-------------------------------------------------------------------
	private boolean canBePaid(Enhancement toAdd) {
		if (model.isRelic()) {
			return (getNewlyInvestedPoints() + toAdd.getSize())<=model.getResource().getValue();
		}
		
		// TODO: check for money
		return true;
	}
	
	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#canBeAdded(org.prelle.splimo.items.Enhancement)
	 */
	@Override
	public boolean canBeAdded(Enhancement toAdd) {
		if (!isSuitable(toAdd))
			return false;
		
		if (!canBePaid(toAdd))
			return false;
		
		if ( (getNewlyInvestedPoints() + toAdd.getSize())>maximumQuality) 
			return false;
		
		int maxApply = toAdd.getMaxApplyCount();
		// Count how often the enhancement already exists
		int count = 0;
		for (EnhancementReference ref : model.getEnhancements()) {
			if (ref.getEnhancement()==toAdd)
				count++;
		}
		
		if (maxApply>0 && (count+1)>maxApply) {
			return false;
		}
		
		
		/*
		 * Hardcoded
		 */
		if (toAdd.getId().equals("load")) {
			return (count+1)<=model.getItem().getLoad()/2;
		}
		if (toAdd.getId().startsWith("embedspell")) {
			return true;
		}
		
//		logger.warn("Don't know how to check for "+toAdd);
		return true;
	}
	
	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#canBeRemoved(org.prelle.splimo.items.EnhancementReference)
	 */
	@Override
	public boolean canBeRemoved(EnhancementReference toRemove) {
		if (nonRemovable.contains(toRemove))
			return false;
		return model.getEnhancements().contains(toRemove);
	}

	//--------------------------------------------------------------------
	private void calculateAvailable() {
		logger.debug("calculateAvailable");
		availEnhancements.clear();
		for (Enhancement enhance : allEnhancements) {
			if (canBeAdded(enhance))
				availEnhancements.add(enhance);
		}
	}

	//--------------------------------------------------------------------
	private void calculateAvailableMaterials() {
		logger.debug("calculateAvailableMaterials");
		availMaterials.clear();
		for (Material material: SplitterMondCore.getMaterials()) {
			if (material.getType()==model.getItem().getMaterialType())
				availMaterials.add(material);
		}
	}

	//--------------------------------------------------------------------
	private void calculateAllPersonalizations() {
		logger.debug("calculateAllPersonalizations");
		allPersonal.clear();
		for (Personalization personal: SplitterMondCore.getPersonalizations()) {
			if (!model.getItem().isType(personal.getSupportedItemType()) && !(model.getItem().isType(ItemType.LONG_RANGE_WEAPON) && personal.getSupportedItemType()==ItemType.WEAPON))
				continue;
			logger.debug("* "+personal.getId()+" (mods="+personal.getModifications()+" )");
			
			if (personal.getId().equals("weapon_changeattr")) {
				Weapon weapon = model.getItem().getType(Weapon.class);
				if (weapon==null)
					weapon = model.getItem().getType(LongRangeWeapon.class);
				Attribute[] possReplace = {Attribute.AGILITY, Attribute.INTUITION, Attribute.CONSTITUTION, Attribute.STRENGTH};
				for (Attribute replacedBy : possReplace) {
					// Avoid duplicate attributes
					if (replacedBy==weapon.getAttribute1() || replacedBy==weapon.getAttribute2())
						continue;
					if (replacedBy!=weapon.getAttribute1()) {
						PersonalizationReference ref = new PersonalizationReference(personal);
						ref.addModification(new AttributeChangeModification(weapon.getAttribute1(), replacedBy));
						allPersonal.add(ref);						
					}
					if (replacedBy!=weapon.getAttribute2()) {
						PersonalizationReference ref = new PersonalizationReference(personal);
						ref.addModification(new AttributeChangeModification(weapon.getAttribute2(), replacedBy));
						allPersonal.add(ref);						
					}
				}
				
			} else if (personal.getId().equals("shield_changeattr")) {
				Shield shield = model.getItem().getType(Shield.class);
				for (Requirement req : shield.getRequirements()) {
					if (req instanceof AttributeRequirement) {
						AttributeRequirement areq = (AttributeRequirement)req;
						Attribute[] possReplace = {Attribute.AGILITY, Attribute.INTUITION, Attribute.CONSTITUTION, Attribute.STRENGTH};
						for (Attribute replacedBy : possReplace) {
							if (replacedBy==areq.getAttribute())
								continue;
							PersonalizationReference ref = new PersonalizationReference(personal);
							ref.addModification(new AttributeChangeModification(areq.getAttribute(), replacedBy));
							allPersonal.add(ref);
						}
					}
				}
				
			} else if (personal.getId().equals("weapon_skillplus")) {
				Skill skill = model.getItem().getSkill();
				PersonalizationReference ref = new PersonalizationReference(personal);
				for (Modification mod : personal.getModifications()) {
					if (mod instanceof MastershipModification) {
//						MastershipModification mmod = (MastershipModification)mod;
//						if (mmod.getSkill()==null && mmod.getSpecialization()==null)
						MastershipModification mod2 = new MastershipModification(new SkillSpecialization(skill, SkillSpecializationType.WEAPON, model.getItem().getID()), 1);
						mod2.setSource(personal);
						logger.debug("  convert "+mod+" to "+mod2);
						ref.addModification(mod2);
					} else {
						logger.warn("  Don't know how to convert "+mod);
						ref.addModification(mod);
					}
				}
				logger.debug("Add personalization "+ref);
				allPersonal.add(ref);
				
			} else {
				PersonalizationReference ref = new PersonalizationReference(personal);
				for (Modification mod : personal.getModifications())
					ref.addModification(mod);
				allPersonal.add(ref);
			}
		}
	}

	//--------------------------------------------------------------------
	private void calculateAvailablePersonalizations() {
		logger.debug("calculateAvailablePersonalizations");
		availPersonal.clear();
		for (PersonalizationReference ref : allPersonal) {
			if (model.getPersonalizations().contains(ref))
				continue;
			availPersonal.add(ref);
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#getItem()
	 */
	@Override
	public CarriedItem getItem() {
		return model;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#getAvailableEnhancements()
	 */
	@Override
	public List<Enhancement> getAvailableEnhancements() {
		return availEnhancements;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#getAvailableEnhancements(org.prelle.splimo.items.Enhancement.EnhancementType)
	 */
	@Override
	public List<Enhancement> getAvailableEnhancements(EnhancementType type) {
		List<Enhancement> ret = new ArrayList<Enhancement>();
		availEnhancements.forEach(cons -> {
			if (cons.getType()==type)
				ret.add(cons);
		});

		return ret;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#getAvailableEnhancements(org.prelle.splimo.DeityType)
	 */
	@Override
	public List<Enhancement> getAvailableEnhancements(DeityType type) {
		List<Enhancement> ret = new ArrayList<Enhancement>();
		outer:
		for (HolyPower tmp : SplitterMondCore.getHolyPowers()) {
			// Ignore those not matching deity type
			if (tmp.getDeityType()!=type)
				continue;
			// Ignore those already selected
			for (EnhancementReference ref : model.getEnhancements()) {
				if (ref.getEnhancement()==tmp) {
					continue outer;
				}
			}
			ret.add(tmp);
		}

		return ret;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#addEnhancement(org.prelle.splimo.items.Enhancement)
	 */
	@Override
	public EnhancementReference addEnhancement(Enhancement toAdd) {
		logger.debug("Add enhancement "+toAdd);
		if (!canBeAdded(toAdd))
			return null;
		
		EnhancementReference ref = new EnhancementReference(toAdd);
		model.addEnhancement(ref);
		ref.setUniqueID(UUID.randomUUID().toString());
		
		if (toAdd==SplitterMondCore.getEnhancement("specialization")) {
			Weapon          dataWeapon = model.getItem().getType(Weapon.class);
			LongRangeWeapon dataRanged = model.getItem().getType(LongRangeWeapon.class);
//			Armor           dataArmor  = model.getItem().getType(Armor.class);
//			Shield          dataShield = model.getItem().getType(Shield.class);
			if (dataWeapon!=null) {
				SkillSpecialization skillSpec = new SkillSpecialization(dataWeapon.getSkill(), SkillSpecializationType.WEAPON, model.getItem().getID());
				ref.setSkillSpecialization(skillSpec);
			} else if (dataRanged!=null) {
				SkillSpecialization skillSpec = new SkillSpecialization(dataRanged.getSkill(), SkillSpecializationType.WEAPON, model.getItem().getID());
				ref.setSkillSpecialization(skillSpec);
			} else {
				logger.error("Don't know what to do here");
			}
		}
		
		calculateAvailable();
		fireItemChange();
		return ref;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#addEnhancement(org.prelle.splimo.items.Enhancement)
	 */
	@Override
	public EnhancementReference addEnhancement(Enhancement toAdd, SkillSpecialization skillSpec) {
		logger.debug("Add skill enhancement "+skillSpec+" to "+model);
		
		EnhancementReference ref = new EnhancementReference(toAdd);
		ref.setSkillSpecialization(skillSpec);
		ref.setUniqueID(UUID.randomUUID().toString());
		model.addEnhancement(ref);
		
		calculateAvailable();
		fireItemChange();
		return ref;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#addEnhancement(org.prelle.splimo.items.Enhancement, org.prelle.splimo.SpellValue)
	 */
	@Override
	public EnhancementReference addEnhancement(Enhancement toAdd, SpellValue spell) {
		logger.info("Add spell enhancement "+spell+" to "+model);
		
		EnhancementReference ref = new EnhancementReference(toAdd);
		ref.setSpellValue(spell);
		model.addEnhancement(ref);
		
		calculateAvailable();
		fireItemChange();
		return ref;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#removeEnhancement(org.prelle.splimo.items.EnhancementReference)
	 */
	@Override
	public void removeEnhancement(EnhancementReference toRemove) {
		logger.debug("Remove enhancement "+toRemove);
		
		if (model.removeEnhancement(toRemove))
			logger.debug("Enhancement removed successfully");
		
		calculateAvailable();
		fireItemChange();
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#getAvailableMaterials()
	 */
	@Override
	public List<Material> getAvailableMaterials() {
		return availMaterials;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#setMaterial(org.prelle.splimo.items.Material)
	 */
	@Override
	public void setMaterial(Material material) {
		if (material!=null)
			logger.debug("Set material "+material.getName());
		else
			logger.debug("Clear material");
		logger.warn("TODO: update enhancements and modifications from material");
		model.setMaterial(material);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#getPrice()
	 */
	@Override
	public int getPrice() {
		int basePrice = model.getItem().getPrice();
		int maxQuality = model.getItemQuality();
		if (model.getMaterial()!=null && model.getMaterial().getQuality()>0)
			Math.max(model.getItemQuality(), model.getMaterial().getQuality());
		if (model.getItem().getCatalogProfile()!=null) {
			// The catalogue price already pays for the included recipe quality.
			maxQuality -= model.getItem().getCatalogProfile().getItemQuality();
		}
		switch (maxQuality) {
		case -3: basePrice = (int)Math.round( ((float)basePrice) * 0.7 ); break;
		case -2: basePrice = (int)Math.round( ((float)basePrice) * 0.8 ); break;
		case -1: basePrice = (int)Math.round( ((float)basePrice) * 0.9 ); break;
		case 0: break;
		case 1: basePrice += 1500; break;
		case 2: basePrice += 3000; break;
		case 3: basePrice += 6000; break;
		case 4: basePrice += 9000; break;
		case 5: basePrice += 15000; break;
		case 6: basePrice += 21000; break;
		default:
			basePrice += 21000;
			basePrice += (maxQuality-6) * 7500;			
		}
		
		/*
		 * Now for artifact quality
		 */
		switch (model.getArtifactQuality()) {
		case 0: break;
		case 1: basePrice += 1500; break;
		case 2: basePrice += 3000; break;
		case 3: basePrice += 6000; break;
		case 4: basePrice += 9000; break;
		case 5: basePrice += 15000; break;
		case 6: basePrice += 21000; break;
		default:
			basePrice += 21000;
			basePrice += (maxQuality-6) * 7500;			
		}
		
		// Personalizations
		basePrice += model.getPersonalizations().size()*1500;
		return basePrice;		
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#setFirstPersonalization(org.prelle.splimo.items.Personalization)
	 */
	@Override
	public void setFirstPersonalization(PersonalizationReference toAdd) {
		logger.info("Set 1. personalization to "+toAdd);
		if (!model.getPersonalizations().isEmpty()) { 
			model.removePersonalization(model.getPersonalizations().get(0)); 
		}
		if (toAdd != null && (model.getPersonalizations().size() == makeCompartibleList(toAdd, model.getPersonalizations()).size())) {
			model.getPersonalizations().add(0, toAdd); 
		}
		
		fireItemChange();
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#setSecondPersonalization(org.prelle.splimo.items.Personalization)
	 */
	@Override
	public void setSecondPersonalization(PersonalizationReference toAdd) {
		if (model.getPersonalizations().size() >= 2) { 
			model.removePersonalization(model.getPersonalizations().get(1)); 
		}
		if (toAdd != null && (model.getPersonalizations().size() == makeCompartibleList(toAdd, model.getPersonalizations()).size())) {
				model.getPersonalizations().add(toAdd); 
		} 
		
		fireItemChange();
	}

	@Override
	public List<PersonalizationReference> makeCompartibleList(PersonalizationReference template, List<PersonalizationReference> values) {
	 	List<PersonalizationReference> result = new ArrayList<>(values);
		if (values.isEmpty() || template == null) {
			return result;
		}
		List<AttributeChangeModification> attrs = template.getModifications().stream()
								                          .filter(AttributeChangeModification.class::isInstance)
								                          .map(AttributeChangeModification.class::cast)
								                          .collect(Collectors.toList());   
		for (Iterator<PersonalizationReference> iter = result.iterator(); iter.hasNext();) {
			// don't add a modification with the same attribute as toAdd
			PersonalizationReference next = iter.next();
			if (next != null && next.getModifications().stream().filter(AttributeChangeModification.class::isInstance)
				    			    		           .map(AttributeChangeModification.class::cast) 
												  	   .anyMatch(mod -> attrs.stream().anyMatch(attr ->    attr.getFrom().equals(mod.getFrom())  
													            		                                || attr.getTo()  .equals(mod.getTo()  )))) 
			{
				iter.remove();
			}
		}
		return result;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#isSecondPersonalizationAllowed()
	 */
	@Override
	public boolean isSecondPersonalizationAllowed() {
		if (model==null || model.getMaterial()==null)
			return false;
		return "oreofthedead".equals(model.getMaterial().getId());
	}
	
	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.Controller#getToDos()
	 */
	@Override
	public List<String> getToDos() {
		List<String> ret = new ArrayList<>();
		int quality = model.getArtifactQuality() + model.getItemQuality();
		if (quality<maximumQuality)
			ret.add(String.format(RES.getString("itemgen.todo.points"), maximumQuality-quality));

		return ret;
	}

	//--------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#getAvailableFirstPersonalizations()
	 */
	@Override
	public List<PersonalizationReference> getAvailableFirstPersonalizations() {
		List<PersonalizationReference> ret = new ArrayList<>(availPersonal);
		ret.add(0, null);
		if (model.getPersonalizations().size()>0) {
			ret.add(model.getPersonalizations().get(0));
		}
		
//		if (model.getItem().isType(ItemType.LONG_RANGE_WEAPON)) {
//			for (Personalization perso : SplitterMondCore.getPersonalizations(ItemType.LONG_RANGE_WEAPON)) {
//				ret.add(new PersonalizationReference(perso));
//			}
//		} else if (model.getItem().isType(ItemType.WEAPON)) {
//			for (Personalization perso : SplitterMondCore.getPersonalizations(ItemType.WEAPON)) {
//				ret.add(new PersonalizationReference(perso));
//			}
//		} else if (model.getItem().isType(ItemType.SHIELD)) {
//			for (Personalization perso : SplitterMondCore.getPersonalizations(ItemType.SHIELD)) {
//				ret.add(new PersonalizationReference(perso));
//			}
//		} else if (model.getItem().isType(ItemType.ARMOR)) {
//			for (Personalization perso : SplitterMondCore.getPersonalizations(ItemType.ARMOR)) {
//				ret.add(new PersonalizationReference(perso));
//			}
//		}
		return ret;
	}

	//--------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.charctrl.NewItemController#getAvailableSecondPersonalizations()
	 */
	@Override
	public List<PersonalizationReference> getAvailableSecondPersonalizations() {
		List<PersonalizationReference> ret = new ArrayList<>(availPersonal);
		ret.add(0, null);
		if (model.getPersonalizations().size()>1)
			ret.add(model.getPersonalizations().get(1));
		return ret;
	}

}
