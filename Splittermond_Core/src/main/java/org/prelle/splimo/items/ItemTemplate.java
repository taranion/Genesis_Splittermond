package org.prelle.splimo.items;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.MissingResourceException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prelle.simplepersist.AttribConvert;
import org.prelle.simplepersist.Attribute;
import org.prelle.simplepersist.ElementList;
import org.prelle.simplepersist.ElementListUnion;
import org.prelle.simplepersist.Root;
import org.prelle.splimo.BasePluginData;
import org.prelle.splimo.Skill;
import org.prelle.splimo.SkillSpecialization;
import org.prelle.splimo.persist.ComplexityConverter;
import org.prelle.splimo.persist.ItemTypeConverter;
import org.prelle.splimo.persist.SkillConverter;
import org.prelle.splimo.persist.SpecializationConverter;

/**
 * @author prelle
 *
 */
@Root(name = "item")
public class ItemTemplate extends BasePluginData implements Comparable<ItemTemplate> {

	private static Logger logger = LogManager.getLogger("splittermond.items");

	@Attribute(required=true)
	private String id;
	@Attribute(required=false)
	private String customName;
	@Attribute(required=true)
	private int load;
	@Attribute(name="robust",required=true)
	private int rigidity;
	@Attribute(name="avail",required=true)
	private Availability availability;
	/* Price in Telaren */
	@Attribute(required=true)
	private int price;
	@Attribute(name="cplx",required=false)
	@AttribConvert(ComplexityConverter.class)
	private Complexity complexity;
	@Attribute(name="type",required=false)
	@AttribConvert(ItemTypeConverter.class)
	private Collection<ItemType> types;
	@ElementListUnion({
	    @ElementList(entry="weapon", type=Weapon.class),
	    @ElementList(entry="rangeweapon", type=LongRangeWeapon.class),
	    @ElementList(entry="shield", type=Shield.class),
	    @ElementList(entry="armor", type=Armor.class),
	    @ElementList(entry="projectile", type=Projectile.class),
	    @ElementList(entry="light", type=Light.class),
	 })
	private List<ItemTypeData> typeData;
	@Attribute(name="material")
	private MaterialType materialType;
	/**
	 * Which skill do enhancements of this object influence
	 */
	@Attribute(name="skill")
	@AttribConvert(SkillConverter.class)
	private Skill skill;
	/**
	 * Which skill specialization do enhancements of this object influence
	 */
	@Attribute(name="spec")
	@AttribConvert(SpecializationConverter.class)
	private SkillSpecialization specialization;
	
	/** Loaded from optional sidecar data; never written into legacy item definitions. */
	private transient List<ItemSpellBonus> spellBonuses = new ArrayList<>();
	/** Optional sidecar metadata; original catalogue XML remains readable by old cores. */
	private transient ItemCatalogProfile catalogProfile;

	public ItemCatalogProfile getCatalogProfile() { return catalogProfile; }

	public void setCatalogProfile(ItemCatalogProfile profile) { catalogProfile = profile; }

	public List<ItemSpellBonus> getSpellBonuses() {
		return java.util.Collections.unmodifiableList(spellBonuses);
	}

	public void setSpellBonuses(List<ItemSpellBonus> bonuses) {
		spellBonuses = new ArrayList<>(bonuses);
	}

	//--------------------------------------------------------------------
	public ItemTemplate() {
		typeData = new ArrayList<ItemTypeData>();
		types    = new ArrayList<>();
		materialType = MaterialType.OTHER;
	}
	
	//--------------------------------------------------------------------
	public ItemTemplate(String id) {
		this();
		this.id = id;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.PluginData#getId()
	 */
	@Override
	public String getId() {
		return id;
	}
	
	//-------------------------------------------------------------------
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer(id+"(");
		buf.append("ld="+load);
		buf.append(",rob="+rigidity);
		buf.append(",cplx="+complexity);
		buf.append(",types="+typeData);
		buf.append(")");
		return buf.toString();
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.BasePluginData#getPageI18NKey()
	 */
	@Override
	public String getPageI18NKey() {
		return "item."+id+".page";
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.ubiquity.BasePluginData#getHelpI18NKey()
	 */
	@Override
	public String getHelpI18NKey() {
		return "item."+id+".desc";
	}
	
	//--------------------------------------------------------------------
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (o instanceof ItemTemplate) {
			ItemTemplate other = (ItemTemplate)o;
			if (!id.equals(other.getID())) return false;
			if (load!=other.getLoad()) return false;
			if (rigidity!=other.getRigidity()) return false;
			if (availability!=other.getAvailability()) return false;
			if (price!=other.getPrice()) return false;
			if (complexity!=other.getComplexity()) return false;
			return true;
		}
		return false;
	}

	//--------------------------------------------------------------------
	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ItemTemplate other) {
//		int cmp = type.compareTo(other.getType());
//		if (cmp!=0) return cmp;
		
		return Collator.getInstance().compare(getName(), other.getName());
	}
	
	//--------------------------------------------------------------------
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return id.hashCode();
	}

	//-------------------------------------------------------------------
	public String getName() {
		if (i18n==null || customName!=null)
			return customName;
		try {
			return i18n.getString("item."+id);
		} catch (MissingResourceException e) {
			logger.error(e.getMessage()+" in "+i18n.getBaseBundleName());
			return "item."+id;
		}
	}
	
	//--------------------------------------------------------------------
	public String getID() {
		return id;
	}
	
	//--------------------------------------------------------------------
	public void setID(String newID) {
		this.id = id;
	}

	//--------------------------------------------------------------------
	/**
	 * @return the load
	 */
	public int getLoad() {
		return load;
	}

	//--------------------------------------------------------------------
	/**
	 * @param load the load to set
	 */
	public void setLoad(int load) {
		this.load = load;
	}

	//--------------------------------------------------------------------
	/**
	 * @return the robustness
	 */
	public int getRigidity() {
		return rigidity;
	}

	//--------------------------------------------------------------------
	/**
	 * @param rigidity the robustness to set
	 */
	public void setRigidity(int rigidity) {
		this.rigidity = rigidity;
	}

	//--------------------------------------------------------------------
	/**
	 * @return the durability
	 */
	public int getDurability() {
		return load+rigidity;
	}

	//--------------------------------------------------------------------
	/**
	 * @return the availability
	 */
	public Availability getAvailability() {
		return availability;
	}

	//--------------------------------------------------------------------
	/**
	 * @param availability the availability to set
	 */
	public void setAvailability(Availability availability) {
		this.availability = availability;
	}

	//--------------------------------------------------------------------
	/**
	 * @return the complexity
	 */
	public Complexity getComplexity() {
		return complexity;
	}

	//--------------------------------------------------------------------
	/**
	 * @param complexity the complexity to set
	 */
	public void setComplexity(Complexity complexity) {
		this.complexity = complexity;
	}

	//--------------------------------------------------------------------
	/**
	 * @return the price
	 */
	public int getPrice() {
		return price;
	}

	//--------------------------------------------------------------------
	/**
	 * @param price the price to set
	 */
	public void setPrice(int price) {
		this.price = price;
	}

	//--------------------------------------------------------------------
	public void overwriteItemTypeData(ItemTypeData data) {
		typeData.clear();
		typeData.add(data);
	}

	//--------------------------------------------------------------------
	public Collection<ItemTypeData> getTypeData() {
		return new ArrayList<ItemTypeData>(typeData);
	}

	//--------------------------------------------------------------------
	public boolean isType(ItemType type) {
		if ( (typeData.isEmpty() && types.isEmpty()) && type == ItemType.OTHER) {
			return true;
		}
		for (ItemTypeData data : typeData) {
			if (data.getType() == type) {
				return true;
			}
		}		

		return types.contains(type);
	}

	//--------------------------------------------------------------------
	public ItemTypeData getType(ItemType type) {
		for (ItemTypeData data : typeData)
			if (data.getType()==type)
				return data;
		return null;
	}

	//--------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public <T extends ItemTypeData> T getType(Class<T> type) {
		for (ItemTypeData data : typeData)
			if (data.getClass()==type)
				return (T)data;
		return null;
	}

	//--------------------------------------------------------------------
	public ItemType getFirstItemType() {
		if (types.isEmpty())
			return ItemType.OTHER;
		return types.iterator().next();
	}

	//--------------------------------------------------------------------
	public void addItemType(ItemType type) {
		types.add(type);
	}

	//--------------------------------------------------------------------
	public void replaceItemType(ItemType type) {
		types.clear();
		typeData.clear();
		types.add(type);
	}

	//--------------------------------------------------------------------
	public void addItemTypeData(ItemTypeData attributes) {
		typeData.add(attributes);
	}

	//--------------------------------------------------------------------
	public void add(Object value) {
		if (value instanceof ItemType)
			types.add((ItemType) value);
		else if (value instanceof ItemTypeData)
			typeData.add((ItemTypeData) value);
		else
			logger.error("Cannot add "+value.getClass());
	}

	//-------------------------------------------------------------------
	/**
	 * @return the materialType
	 */
	public MaterialType getMaterialType() {
		return materialType;
	}

	//-------------------------------------------------------------------
	/**
	 * @param materialType the materialType to set
	 */
	public void setMaterialType(MaterialType materialType) {
		this.materialType = materialType;
	}

	//-------------------------------------------------------------------
	public void setCustomName(String name) {
		this.customName = name;
		// Calculcate ID from name
		StringBuffer buf = new StringBuffer("custom");
		for (int i=0; i<name.length(); i++) {
			if (Character.isWhitespace(name.charAt(i)))
				continue;
			buf.append(name.charAt(i));
		}
		id = buf.toString();
	}

	//-------------------------------------------------------------------
	public boolean isCustom() {
		return id.startsWith("custom");
	}

	//-------------------------------------------------------------------
	/**
	 * @return the skill
	 */
	public Skill getSkill() {
		try {
			if (isType(ItemType.LONG_RANGE_WEAPON))
				return getType(LongRangeWeapon.class).getSkill();
			if (isType(ItemType.WEAPON))
				return getType(Weapon.class).getSkill();
		} catch (NullPointerException e) {
			logger.error("NPE while getting skill of "+this,e);
		}
		return skill;
	}

	//-------------------------------------------------------------------
	public void setSkill(Skill skill) {
		this.skill = skill;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the specialization
	 */
	public SkillSpecialization getSpecialization() {
		return specialization;
	}

	//-------------------------------------------------------------------
	public void setSpecialization(SkillSpecialization value) {
		this.specialization = value;
	}

}
