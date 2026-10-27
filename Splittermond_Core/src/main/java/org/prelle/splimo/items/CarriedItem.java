package org.prelle.splimo.items;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prelle.simplepersist.AttribConvert;
import org.prelle.simplepersist.Element;
import org.prelle.simplepersist.ElementList;
import org.prelle.simplepersist.Root;
import org.prelle.splimo.Attribute;
import org.prelle.splimo.ResourceReference;
import org.prelle.splimo.Skill;
import org.prelle.splimo.UniqueObject;
import org.prelle.splimo.items.Enhancement.EnhancementType;
import org.prelle.splimo.modifications.AttributeChangeModification;
import org.prelle.splimo.modifications.FeatureModification;
import org.prelle.splimo.modifications.ItemModification;
import org.prelle.splimo.modifications.ModificationList;
import org.prelle.splimo.persist.ItemConverter;
import org.prelle.splimo.persist.MaterialConverter;
import org.prelle.splimo.requirements.Requirement;

import de.rpgframework.genericrpg.modification.Modification;

@Root(name = "itemref")
public class CarriedItem extends UniqueObject implements Comparable<CarriedItem> {

	private final static Logger logger = LogManager.getLogger("splittermond.items");

	@org.prelle.simplepersist.Attribute(name="ref",required=true)
	@AttribConvert(ItemConverter.class)
	private ItemTemplate ref;
	@org.prelle.simplepersist.Attribute(name="count",required=false)
	private int count = 1;
	@org.prelle.simplepersist.Attribute
	private ItemLocationType location;
	@org.prelle.simplepersist.Attribute(required=false)
	private int quality;
	@org.prelle.simplepersist.Attribute(required=false)
	private String customName;
	@org.prelle.simplepersist.Attribute(required=false)
	@AttribConvert(MaterialConverter.class)
	private Material material;

	@Element(name="modifications", required=false)
	private ModificationList ignoreModifications;
	@ElementList(entry="enhancement",type=EnhancementReference.class)
	private List<EnhancementReference> enhancements;
	/**
	 * Modifications made to the item. Calculated from enhancements
	 */
	private transient List<Modification> modifications;
	/**
	 * Calculated from enhancements
	 */
	@ElementList(entry="personalization",type=PersonalizationReference.class)
	private List<PersonalizationReference> personalizations;
	@Element
	private String description;
	private transient ResourceReference resourceRef;

	/**
	 * Modifications made to the character when equipped. Calculated from
	 * enhancements
	 */
	private transient List<Modification> characterModifications;
	/**
	 * Modifications made to the character when equipped, resulting from not
	 * fulfilled requirements.
	 */
	private transient List<Modification> requirementModifications;

	//--------------------------------------------------------------------
	public CarriedItem() {
		ignoreModifications = new ModificationList();
		location = ItemLocationType.CONTAINER;
		enhancements = new ArrayList<>();
		modifications = new ArrayList<>();
		personalizations = new ArrayList<>();
		characterModifications = new ArrayList<>();
		requirementModifications = new ArrayList<>();
		count = 1;
	}

	//--------------------------------------------------------------------
	public CarriedItem(int quality) {
		this();
		this.quality = quality;
	}

	//--------------------------------------------------------------------
	public CarriedItem(ItemTemplate template) {
		this();
		ref = template;
		this.count = 1;
	}

	//--------------------------------------------------------------------
	public CarriedItem(ItemTemplate template, int count) {
		this();
		ref = template;
		this.count = count;
	}

	//--------------------------------------------------------------------
	public boolean equals(Object o) {
		if (o instanceof CarriedItem) {
			CarriedItem other = (CarriedItem)o;
			if (ref!=other.getItem()) return false;
			if (location!=other.getLocation()) return false;
			if (customName!=null && !customName.equals(other.getName())) return false;
			if (getUniqueId()!=null && other.getUniqueId()==null) return false;
			if (getUniqueId()==null && other.getUniqueId()!=null) return false;
			if (!getUniqueId().equals(other.getUniqueId())) return false;
			return enhancements.equals(other.getEnhancements());
		}
		return false;
	}

	//--------------------------------------------------------------------
	public String toString() {
		return "CarriedItem "+ref; //+" (Mods: "+modifications+")"; //+" (Depracated: "+ignoreModifications+")";
	}

	//--------------------------------------------------------------------
	/**
	 * @return the item
	 */
	public ItemTemplate getItem() {
		return ref;
	}

	//--------------------------------------------------------------------
	/**
	 * @param item the item to set
	 */
	public void setItem(ItemTemplate item) {
		this.ref = item;
	}

	//--------------------------------------------------------------------
	public ItemLocationType getLocation() {
		return location;
	}

	//--------------------------------------------------------------------
	public void setItemLocation(ItemLocationType location) {
		this.location = location;
	}

	//--------------------------------------------------------------------
	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(CarriedItem other) {
		return Collator.getInstance().compare(getName(), other.getName());
	}

	//--------------------------------------------------------------------
	public boolean isType(ItemType type) {
		return ref.isType(type);
	}

	//--------------------------------------------------------------------
	public int getDamage(ItemType type) {
		// Ensure template is of the expected type
		if (!ref.isType(type))
			return 0;


		int result = 0;

		switch (type) {
		case WEAPON:
			Weapon weapon = ref.getType(Weapon.class);
			result = weapon.getDamage();
			break;
		case LONG_RANGE_WEAPON:
			LongRangeWeapon ranged = ref.getType(LongRangeWeapon.class);
			result = ranged.getDamage();
			break;
		default:
			logger.warn("No damage attribute for type "+type);
			return 0;
		}

		// Apply modifier
		for (Modification mod : modifications) {
			if (mod instanceof ItemModification) {
				ItemModification imod = (ItemModification)mod;
				logger.trace("Consider "+mod);
				int oldHigh = result/100*100;
				int oldLow  = result%100;
				if (imod.getAttribute()==ItemAttribute.DAMAGE && (type==ItemType.WEAPON || type==ItemType.LONG_RANGE_WEAPON)) {
					oldLow += imod.getValue();
					if (oldLow<0) oldLow=(100+oldLow);
					result = oldHigh + (oldLow%100);
				}
			}
		}


		return result;
	}

	//-------------------------------------------------------------------
	public String getName() {
		if (customName!=null)
			return customName;
		if (ref==null)
			return "??Unknown??";
		return ref.getName();
	}

	//-------------------------------------------------------------------
	public void setCustomName(String name) {
		customName = name;
	}

	//-------------------------------------------------------------------
	public int getRigidity() {
		int value = ref.getRigidity();
		for (Modification mod : modifications) {
			if (mod instanceof ItemModification) {
				ItemModification imod = (ItemModification)mod;
				if (imod.getAttribute()==ItemAttribute.RIGIDITY)
					value += imod.getValue();
			}
		}

		// This material's lower bound cannot be represented by an additive XML modifier.
		if (material!=null && "ghostiron".equals(material.getId()))
			return Math.max(1, value-2);
		return value;
	}

	//-------------------------------------------------------------------
	public int getPrice() {
		return ref.getPrice();
	}

	//--------------------------------------------------------------------
	public int getLoad() {
		int value = ref.getLoad();
		for (Modification mod : modifications) {
			if (mod instanceof ItemModification) {
				ItemModification imod = (ItemModification)mod;
				if (imod.getAttribute()==ItemAttribute.LOAD)
					value += imod.getValue();
			}
		}

		return value;
	}

	//--------------------------------------------------------------------
	public Availability getAvailability() {
		return ref.getAvailability();
	}

	//--------------------------------------------------------------------
	public int getDamageReduction(ItemType type) {
		// Ensure template is of the expected type
		if (!ref.isType(type))
			return 0;


		int result = 0;

		switch (type) {
		case ARMOR:
			Armor armor = ref.getType(Armor.class);
			result = armor.getDamageReduction();
			break;
		default:
			logger.warn("No DamageReduction attribute for type "+type);
					return 0;
		}

		// Apply modifier
		for (Modification mod : modifications) {
			if (mod instanceof ItemModification) {
				ItemModification imod = (ItemModification)mod;
				if (imod.getAttribute()==ItemAttribute.DAMAGE_REDUCTION)
					result += imod.getValue();
			}
		}

		return result;
	}

	//--------------------------------------------------------------------
	public int getDefense(ItemType type) {
		// Ensure template is of the expected type
		if (!ref.isType(type))
			return 0;


		int result = 0;

		switch (type) {
		case ARMOR:
			Armor armor = ref.getType(Armor.class);
			result = armor.getDefense();
			break;
		case SHIELD:
			Shield shield = ref.getType(Shield.class);
			if (shield!=null)
				result = shield.getDefense();
			break;
		default:
			logger.warn("No Defense attribute for type "+type);
					return 0;
		}

		// Apply modifier
		for (Modification mod : modifications) {
			if (mod instanceof ItemModification) {
				ItemModification imod = (ItemModification)mod;
				if (imod.getAttribute()==ItemAttribute.DEFENSE)
					result += imod.getValue();
			}
		}

		// No negative results
		if (result<0)
			result = 0;

		return result;
	}

	//--------------------------------------------------------------------
	/**
	 * Like getDefense() but ignoring some modifications (e.g. from personalization)
	 */
	public int getDefenseEvade(ItemType type) {
		// Ensure template is of the expected type
		if (!ref.isType(type))
			return 0;


		int result = 0;

		switch (type) {
		case ARMOR:
			Armor armor = ref.getType(Armor.class);
			result = armor.getDefense();
			break;
		case SHIELD:
			Shield shield = ref.getType(Shield.class);
			if (shield!=null)
				result = shield.getDefense();
			break;
		default:
			logger.warn("No Defense attribute for type "+type);
					return 0;
		}

		// Apply modifier
		for (Modification mod : modifications) {
			if (mod.getSource() instanceof PersonalizationReference)
				continue;
			if (mod instanceof ItemModification) {
				ItemModification imod = (ItemModification)mod;
				if (imod.getAttribute()==ItemAttribute.DEFENSE)
					result += imod.getValue();
			}
		}

		// No negative results
		if (result<0)
			result = 0;

		return result;
	}

	//--------------------------------------------------------------------
	public int getHandicap(ItemType type) {
		// Ensure template is of the expected type
		if (!ref.isType(type))
			return 0;


		int result = 0;

		switch (type) {
		case ARMOR:
			Armor armor = ref.getType(Armor.class);
			result = armor.getHandicap();
			break;
		case SHIELD:
			Shield shield = ref.getType(Shield.class);
			if (shield!=null)
				result = shield.getHandicap();
			break;
		default:
			logger.warn("No Handicap attribute for type "+type);
					return 0;
		}

		// Apply modifier
		for (Modification mod : modifications) {
			if (mod instanceof ItemModification) {
				ItemModification imod = (ItemModification)mod;
				if (imod.getAttribute()==ItemAttribute.HANDICAP)
					result += imod.getValue();
			}
		}

		return result;
	}

	//--------------------------------------------------------------------
	public List<Requirement> getRequirements(ItemType type) {
		if (!ref.isType(type))
			return new ArrayList<>();

		try {
			switch (type) {
			case ARMOR:
				return ref.getType(Armor.class).getRequirements();
			case WEAPON:
				return ref.getType(Weapon.class).getRequirements();
			case LONG_RANGE_WEAPON:
				return ref.getType(LongRangeWeapon.class).getRequirements();
			case SHIELD:
				return ref.getType(Shield.class).getRequirements();
			default:
				return new ArrayList<>();
			}
		} catch (NullPointerException e) {
			return new ArrayList<>();
		}
	}

	//--------------------------------------------------------------------
	public int getTickMalus(ItemType type) {
		// Ensure template is of the expected type
		if (!ref.isType(type))
			return 0;


		int result = 0;

		switch (type) {
		case ARMOR:
			Armor armor = ref.getType(Armor.class);
			result = armor.getTickMalus();
			break;
		case SHIELD:
			Shield shield = ref.getType(Shield.class);
			if (shield!=null)
				result = shield.getTickMalus();
			break;
		default:
			logger.warn("No TickMalus attribute for type "+type);
					return 0;
		}

		// Apply modifier
		for (Modification mod : modifications) {
			if (mod instanceof ItemModification) {
				ItemModification imod = (ItemModification)mod;
				if (imod.getAttribute()==ItemAttribute.TICK_MALUS)
					result += imod.getValue();
			}
		}

		return result;
	}

	//-------------------------------------------------------------------
	public List<Feature> getFeatures(ItemType type) {
		// Ensure template is of the expected type
		List<Feature> result = new FeatureList();
		if (!ref.isType(type))
			return result;

		switch (type) {
		case ARMOR:
			Armor armor = ref.getType(Armor.class);
			if (armor!=null)
				result.addAll(armor.deliverFeaturesAsDeepClone());
			break;
		case SHIELD:
			Shield shield = ref.getType(Shield.class);
			if (shield!=null)
				result.addAll(shield.deliverFeaturesAsDeepClone());
			break;
		case LONG_RANGE_WEAPON:
			LongRangeWeapon longRangeWeapon = ref.getType(LongRangeWeapon.class);
			result.addAll(longRangeWeapon.deliverFeaturesAsDeepClone());
			break;
		case WEAPON:
			Weapon weapon = ref.getType(Weapon.class);
			result.addAll(weapon.deliverFeaturesAsDeepClone());
			break;
		default:
			logger.warn("No Features attribute for type " + type);
					break;
		}

		/*
		 * Now apply modifications
		 */
		 outer:
			 for (Modification mod : getItemModifications()) {
				 logger.info("Apply "+mod);
				 if (mod instanceof FeatureModification) {
					 FeatureModification fMod = (FeatureModification)mod;
					 if (fMod.isRemoved()) {
						 // Find matching feature and either reduce it or remove it
						 for (Feature feat : result) {
							 if (feat.getType()==fMod.getFeature()) {
								 if (feat.getLevel()>1)
									 feat.setLevel(feat.getLevel()-1);
								 else
									 result.remove(feat);
								 break;
							 }
						 }
					 } else {
						 // if feature already exists, increase it
						 for (Feature feat : result) {
							 if (feat.getType()==fMod.getFeature()) {
								 feat.setLevel(feat.getLevel()+1);
								 continue outer;
							 }
						 }
						 result.add(new Feature(fMod.getFeature(),1));
					 }
				 }
			 }

		return result;
	}

	//--------------------------------------------------------------------
	public Attribute getAttribute1(ItemType type){ 
		return getAttribute(type, weapon -> weapon.getAttribute1());
	}
	
	private Attribute getAttribute(ItemType type, Function<Weapon, Attribute> getter){
		Attribute result = null;
		if (!ref.isType(type)) {
			return result;
		}
		switch (type) {
		case WEAPON:
			Weapon weapon = ref.getType(Weapon.class);
			result = getter.apply(weapon); //weapon.getAttribute1();
			break;
		case LONG_RANGE_WEAPON:
			Weapon longRangeWeapon = ref.getType(LongRangeWeapon.class);
			result = getter.apply(longRangeWeapon);//longRangeWeapon.getAttribute1();
			break;
		default:
			logger.warn("No attribute for type " + type);
			return result;
		}

		// Now apply modifications
		for (PersonalizationReference ref : personalizations) {
			for (Modification mod : ref.getModifications()) {
				if (mod instanceof AttributeChangeModification) {
					AttributeChangeModification aMod = (AttributeChangeModification)mod;
					if (aMod.getFrom()==result)
						result = aMod.getTo();
				}
			}
		}
		return result;
	} 

	//--------------------------------------------------------------------
	public Attribute getAttribute2(ItemType type){ 
		return getAttribute(type, weapon -> weapon.getAttribute2()); 
	}

	//--------------------------------------------------------------------
	public Skill getSkill(ItemType type){
		Skill result = ref.getSkill();
		if (!ref.isType(type)) {
			return result;
		}
		switch (type) {

		case WEAPON:
			Weapon weapon = ref.getType(Weapon.class);
			result = weapon.getSkill();
			break;
		case LONG_RANGE_WEAPON:
			LongRangeWeapon longRangeWeapon = ref.getType(LongRangeWeapon.class);
			result = longRangeWeapon.getSkill();
			break;
		default:
			logger.warn("No Skill attribute for type " + type);
					break;
		}
		return result;
	}

	//--------------------------------------------------------------------
	public int getSpeed(ItemType type) {
		int result = 0;
		if (!ref.isType(type)) {
			logger.warn("Item "+ref+" should be of type "+type);
			return result;
		}
		switch (type) {
		case WEAPON:
			Weapon weapon = ref.getType(Weapon.class);
			result = weapon.getSpeed();
			break;
		case LONG_RANGE_WEAPON:
			LongRangeWeapon longRangeWeapon = ref.getType(LongRangeWeapon.class);
			result = longRangeWeapon.getSpeed();
			break;
		default:
			logger.warn("No Speed attribute for type " + type);
			break;
		}

		// Apply modifier
		for (Modification mod : modifications) {
			if (mod instanceof ItemModification) {
				ItemModification imod = (ItemModification)mod;
				if (imod.getAttribute()==ItemAttribute.SPEED && (type==ItemType.WEAPON || type==ItemType.LONG_RANGE_WEAPON))
					result += imod.getValue();
			}
		}

		return result;
	}

	//	//--------------------------------------------------------------------
	//    public static int getModificationSize(Modification mod) {
	//    	if (mod instanceof AttributeModification) {
	//    		Attribute attrib = ((AttributeModification)mod).getAttribute();
	//    		switch (attrib) {
	//    		case BODYRESIST:
	//    		case MINDRESIST:
	//    			return 2*((AttributeModification)mod).getValue();
	//    		case AGILITY:
	//    		case CHARISMA:
	//    		case CONSTITUTION:
	//    		case FOCUS:
	//    		case INTUITION:
	//    		case MYSTIC:
	//    		case STRENGTH:
	//    		case WILLPOWER:
	//    			return 1;
	//    		default:
	//    			return 0;
	//    		}
	//    	} else if (mod instanceof ItemModification) {
	//    		ItemAttribute attrib = ((ItemModification)mod).getAttribute();
	//    		switch (attrib) {
	//    		case LOAD:
	//    			return -1 * ((ItemModification)mod).getValue();
	//    		case DAMAGE:
	//    		case RIGIDITY:
	//    			return 1 * ((ItemModification)mod).getValue();
	//    		case SPEED:
	//    		case HANDICAP:
	//    		case TICK_MALUS:
	//    			return -2 * ((ItemModification)mod).getValue();
	//    		case DAMAGE_REDUCTION:
	//    			return 2 * ((ItemModification)mod).getValue();
	//    		}
	//    		return 0;
	//    	} else if (mod instanceof SpellModification) {
	//    		SpellValue spell = ((SpellModification)mod).getSpell();
	//    		return spell.getSpellLevel()+1;
	//    	} else if (mod instanceof SkillModification) {
	//    		return 2;
	//    	} else if (mod instanceof MastershipModification) {
	//    		MastershipModification mMod = (MastershipModification)mod;
	//    		if (mMod.getSpecialization()!=null)
	//    			return mMod.getSpecialization().getLevel()*2;
	//       		logger.error("Don't know how to determine quality cost for "+mod);
	//       		return 0;
	//    	} else
	//    		logger.error("Don't know how to determine quality cost for "+mod);
	//    	return 0;
	//    }

	//--------------------------------------------------------------------
	public void addItemModification(Modification toAdd) {
		logger.info("Add item modification "+toAdd+" to "+getName());
		//    	if (!modifications.contains(toAdd))
		modifications.add(toAdd);
	}

	//--------------------------------------------------------------------
	public void removeItemModification(Modification toRem) {
		modifications.remove(toRem);
	}

	//--------------------------------------------------------------------
	public List<Modification> getItemModifications() {
		return modifications;
	}

	//--------------------------------------------------------------------
	@Deprecated
	public void addModification(Modification toAdd) {
		if (!ignoreModifications.contains(toAdd))
			ignoreModifications.add(toAdd);
	}

	//--------------------------------------------------------------------
	@Deprecated
	public void removeModification(Modification toRem) {
		ignoreModifications.remove(toRem);
	}

	//--------------------------------------------------------------------
	@Deprecated
	public List<Modification> getModifications() {
		return ignoreModifications;
	}

	//--------------------------------------------------------------------
	public void addPersonalization(PersonalizationReference toAdd) {
		if (!personalizations.contains(toAdd)) {
			personalizations.add(toAdd);
			logger.debug("add modifications from perso "+toAdd.getModifications());
			for (Modification mod : toAdd.getModifications()) {
				addItemModification(mod);
			}
		}
	}

	//--------------------------------------------------------------------
	public void removePersonalization(PersonalizationReference toRem) {
		if (personalizations.remove(toRem)) {
			logger.warn("TODO: remove modifications from perso "+toRem.getModifications());
			for (Modification mod : toRem.getModifications()) {
				removeItemModification(mod);
			}
		}
	}

	//--------------------------------------------------------------------
	public List<PersonalizationReference> getPersonalizations() {
		return personalizations;
	}

	//--------------------------------------------------------------------
	private int[] getQuality() {
		int normal = 0;
		int alchemy = ref.getCatalogProfile()==null ? 0 : ref.getCatalogProfile().getAlchemyCost();
		int magic  = 0;
		int relic  = 0;
//		if (material!=null && !material.getId().startsWith("common"))
//			normal++;
		for (EnhancementReference enhance : enhancements) {
//			logger.warn("****Enhancement "+enhance+" is of type "+enhance.getEnhancement().getType()+" and of size "+enhance.getEnhancement().getSize());
			switch (enhance.getEnhancement().getType()) {
			case MAGIC: magic+=enhance.getEnhancement().getSize(); break;
			case ALCHEMY:
				if (ref.getCatalogProfile()!=null) alchemy+=enhance.getEnhancement().getSize();
				else normal+=enhance.getEnhancement().getSize();
				break;
//			case RELIC: relic+=enhance.getEnhancement().getSize(); break;
			default:
				normal+=enhance.getEnhancement().getSize();
			}
		}
		
		if (ref.getCatalogProfile()!=null) normal += Math.max(0, alchemy-1);
		if (material!=null) {
			normal = Math.max(normal, material.getQuality());
		}
		return new int[]{normal, magic, relic};
	}

	//--------------------------------------------------------------------
	public int getQuality(EnhancementType type) {
		int count = type==EnhancementType.ALCHEMY && ref.getCatalogProfile()!=null
				? ref.getCatalogProfile().getAlchemyCost() : 0;
		for (EnhancementReference enhance : enhancements) {
			if (enhance.getEnhancement().getType()==type)
				count += enhance.getEnhancement().getSize();
		}
		return count;
	}

	//--------------------------------------------------------------------
	/**
	 * @return the quality
	 */
	public int getArtifactQuality() {
		return getQuality()[1];
	}

	//--------------------------------------------------------------------
	/**
	 * @return the quality
	 */
	public int getItemQuality() {
		if (ref.isType(ItemType.POTION) && ref.getCatalogProfile()==null)
			return getQuality()[0]-1;
		return getQuality()[0];
	}

	//--------------------------------------------------------------------
	public int getRelicQuality() {
		int[] quality = getQuality();
		int sum = 0;
		for (int tmp : quality)
			sum+=tmp;
		if (material!=null && !material.getId().startsWith("common"))
			sum++;
		return sum;
	}

	//--------------------------------------------------------------------
	public int getTotalQuality() {
		// Finished catalogue items carry their included quality, not an empty recipe budget.
		if (ref.getCatalogProfile()!=null) return getItemQuality() + getArtifactQuality();
		int quality = 0;
		for (EnhancementReference enhance : enhancements) {
			quality+=enhance.getEnhancement().getSize();
		}
		
		if (material!=null) {
			quality = Math.max(quality, material.getQuality());
		}
		return quality;
	}

	//--------------------------------------------------------------------
	/**
	 * @return TRUE if the attribute has not the standard value
	 */
	public boolean isModified(ItemAttribute attr) {
		for (Modification mod : modifications) {
			if (mod instanceof ItemModification) {
				ItemModification iMod = (ItemModification)mod;
				if (iMod.getAttribute()==attr)
					return true;
			}
		}
		return false;
	}

	//--------------------------------------------------------------------
	public int getAttribute(ItemAttribute attrib) {
		switch (attrib) {
		case LOAD: return getLoad();
		case RIGIDITY: return getRigidity();
		case DAMAGE_REDUCTION: return getDamageReduction(ItemType.ARMOR);
		case HANDICAP: return getHandicap(ItemType.ARMOR);
		case DAMAGE:
			throw new IllegalArgumentException(attrib+" not supported here. Use getDamage(ItemType");
		case SPEED:
			throw new IllegalArgumentException(attrib+" not supported here. Use getSpeed(ItemType");
		default:
			throw new IllegalArgumentException("Attribute "+attrib+" not implemeted yet");
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @return the enhancements
	 */
	public List<EnhancementReference> getEnhancements() {
		return enhancements;
	}

	//-------------------------------------------------------------------
	public List<EnhancementReference> getEnhancements(EnhancementType type) {
		List<EnhancementReference> ret = new ArrayList<EnhancementReference>();
		enhancements.forEach(cons -> {
			if (cons.getEnhancement().getType()==type)
				ret.add(cons);
		});
		return ret;
	}

	//-------------------------------------------------------------------
	public boolean addEnhancement(EnhancementReference enhancement) {
		if (enhancements.contains(enhancement)) {
			logger.warn("prevented double addition of enhancement '"+enhancement+"' to item");
			return false;
		}
		enhancements.add(enhancement);

		updateModifications();
		return true;
	}

	//-------------------------------------------------------------------
	public boolean removeEnhancement(EnhancementReference enhancement) {
		if (enhancements.contains(enhancement)) {
			enhancements.remove(enhancement);

			updateModifications();
			return true;
		} else
			logger.warn("Item did not contain removed enhancement "+enhancement);
		return false;
	}

	//-------------------------------------------------------------------
	private void updateModifications() {
		modifications.clear();
		for (EnhancementReference ref : enhancements) {
			modifications.addAll(ref.getEnhancement().getModifications());
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @return the material
	 */
	public Material getMaterial() {
		return material;
	}

	//-------------------------------------------------------------------
	/**
	 * @param material the material to set
	 */
	public void setMaterial(Material material) {
		this.material = material;
	}

	//-------------------------------------------------------------------
	public boolean isRelic() {
		return resourceRef!=null;
	}

	//-------------------------------------------------------------------
	public ResourceReference getResource() {
		return resourceRef;
	}

	//-------------------------------------------------------------------
	/**
	 * @param resrc the relic to set
	 */
	public void setResource(ResourceReference resrc) {
		this.resourceRef = resrc;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	//-------------------------------------------------------------------
	/**
	 * @param count the count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the characterModifications
	 */
	public List<Modification> getCharacterModifications() {
		return characterModifications;
	}

	//-------------------------------------------------------------------
	public void addCharacterModification(Modification mod) {
//		if (!characterModifications.contains(mod))
			characterModifications.add(mod);
	}

	//-------------------------------------------------------------------
	public void removeCharacterModification(Modification mod) {
		characterModifications.remove(mod);
	}

	//-------------------------------------------------------------------
	/**
	 * Get the modifications calculated based upon requirements not met by
	 * the character
	 */
	public List<Modification> getRequirementModifications() {
		return requirementModifications;
	}

	//-------------------------------------------------------------------
	public void addRequirementModification(Modification mod) {
		if (!requirementModifications.contains(mod))
			requirementModifications.add(mod);
	}

	//-------------------------------------------------------------------
	public void clearRequirementModification() {
		requirementModifications.clear();
	}

	//--------------------------------------------------------------------
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	//--------------------------------------------------------------------
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

}
