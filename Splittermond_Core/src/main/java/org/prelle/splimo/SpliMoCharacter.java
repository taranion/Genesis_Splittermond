/**
 *
 */
package org.prelle.splimo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PropertyResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prelle.simplepersist.AfterLoadHook;
import org.prelle.simplepersist.AttribConvert;
import org.prelle.simplepersist.Element;
import org.prelle.simplepersist.ElementList;
import org.prelle.simplepersist.Root;
import org.prelle.splimo.Skill.SkillType;
import org.prelle.splimo.SkillSpecialization.SkillSpecializationType;
import org.prelle.splimo.creature.Creature;
import org.prelle.splimo.creature.CreatureFeature;
import org.prelle.splimo.creature.CreatureReference;
import org.prelle.splimo.creature.CreatureTypeValue;
import org.prelle.splimo.creature.CreatureWeapon;
import org.prelle.splimo.creature.Lifeform;
import org.prelle.splimo.items.CarriedItem;
import org.prelle.splimo.items.CarriedItemList;
import org.prelle.splimo.items.EnhancementReference;
import org.prelle.splimo.items.ItemLocationType;
import org.prelle.splimo.items.ItemTemplate;
import org.prelle.splimo.modifications.ModificationList;
import org.prelle.splimo.persist.CustomItemHook;
import org.prelle.splimo.persist.DeityConverter;
import org.prelle.splimo.requirements.AnyRequirement;
import org.prelle.splimo.requirements.AttributeRequirement;
import org.prelle.splimo.requirements.FavoredSkillRequirement;
import org.prelle.splimo.requirements.MastershipRequirement;
import org.prelle.splimo.requirements.PowerRequirement;
import org.prelle.splimo.requirements.Requirement;
import org.prelle.splimo.requirements.ResourceRequirement;
import org.prelle.splimo.requirements.SkillRequirement;
import org.prelle.splimo.requirements.SpecialRequirement;
import org.prelle.splimo.requirements.SpellRequirement;

import de.rpgframework.character.RuleSpecificCharacterObject;
import de.rpgframework.genericrpg.Reward;
import de.rpgframework.genericrpg.modification.Modification;

/**
 * @author prelle
 *
 */
@Root(name = "splimochar")
public class SpliMoCharacter implements Lifeform, RuleSpecificCharacterObject {

	private static Logger logger = LogManager.getLogger("splittermond");
	private static PropertyResourceBundle res = SplitterMondCore.getI18nResources();

	public enum Gender {
		MALE,
		FEMALE,
		DIVERS
		;
		public String toString() {
			return res.getString("gender."+name().toLowerCase());
		}
	}

	@org.prelle.simplepersist.Attribute(name="gen")
    private boolean generationMode;
	@Element
	private String name;
	@Element(name="chargen")
    private String chargenUsed;
    @Element(name="chargenSettings")
    private String chargenSettings;
    private transient Object chargenSettingsObject;
	@org.prelle.simplepersist.Attribute(name="race")
	private String race;

	@Element(name="ownCult",required=false)
	private Culture ownCulture;
	@org.prelle.simplepersist.Attribute(name="culture")
	//	@XmlJavaTypeAdapter(CultureAdapter.class)
	private String culture;

	@Element(required=false)
	private Background ownBground;
	@org.prelle.simplepersist.Attribute(name="bground")
	//	@XmlJavaTypeAdapter(CultureAdapter.class)
	private String background;

	@Element(name="ownEdu",required=false)
	private Education ownEducation;
	@org.prelle.simplepersist.Attribute(name="edu")
	private String education;

	@ElementList(entry="cultloreref", type=CultureLoreReference.class)
	private List<CultureLoreReference> culturelores;
	@ElementList(entry="languageref", type=LanguageReference.class)
	private List<LanguageReference>    languages;
	@ElementList(entry="powerref", type=PowerReference.class)
	private List<PowerReference> powerrefs;
	@ElementList(entry="resourceref", type=ResourceReference.class)
	private List<ResourceReference> resourcerefs;
	@org.prelle.simplepersist.Attribute
	private Moonsign splinter;
	@Element
	private Attributes attributes;
	@ElementList(entry="skillval", type=SkillValue.class)
	private List<SkillValue> skillvals;
	@ElementList(entry="spellval", type=SpellValue.class)
	private List<SpellValue> spellvals;
	@org.prelle.simplepersist.Attribute(required=false)
	private transient int level;
	@org.prelle.simplepersist.Attribute
	private int expfree;
	@org.prelle.simplepersist.Attribute
	private int expinv;
	@Element
	private byte[] image;
	@Element
	private String hairColor, eyeColor, furColor, birthPlace;
	@Element
	private int size;
	@Element
	private int weight;
	@Element
	private Gender gender;
	@ElementList(entry="weakness", type=String.class)
	private List<String> weaknesses;
	@org.prelle.simplepersist.Attribute
	@AttribConvert(DeityConverter.class)
	private Deity deity;


	@ElementList(entry="item", type=ItemTemplate.class)
	@AfterLoadHook(CustomItemHook.class)
	private List<ItemTemplate> itemdefs;
	@ElementList(entry="creature", type=Creature.class)
	private List<Creature> creaturedefs;

	@Element(name="carries")
	private CarriedItemList carries;
	@ElementList(entry="creatures", type=CreatureReference.class)
	private List<CreatureReference> creatures;

	@Element
	private ModificationList history;
	@Element
	private RewardList rewards;
	@org.prelle.simplepersist.Attribute
	private int telare;
	@Element
	protected String notes;

	//-------------------------------------------------------------------
	/**
	 */
	public SpliMoCharacter() {
		attributes   = new Attributes();
		skillvals    = new ArrayList<SkillValue>();
		spellvals    = new ArrayList<SpellValue>();
		powerrefs    = new ArrayList<PowerReference>();
		resourcerefs = new ArrayList<ResourceReference>();
		culturelores = new ArrayList<CultureLoreReference>();
		languages    = new ArrayList<LanguageReference>();
		weaknesses   = new ArrayList<String>();

		// Fill blank skills
		for (Skill tmp : SplitterMondCore.getSkills())
			skillvals.add(new SkillValue(tmp, 0));

		carries   = new CarriedItemList();
		history   = new ModificationList();
		level     = 1;
		rewards   = new RewardList();
		creatures = new ArrayList<CreatureReference>();

		itemdefs     = new ArrayList<>();
		creaturedefs = new ArrayList<>();
	}

	//-------------------------------------------------------------------
	public String dump() {
		StringBuffer buf = new StringBuffer();
		buf.append("Name  : "+name);
		buf.append("\nRasse : "+race);

		for (Attribute attr : Attribute.values()) {
			if (attr == Attribute.DAMAGE_REDUCTION) {
				continue;
			}
			buf.append("\n"+attr.getShortName()+": "+attributes.get(attr)+"  \t");
		}

		buf.append("\n\nKulturkunden:\n=============");
		for (CultureLoreReference data : culturelores) {
			buf.append("\n"+data.getCultureLore().getName());
		}

		buf.append("\n\nSprachen:\n=============");
		for (LanguageReference data : languages) {
			buf.append("\n"+data.getLanguage().getName());
		}

		buf.append("\n\nStärken:\n=============");
		for (PowerReference power : powerrefs) {
			buf.append("\n"+power.getPower().getName()+" "+power.getCount());
		}

		buf.append("\n\nSchwächen:\n=============");
		for (String weak : weaknesses) {
			buf.append("\n"+weak);
		}

		buf.append("\n\nFertigkeiten:\n=============");
		for (SkillValue tmp : skillvals) {
			buf.append("\n "+tmp.getSkill().getName()+" : "+tmp.getValue());
		}

		buf.append("\n\nZauber:\n=============");
		for (SpellValue tmp : spellvals) {
			buf.append("\n "+tmp.getSpell().getName()+" : "+tmp);
		}

		buf.append("\n\nInventar:\n=============");
		for (CarriedItem tmp : carries) {
			buf.append("\n "+tmp.getItem().getName());
		}

		buf.append("\n\nKreaturen:\n=============");
		for (CreatureReference tmp : creatures) {
			buf.append("\n "+tmp.getName());
		}

		return buf.toString();
	}

	//-------------------------------------------------------------------
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	//-------------------------------------------------------------------
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the race
	 */
	public Race getRace() {
		if (race!=null)
			return SplitterMondCore.getRace(race);
		return null;
	}

	//-------------------------------------------------------------------
	/**SplitterMondCore.getSkill(v)
	 * @param race the race to set
	 */
	public void setRace(String race) {
		this.race = race;
	}

	//-------------------------------------------------------------------
	public void setCulture(String data) {
		this.culture = data;
	}

	//-------------------------------------------------------------------
	public Culture getCulture() {
		if (ownCulture!=null)
			return ownCulture;
		if (culture!=null)
			return SplitterMondCore.getCulture(culture);
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the ownRace
	 */
	public Culture getOwnCulture() {
		return ownCulture;
	}

	//-------------------------------------------------------------------
	/**
	 * @param ownCulture the ownCulture to set
	 */
	public void setOwnCulture(Culture ownCulture) {
		this.ownCulture = ownCulture;
	}

	//-------------------------------------------------------------------
	public void setBackground(String data) {
		this.background = data;
	}

	//--------------------------------------------------------------------
	/**
	 * @param ownBground the ownBground to set
	 */
	public void setOwnBackground(Background ownBground) {
		this.ownBground = ownBground;
	}

	//--------------------------------------------------------------------
	public Background getOwnBackground() {
		return ownBground;
	}

	//-------------------------------------------------------------------
	public Background getBackground() {
		if (ownBground!=null)
			return ownBground;
		if (background!=null)
			return SplitterMondCore.getBackground(background);
		return null;
	}

	//-------------------------------------------------------------------
//	@Override
	public void setAttribute(Attribute key, int val) {
		attributes.set(key, val);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.creature.Lifeform#getAttribute(org.prelle.splimo.Attribute)
	 */
	@Override
	public AttributeValue getAttribute(Attribute key) {
		return attributes.get(key);
	}

	//-------------------------------------------------------------------
//	@Override
	public void setSkill(SkillValue skillVal) {
		if (!skillvals.contains(skillVal))
			skillvals.add(skillVal);
	}

	//-------------------------------------------------------------------
//	@Override
	public int getSkillPoints(Skill key) {
		for (SkillValue tmp : skillvals)
			if (tmp.getSkill()==key)
				return tmp.getModifiedValue();
		throw new NoSuchElementException();
	}

	//-------------------------------------------------------------------
	public void addGroupedSkill(SkillValue value) {
		if (!value.getSkill().isGrouped())
			throw new IllegalArgumentException("Can only add grouped skills");
		skillvals.add(value);
	}

	//-------------------------------------------------------------------
	public void removeGroupedSkill(SkillValue value) {
		if (!value.getSkill().isGrouped())
			throw new IllegalArgumentException("Can only add grouped skills");
		skillvals.remove(value);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.creature.Lifeform#getSkills()
	 */
	@Override
	public List<SkillValue> getSkills() {
		List<SkillValue> ret = new ArrayList<SkillValue>(skillvals);
		// Sort alphabetically
		Collections.sort(ret);
		return ret;
	}

	//-------------------------------------------------------------------
	/**
	 * Returns a list of skills of a given type.
	 * @param type SkillType
	 * @return List of skills of the given type, sorted alphabetically
	 * @see org.prelle.splimo.creature.Lifeform#getSkills(org.prelle.splimo.Skill.SkillType)
	 */
	@Override
	public List<SkillValue> getSkills(SkillType type) {
		List<SkillValue> ret = new ArrayList<SkillValue>();
		for (SkillValue tmp : new ArrayList<SkillValue>(skillvals)) {
			if (tmp.getSkill()==null) {
				logger.warn("Remove unset skill from character: "+tmp);
				skillvals.remove(tmp);
				continue;
			}
			if (tmp.getSkill().getType()==type)
				ret.add(tmp);
		}
		// Sort alphabetically
		Collections.sort(ret);
		return ret;
	}


	//--------------------------------------------------------------------
	public boolean hasPower(Power power) {
		for (PowerReference ref : powerrefs)
			if (ref.getPower()==power)
				return true;
		return false;
	}

	//--------------------------------------------------------------------
	public PowerReference getPower(Power power) {
		for (PowerReference ref : powerrefs)
			if (ref.getPower()==power)
				return ref;
		return null;
	}

	//-------------------------------------------------------------------
	public void addPower(PowerReference ref) {
		powerrefs.add(ref);
	}

	//-------------------------------------------------------------------
	public void removePower(PowerReference ref) {
		powerrefs.remove(ref);
	}

	//-------------------------------------------------------------------
	public List<PowerReference> getPowers() {
		return new ArrayList<PowerReference>(powerrefs);
	}

	//-------------------------------------------------------------------
	public void addResource(ResourceReference ref) {
		if (!resourcerefs.contains(ref))
			resourcerefs.add(ref);
	}

	//-------------------------------------------------------------------
	public void removeResource(ResourceReference ref) {
		resourcerefs.remove(ref);
	}

	//-------------------------------------------------------------------
	public List<ResourceReference> getResources() {
		return new ArrayList<ResourceReference>(resourcerefs);
	}

	//--------------------------------------------------------------------
	/**
	 * @return the expfree
	 */
	public int getExperienceFree() {
		return expfree;
	}

	//--------------------------------------------------------------------
	/**
	 * @param expfree the expfree to set
	 */
	public void setExperienceFree(int expfree) {
		this.expfree = expfree;
	}

	//--------------------------------------------------------------------
	/**
	 * @return the expinv
	 */
	public int getExperienceInvested() {
		return expinv;
	}

	//--------------------------------------------------------------------
	/**
	 * @param expinv the expinv to set
	 */
	public void setExperienceInvested(int expinv) {
		this.expinv = expinv;
	}

	//--------------------------------------------------------------------
	/**
	 * @return the splinter
	 */
	public Moonsign getSplinter() {
		return splinter;
	}

	//--------------------------------------------------------------------
	/**
	 * @param splinter the splinter to set
	 */
	public void setSplinter(Moonsign splinter) {
		this.splinter = splinter;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the image
	 */
	public byte[] getImage() {
		return image;
	}

	//-------------------------------------------------------------------
	/**
	 * @param image the image to set
	 */
	public void setImage(byte[] image) {
		this.image = image;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the hairColor
	 */
	public String getHairColor() {
		return hairColor;
	}

	//-------------------------------------------------------------------
	/**
	 * @param hairColor the hairColor to set
	 */
	public void setHairColor(String hairColor) {
		this.hairColor = hairColor;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the eyeColor
	 */
	public String getEyeColor() {
		return eyeColor;
	}

	//-------------------------------------------------------------------
	/**
	 * @param eyeColor the eyeColor to set
	 */
	public void setEyeColor(String eyeColor) {
		this.eyeColor = eyeColor;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the furColor
	 */
	public String getFurColor() {
		return furColor;
	}

	//-------------------------------------------------------------------
	/**
	 * @param furColor the furColor to set
	 */
	public void setFurColor(String furColor) {
		this.furColor = furColor;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the birthPlace
	 */
	public String getBirthplace() {
		return birthPlace;
	}

	//-------------------------------------------------------------------
	/**
	 * @param birthPlace the birthPlace to set
	 */
	public void setBirthPlace(String birthPlace) {
		this.birthPlace = birthPlace;
	}
	//-------------------------------------------------------------------
	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	//-------------------------------------------------------------------
	/**
	 * @param size the size to set
	 */
	public void setSize(int size) {
		this.size = size;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the gender
	 */
	public Gender getGender() {
		return gender;
	}

	//-------------------------------------------------------------------
	/**
	 * @param gender the gender to set
	 */
	public void setGender(Gender gender) {
		this.gender = gender;
	}

	//-------------------------------------------------------------------
//	@Override
	public void addSpell(SpellValue tmp) {
		spellvals.add(tmp);
	}

	//--------------------------------------------------------------------
//	@Override
	public void removeSpell(SpellValue spell) {
		spellvals.remove(spell);
	}

	//-------------------------------------------------------------------
	@Override
	public List<SpellValue> getSpells() {
		List<SpellValue> ret = new ArrayList<SpellValue>(spellvals);
		// Add spells from items
		for (CarriedItem item : getItems()) {
			for (EnhancementReference enh : item.getEnhancements()) {
				if (enh.getSpellValue()!=null)
					ret.add(enh.getSpellValue());
			}
		}
		Collections.sort(ret);
		return ret;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.creature.Lifeform#hasSpell(org.prelle.splimo.SpellValue)
	 */
	@Override
	public boolean hasSpell(SpellValue spell) {
		return getSpells().contains(spell);
	}

	//-------------------------------------------------------------------
//	@Override
	public void clearSpells() {
		spellvals.clear();
	}

	//-------------------------------------------------------------------
	/**
	 * Get the value for a specific spell, including skill value and
	 * eventually existing spell type specializations
	 */
	@Override
	public int getSpellValueFor(SpellValue spellVal) {
		SkillValue skillVal = getSkillValue(spellVal.getSkill());
		// Calculate value for skill, including attributes
		int val = skillVal.getModifiedValue();
		val += getAttribute(skillVal.getSkill().getAttribute1()).getValue();
		val += getAttribute(skillVal.getSkill().getAttribute2()).getValue();

		for (SpellType type : spellVal.getSpell().getTypes()) {
			SkillSpecialization spec = new SkillSpecialization();
			spec.setType(SkillSpecializationType.SPELLTYPE);
			spec.setId(type.name());
			val += skillVal.getSpecializationLevel(spec);
		}

		return val + EquipmentTools.getSpellBonus(this, spellVal);
	}

	//-------------------------------------------------------------------
	/**
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}

	//-------------------------------------------------------------------
	/**
	 * @param level the level to set
	 */
	public void setLevel(int level) {
		this.level = level;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.creature.Lifeform#hasSkill(org.prelle.splimo.Skill)
	 */
	@Override
    public boolean hasSkill(Skill skill) {
		for (SkillValue tmp : skillvals)
			if (tmp.getSkill()==skill && tmp.getModifiedValue()>0)
				return true;
    	return false;
    }

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.creature.Lifeform#getSkillValue(org.prelle.splimo.Skill)
	 */
	@Override
	public SkillValue getSkillValue(Skill skill) {
		if (skill==null)
			throw new NullPointerException();
		for (SkillValue tmp : skillvals)
			if (tmp.getSkill()==skill)
				return tmp;

		SkillValue sVal = new SkillValue(skill, 0);
		skillvals.add(sVal);
		return sVal;
	}

//	//-------------------------------------------------------------------
////	@Override
//	public SkillValue getGroupedSkillValue(Skill skill, SkillSpecialization focus) {
//		for (SkillValue tmp : skillvals)
//			if (tmp.getSkill()==skill && tmp.getFocus()==focus)
//				return tmp;
//
//		return null;
//	}

	//-------------------------------------------------------------------
	public Education getEducation() {
		if (ownEducation!=null)
			return ownEducation;
		if (education!=null)
			return SplitterMondCore.getEducation(education);
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @param education the education to set
	 */
	public void setEducation(String education) {
		this.education = education;
	}

	//-------------------------------------------------------------------
	/**
	 * @param education the education to set
	 */
	public void setOwnEducation(Education education) {
		this.ownEducation = education;
	}

	//-------------------------------------------------------------------
	public Education getOwnEducation() {
		return ownEducation;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the weight
	 */
	public int getWeight() {
		return weight;
	}

	//-------------------------------------------------------------------
	/**
	 * @param weight the weight to set
	 */
	public void setWeight(int weight) {
		this.weight = weight;
	}

	//-------------------------------------------------------------------
	public void addCultureLore(CultureLoreReference data) {
		if (!culturelores.contains(data))
			culturelores.add(data);
	}

	//-------------------------------------------------------------------
	public void removeCultureLore(CultureLoreReference data) {
		culturelores.remove(data);
	}

	//-------------------------------------------------------------------
	public boolean hasCultureLore(CultureLore lore) {
		for (CultureLoreReference ref : culturelores)
			if (ref.getCultureLore()==lore)
				return true;
		return false;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the culturelores
	 */
	public List<CultureLoreReference> getCultureLores() {
		return culturelores;
	}

	//-------------------------------------------------------------------
	public void addLanguage(LanguageReference data) {
		if (!languages.contains(data))
			languages.add(data);
	}

	//-------------------------------------------------------------------
	public void removeLanguage(LanguageReference data) {
		languages.remove(data);
	}

	//--------------------------------------------------------------------
	public boolean hasLanguage(Language lang) {
		for (LanguageReference ref : languages)
			if (ref.getLanguage()==lang)
				return true;
		return false;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the languages
	 */
	public List<LanguageReference> getLanguages() {
		return languages;
	}

	//-------------------------------------------------------------------
	public boolean hasMastership(Mastership master) {
		if (master==null)
			return false;
		for (MastershipReference ref : getSkillValue(master.getSkill()).getMasterships()) {
			if (ref.getMastership()==null)
				continue;
			if (ref.getMastership().getKey().equals(master.getKey())) {
				return true;
			}
			if (ref.getMastership().getKey().startsWith(master.getKey())) {
				return true;
			}
		}
		return false;
	}

	//-------------------------------------------------------------------
	/**
	 * @param focus May be empty
	 */
	public boolean hasMastership(Mastership master, SkillSpecialization focus) {
		String searchKey = master.getId()+((focus!=null)?("_"+focus.getId()):"");
		SkillValue sVal = getSkillValue(master.getSkill());
		if (sVal==null)
			return false;
		for (MastershipReference ref : sVal.getMasterships()) {
			if (ref.getMastership()==null)
				continue;
			if (ref.getMastership().getKey().equals(searchKey)) {
				return true;
			}
		}
		return false;
	}

	//-------------------------------------------------------------------
	public int getSkillSpecializationLevel(SkillSpecialization master) {
		return getSkillValue(master.getSkill()).getSpecializationLevel(master);
	}

	//-------------------------------------------------------------------
	public boolean meetsRequirement(Requirement req) {
		//		logger.info("meetsRequirement "+req);
		try {
			if (req instanceof AttributeRequirement) {
				AttributeRequirement real = (AttributeRequirement)req;
				return getAttribute(real.getAttribute()).getValue()>=real.getValue();
			} else if (req instanceof MastershipRequirement) {
				MastershipRequirement real = (MastershipRequirement)req;
				logger.debug("Check if mastership "+real.getMastership()+" exists");
				if (real.getMastership()!=null) {
					if (real.getFokus()==null)
						return hasMastership(real.getMastership());
					else
						return hasMastership(real.getMastership(), real.getFokus());
				}
				return false;
			} else if (req instanceof SpecialRequirement) {
				SpecialRequirement real = (SpecialRequirement)req;
				if (real.getSpecialization()!=null)
					return getSkillSpecializationLevel(real.getSpecialization().getSpecial())>0;
					return false;
			} else if (req instanceof PowerRequirement) {
				PowerRequirement real = (PowerRequirement)req;
				for (PowerReference ref : powerrefs)
					if (ref.getPower()==real.getPower())
						return true;
				return false;
			} else if (req instanceof ResourceRequirement) {
				ResourceRequirement real = (ResourceRequirement)req;
				for (ResourceReference ref : resourcerefs)
					if (ref.getResource()==real.getResource() && ref.getValue()>=real.getValue())
						return true;
				return false;
			} else if (req instanceof SkillRequirement) {
				SkillRequirement real = (SkillRequirement)req;
				SkillValue sVal = getSkillValue(real.getSkill());
				return sVal.getValue()>=real.getValue();
			} else if (req instanceof AnyRequirement) {
				AnyRequirement real = (AnyRequirement)req;
				for (Requirement opt : real.getOptionList()) {
					if (meetsRequirement(opt))
						return true;
				}
				return false;
			} else if (req instanceof SpellRequirement) {
				SpellRequirement real = (SpellRequirement)req;
				logger.warn("real = "+real);
				for (SpellValue sVal : getSpells()) {
					logger.warn("  real.getSpell = "+real.getSpell());
					logger.warn("  sval = "+sVal);
					if (real.getSpell().equals(sVal.getSpell()))
						return true;
				}
				return false;
			}
			if (req instanceof FavoredSkillRequirement)
				return true;

			logger.error("Don't know how to check requirement "+req.getClass());
		} catch (Exception e) {
			logger.error("Failed checking requirement",e);
		}
		return false;
	}

	//--------------------------------------------------------------------
	public int getMeleeValue() {
		int skillValue = getSkillValue(SplitterMondCore.getSkill("melee")).getValue();
		int bew = getAttribute(Attribute.AGILITY).getValue();
		int str = getAttribute(Attribute.STRENGTH).getValue();
		return skillValue + bew + str;
	}

	//--------------------------------------------------------------------
	public List<CarriedItem> getItems() {
		List<CarriedItem> items = new ArrayList<CarriedItem>();
		items.addAll(carries);

		Collections.sort(items);

		return items;
	}

	//--------------------------------------------------------------------
	public List<CarriedItem> getItems(ItemLocationType location) {
		return getItems().stream().filter(item -> item.getLocation()==location).collect(Collectors.toList());
	}

	//--------------------------------------------------------------------
	public void addToHistory(Modification mod) {
		history.add(mod);
	}

	//--------------------------------------------------------------------
	public void removeFromHistory(Modification mod) {
		history.remove(mod);
	}

	//--------------------------------------------------------------------
	public List<Modification> getHistory() {
		return new ArrayList<Modification>(history);
	}

	//--------------------------------------------------------------------
	public void addReward(Reward rew) {
		if (!rewards.contains(rew))
			rewards.add(rew);
	}

	//--------------------------------------------------------------------
	public List<Reward> getRewards() {
		return new ArrayList<Reward>(rewards);
	}

	//-------------------------------------------------------------------
	/**
	 * @return the weakness
	 */
	public List<String> getWeaknesses() {
		return weaknesses;
	}

	//-------------------------------------------------------------------
	public void addWeakness(String weakness) {
		weaknesses.add(weakness);
	}

	//-------------------------------------------------------------------
	public void removeWeakness(String weakness) {
		weaknesses.remove(weakness);
	}

	//-------------------------------------------------------------------
	public void addItem(CarriedItem data) {
		if (!carries.contains(data))
			carries.add(data);
	}

	//-------------------------------------------------------------------
	public void removeItem(CarriedItem itemToRemove) {
		carries.remove(itemToRemove);
	}


	//-------------------------------------------------------------------

	/**
	 * Returns the highest closecombat SkillValue which is use as shield skill.
	 * @return highest closecombat SkillValue
	 */
	public SkillValue getShieldSkillValue(){
		SkillValue value = null;
		for (SkillValue skillValue : this.getSkills(SkillType.COMBAT)) {
			Skill skill = skillValue.getSkill();

			if (skill.getId().equals("throwing") || skill.getId().equals("longrange")) {
				continue;
			}

			if (value == null||skillValue.getValue() > value.getValue()) {
				value = skillValue;
			}
		}
		return value;
	}

	//-------------------------------------------------------------------
	/**
	 * Not for public use.
	 */
	public Attributes impl_getAttributes() {
		return attributes;
	}

	//-------------------------------------------------------------------
	public List<CreatureReference> getCreatures() {
		return new ArrayList<>(creatures);
	}

	//-------------------------------------------------------------------
	public void addCreature(CreatureReference data) {
		if (!creatures.contains(data))
			creatures.add(data);
	}

	//-------------------------------------------------------------------
	public void removeCreature(CreatureReference data) {
		creatures.remove(data);
	}

	//-------------------------------------------------------------------
	/**
	 * @return the weakness
	 */
	public List<UniqueObject> getUniqueObjects() {
		List<UniqueObject> ret = new ArrayList<>();
		// Creatures
		ret.addAll(creatures);
		ret.addAll(carries);
		return ret;
	}

	//-------------------------------------------------------------------
	public UniqueObject getUniqueObject(UUID uniqueID) {
		for (UniqueObject tmp : getUniqueObjects()) {
			if (tmp.getUniqueId().equals(uniqueID))
				return tmp;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public void addCustomItem(ItemTemplate data) {
		if (!itemdefs.contains(data))
			itemdefs.add(data);
	}

	//-------------------------------------------------------------------
	public void removeCustomItem(ItemTemplate toRemove) {
		itemdefs.remove(toRemove);
	}

	//-------------------------------------------------------------------
	public List<ItemTemplate> getCustomItems() {
		return new ArrayList<>(itemdefs);
	}

	//-------------------------------------------------------------------
	public void clearCustomItems() {
		itemdefs.clear();
	}

	//-------------------------------------------------------------------
	public void addCustomCreature(Creature data) {
		if (!creaturedefs.contains(data))
			creaturedefs.add(data);
	}

	//-------------------------------------------------------------------
	public void removeCustomCreature(Creature toRemove) {
		creaturedefs.remove(toRemove);
	}

	//-------------------------------------------------------------------
	public List<Creature> getCustomCreatures() {
		return new ArrayList<>(creaturedefs);
	}

	//-------------------------------------------------------------------
	public void clearCustomCreatures() {
		creaturedefs.clear();
	}

	//-------------------------------------------------------------------
	/**
	 * @return the telare
	 */
	public int getTelare() {
		return telare;
	}

	//-------------------------------------------------------------------
	/**
	 * @param telare the telare to set
	 */
	public void setTelare(int telare) {
		this.telare = telare;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.creature.Lifeform#getCreatureTypes()
	 */
	@Override
	public List<CreatureTypeValue> getCreatureTypes() {
		CreatureTypeValue val = new CreatureTypeValue(SplitterMondCore.getCreatureType("HUMANOID"));
		return Arrays.asList(val);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.creature.Lifeform#getCreatureWeapons()
	 */
	@Override
	public List<CreatureWeapon> getCreatureWeapons() {
		logger.warn("TODO: getCreatureWeapons()");
		return new ArrayList<>();
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.splimo.creature.Lifeform#getFeatures()
	 */
	@Override
	public List<CreatureFeature> getFeatures() {
		List<CreatureFeature> ret = new ArrayList<>();
		ret.add(new CreatureFeature(SplitterMondCore.getCreatureFeatureType("TACTICIAN")));
		logger.warn("TODO: getFeatures()");
		return ret;
	}

	//-------------------------------------------------------------------
	public String getNotes() {
		return notes;
	}

	//-------------------------------------------------------------------
	public void setNotes(String txt) {
		this.notes = txt;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the deity
	 */
	public Deity getDeity() {
		return deity;
	}

	//-------------------------------------------------------------------
	/**
	 * @param deity the deity to set
	 */
	public void setDeity(Deity deity) {
		this.deity = deity;
	}

	//-------------------------------------------------------------------
    /**
     * @return the generationMode
     */
    public boolean isGenerationMode() {
            return generationMode;
    }

    //-------------------------------------------------------------------
    /**
     * @param generationMode the generationMode to set
     */
    public void setGenerationMode(boolean generationMode) {
            this.generationMode = generationMode;
    }

    //-------------------------------------------------------------------
    /**
     * @return the chargenUsed
     */
    public String getChargenUsed() {
            return chargenUsed;
    }

    //-------------------------------------------------------------------
    /**
     * @param chargenUsed the chargenUsed to set
     */
    public void setChargenUsed(String chargenUsed) {
            this.chargenUsed = chargenUsed;
    }

    //-------------------------------------------------------------------
    /**
     * @return the chargenSettings
     */
    public String getChargenSettings() {
            return chargenSettings;
    }

    //-------------------------------------------------------------------
    /**
     * @param chargenSettings the chargenSettings to set
     */
    public void setChargenSettings(String chargenSettings) {
    	this.chargenSettings = chargenSettings;
    }

    //-------------------------------------------------------------------
    public void setTemporaryChargenSettings(Object settings) {
            this.chargenSettingsObject = settings;
    }

    //-------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public <T> T getTemporaryChargenSettings(Class<T> cls) {
            return (T)chargenSettingsObject;
    }
    
}
