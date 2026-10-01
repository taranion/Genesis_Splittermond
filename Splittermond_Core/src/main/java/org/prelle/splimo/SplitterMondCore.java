/**
 *
 */
package org.prelle.splimo;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.prelle.simplepersist.Persister;
import org.prelle.simplepersist.SerializationException;
import org.prelle.splimo.Skill.SkillType;
import org.prelle.splimo.SkillSpecialization.SkillSpecializationType;
import org.prelle.splimo.commandbus.SplittermondCommandBus;
import org.prelle.splimo.creature.Creature;
import org.prelle.splimo.creature.CreatureFeature;
import org.prelle.splimo.creature.CreatureFeatureType;
import org.prelle.splimo.creature.CreatureFeatureTypeList;
import org.prelle.splimo.creature.CreatureList;
import org.prelle.splimo.creature.CreatureModule;
import org.prelle.splimo.creature.CreatureModuleList;
import org.prelle.splimo.creature.CreatureReference;
import org.prelle.splimo.creature.CreatureType;
import org.prelle.splimo.creature.CreatureTypeList;
import org.prelle.splimo.gamemaster.SpliMoNameGenerator;
import org.prelle.splimo.items.Armor;
import org.prelle.splimo.items.CarriedItem;
import org.prelle.splimo.items.Enhancement;
import org.prelle.splimo.items.EnhancementList;
import org.prelle.splimo.items.FeatureType;
import org.prelle.splimo.items.FeatureTypeList;
import org.prelle.splimo.items.ItemList;
import org.prelle.splimo.items.ItemLocationType;
import org.prelle.splimo.items.ItemTemplate;
import org.prelle.splimo.items.ItemType;
import org.prelle.splimo.items.ItemTypeData;
import org.prelle.splimo.items.LongRangeWeapon;
import org.prelle.splimo.items.Material;
import org.prelle.splimo.items.MaterialList;
import org.prelle.splimo.items.MaterialType;
import org.prelle.splimo.items.Personalization;
import org.prelle.splimo.items.PersonalizationList;
import org.prelle.splimo.items.Shield;
import org.prelle.splimo.items.Weapon;
import org.prelle.splimo.modifications.AttributeModification;
import org.prelle.splimo.modifications.CultureLoreModification;
import org.prelle.splimo.modifications.ModificationChoice;
import org.prelle.splimo.modifications.ModificationSource;
import org.prelle.splimo.modifications.SkillModification;
import org.prelle.splimo.persist.ReferenceException;
import org.prelle.splimo.requirements.AnyRequirement;
import org.prelle.splimo.requirements.MastershipRequirement;
import org.prelle.splimo.requirements.Requirement;
import org.prelle.splimo.requirements.SpecialRequirement;

import de.rpgframework.RPGFramework;
import de.rpgframework.RPGFrameworkLoader;
import de.rpgframework.character.RulePlugin;
import de.rpgframework.core.BabylonEventBus;
import de.rpgframework.core.BabylonEventType;
import de.rpgframework.core.CommandBus;
import de.rpgframework.core.RoleplayingSystem;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.worldinfo.GeneratorRegistry;

/**
 * @author prelle
 *
 */
public class SplitterMondCore {

	private static Logger logger = LogManager.getLogger("splittermond");

	private final static String PROMOSERVER_CHECK_URL    = "http://license.rpgframework.de:4001/checkPromo?code=%s&secret=%s";
	private final static String PROMOSERVER_REGISTER_URL = "http://license.rpgframework.de:4001/registerPromo?code=%s&secret=%s";

	private static boolean alreadyInitialized = false;

	private static PropertyResourceBundle i18NResources;
	private static PropertyResourceBundle i18NHelpResources;
	private static Persister serializer;

	private static List<Skill> skills;
	private static SpellList spells;
	private static RaceList races;
	private static LanguageList languages;
	private static CultureLoreList cultureLores;
	private static CultureList cultures;
	private static BackgroundList backgrounds;
	private static EducationList educations;
	private static PowerList powers;
	private static ResourceList resources;
	private static FeatureTypeList featureTypes;
	private static MaterialList materials;
	private static ItemList items;
	private static EnhancementList enhancements;
	private static PersonalizationList personalizations;
	private static CreatureTypeList creatureTypes;
	private static CreatureFeatureTypeList creatFeatTypes;
	private static CreatureList creatures;
	private static AspectList aspects;
	private static DeityList deities;
	private static DeityTypeList deityTypes;
	private static HolyPowerList holyPowers;
	private static CreatureModuleList creatureModules;
	private static PromoDataList promos;
	private static Map<Culture, SpliMoNameTable> nameTables;
	private static TownList towns;

	private static ArrayList<Resource> BASE_RESOURCES;

	private static boolean missingLicense;

	//-------------------------------------------------------------------
	static {
		i18NResources = (PropertyResourceBundle) ResourceBundle.getBundle("org/prelle/splimo/i18n/splittermond-core");
		i18NHelpResources = (PropertyResourceBundle) ResourceBundle.getBundle("org/prelle/splimo/i18n/splittermond-core-help");
		serializer  = new Persister();
		skills      = new ArrayList<>();
		spells      = new SpellList();
		races       = new RaceList();
		cultureLores= new CultureLoreList();
		languages   = new LanguageList();
		cultures    = new CultureList();
		backgrounds = new BackgroundList();
		powers      = new PowerList();
		resources   = new ResourceList();
		educations  = new EducationList();
		featureTypes= new FeatureTypeList();
		items       = new ItemList();
		materials   = new MaterialList();
		enhancements= new EnhancementList();
		personalizations = new PersonalizationList();
		creatureTypes = new CreatureTypeList();
		creatFeatTypes= new CreatureFeatureTypeList();
		creatures   = new CreatureList();
		aspects     = new AspectList();
		deities     = new DeityList();
		deityTypes  = new DeityTypeList();
		holyPowers  = new HolyPowerList();
		creatureModules = new CreatureModuleList();
		promos      = new PromoDataList();
		nameTables  = new HashMap<>();
		towns       = new TownList();

		/*
		 * Information for Gamemaster
		 */
		GeneratorRegistry.register(new SpliMoNameGenerator());
	}

	//-------------------------------------------------------------------
	public static PropertyResourceBundle getI18nResources() {
		return i18NResources;
	}

	//-------------------------------------------------------------------
	public static void initialize(RulePlugin<SpliMoCharacter> plugin) {
		if (alreadyInitialized)
			return;
		missingLicense = !RPGFrameworkLoader.getInstance().getLicenseManager().hasLicense(RoleplayingSystem.SPLITTERMOND, "ALL");

		/*
		 * Register to receive commands
		 */
		CommandBus.registerBusCommandListener(new SplittermondCommandBus());

		alreadyInitialized = true;
	}

	//-------------------------------------------------------------------
	public static boolean hasLicense() {
		return !missingLicense;
	}

	//-------------------------------------------------------------------
	public static void loadSkills(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resources, ResourceBundle helpResources) {
		logger.debug("Load skills (Plugin="+plugin.getID()+")");
		try {
			SkillList addSkills = serializer.read(SkillList.class, in);
			logger.info("Successfully loaded "+addSkills.size()+" skills");

			// Set translation
			for (Skill tmp : addSkills) {
				tmp.setResourceBundle(resources);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled()) {
					logger.debug("* "+tmp.getName());
				}
			}

			// Add plugin data to complete list
			skills.addAll(addSkills);
			Collections.sort(skills);
			logger.debug("Total known skills now: "+skills.size());

			// For all skills ...
			for (Skill tmp : addSkills) {
				// Check specializations
				for (SkillSpecialization spec : tmp.getSpecializations()) {
					spec.setSkill(tmp);
				}
				// Check masterships
				for (Mastership master : new ArrayList<>(tmp.getMasterships())) {
					master.setSkill(tmp);
					master.setResourceBundle(resources);
					master.setHelpResourceBundle(helpResources);
					master.setPlugin(plugin);
					// Requirements
					for (Requirement req : master.getPrerequisites()) {
						if (req instanceof MastershipRequirement)
							((MastershipRequirement)req).setSkill(tmp);
						if (req instanceof SpecialRequirement)
							((SpecialRequirement)req).setSkill(tmp);
						if (req instanceof AnyRequirement) {
							for (Requirement req2 : ((AnyRequirement)req).getOptions()) {
								if (req2 instanceof MastershipRequirement)
									((MastershipRequirement)req2).setSkill(tmp);
								if (req2 instanceof SpecialRequirement)
									((SpecialRequirement)req2).setSkill(tmp);
							}
						}
						if (!req.resolve()) {
							logger.error(String.format("Skill '%s', Mastership '%s' cannot resolve %s '%s'", tmp.toString(), master.toString(), req.getClass().getSimpleName(), req.toString()));
							System.exit(0);
						}
					}
					// Modifications
					for (Modification mod : master.getModifications()) {
						mod.setSource(master);
					}
					// Verifications
					if (master.getName()==null)
						logger.error("Missing name for "+master.getKey());
					master.getPage();
					master.getShortDescription();
					master.getHelpText(); // Triggers error when key is missing
					/*
					 * For grouped skills multiply masterships "journeyman", "expert" and "master" with all
					 * skill specializations
					 */
					if (tmp.isGrouped() && ("journeyman".equals(master.getId()) || "expert".equals(master.getId()) || "master".equals(master.getId()))) {
						for (SkillSpecialization focus : tmp.getSpecializations()) {
							Mastership add = new Mastership(master, focus);
							tmp.addMastership(add);
							logger.debug("Created mastership "+add.getId());
							/*
							 * For masterships that require another journeyman, expert or master
							 */
							for (Requirement req : add.getPrerequisites()) {
								if (req instanceof MastershipRequirement) {
									MastershipRequirement mReq = (MastershipRequirement)req;
									if (mReq.getMastership().getId().equals("journeyman") || mReq.getMastership().getId().equals("expert") || mReq.getMastership().getId().equals("master")) {
										String searchID = mReq.getMastership().getId()+"_"+focus.getId();
										Mastership newMaster = tmp.getMastership(searchID);
//										logger.debug("  Replace requirement for "+mReq.getMastership()+" with "+newMaster);
										add.replaceRequirement(mReq, new MastershipRequirement(newMaster));
									}
								}

							}
						}
					}
				}
//				if (addSkills.indexOf(tmp)==8) {
//					logger.fatal("Stop here");
//					System.exit(0);
//				}
			}
		} catch (Exception e) {
			logger.fatal("Failed deserializing skills",e);
			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static void loadMasterships(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resources, ResourceBundle helpResources) {
		logger.debug("Load masterships (Plugin="+plugin.getID()+")");
		try {
			SkillList addSkills = serializer.read(SkillList.class, in);
			logger.debug("Successfully loaded masterships list");

			// Set translation
			int count=0;
			for (Skill tmp : addSkills) {
				Skill realSkill = getSkill(tmp.getId());
				if (realSkill==null) {
					logger.error("Cannot add mastership for unknown skill "+tmp);
					continue;
				}

				for (Mastership master : tmp.getMasterships()) {
					count++;
					master.setSkill(realSkill);
					master.setResourceBundle(resources);
					master.setHelpResourceBundle(helpResources);
					master.setPlugin(plugin);
					Mastership toReplace = realSkill.getMastership(master.getKey());
					if (toReplace!=null) {
						logger.debug("  Replace "+toReplace.getName()+" ("+toReplace.getProductNameShort()+" "+toReplace.getPage()+") with that from "+master.getProductNameShort()+" "+master.getPage());
						realSkill.removeMastership(toReplace);
					}
					realSkill.addMastership(master);
					if (logger.isDebugEnabled())
						logger.debug("* "+realSkill.getName()+" / "+master.getName());
					master.getName();
					master.getPage();
					master.getShortDescription();
					master.getHelpText();

				}

//				if (missingLicense) {
//					master.setHelpText(null);
//				}
			}

			// Now validate
			for (Skill tmp : addSkills) {
				for (Mastership master : tmp.getMasterships()) {
					// Validate requirements
					for (Requirement req : master.getPrerequisites()) {
						try {
							if (!req.resolve()) {
								logger.error("Plugin: "+plugin.getID()+",  Skill="+tmp.getId()+",  mastership="+master.getId()+"  has an unresolvable requirement "+req);
								System.exit(1);
							}
						} catch (ReferenceException e) {
							logger.error("Plugin: "+plugin.getID()+",  Skill="+tmp.getId()+",  mastership="+master.getId()+"  has an unresolvable reference to "+e.getMessage());
							System.exit(1);
						}
					}
					/*
					 * For grouped skills multiply masterships "journeyman", "expert" and "master" with all
					 * skill specializations
					 */
					if (tmp.isGrouped() && ("journeyman".equals(master.getId()) || "expert".equals(master.getId()) || "master".equals(master.getId()))) {
						for (SkillSpecialization focus : tmp.getSpecializations()) {
							Mastership add = new Mastership(master, focus);
							tmp.addMastership(add);
							logger.error("Created mastership "+add.getName());
							System.exit(0);
						}
					}
				}
			}

			logger.info("Successfully loaded "+count+" new masterships from "+addSkills.size()+" skills from "+plugin.getID());
		} catch (Exception e) {
			logger.fatal("Failed deserializing skills",e);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static void loadSpells(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resources, ResourceBundle helpResources) {
		logger.debug("Load spells (Plugin="+plugin.getID()+")");
		if (in==null)
			throw new MissingResourceException("Missing spells.xml", SplitterMondCore.class.getName(), null);

		try {
			SpellList addSpells = serializer.read(SpellList.class, in);
			logger.info("Successfully loaded "+addSpells.size()+" spells");

			// Set translation
			for (Spell tmp : addSpells) {
				tmp.setResourceBundle(resources);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName()+"  ("+tmp.getSchools()+"  p."+tmp.getPage());
				tmp.getName();
				tmp.getPage();
				tmp.getDescription();
				tmp.getEnhancementDescription();
				for (SpellType type : tmp.getTypes()) {
					if (type==null) {
						logger.warn("Spell "+tmp+" has no types");
						logger.warn(tmp.dump());
					}
				}
				if (tmp.getEnhancementtype().isEmpty()) {
					if (tmp.getCost().getChannelled()>1) {
						tmp.getEnhancementtype().add(SpellEnhancementType.CHANNELIZED_FOCUS);
					} else {
						if (tmp.getCost().getExhausted()>1)
							tmp.getEnhancementtype().add(SpellEnhancementType.EXHAUSTED_FOCUS);
					}
					if (tmp.getCost().getConsumed()>1)
						tmp.getEnhancementtype().add(SpellEnhancementType.CONSUMED_FOCUS);
					if (tmp.getCastRange()>0)
						tmp.getEnhancementtype().add(SpellEnhancementType.RANGE);
					if (tmp.getEffectRange()!=null && tmp.getEffectRange()>0)
						tmp.getEnhancementtype().add(SpellEnhancementType.EFFECT_RANGE);
					if (tmp.hasSpellDuration())
						tmp.getEnhancementtype().add(SpellEnhancementType.SPELLDURATION);
					tmp.getEnhancementtype().add(SpellEnhancementType.RELEASETIME);

				}

				if (missingLicense) {
					tmp.setEffectRange(-1);
					tmp.setCastDuration(0);
					tmp.setSpellDuration(0);
					tmp.setCost(new SpellCost(0, 0, 0));
					tmp.getEnhancementtype().clear();
				}
			}

			// Add plugin data to complete list
			spells.addAll(addSpells);
			Collections.sort(spells);

			// For magic schools add spell types as skill specializations
			for (Skill tmp : skills) {
				if (tmp.getType()==SkillType.MAGIC) {
					for (SpellType type : getSpellTypes(tmp)) {
						SkillSpecialization spec = tmp.getSpecialization(type.name());
						if (spec!=null)
							continue;
						spec = new SkillSpecialization();
						spec.setType(SkillSpecializationType.SPELLTYPE);
						spec.setId(type.name());
						tmp.addSpecialization(spec);
					}
				}
			}

			// Spell types by school
			if (logger.isTraceEnabled()) {
				for (Skill skill : skills) {
					if (skill.getType()!=SkillType.MAGIC)
						continue;
					Map<SpellType,Integer> count = new HashMap<SpellType,Integer>();
					for (Spell spell : getSpells(skill)) {
						for (SpellType type :spell.getTypes()) {
							if (type==null)
								throw new NullPointerException("Unknown spell type for spell '"+spell.getId()+"'");
							if (!count.containsKey(type))
								count.put(type, Integer.valueOf(0));
							count.put(type, Integer.valueOf(1+count.get(type)));
						}
					}
					logger.trace("Zauberschule "+skill+":");
					Set<Entry<SpellType, Integer>> entries = count.entrySet();
					List<Entry<SpellType, Integer>> list = new ArrayList<Map.Entry<SpellType,Integer>>(entries);
					Collections.sort(list, new Comparator<Entry<SpellType, Integer>>() {

						@Override
						public int compare(Entry<SpellType, Integer> o1,
								Entry<SpellType, Integer> o2) {
							int cmp = -o1.getValue().compareTo(o2.getValue());
							if (cmp==0) return o1.getKey().compareTo(o2.getKey());
							return cmp;
						}
					});
					for (Entry<SpellType, Integer> entry : list) {
						logger.trace(String.format("%25s = %d", entry.getKey().getName(), entry.getValue()));
					}
				}
			}

			// Add plugin data to complete list
//			spells.addAll(addSpells);
			Collections.sort(spells);
		} catch (IOException e) {
			logger.fatal("Failed loading spells: "+e,e);
		}
	}

	//-------------------------------------------------------------------
	public static void loadPowers(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resources, ResourceBundle helpResources) {
		logger.debug("Load powers (Plugin="+plugin.getID()+")");
		try {
			PowerList addPowers = serializer.read(PowerList.class, in);
			logger.info("Successfully loaded "+addPowers.size()+" powers");

			// Set translation
			for (Power tmp : addPowers) {
				tmp.setResourceBundle(resources);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				// Set modification source
				for (Modification mod : tmp.getModifications())
					mod.setSource(tmp);
				tmp.getName();
				tmp.getPage();
				tmp.getDescription();
				tmp.getHelpText();
			}
			Collections.sort(addPowers);
			powers.addAll(addPowers);
			Collections.sort(powers);

			// Resolve requirements
			for (Power power : addPowers) {
				for (Requirement req : power.getRequirement()) {
					if (!req.resolve()) {
						logger.error(String.format("Power '%s', cannot resolve %s '%s'", power.toString(), req.getClass().getSimpleName(), req.toString()));
						System.exit(0);
					}
				}
			}

			if (logger.isDebugEnabled()) {
				for (Power tmp : addPowers)
					logger.debug("* "+tmp.getName()+" "+tmp.getSelectable());
			}
		} catch (Exception e) {
			logger.fatal("Failed loading powers: "+e,e);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static void loadResources(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load resources (Plugin="+plugin.getID()+")");
		try {
			ResourceList addResources = serializer.read(ResourceList.class, in);
			logger.info("Successfully loaded "+addResources.size()+" resources");

			// Set translation
			for (Resource tmp : new ArrayList<Resource>(addResources)) {
				Resource resource = getResource(tmp.getId());
				if (resource != null) {
					resource.setResourceBundle(resrc);
					resource.setHelpResourceBundle(helpResources);
					resource.setPlugin(plugin);
					addResources.remove(tmp);
					resource.getName();
					resource.getPage();
					resource.getHelpText();
				} else {
					tmp.setResourceBundle(resrc);
					tmp.setHelpResourceBundle(helpResources);
					tmp.setPlugin(plugin);
					tmp.getName();
					tmp.getPage();
					tmp.getHelpText();
				}
			}

			resources.addAll(addResources);
			Collections.sort(resources);
			BASE_RESOURCES = new ArrayList<Resource>(Arrays.asList(new Resource[]{
					SplitterMondCore.getResource("reputation"),
					SplitterMondCore.getResource("status"),
					SplitterMondCore.getResource("contacts"),
					SplitterMondCore.getResource("wealth")
			}));
		} catch (Exception e) {
			logger.fatal("Failed loading resources: "+e,e);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static void loadRaces(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resources, ResourceBundle helpResources) {
		logger.debug("Load races (Plugin="+plugin.getID()+")");
		try {
			RaceList addRaces = serializer.read(RaceList.class, in);
			logger.info("Successfully loaded "+addRaces.size()+" races");

			// Set translation
			for (Race tmp : addRaces) {
				tmp.setResourceBundle(resources);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				tmp.fixModifications();
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName());
			}

			races.addAll(addRaces);
			Collections.sort(races);
		} catch (Exception e) {
			logger.fatal("Failed loading races: "+e,e);
		}
	}

	//-------------------------------------------------------------------
	public static void loadCultureLores(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resources, ResourceBundle helpResources) {
		logger.debug("Load culture lores (Plugin="+plugin.getID()+")");
		try {
			CultureLoreList toAdd = serializer.read(CultureLoreList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" culture lores");

			// Set translation
			for (CultureLore tmp : toAdd) {
				tmp.setResourceBundle(resources);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName());
			}

			cultureLores.addAll(toAdd);
			Collections.sort(cultureLores);
		} catch (Exception e) {
			logger.fatal("Failed loading culture lores: "+e,e);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static void loadLanguages(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resources, ResourceBundle helpResources) {
		logger.debug("Load languages (Plugin="+plugin.getID()+")");
		try {
			LanguageList addLangs = serializer.read(LanguageList.class, in);
			logger.info("Successfully loaded "+addLangs.size()+" languages");

			// Set translation
			for (Language tmp : addLangs) {
				tmp.setResourceBundle(resources);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName());
			}

			languages.addAll(addLangs);
			Collections.sort(languages);
		} catch (Exception e) {
			logger.fatal("Failed loading languages: "+e,e);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static void loadCultures(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load cultures (Plugin="+plugin.getID()+")");
		try {
			CultureList toAdd = serializer.read(CultureList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" cultures");

			// Set translation
			for (Culture tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName());
			}

			cultures.addAll(toAdd);
			Collections.sort(cultures);

			// Set the culture and language to all culture lores
			logger.debug("Update culture lores with cultures and languages");
			for (Culture tmp : toAdd) {
				// Find culture lore modification
				for (Modification mod : tmp.getModifications()) {
					if (mod instanceof CultureLoreModification) {
						CultureLoreModification cMod = (CultureLoreModification)mod;
						CultureLore lore = cMod.getData();
						lore.addCulture(tmp);
					} else if (mod instanceof ModificationChoice) {
						ModificationChoice cMod = (ModificationChoice)mod;
						for (Modification mod2 : cMod.getOptionList()) {
							if (mod2 instanceof CultureLoreModification) {
								CultureLoreModification cMod2 = (CultureLoreModification)mod2;
								CultureLore lore = cMod2.getData();
								lore.addCulture(tmp);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.fatal("Failed loading cultures: "+e,e);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static void loadEducations(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load educations (Plugin="+plugin.getID()+")");
		try {
			EducationList toAdd = serializer.read(EducationList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" educations");

			// Set translation
			for (Education tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				tmp.getName();
				tmp.getPage();
				tmp.getHelpText();
				if (logger.isDebugEnabled()) {
					logger.debug("* "+tmp.getName());
					tmp.getHelpText();
				}
			}

			/*
			 * Fix hierarchy
			 */
			for (Education edu : new ArrayList<>(toAdd)) {
				if (edu.getVariantOf()!=null) {
					Education parent = null;
					// Search parent education in previously loaded educations
					for (Education check : educations) {
						if (check.getKey().equals(edu.getVariantOf())) {
							parent = check;
							break;
						}
					}
					// Search parent education in educations from this pass
					if (parent==null) {
						for (Education check : toAdd) {
							if (check.getKey().equals(edu.getVariantOf())) {
								parent = check;
								break;
							}
						}
					}
					if (parent==null) {
						logger.warn("Education "+edu.getKey()+"/"+edu.getName()+" claims to be a variant of the unknown education '"+edu.getVariantOf()+"'");
					} else {
						toAdd.remove(edu);
						parent.addVariant(edu);
						logger.debug("  "+edu.getName()+" is a variant of "+parent.getName());
					}
				}
			}

			educations.addAll(toAdd);
			Collections.sort(educations);
		} catch (Exception e) {
			logger.fatal("Failed loading educations: "+e,e);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static void loadBackgrounds(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load backgrounds (Plugin="+plugin.getID()+")");
		try {
			BackgroundList toAdd = serializer.read(BackgroundList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" backgrounds");

			// Set translation
			for (Background tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				tmp.getName();
				tmp.getPage();
				tmp.getHelpText();
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName());
			}

			backgrounds.addAll(toAdd);
			Collections.sort(backgrounds);
		} catch (Exception e) {
			logger.fatal("Failed loading backgrounds: "+e,e);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<Skill> getSkills() {
		return skills;
	}

	//-------------------------------------------------------------------
	/**
	 * Get the skills of a specific type (e.g. combat)
	 * @see Skill.SkillType
	 */
	public static List<Skill> getSkills(SkillType type) {
		List<Skill> typedSkills = new ArrayList<Skill>();
		for (Skill skill : skills)
			if (skill.getType()==type)
				typedSkills.add(skill);
		Collections.sort(typedSkills);
		return typedSkills;
	}

	//-------------------------------------------------------------------
	public static List<SpellType> getSpellTypes(Skill skill) {
		final Map<SpellType, Integer> count = new HashMap<SpellType, Integer>();
		List<SpellType> ret = new ArrayList<SpellType>();
		if (getSpells(skill).isEmpty()) {
			logger.fatal("No spells for school "+skill);
			throw new NoSuchElementException("No spell types for school "+skill+" loaded yet");
		}
		for (Spell spell : getSpells(skill)) {
			for (SpellType type : spell.getTypes()) {
				// Count
				Integer old = count.get(type);
				if (old==null)
					count.put(type, Integer.valueOf(1));
				else
					count.put(type, Integer.valueOf(old+1));

				// Add
				if (!ret.contains(type))
					ret.add(type);
			}
		}

		// Sort by count
		Collections.sort(ret, new Comparator<SpellType>() {
			public int compare(SpellType o1, SpellType o2) {
				return -1 * count.get(o1).compareTo(count.get(o2));
			}
		});

		if (ret.isEmpty()) {
			logger.fatal("No spell types found");
			System.exit(0);
		}

		return ret;
	}

	//-------------------------------------------------------------------
	public static Skill getSkill(String key) {
		for (Skill tmp : skills) {
			if (tmp.getId().equals(key))
				return tmp;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static List<Spell> getSpells() {
		return spells;
	}

	//-------------------------------------------------------------------
	/**
	 * Get the skills of a specific type (e.g. combat)
	 * @see Skill.SkillType
	 */
	public static List<Spell> getSpells(Skill type) {
		List<Spell> typedSpells = new ArrayList<Spell>();
		for (Spell spell : spells) {
			for (SpellSchoolEntry school : spell.getSchools()) {
				if (school.getSchool()==type)
					typedSpells.add(spell);
			}
		}
		return typedSpells;
	}

	//-------------------------------------------------------------------
	public static Spell getSpell(String key) {
		for (Spell tmp : spells) {
			if (tmp.getId().equals(key))
				return tmp;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static Power getPower(String key) {
		for (Power tmp : powers) {
			if (tmp.getId().equals(key))
				return tmp;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static List<Power> getPowers() {
		Collections.sort(powers);
		return powers;
	}

	//-------------------------------------------------------------------
	public static Resource getResource(String key) {
		for (Resource tmp : resources) {
			if (tmp.getId().equals(key))
				return tmp;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static boolean isBaseResource(Resource res) {
		return BASE_RESOURCES.contains(res);
	}

	//-------------------------------------------------------------------
	public static List<Education> getEducations() {
		Collections.sort(educations);
		return educations;
	}

	//-------------------------------------------------------------------
	public static Education getEducation(String key) {
		for (Education tmp : educations) {
			if (tmp.getKey().equals(key))
				return tmp;
			// Search in variants
			for (Education variant : tmp.getVariants()) {
				if (variant.getKey().equals(key))
					return variant;
			}
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static List<Resource> getResources() {
		Collections.sort(resources);
		return resources;
	}

	//-------------------------------------------------------------------
	public static List<CultureLore> getCultureLores() {
		return cultureLores;
	}

	//-------------------------------------------------------------------
	public static CultureLore getCultureLore(String key) {
		for (CultureLore tmp : cultureLores) {
			if (tmp.getKey().equals(key))
				return tmp;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static List<Language> getLanguages() {
		return languages;
	}

	//-------------------------------------------------------------------
	public static Language getLanguage(String key) {
		for (Language tmp : languages) {
			if (tmp.getKey().equals(key))
				return tmp;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static List<Race> getRaces() {
		return races;
	}

	//-------------------------------------------------------------------
	public static Race getRace(String key) {
		for (Race race : races) {
			if (race.getKey().equals(key))
				return race;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static List<Culture> getCultures() {
		return cultures;
	}

	//-------------------------------------------------------------------
	public static Culture getCulture(String key) {
		for (Culture tmp : cultures) {
			if (tmp.getKey().equals(key))
				return tmp;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static List<Background> getBackgrounds() {
		return backgrounds;
	}

	//-------------------------------------------------------------------
	public static Background getBackground(String key) {
		for (Background tmp : backgrounds) {
			if (tmp.getKey().equals(key))
				return tmp;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static byte[] save(SpliMoCharacter character) {
		// Ensure that all custom items are added to char
		// - and eliminate all others
		character.clearCustomItems();
		for (CarriedItem item : character.getItems())
			if (item.getItem().isCustom())
				character.addCustomItem(item.getItem());

		// Ensure that all custom creatures are added to char
		// - and eliminate all others
		character.clearCustomCreatures();
		for (CreatureReference item : character.getCreatures())
			if (item.getTemplate()!=null && item.getTemplate().isCustom())
				character.addCustomCreature(item.getTemplate());


		try {
			StringWriter out = new StringWriter();
			serializer.write(character, out);
			return out.toString().getBytes(Charset.forName("UTF-8"));
		} catch (IOException e) {
			logger.error("Failed generating XML for char",e);
			StringWriter mess = new StringWriter();
			mess.append("Failed saving character\n\n");
			e.printStackTrace(new PrintWriter(mess));
			BabylonEventBus.fireEvent(BabylonEventType.UI_MESSAGE, 2, mess.toString());
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static SpliMoCharacter load(byte[] raw) throws IOException {
		return load(new StreamSource( new ByteArrayInputStream( raw ) ));
	}

	//-------------------------------------------------------------------
	public static SpliMoCharacter load(InputStream ins) throws IOException {
		return load(new StreamSource( ins ));
	}

	//-------------------------------------------------------------------
	public static SpliMoCharacter load(Path path) throws IOException {
		String buf = new String(Files.readAllBytes(path));
		return load(new StreamSource( new StringReader( buf ) ));
	}

	//-------------------------------------------------------------------
	public static SpliMoCharacter load(Source source) throws IOException {
		try {
			SpliMoCharacter ret = serializer.read(SpliMoCharacter.class, source);
			logger.info("Character successfully loaded: "+ret);

			SplitterTools.applyAfterLoadModifications(ret);
			loadEquipmentModifications(ret);
			return ret;
		} catch (Exception e) {
			logger.fatal("Failed loading character: "+e,e);
			throw new IOException(e);
		}
	}

	//-------------------------------------------------------------------
	/**
	 * Prepare all character equipment for usage and eventually equip it
	 */
	private static void loadEquipmentModifications(SpliMoCharacter model) {
		logger.debug("Update modifications from "+model.getItems().size()+" equipment pieces");
		for (CarriedItem item : model.getItems()) {
			logger.debug("************"+item.getName()+"***************");
			if (item.getPersonalizations().isEmpty())
				logger.debug("*** No personalizations");
			else
				logger.debug("*** personalizations = "+item.getPersonalizations());
			// Update from V1
			SplitterTools.convertFromV1Chars(item);
			// Determine enhancement modifications for real object
			EquipmentTools.determineEnhancementReferenceModifications(item);

			if (item.getLocation()!=ItemLocationType.BODY) {
				EquipmentTools.applyEnhancements(model, item);
			} else {
				EquipmentTools.equip(model, item);
			}
		}

		EquipmentTools.updateTotalHandicap(model);
	}

	//-------------------------------------------------------------------
	private static void makeEquipmentUnlicensed(ItemTemplate tmp) {
		if (tmp.getPlugin().getID().equals("CORE"))
			return;
		if (tmp.isType(ItemType.WEAPON)) {
			if (tmp.getType(Weapon.class)==null) {
				logger.error("Invalid XML data for "+tmp);
				return;
			}
			tmp.getType(Weapon.class).setDamage(-1);
			tmp.getType(Weapon.class).setSpeed(-1);
		}
		if (tmp.isType(ItemType.LONG_RANGE_WEAPON)) {
			if (tmp.getType(LongRangeWeapon.class)==null) {
				logger.error("Invalid XML data for "+tmp);
				return;
			}
			tmp.getType(LongRangeWeapon.class).setDamage(-1);
			tmp.getType(LongRangeWeapon.class).setSpeed(-1);
		}
		if (tmp.isType(ItemType.ARMOR)) {
			if (tmp.getType(Armor.class)==null) {
				logger.error("Invalid XML data for "+tmp);
				return;
			}
			tmp.getType(Armor.class).setDefense(-1);
			tmp.getType(Armor.class).setTickMalus(-1);
			tmp.getType(Armor.class).setDamageReduction(-1);
			tmp.getType(Armor.class).setHandicap(-1);
		}
		if (tmp.isType(ItemType.SHIELD)) {
			if (tmp.getType(Shield.class)==null) {
				logger.error("Invalid XML data for "+tmp);
				return;
			}
			tmp.getType(Shield.class).setDefense(-1);
			tmp.getType(Shield.class).setTickMalus(-1);
			tmp.getType(Shield.class).setHandicap(-1);
		}
	}

	//-------------------------------------------------------------------
	private static void loadEquipmentSpellBonuses(RulePlugin<?> plugin, ResourceBundle resources,
			List<ItemTemplate> loadedItems) throws Exception {
		String key = "equipment.bonuses.resource";
		if (resources==null || !resources.containsKey(key))
			return;
		try (InputStream stream = plugin.getClass().getResourceAsStream(resources.getString(key))) {
			if (stream==null)
				throw new IOException("Missing equipment bonus resource: "+resources.getString(key));
			org.prelle.splimo.items.ItemSpellBonusList bonuses = serializer.read(
					org.prelle.splimo.items.ItemSpellBonusList.class, stream);
			for (ItemTemplate item : loadedItems) {
				List<org.prelle.splimo.items.ItemSpellBonus> matching = new ArrayList<>();
				for (org.prelle.splimo.items.ItemSpellBonus bonus : bonuses) {
					if (item.getId().equals(bonus.getItemId())) {
						if (bonus.getSkill()==null || bonus.getValue()<=0 || bonus.getTypes().isEmpty())
							throw new IllegalArgumentException("Invalid spell bonus for "+item.getId());
						matching.add(bonus);
					}
				}
				item.setSpellBonuses(matching);
			}
		}
	}

	private static void loadEquipmentCatalogProfiles(RulePlugin<?> plugin, ResourceBundle resources,
			List<ItemTemplate> loadedItems) throws Exception {
		String key = "equipment.profiles.resource";
		if (resources==null || !resources.containsKey(key)) return;
		try (InputStream stream = plugin.getClass().getResourceAsStream(resources.getString(key))) {
			if (stream==null) throw new IOException("Missing equipment profile resource: "+resources.getString(key));
			org.prelle.splimo.items.ItemCatalogProfileList profiles = serializer.read(
					org.prelle.splimo.items.ItemCatalogProfileList.class, stream);
			java.util.Set<String> ids = new java.util.HashSet<>();
			for (org.prelle.splimo.items.ItemCatalogProfile profile : profiles) {
				if (profile.getAlchemyCost()<0 || !ids.add(profile.getItemId()))
					throw new IllegalArgumentException("Invalid catalogue profile: "+profile.getItemId());
				for (ItemTemplate item : loadedItems) {
					if (item.getId().equals(profile.getItemId())) item.setCatalogProfile(profile);
				}
			}
		}
	}

	public static void loadEquipment(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load equipment (Plugin="+plugin.getID()+")");

		if (missingLicense) {
			logger.debug("Missing license for "+plugin.getID()+" equipment");
		}
		try {
			ItemList toAdd = serializer.read(ItemList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" equipments");

			// Set translation
			for (ItemTemplate tmp : new ArrayList<>(toAdd)) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled()) {
					logger.debug("* "+tmp.getName());
					tmp.getPage();
				}
				ItemTemplate prev = getItem(tmp.getID());
				if (prev!=null) {
					if (missingLicense) {
						logger.debug("  Prefer "+tmp.getID()+" from '"+prev.getProductName()+"' over that from '"+tmp.getProductName()+"', since license not granted");
						toAdd.remove(tmp);
						continue;
					} else {
						// Replace old
						items.remove(prev);
						logger.debug("  Replace "+prev.getName()+" with definition from "+plugin);
					}
				}

				if (missingLicense) {
					makeEquipmentUnlicensed(tmp);
				}
			}

			items.addAll(toAdd);
			if (!missingLicense) {
				loadEquipmentSpellBonuses(plugin, resrc, toAdd);
				loadEquipmentCatalogProfiles(plugin, resrc, toAdd);
			}
			Collections.sort(items);

			/*
			 * For all weapons add possible specialization
			 */
			logger.debug("Add weapon specializations to skills");
			for (ItemTemplate templ : toAdd) {
				Weapon tmp = templ.getType(Weapon.class);
				if (tmp==null)
					continue;
				Skill skill = tmp.getSkill();
				if (skill.getSpecialization(templ.getID())!=null) {
					logger.debug("ignore already known skill spec "+templ);
					continue;
				}
				SkillSpecialization spec = new SkillSpecialization();
				spec.setType(SkillSpecializationType.WEAPON);
				spec.setId(templ.getID());
				skill.addSpecialization(spec);
				logger.trace("Added skill special "+spec);
			}
			/*
			 * For all range weapons add possible specialization
			 */
			logger.debug("Add range weapon specializations to skills");
			for (ItemTemplate templ : toAdd) {
				LongRangeWeapon tmp = templ.getType(LongRangeWeapon.class);
				if (tmp==null)
					continue;
				Skill skill = tmp.getSkill();
				if (skill.getSpecialization(templ.getID())!=null) {
					logger.debug("ignore already known skill spec "+templ.getID()+" from "+plugin.getID());
					continue;
				}
				SkillSpecialization spec = new SkillSpecialization();
				spec.setType(SkillSpecializationType.WEAPON);
				spec.setId(templ.getID());
				skill.addSpecialization(spec);
				logger.trace("Added skill special "+spec);
			}

		} catch (Exception e) {
			logger.fatal("Failed loading equipment: "+e,e);
			throw new RuntimeException("Error loading equipment from Splittermond Plugin '"+plugin.getReadableName()+"'");
//			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<ItemTemplate> getItems() {
        Collections.sort(items);
		return items;
	}

	//-------------------------------------------------------------------
	public static List<ItemTemplate> getItems(ItemType type) {
		List<ItemTemplate> ret = new ArrayList<ItemTemplate>();
		for (ItemTemplate tmp : items)
			if (tmp.isType(type))
				ret.add(tmp);

		Collections.sort(ret);
		return ret;
	}

	//-------------------------------------------------------------------
	public static List<ItemTemplate> getItems(Class<? extends ItemTypeData> cls) {
		List<ItemTemplate> ret = new ArrayList<ItemTemplate>();
		for (ItemTemplate tmp : items) {
			for (ItemTypeData data : tmp.getTypeData()) {
				if (data.getClass() == cls)
					ret.add(tmp);
			}
		}

		Collections.sort(ret);
		return ret;
	}

	//-------------------------------------------------------------------
	public static ItemTemplate getItem(String key) {
		for (ItemTemplate tmp : items)
			if (tmp.getID().equals(key))
				return tmp;

		return null;
	}

	//-------------------------------------------------------------------
	public static List<ItemTemplate> getWeaponItems(Skill skill) {
		List<ItemTemplate> ret = new ArrayList<ItemTemplate>();
		for (ItemTemplate tmp : items) {
			if (tmp.isType(ItemType.WEAPON) && tmp.getType(Weapon.class).getSkill()==skill)
				ret.add(tmp);
			else if (tmp.isType(ItemType.LONG_RANGE_WEAPON) && tmp.getType(LongRangeWeapon.class).getSkill()==skill)
				ret.add(tmp);
		}

		Collections.sort(ret);
		return ret;
	}

//	//-------------------------------------------------------------------
//	public static void addCustomItem(ItemTemplate item) {
//		logger.info("Add custom item: "+item);
//		if (!item.getID().startsWith("custom"))
//			throw new IllegalArgumentException("Not a custom item");
//
//		if (!items.contains(item)) {
//			items.add(item);
//			Collections.sort(items);
//		}
//
//		try {
//			saveCustomItems();
//		} catch (IOException e) {
//			logger.error("Failed saving custom items",e);
//			BabylonEventBus.fireEvent(BabylonEventType.UI_MESSAGE, 2, "Failed writing custom item data: "+e.getMessage());
//		}
//	}
//
//	//-------------------------------------------------------------------
//	public static void removeCustomItem(ItemTemplate item) {
//		logger.info("Remove custom item: "+item);
//		if (!item.getID().startsWith("custom"))
//			throw new IllegalArgumentException("Not a custom item");
//
//		items.remove(item);
//		Collections.sort(items);
//
//		try {
//			saveCustomItems();
//		} catch (IOException e) {
//			logger.error("Failed saving custom items",e);
//			BabylonEventBus.fireEvent(BabylonEventType.UI_MESSAGE, 2, "Failed writing custom item data: "+e.getMessage());
//		}
//	}
//
//	//-------------------------------------------------------------------
//	public static void saveCustomItems() throws SerializationException, IOException {
//		ItemList list = new ItemList();
//		for (ItemTemplate item : items) {
////			logger.debug(item.toString());
//			if (item.getID().startsWith("custom")) {
//				logger.debug("Found "+item.getID());
//				list.add(item);
//			}
//		}
//
//		String dataDir = RPGFrameworkLoader.getInstance().getConfiguration().getOption(RPGFramework.PROP_DATADIR).getStringValue();
////		Preferences usr = Preferences.userRoot().node("/de/rpgframework");
////		String defaultDir =
////				System.getProperty("user.home")
////				+System.getProperty("file.separator")
////				+"rpgframework"
////				+System.getProperty("file.separator")
////				+"custom";
////		String localDirName = usr.get("custom.datadir", defaultDir);
////		usr.put("pluginDir.rules", localDirName);
////
//		Path path = FileSystems.getDefault().getPath(dataDir, "custom", RoleplayingSystem.SPLITTERMOND.name().toLowerCase(),"items.xml");
//		Files.createDirectories(path.getParent());
//		logger.info("Save "+list.size()+" custom items to "+path);
//
//		serializer.write(list, path.toFile());
//	}
//
//	//-------------------------------------------------------------------
//	public static List<ItemTemplate> getCustomItems() throws SerializationException, IOException {
//		String dataDir = RPGFrameworkLoader.getInstance().getConfiguration().getOption(RPGFramework.PROP_DATADIR).getStringValue();
//		logger.info("Search for custom items in "+dataDir);
////		Preferences usr = Preferences.userRoot().node("/de/rpgframework");
////		String defaultDir =
////				System.getProperty("user.home")
////				+System.getProperty("file.separator")
////				+"rpgframework"
////				+System.getProperty("file.separator")
////				+"custom";
////		String localDirName = usr.get("custom.datadir", defaultDir);
////		usr.put("pluginDir.rules", localDirName);
////
////		Path path = FileSystems.getDefault().getPath(localDirName, "splittermond","items.xml");
//		Path path = FileSystems.getDefault().getPath(dataDir, "custom", RoleplayingSystem.SPLITTERMOND.name().toLowerCase(),"items.xml");
//		if (!Files.exists(path)) {
//			logger.info("No custom items found at "+path);
//			return new ArrayList<>();
//		}
//		logger.debug("Load custom items to "+path);
//
//		ItemList list = serializer.read(ItemList.class, new FileReader(path.toFile()));
//		Collections.sort(list);
//		return list;
//	}
//
//	//-------------------------------------------------------------------
//	public static void loadCustomItems() throws SerializationException, IOException {
//		String dataDir = RPGFrameworkLoader.getInstance().getConfiguration().getOption(RPGFramework.PROP_DATADIR).getStringValue();
//		logger.info("Search for custom items in "+dataDir);
////		Preferences usr = Preferences.userRoot().node("/de/rpgframework");
////		String defaultDir =
////				System.getProperty("user.home")
////				+System.getProperty("file.separator")
////				+"rpgframework"
////				+System.getProperty("file.separator")
////				+"custom";
////		String localDirName = usr.get("custom.datadir", defaultDir);
////		usr.put("pluginDir.rules", localDirName);
////
////		Path path = FileSystems.getDefault().getPath(localDirName, "splittermond","items.xml");
//		Path path = FileSystems.getDefault().getPath(dataDir, "custom", RoleplayingSystem.SPLITTERMOND.name().toLowerCase(),"items.xml");
//		if (!Files.exists(path)) {
//			logger.info("No custom items found at "+path);
//			return;
//		}
//		logger.debug("Load custom items to "+path);
//
//		ItemList list = serializer.read(ItemList.class, new FileReader(path.toFile()));
//		items.addAll(list);
//		logger.info("Loaded "+list.size()+" custom items from "+path);
//
//		/*
//		 * For all weapons add possible specialization
//		 */
//		logger.debug("Add weapon specializations to skills");
//		for (ItemTemplate templ : new ArrayList<>(list)) {
//			Weapon tmp = templ.getType(Weapon.class);
//			if (tmp==null)
//				continue;
//			Skill skill = tmp.getSkill();
//			if (skill==null) {
//				logger.error("No skill assigned to custom weapon "+templ.getID());
//				BabylonEventBus.fireEvent(BabylonEventType.UI_MESSAGE, 1, "Cannot load weapon '"+templ.getID()+"' since it has no skill assigned.\nDeleting the file "+path+" might help.\nMake sure you delete that item from all your characters");
//				list.remove(tmp);
//				removeCustomItem(templ);
//			}
//			if (skill.getSpecialization(templ.getID())!=null) {
//				logger.debug("ignore already known skill spec "+templ);
//				continue;
//			}
//			SkillSpecialization spec = new SkillSpecialization();
//			spec.setType(SkillSpecializationType.WEAPON);
//			spec.setId(templ.getID());
//			skill.addSpecialization(spec);
//			logger.trace("Added skill special "+spec);
//		}
//		/*
//		 * For all range weapons add possible specialization
//		 */
//		logger.debug("Add range weapon specializations to skills");
//		for (ItemTemplate templ : list) {
//			LongRangeWeapon tmp = templ.getType(LongRangeWeapon.class);
//			if (tmp==null)
//				continue;
//			Skill skill = tmp.getSkill();
//			if (skill.getSpecialization(templ.getID())!=null) {
//				logger.warn("ignore already known skill spec "+templ);
//				continue;
//			}
//			SkillSpecialization spec = new SkillSpecialization();
//			spec.setType(SkillSpecializationType.WEAPON);
//			spec.setId(templ.getID());
//			skill.addSpecialization(spec);
//			logger.trace("Added skill special "+spec);
//		}
//
//		Collections.sort(items);
//	}

	//-------------------------------------------------------------------
	public static PromoData registerPromo(String code, String secret) {
		return null;
	}

	//-------------------------------------------------------------------
	private static boolean verifyPromo(String code, String secret) {
		String url_s = String.format(PROMOSERVER_CHECK_URL, code, secret);
		try {
			HttpURLConnection con = (HttpURLConnection) (new URL(url_s)).openConnection();
			con.setRequestMethod("GET");
			con.connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	//-------------------------------------------------------------------
	public static void loadPromoData() throws SerializationException, IOException {
		if (true)
			return;
		String dataDir = RPGFrameworkLoader.getInstance().getConfiguration().getOption(RPGFramework.PROP_PROMODIR).getStringValue();
		logger.info("Search for promo data in "+dataDir);
//		Preferences usr = Preferences.userRoot().node("/de/rpgframework");
//		String defaultDir =
//				System.getProperty("user.home")
//				+System.getProperty("file.separator")
//				+"rpgframework"
//				+System.getProperty("file.separator")
//				+"custom";
//		String localDirName = usr.get("custom.datadir", defaultDir);
//		usr.put("pluginDir.rules", localDirName);
//
//		Path path = FileSystems.getDefault().getPath(localDirName, "splittermond","items.xml");
		Path path = FileSystems.getDefault().getPath(dataDir, "custom", RoleplayingSystem.SPLITTERMOND.name().toLowerCase(),"items.xml");
		if (!Files.exists(path)) {
			logger.info("No promo items found at "+path+" - generate empty file");
			promos = new PromoDataList();
			promos.setSecret(System.getProperty("user.name") +"-"+ UUID.randomUUID());
			serializer.write(promos, path.toFile());
			return;
		}
		logger.debug("Load promo data to "+path);

		promos = serializer.read(PromoDataList.class, new FileReader(path.toFile()));
		logger.info("Loaded "+promos.size()+" promo data from "+path+" for user "+promos.getSecret());

		/*
		 * For all data verify that
		 */
		for (PromoData promo : new ArrayList<>(promos)) {
			// Verify code
			boolean verified = verifyPromo(promo.getCode(), promos.getSecret());
			if (!verified) {
				if (promo.getItem()!=null) {
					String mess = String.format(i18NResources.getString("error.unauthorized_promo.item"), promo.getItem().getName());
					makeEquipmentUnlicensed(promo.getItem());
					BabylonEventBus.fireEvent(BabylonEventType.UI_MESSAGE, 2, mess);
				}
			}

			// Add item data
			if (promo.getItem()!=null) {
				promo.getItem().setResourceBundle(i18NResources);
				promo.getItem().setHelpResourceBundle(i18NHelpResources);
				items.add(promo.getItem());
				ItemTemplate templ = promo.getItem();
				Weapon tmp = templ.getType(Weapon.class);
				if (tmp!=null) {
					Skill skill = tmp.getSkill();
					if (skill.getSpecialization(templ.getID())==null) {
						SkillSpecialization spec = new SkillSpecialization();
						spec.setType(SkillSpecializationType.WEAPON);
						spec.setId(templ.getID());
						skill.addSpecialization(spec);
						logger.trace("Added skill special "+spec);
					}
				}
				tmp = templ.getType(LongRangeWeapon.class);
				if (tmp!=null) {
					Skill skill = tmp.getSkill();
					if (skill.getSpecialization(templ.getID())!=null) {
						logger.warn("ignore already known skill spec "+templ);
						continue;
					}
					SkillSpecialization spec = new SkillSpecialization();
					spec.setType(SkillSpecializationType.WEAPON);
					spec.setId(templ.getID());
					skill.addSpecialization(spec);
					logger.trace("Added skill special "+spec);
				}
			}

			// Add creature data
			if (promo.getCreature()!=null) {
				promo.getCreature().setResourceBundle(i18NResources);
				promo.getCreature().setHelpResourceBundle(i18NHelpResources);
				creatures.add(promo.getCreature());
			}
		}

		Collections.sort(items);
		Collections.sort(creatures);
	}

	//-------------------------------------------------------------------
	public static void loadMaterials(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load materials (Plugin="+plugin.getID()+")");

		try {
			MaterialList toAdd = serializer.read(MaterialList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" materials");

			// Set translation
			for (Material tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);

				if (missingLicense)
					tmp.getModifications().clear();

				if (logger.isDebugEnabled()) {
					logger.debug("* "+tmp.getName());
				}
				tmp.getName();
				tmp.getPage();
				tmp.getHelpText();
				tmp.getEffectText();
			}

			materials.addAll(toAdd);
			Collections.sort(materials);

			if (missingLicense) {
				logger.info("No license available for "+plugin.getReadableName()+" - removing material modifications");
			}
		} catch (Exception e) {
			logger.fatal("Failed loading material: "+e,e);
			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<Material> getMaterials() {
		return new ArrayList<>(materials);
	}

	//-------------------------------------------------------------------
	public static List<Material> getMaterials(MaterialType type) {
		List<Material> ret = new ArrayList<>();
		for (Material check : materials) {
			if (check.getType()==type)
				ret.add(check);
		}
		Collections.sort(ret);
		return ret;
	}

	//-------------------------------------------------------------------
	public static Material getMaterial(String key) {
		for (Material tmp : materials)
			if (tmp.getId().equals(key))
				return tmp;

		return null;
	}


	//-------------------------------------------------------------------
	public static void loadEnhancements(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load enhancements (Plugin="+plugin.getID()+")");
		try {
			EnhancementList toAdd = serializer.read(EnhancementList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" enhancements");

			// Set translation
			for (Enhancement tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				tmp.getHelpText();
				tmp.getName();
				tmp.getPage();
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName()+"   type="+tmp.getType());

				// Set modification source
				for (Modification mod : tmp.getModifications()) {
					mod.setSource(tmp);
					if ((mod instanceof AttributeModification)) {
						switch (tmp.getType()) {
						case ALCHEMY:
						case NORMAL:
						case RELIC:
							((AttributeModification)mod).setModificationSource(ModificationSource.EQUIPMENT);
							break;
						case DIVINE:
						case MAGIC:
						case SAINT:
							((AttributeModification)mod).setModificationSource(ModificationSource.MAGICAL);
							break;
						}
					}
					if ((mod instanceof SkillModification)) {
						switch (tmp.getType()) {
						case ALCHEMY:
						case NORMAL:
						case RELIC:
							((SkillModification)mod).setModificationSource(ModificationSource.EQUIPMENT);
							break;
						case DIVINE:
						case MAGIC:
						case SAINT:
							((SkillModification)mod).setModificationSource(ModificationSource.MAGICAL);
							break;
						}
					}
				}
			}

			enhancements.addAll(toAdd);
			Collections.sort(enhancements);
		} catch (Exception e) {
			logger.fatal("Failed loading material: "+e,e);
			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static void addEnhancement(Enhancement data) {
		if (!enhancements.contains(data))
			enhancements.add(data);
	}

	//-------------------------------------------------------------------
	/**
	 * Return all enhancements user has the license to
	 */
	public static List<Enhancement> getEnhancements() {
		List<Enhancement> ret = new ArrayList<>();
		for (Enhancement check : enhancements) {
				ret.add(check);
		}
		return ret;
	}

	//-------------------------------------------------------------------
	public static Enhancement getEnhancement(String key) {
		for (Enhancement tmp : enhancements)
			if (tmp.getId().equals(key))
				return tmp;

		return null;
	}

	//-------------------------------------------------------------------
	public static void loadPersonalizations(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load personalizations (Plugin="+plugin.getID()+")");
		try {
			PersonalizationList toAdd = serializer.read(PersonalizationList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" personalizations");

			// Set translation
			for (Personalization tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName());
				// Where applicable declare as modification sources
				for (Modification mod : tmp.getModifications()) {
					if (mod instanceof AttributeModification && ((AttributeModification)mod).getModificationSource()==null)
						((AttributeModification)mod).setModificationSource(ModificationSource.EQUIPMENT);
					if (mod instanceof SkillModification && ((SkillModification)mod).getModificationSource()==null)
						((SkillModification)mod).setModificationSource(ModificationSource.EQUIPMENT);
				}
			}

			personalizations.addAll(toAdd);
		} catch (Exception e) {
			logger.fatal("Failed loading personalization: "+e,e);
			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<Personalization> getPersonalizations() {
		return new ArrayList<>(personalizations);
	}

	//-------------------------------------------------------------------
	public static void addPersonalizations(Personalization perso) {
		if (!personalizations.contains(perso))
			personalizations.add(perso);
	}

	//-------------------------------------------------------------------
	public static List<Personalization> getPersonalizations(ItemType type) {
		List<Personalization> ret = new ArrayList<>();
		for (Personalization check : personalizations) {
			if (check.getSupportedItemType()==type)
				ret.add(check);
		}
		return ret;
	}

	//-------------------------------------------------------------------
	public static Personalization getPersonalization(String key) {
		for (Personalization tmp : personalizations)
			if (tmp.getId().equals(key))
				return tmp;

		return null;
	}

	//-------------------------------------------------------------------
	public static void loadCreatures(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load creatures (Plugin="+plugin.getID()+")");
		try {
			CreatureList toAdd = serializer.read(CreatureList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" creatures");

			// Set translation
			for (Creature tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName());

//				if (tmp.getId().equals("fightingdog"))
//					System.err.println(tmp.dump());
//				SplitterTools.fixCreatureSkillPoints(tmp);
//				if (tmp.getId().equals("fightingdog"))	 {
//					System.err.println(tmp.dump());
//					System.exit(0);
//				}
				// Check features for completeness
				for (CreatureFeature feat : tmp.getFeatures()) {
					if (feat.getType().hasLevels() && feat.getLevel()<1)
						logger.warn("Creature "+tmp.getId()+" misses a level in feature "+feat.getName());
					if (feat.getType().getSelect()==CreatureFeatureType.Selector.DAMAGE_TYPE && feat.getDamageType()==null)
						logger.warn("Creature "+tmp.getId()+" must choose damage type in feature "+feat.getName());
					if (feat.getType().hasCount() && feat.getCount()<1)
						logger.warn("Creature "+tmp.getId()+" misses a count in feature "+feat.getName());
					if (feat.getType().hasModifications() && feat.getModifications().isEmpty())
						logger.warn("Creature "+tmp.getId()+" misses modifications in feature "+feat.getName());
				}
			}

			creatures.addAll(toAdd);
		} catch (Exception e) {
			logger.fatal("Failed loading creatures: "+e,e);
//			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static void loadCreatureTypes(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load creature types (Plugin="+plugin.getID()+")");
		try {
			CreatureTypeList toAdd = serializer.read(CreatureTypeList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" creature types");

			// Set translation
			for (CreatureType tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				tmp.getName();
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName()+"  "+(tmp.hasLevel()?"with levels":""));
			}

			creatureTypes.addAll(toAdd);
			Collections.sort(creatureTypes);
		} catch (Exception e) {
			logger.fatal("Failed loading creature types: "+e,e);
			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<Creature> getCreatures() {
		return new ArrayList<>(creatures);
	}

	//-------------------------------------------------------------------
	public static void addCreature(Creature perso) {
		if (!creatures.contains(perso))
			creatures.add(perso);
	}

	//-------------------------------------------------------------------
	public static Creature getCreature(String key) {
		for (Creature tmp : creatures)
			if (tmp.getId().equals(key))
				return tmp;

		return null;
	}

	//-------------------------------------------------------------------
	public static List<Creature> getCreatures(CreatureFeatureType feature) {
		List<Creature> ret = new ArrayList<>();
		for (Creature creature : creatures) {
			for (CreatureFeature feat : creature.getFeatures()) {
				if (feat.getType()==feature) {
					ret.add(creature);
					break;
				}
			}
		}
		Collections.sort(ret);
		return ret;
	}

	//-------------------------------------------------------------------
	public static List<CreatureType> getCreatureTypes() {
		return new ArrayList<>(creatureTypes);
	}

	//-------------------------------------------------------------------
	public static void addCreatureType(CreatureType perso) {
		if (!creatureTypes.contains(perso))
			creatureTypes.add(perso);
	}

	//-------------------------------------------------------------------
	public static CreatureType getCreatureType(String key) {
		for (CreatureType tmp : creatureTypes)
			if (tmp.getKey().equals(key))
				return tmp;

		return null;
	}

	//-------------------------------------------------------------------
	public static void saveCustomCreatures() throws SerializationException, IOException {
		CreatureList list = new CreatureList();
		for (Creature data : creatures) {
			if (data.getId().startsWith("custom")) {
				logger.debug("Found "+data.getId());
				list.add(data);
			}
		}

		String dataDir = RPGFrameworkLoader.getInstance().getConfiguration().getOption(RPGFramework.PROP_DATADIR).getStringValue();
		Path path = FileSystems.getDefault().getPath(dataDir, "custom", RoleplayingSystem.SPLITTERMOND.name().toLowerCase(),"creatures.xml");
		Files.createDirectories(path.getParent());
		logger.info("Save "+list.size()+" custom creatures to "+path);

		serializer.write(list, path.toFile());
	}

	//-------------------------------------------------------------------
	public static void loadCustomCreatures() throws SerializationException, IOException {
		String dataDir = RPGFrameworkLoader.getInstance().getConfiguration().getOption(RPGFramework.PROP_DATADIR).getStringValue();
		Path path = FileSystems.getDefault().getPath(dataDir, "custom", RoleplayingSystem.SPLITTERMOND.name().toLowerCase(),"creatures.xml");
		if (!Files.exists(path)) {
			logger.info("No custom creatures found at "+path);
			return;
		}
		logger.debug("Load custom creatures from "+path);

		CreatureList list = serializer.read(CreatureList.class, new FileReader(path.toFile()));
		creatures.addAll(list);
		logger.info("Loaded "+list.size()+" custom creatures from "+path);

		Collections.sort(creatures);
	}

	//-------------------------------------------------------------------
	public static void addCustomCreature(Creature creature) {
		logger.info("Add custom creature: "+creature);
		if (!creature.getId().startsWith("custom"))
			throw new IllegalArgumentException("Not a custom creature");

		if (!creatures.contains(creature)) {
			creatures.add(creature);
			Collections.sort(creatures);
		}

		try {
			saveCustomCreatures();
		} catch (IOException e) {
			logger.error("Failed saving custom creatures",e);
			BabylonEventBus.fireEvent(BabylonEventType.UI_MESSAGE, 2, "Failed writing custom creature data: "+e.getMessage());
		}
	}

	//-------------------------------------------------------------------
	public static void loadFeatureTypes(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load feature types (Plugin="+plugin.getID()+")");
		try {
			FeatureTypeList toAdd = serializer.read(FeatureTypeList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" feature types");

			// Set translation
			for (FeatureType tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName()+"  "+(tmp.hasLevel()?"with levels":""));
			}

			featureTypes.addAll(toAdd);
		} catch (Exception e) {
			logger.fatal("Failed loading feature types: "+e,e);
			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<FeatureType> getFeatureTypes() {
		return new ArrayList<>(featureTypes);
	}

	//-------------------------------------------------------------------
	public static void addFeatureType(FeatureType perso) {
		if (!featureTypes.contains(perso))
			featureTypes.add(perso);
	}

	//-------------------------------------------------------------------
	public static FeatureType getFeatureType(String key) {
		for (FeatureType tmp : featureTypes)
			if (tmp.getId().equals(key))
				return tmp;
		logger.warn("Missing "+key+" in "+featureTypes);
		return null;
	}

	//-------------------------------------------------------------------
	public static void loadAspects(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load deity aspects (Plugin="+plugin.getID()+")");
		try {
			AspectList toAdd = serializer.read(AspectList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" aspects");

			// Set translation
			for (Aspect tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName());
			}

			aspects.addAll(toAdd);
			Collections.sort(aspects);
		} catch (Exception e) {
			logger.fatal("Failed loading deity aspects: "+e,e);
			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<Aspect> getAspects() {
		return new ArrayList<>(aspects);
	}

	//-------------------------------------------------------------------
	public static void addAspect(Aspect perso) {
		if (!aspects.contains(perso))
			aspects.add(perso);
	}

	//-------------------------------------------------------------------
	public static Aspect getAspect(String key) {
		for (Aspect tmp : aspects)
			if (tmp.getId().equals(key))
				return tmp;
		logger.warn("Missing "+key+" in "+aspects);
		return null;
	}

	//-------------------------------------------------------------------
	public static void loadDeities(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load deities (Plugin="+plugin.getID()+")");
		try {
			DeityList toAdd = serializer.read(DeityList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" deities");

			// Set translation
			for (Deity tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName()+"  ("+tmp.getAspects()+")  ("+tmp.getFavoredSkills()+")  ("+tmp.getUnusualSkills()+")  ("+tmp.getFavoredEducations()+")");
			}

			deities.addAll(toAdd);
			Collections.sort(deities);
		} catch (Exception e) {
			logger.fatal("Failed loading deities: "+e,e);
//			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<Deity> getDeities() {
		return new ArrayList<>(deities);
	}

	//-------------------------------------------------------------------
	public static void addDeity(Deity perso) {
		if (!deities.contains(perso))
			deities.add(perso);
	}

	//-------------------------------------------------------------------
	public static Deity getDeity(String key) {
		for (Deity tmp : deities)
			if (tmp.getId().equals(key))
				return tmp;
		logger.warn("Missing "+key+" in "+aspects);
		return null;
	}

	//-------------------------------------------------------------------
	public static void loadDeityTypes(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load deity types (Plugin="+plugin.getID()+")");
		try {
			DeityTypeList toAdd = serializer.read(DeityTypeList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" deity types");

			// Set translation
			for (DeityType tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled()) {
					logger.debug("* "+tmp.getName()+"  ("+tmp.getAspects()+")");
					tmp.getPage();
					tmp.getHelpText();
				}
			}

			deityTypes.addAll(toAdd);
			Collections.sort(deityTypes);
		} catch (Exception e) {
			logger.fatal("Failed loading deities: "+e,e);
			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<DeityType> getDeityTypes() {
		return new ArrayList<>(deityTypes);
	}

	//-------------------------------------------------------------------
	public static void addDeityType(DeityType perso) {
		if (!deityTypes.contains(perso))
			deityTypes.add(perso);
	}

	//-------------------------------------------------------------------
	public static DeityType getDeityType(String key) {
		for (DeityType tmp : deityTypes)
			if (tmp.getId().equals(key))
				return tmp;
		logger.warn("Missing "+key+" in "+deityTypes);
		return null;
	}

	//-------------------------------------------------------------------
	public static void loadHolyPowers(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load holy powers (Plugin="+plugin.getID()+")");
		try {
			HolyPowerList toAdd = serializer.read(HolyPowerList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" holy powers");

			// Set translation
			for (HolyPower tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled()) {
					logger.debug("* "+tmp.getName()+"  ("+tmp.getDeityType()+" "+tmp.getLevel()+")");
					tmp.getPage();
					tmp.getHelpText();
				}
			}

			holyPowers.addAll(toAdd);
			Collections.sort(holyPowers);
		} catch (Exception e) {
			logger.fatal("Failed loading holy powers: "+e,e);
			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<HolyPower> getHolyPowers() {
		return new ArrayList<>(holyPowers);
	}

	//-------------------------------------------------------------------
	public static void addHolyPower(HolyPower perso) {
		if (!holyPowers.contains(perso))
			holyPowers.add(perso);
	}

	//-------------------------------------------------------------------
	public static HolyPower getHolyPower(String key) {
		for (HolyPower tmp : holyPowers)
			if (tmp.getId().equals(key))
				return tmp;
		logger.warn("Missing "+key+" in "+holyPowers);
		return null;
	}

	//-------------------------------------------------------------------
	public static void loadCreatureModules(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load creature modules (Plugin="+plugin.getID()+")");
		try {
			CreatureModuleList toAdd = serializer.read(CreatureModuleList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" creature modules");

			// Set translation
			for (CreatureModule tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled()) {
					logger.debug("* "+tmp.getType()+" "+tmp.getName());
					tmp.getPage();
					tmp.getHelpText();
				}
			}

			creatureModules.addAll(toAdd);

			// Check requirements
			for (CreatureModule tmp : toAdd) {
				for (Requirement req : tmp.getRequirements()) {
					if (!req.resolve()) {
						logger.error("Unresolved requirement "+req.getClass()+" for creature module "+tmp.getId()+" in plugin "+plugin.getID());
					}
				}
			}

			Collections.sort(creatureModules);
		} catch (Exception e) {
			logger.fatal("Failed loading creature modules: "+e,e);
			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<CreatureModule> getCreatureModules() {
		return new ArrayList<>(creatureModules);
	}

	//-------------------------------------------------------------------
	public static void addCreatureModule(CreatureModule perso) {
		if (!creatureModules.contains(perso))
			creatureModules.add(perso);
	}

	//-------------------------------------------------------------------
	public static CreatureModule getCreatureModule(String key) {
		for (CreatureModule tmp : creatureModules)
			if (tmp.getId().equals(key))
				return tmp;
		logger.warn("Missing '"+key+"' in "+creatureModules.size()+" creature modules");
		return null;
	}

	//-------------------------------------------------------------------
	public static void loadCreatureFeatureTypes(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load creature feature types (Plugin="+plugin.getID()+")");
		try {
			CreatureFeatureTypeList toAdd = serializer.read(CreatureFeatureTypeList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" creature feature types");

			// Set translation
			for (CreatureFeatureType tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName());
				tmp.getPage();
				tmp.getHelpText();
			}

			creatFeatTypes.addAll(toAdd);
		} catch (Exception e) {
			logger.fatal("Failed loading creature feature types: "+e,e);
			System.exit(0);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static CreatureFeatureType getCreatureFeatureType(String v) {
		for (CreatureFeatureType tmp : creatFeatTypes) {
			if (tmp.getId().equals(v))
				return tmp;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public static List<CreatureFeatureType> getCreatureFeatureTypes() {
		return new ArrayList<>(creatFeatTypes);
	}

	//-------------------------------------------------------------------
	public static void loadNameTable(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load name table (Plugin="+plugin.getID()+")");
		try {
			SpliMoNameTable toAdd = serializer.read(SpliMoNameTable.class, in);
			logger.info("Successfully loaded name table for culture "+toAdd.getCulture());

			nameTables.put(toAdd.getCulture(), toAdd);
		} catch (Exception e) {
			logger.fatal("Failed loading name table: "+e,e);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static SpliMoNameTable getNameTable(Culture culture) {
		return nameTables.get(culture);
	}

	//-------------------------------------------------------------------
	public static List<SpliMoNameTable> getNameTables() {
		List<SpliMoNameTable> ret = new ArrayList<>();
		for (Culture key : getCultures()) {
			if (nameTables.containsKey(key))
				ret.add(nameTables.get(key));
		}
		return ret;
	}
	//-------------------------------------------------------------------
	public static List<Deity> getDeitiesForCulture(Culture culture) {
		List<Deity> result = new ArrayList<>();
		for (Deity deity: deities) {
			if (deity.getFavoredCultures().contains(culture)) {
				result.add(deity);
			}
		}
		return result;
	}

	//-------------------------------------------------------------------
	public static List<Deity> getDeitiesForEducation(Education education) {
		List<Deity> result = new ArrayList<>();
		for (Deity deity: deities) {
			if (deity.getFavoredEducations().contains(education)) {
				result.add(deity);
			}
		}
		return result;
	}

	//-------------------------------------------------------------------
	public static void loadTowns(RulePlugin<? extends SpliMoCharacter> plugin, InputStream in, ResourceBundle resrc, ResourceBundle helpResources) {
		logger.debug("Load towns (Plugin="+plugin.getID()+")");
		try {
			TownList toAdd = serializer.read(TownList.class, in);
			logger.info("Successfully loaded "+toAdd.size()+" towns");

			// Set translation
			for (Town tmp : toAdd) {
				tmp.setResourceBundle(resrc);
				tmp.setHelpResourceBundle(helpResources);
				tmp.setPlugin(plugin);
				tmp.getName();
				tmp.getCultures();
				if (logger.isDebugEnabled())
					logger.debug("* "+tmp.getName());

				// If town already existed, remove it
				Town prev = getTown(tmp.getId());
				if (prev!=null) {
					logger.debug("  Prefer "+tmp.getId()+" from '"+prev.getProductName()+"' over that from '"+tmp.getProductName()+"'");
					toAdd.remove(tmp);
					continue;
				}
			}

			towns.addAll(toAdd);
			Collections.sort(towns);
		} catch (Exception e) {
			logger.fatal("Failed loading towns: "+e,e);
			return;
		}
	}

	//-------------------------------------------------------------------
	public static List<Town> getTowns() {
		return towns;
	}

	//-------------------------------------------------------------------
	public static Town getTown(String key) {
		for (Town tmp : towns) {
			if (tmp.getId().equals(key))
				return tmp;
		}
		return null;
	}

}

