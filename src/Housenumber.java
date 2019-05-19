import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;




/*

	V1.0, 08.08.2013, Dietmar Seifert
		* works as a cache between table auswertung_hausnummern during the workflow phase auswertung_offline.java,
		* meaning 
		* - at program start (of a job) the existing content of table auswertung_hausnummern will be read into this cache
		* - in the table auswertung_hausnummern the rows will NOT be deleted furthermore
		* - the changes or unchanged calculation will be stored in this cache
		* - at the end, this cache will be written back to the table auswertung_hausnummern in this way
		* 	- untouched cache entries must be deleted in table, because they doesn't exists anymore
		* 	- unchanged cache entries doesn't cause any work
		* 	- changed cache entries will be written to the table back 
		* This cache has two goals
		* - time for program auswertung_offline.java should be dramatically lower, because most of time is insert of rows
		* - the auswertung for the actual job is not available during calculation, thats bad for end users
*/


public class Housenumber {
	
	public enum Treffertyp {
		OSM_ONLY, IDENTICAL, LIST_ONLY, UNSET;
	}
	
	private boolean isHousenumberaddition_exactly;
	private long 	id = -1L;
	private String	strasse = "";			// only valid for new objects
	private String	postcode = "";
	private String	hausnummer_sortierbar = "";
	private String	hausnummer_normalisiert = "";
	private Treffertyp treffertyp = Treffertyp.UNSET;
	private String	hausnummer = "";
	private String	osm_tag = "";			// now "key"=>"value" (including " !!!) - old, up to 23.08.2013: value of osm tag building where found addr:housenumber=*
	private int 	osm_tag_prio = 9999;
	private String	osm_objektart = "";		// "way", "node" or "relation"
	private long	osm_id = -1L;
	private String  lonlat = "";
	private String	lonlat_source = "";
	private String  hausnummerkommentar = "";
			// bad implementation, to set this field at every housenumber instance, but don't know how to get info from housenumbercollection instance
	private HousenumberCollection.FieldsForUniqueAddress	fieldsForUniqueAddress = HousenumberCollection.FieldsForUniqueAddress.STREET_HOUSENUMBER;
	private HousenumberCollection.FieldsForUniqueAddress	alternateFieldsForUniqueAddress = null;
	private HashMap<String, HousenumberCollection.FieldsForUniqueAddress> keypoolFieldsForUniqueAddress = null;


	private String	state = ""; // ""=not set; "untouched"=got from database, without updates; "updated"=updated, "deleted!; "added"=real new entry

	private static final Logger logger = Evaluation.logger;

	private void workcache_debug(String outputtext) {
		PrintWriter debug_output = null;
		String workcache_debugfile = "workcache_debug.txt";
		
		try {
			debug_output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(workcache_debugfile,true),"UTF-8")));
			debug_output.println(outputtext);
			System.out.println("***" + outputtext);
			debug_output.close();
		} catch (IOException ioerror) {
			System.out.println("ERROR: couldn't open file to write, filename was ==="+workcache_debugfile+"===");
			ioerror.printStackTrace();
		}
	}


	public Housenumber(	HousenumberCollection collectioninstance)  {
		this.isHousenumberaddition_exactly = collectioninstance.ishousenumberadditionCaseSentity();
		this.fieldsForUniqueAddress = collectioninstance.getFieldsForUniqueAddress();
		this.alternateFieldsForUniqueAddress = collectioninstance.getAlternateFieldsForUniqueAddress();
		this.keypoolFieldsForUniqueAddress = collectioninstance.getkeypoolFieldsForUniqueAddress();
	}
	
		/**
		 * 
		 * @return Key for the cache entry of the object. Normally will be build with streetname and housenumber
		 */
	public String getListKey() {
		String listkey = "";

		if(this.fieldsForUniqueAddress.compareTo(HousenumberCollection.FieldsForUniqueAddress.STREET_HOUSENUMBER) == 0)
			listkey = this.getStrasse().toLowerCase() + this.getHousenumberNormalized();
		else if(this.fieldsForUniqueAddress.compareTo(HousenumberCollection.FieldsForUniqueAddress.STREET_POSTCODE_HOUSENUMBER) == 0)
			listkey = this.getStrasse().toLowerCase()  + this.getPostcode().toLowerCase() + this.getHousenumberNormalized();
		else if(this.fieldsForUniqueAddress.compareTo(HousenumberCollection.FieldsForUniqueAddress.POSTCODE_HOUSENUMBER) == 0)
			listkey = this.getPostcode().toLowerCase() + this.getHousenumberNormalized();
		return listkey;
	}


		/**
		 * 
		 * @return Key for the cache entry of the object. Normally will be build with streetname and housenumber
		 */
	public String getListKey(String cachename) {
		String listkey = "";
	
		if(this.fieldsForUniqueAddress.compareTo(HousenumberCollection.FieldsForUniqueAddress.STREET_HOUSENUMBER) == 0)
			listkey = cachename + this.getStrasse().toLowerCase() + this.getHousenumberNormalized();
		else if(this.fieldsForUniqueAddress.compareTo(HousenumberCollection.FieldsForUniqueAddress.STREET_POSTCODE_HOUSENUMBER) == 0)
			listkey = cachename + this.getStrasse().toLowerCase()  + this.getPostcode().toLowerCase() + this.getHousenumberNormalized();
		else if(this.fieldsForUniqueAddress.compareTo(HousenumberCollection.FieldsForUniqueAddress.POSTCODE_HOUSENUMBER) == 0)
			listkey = cachename + this.getPostcode().toLowerCase() + this.getHousenumberNormalized();
		return listkey;
	}


	public String getAlternateListKey() {
		String listkey = "";

		if(this.alternateFieldsForUniqueAddress == null)
			return "";

		if(		(this.alternateFieldsForUniqueAddress.compareTo(HousenumberCollection.FieldsForUniqueAddress.STREET_HOUSENUMBER) == 0)
			||	(this.alternateFieldsForUniqueAddress.compareTo(HousenumberCollection.FieldsForUniqueAddress.STREETLOCALIZED_HOUSENUMBER) == 0))
			listkey = this.getStrasse().toLowerCase() + this.getHousenumberNormalized();
		else if(this.alternateFieldsForUniqueAddress.compareTo(HousenumberCollection.FieldsForUniqueAddress.STREET_POSTCODE_HOUSENUMBER) == 0)
			listkey = this.getStrasse().toLowerCase()  + this.getPostcode().toLowerCase() + this.getHousenumberNormalized();
		else if(this.alternateFieldsForUniqueAddress.compareTo(HousenumberCollection.FieldsForUniqueAddress.POSTCODE_HOUSENUMBER) == 0)
			listkey = this.getPostcode().toLowerCase() + this.getHousenumberNormalized();
		return listkey;
	}


	// set an entry
	public void set(Housenumber entry) {
		String list_invalid_params = "";

		if(entry.hausnummer_sortierbar.equals(""))
			list_invalid_params += "|" + "hausnummer_sortierbar" + "|";
		if(entry.treffertyp == Treffertyp.UNSET)
			list_invalid_params += "|" + "treffertyp" + "|";
		if(entry.hausnummer.equals(""))
			list_invalid_params += "|" + "hausnummer" + "|";

		if( ! list_invalid_params.equals("")) {
			boolean morethanoneerror = false;
			if(list_invalid_params.indexOf("||") != -1)
				morethanoneerror = true;
			list_invalid_params = list_invalid_params.replace("||",",");
			list_invalid_params = list_invalid_params.replace("|","");
			if(morethanoneerror) {
				logger.log(Level.WARNING, "Adresseobjekt ohne gültiges addr:housenuber ::: id: "+id
						+ "   hausnummer_sortierbar: "+hausnummer_sortierbar
						+ "   treffertyp: "+treffertyp+"   hausnummer: "+hausnummer+"   osm_tag: "+osm_tag
						+ "   osm_objektart: "+osm_objektart+"   osm_id: "+osm_id
						+ "   lonlat: "+lonlat+"   lonlat_source: "+lonlat_source);
				return;
//TODO verünftig reaktivieren, ohne Programmabbruch				//throw new IllegalArgumentException("Die Parameter '" + list_invalid_params + "' haben ungültige Werte");
			} else {
				return;
//TODO verünftig reaktivieren, ohne Programmabbruch				//throw new IllegalArgumentException("Der Parameter '" + list_invalid_params + "' hat einen ungültigen Wert");
			}
		}
				
		this.id = entry.id;
		this.strasse = entry.strasse;
		this.postcode = entry.postcode;
		this.hausnummer_sortierbar = entry.hausnummer_sortierbar;
		this.setTreffertyp(entry.treffertyp);
		setHausnummer(entry.hausnummer);
		this.osm_tag = entry.osm_tag;
		this.osm_objektart = entry.osm_objektart;
		this.osm_id = entry.osm_id;
		if(this.state.equals(""))		// if state is not already set (by setstate()), then set to an invalid value
			this.state = "invalid";
		this.lonlat = entry.lonlat;
		this.lonlat_source = entry.lonlat_source;
	}

	public String toString() {

		String entryastext = "";
		
		if(this != null) {
			entryastext = "Id: "+ this.id + 
				"\tStrasse: " + this.strasse +
				"\tPostcode: " + this.postcode + 
				"\tHnr: " + this.hausnummer +
				"\tTreffertyp: " + this.getTreffertypText() +
				"\tOSM-Tag: " + this.osm_tag +
				"\tOSM-Objektart: " + this.osm_objektart +
				"\tOSM-Id: " + this.osm_id +
				"\tState: " + this.state;
		}
		return entryastext;
	}
	
	public String toStringlong() {

		String entryastext = "";
		
		if(this != null) {
			entryastext = "Id: "+ this.id + 
				"\tStrasse: " + this.strasse +
				"\tPostcode: " + this.postcode + 
				"\tHnr: " + this.hausnummer +
				"\tHnr_sortiert: " + this.hausnummer_sortierbar + 
				"\tTreffertyp: " + this.getTreffertypText() +
				"\tOSM-Tag: " + this.osm_tag +
				"\tOSM-Objektart: " + this.osm_objektart +
				"\tOSM-Id: " + this.osm_id +
				"\tLon-Lat: " + this.lonlat +
				"\tlon-Lat_Source:" + this.lonlat_source +
				"\tState: " + this.state;
		}
		return entryastext;
	}

	public Housenumber update( Housenumber in_entry) throws HausnummernException {
		if( ! this.hausnummer.equals("")) {
			setHausnummerNormalisiert(this.hausnummer);
		}

		boolean entry_changed = false;
		String changes_string = "";
		if(	(this.getHousenumberNormalized().equals(in_entry.getHousenumberNormalized()))		// check for equal treffertyp is not correct. Change of treffertyp wasn't recognized
		) {

//ToDo: if two objects with same prio exists for one housenumber, they will be used for every second evaluation,
//      that mean in evaluation 1 the object a will be used
//		in evaluation 2 the object b will be used, because the value in this.osm_id have changed
//		either prio must really improve or something more than just osm_id must be changed to switch to another object
			//System.out.println("in_entry.id ==="+in_entry.id+"===     this.id ==="+this.id+"===");
			//System.out.println("in_entry  details ==="+in_entry.toStringlong()+"===");
			//System.out.println("vs.  this details ==="+this.toStringlong()+"===");


			 // if change of treffertyp
			if(! (this.getTreffertyp().compareTo(in_entry.getTreffertyp()) == 0)) {
				String oldtreffertyp = this.getTreffertypText();
				/* production up to 2014-10-13
				if( (	(		(this.getTreffertyp().compareTo(Treffertyp.LIST_ONLY) == 0)  
						||  (this.getTreffertyp().compareTo(Treffertyp.IDENTICAL) == 0)) 
					&& 
					(		(in_entry.getTreffertyp().compareTo(Treffertyp.OSM_ONLY) == 0) 
						||	(in_entry.getTreffertyp().compareTo(Treffertyp.IDENTICAL) == 0))
				)) {
				*/
					// in_entry has absoulte priority: if it is set to identical, then set this with the in_entry values
				if( 	(in_entry.getTreffertyp().compareTo(Treffertyp.IDENTICAL) == 0)
					||	(in_entry.getTreffertyp().compareTo(Treffertyp.OSM_ONLY) == 0)
						) {
					this.setTreffertyp(in_entry.getTreffertyp());
					this.lonlat = in_entry.lonlat;
					this.lonlat_source = in_entry.lonlat_source;
					this.osm_id = in_entry.osm_id;
					this.osm_objektart = in_entry.osm_objektart;
					this.osm_tag = in_entry.osm_tag;
					this.osm_tag_prio = in_entry.osm_tag_prio;
					if(in_entry.state.equals("dbloaded") || in_entry.state.equals("changed"))
						this.state = in_entry.state;		// TODO state to use unsure
					else if(in_entry.state.equals("dbloaded")) {
						// nothing to do
					} else {
						//TODO System.out.println("in_entry.state evtl. noch übernehmen ==="+in_entry.state+"===");
					}
				} else if( in_entry.getTreffertyp().compareTo(Treffertyp.LIST_ONLY) == 0) {
					this.setTreffertyp(Treffertyp.LIST_ONLY);
					this.lonlat = in_entry.lonlat;
					this.lonlat_source = in_entry.lonlat_source;				
					this.osm_id = -1L;
					this.osm_objektart = "";
					this.osm_tag = "";
					this.osm_tag_prio = 9999;
					if(in_entry.state.equals("dbloaded") || in_entry.state.equals("changed"))
						this.state = in_entry.state;		// TODO state to use unsure
					else if(in_entry.state.equals("dbloaded")) {
						// nothing to do
					} else {
						//TODO System.out.println("in_entry.state evtl. noch übernehmen ==="+in_entry.state+"===");
					}
				} else {
					logger.log(Level.SEVERE, "ERROR ERROR: in update Workcache_Entry unexpected Treffertypes: this: "+this.getTreffertypText()+"   in_entry: "+in_entry.getTreffertypText()+"===");
				}
				changes_string += "|(0)" + "treffertyp" + " from ==="+oldtreffertyp+"=== to ==="+this.getTreffertypText()+"===" + "|";
				entry_changed = true;
			} else {

					// ok, existing entry has been checked now, its the same as in_entry
				if(		(this.getTreffertyp().compareTo(Treffertyp.OSM_ONLY) == 0) 	// ok, osm-only object
					|| 	(this.getTreffertyp().compareTo(Treffertyp.IDENTICAL) == 0)) {	// ok, identical object

						// if prio of new object is lower-value (means more important) 
						//	OR if its the same object, 
						//      then update the attributes ...
					if(		(in_entry.osm_tag_prio < this.osm_tag_prio)			// if new object is more important than existing 
						||	(in_entry.id ==  this.id)							// or its the identical object
						||	(in_entry.id == -1)) {								// or the new entry hasn't an id
							// optionaly update attribute osm_tag 
						if((in_entry.osm_tag != null) && ( ! in_entry.osm_tag.equals(""))) {
							if((this.osm_tag  != null) && ( ! this.osm_tag.equals(""))) {
								this.osm_tag_prio = in_entry.osm_tag_prio;
								if( ! this.osm_tag.equals(in_entry.osm_tag)) {
									changes_string += "|(1)" + "objektart" + " from ==="+this.osm_tag+"=== to ==="+in_entry.osm_tag+"===" + "|";
									this.osm_tag = in_entry.osm_tag;
									entry_changed = true;
								}
							} else {
								changes_string += "|(2)" + "objektart" + " from ==="+"null"+"=== to ==="+in_entry.osm_tag+"===" + "|";
								this.osm_tag = in_entry.osm_tag;
								entry_changed = true;
							}
							// else (new object (in_entry) hasn't set osm_tag anymore
						} else {
							if((this.osm_tag != null) && ( ! this.osm_tag.equals(""))){
								changes_string += "|(3)" + "objektart" + " from ==="+this.osm_tag+"=== to ==="+"null"+"===" + "|";
								this.osm_tag = null;
								entry_changed = true;
							}
						}
		
							// optionaly update attribute osm_objektart
						if((in_entry.osm_objektart != null) && ( ! in_entry.osm_objektart.equals(""))) {
							if((this.osm_objektart != null) && ( ! this.osm_objektart.equals(""))) {
								if( ! this.osm_objektart.equals(in_entry.osm_objektart)) {
									changes_string += "|" + "osm_objektart" + " from ==="+this.osm_objektart+"=== to ==="+in_entry.osm_objektart+"===" + "|";
									this.osm_objektart = in_entry.osm_objektart;
									entry_changed = true;
								}
							} else {
								changes_string += "|" + "osm_objektart" + " from ==="+"null"+"=== to ==="+in_entry.osm_objektart+"===" + "|";
								this.osm_objektart = in_entry.osm_objektart;
								entry_changed = true;
							}
						} else {
							if((this.osm_objektart != null) && ( ! this.osm_objektart.equals(""))) {
								changes_string += "|" + "osm_objektart" + " from ==="+this.osm_objektart+"=== to ==="+"null"+"===" + "|";
								this.osm_objektart = null;
								entry_changed = true;
							}
						}
						if(this.osm_id != in_entry.osm_id) {
							changes_string += "|" + "osm_id" + " from ==="+this.osm_id+"=== to ==="+in_entry.osm_id+"===" + "|";
							this.osm_id = in_entry.osm_id;
							entry_changed = true;
						}
						if(	((this.lonlat == null) || (this.lonlat.equals(""))) != ((in_entry.lonlat == null) || (in_entry.lonlat.equals("")))) {
							changes_string += "|" + "lonlat" + " from ==="+this.lonlat+"=== to ==="+in_entry.lonlat+"===" + "|";
							this.lonlat = in_entry.lonlat;
							entry_changed = true;
						}
						if(	((this.lonlat_source == null) || (this.lonlat_source.equals(""))) != ((in_entry.lonlat_source == null) || (in_entry.lonlat_source.equals("")))) {
							changes_string += "|" + "lonlat_source" + " from ==="+this.lonlat_source+"=== to ==="+in_entry.lonlat_source+"===" + "|";
							this.lonlat_source = in_entry.lonlat_source;
							entry_changed = true;
						}
					} else {
						logger.log(Level.FINEST, "no update of Entry object, because prio of new object is not better ("+in_entry.osm_tag_prio+") than existing one ("+this.osm_tag_prio+")");
					}
				}	// end of this and in_entry are both OSM objects - if(this.treffertyp.equals("o")) {
				else if(this.getTreffertyp().compareTo(Treffertyp.LIST_ONLY) == 0) {
					if(	((this.lonlat == null) || (this.lonlat.equals(""))) != ((in_entry.lonlat == null) || (in_entry.lonlat.equals("")))) {
						changes_string += "|" + "lonlat" + " from ==="+this.lonlat+"=== to ==="+in_entry.lonlat+"===" + "|";
						this.lonlat = in_entry.lonlat;
						entry_changed = true;
					}
					if(	((this.lonlat_source == null) || (this.lonlat_source.equals(""))) != ((in_entry.lonlat_source == null) || (in_entry.lonlat_source.equals("")))) {
						changes_string += "|" + "lonlat_source" + " from ==="+this.lonlat_source+"=== to ==="+in_entry.lonlat_source+"===" + "|";
						this.lonlat_source = in_entry.lonlat_source;
						entry_changed = true;
					}
				}
			}	// end of else of change treffertyp

			logger.log(Level.FINEST, "check, if state will be changed, actual state ==="+this.getstate()+"===, change-Flag is "+entry_changed);
			if(entry_changed) {
				if( ! this.getstate().equals("new")) {		// if state is new and now a second object was found, don't change to changed
					logger.log(Level.FINEST, "changing state from ==="+this.getstate()+"=== to ===changed===");
					this.setstate("changed");
				}
			} else {
				if(this.getstate().equals("dbloaded")) {
					logger.log(Level.FINEST, "changing state from ==="+this.getstate()+"=== to ===unchanged===");
					this.setstate("unchanged");
				}
			}
			
			String debugtext = this.id + "\t" + in_entry.hausnummer_sortierbar;
			debugtext += "\t" + in_entry.getTreffertypText() + "\t" + in_entry.hausnummer + "\t" + in_entry.osm_tag;
			debugtext += "\t" + in_entry.osm_objektart + "\t" + in_entry.osm_id;  
			debugtext += "\t" + in_entry.lonlat + "\t" + in_entry.lonlat_source;
			debugtext += "\t" + "update" + "\t" + "(state=" + this.getstate() + "; entry_changed: " + entry_changed + "  changes-String ===" + changes_string + "===";
			workcache_debug(debugtext);
			return this;
		} else {
			// uups, in_entry isn't identically to this-entry, so throw an error
			// as of 2014-03-22, its possible to find two way objects with same addresses (way43198767 vs. way43253396 (hnr. 30 vs. 30-31)
			logger.log(Level.SEVERE, 			"Exception,     this complete ===" + this.toStringlong() + "===");
			logger.log(Level.FINEST, "Exception, in_entry complete ===" + in_entry.toStringlong() + "===");
			throw new HausnummernException("update of Workcache-Entry couldn't be done, because existing entry was not correct one.");
		}
	}

		/**
		 * 
		 * Attention: use this method only for direct copy from get_osm_tag.
		 * 				For analyzing correct osm tags, use set_osm_tag instead
		 * 
		 * @param osmtags  string content with net osm tags. Use it only from get_osm_tag 
		 */
	public void set_osm_tag_rawvalues(Housenumber sourceentry) {
		this.osm_tag = sourceentry.osm_tag;
		this.osm_tag_prio = sourceentry.osm_tag_prio;
	}
		
	
	public void set_osm_tag(HashMap <String,String>  in_tags) {
		int START_PRIO = 99;
		
		int new_prio = START_PRIO;
		String selected_tag = "";

		this.osm_tag = "";
		this.osm_tag_prio = 9999;
		
    	for (Map.Entry<String,String> entry : in_tags.entrySet()) {
			String act_key = entry.getKey();
			String act_value = entry.getValue();
			String act_tag = "\"" + act_key + "\"=>\"" + act_value + "\"";		// former get original from DB column, now building artifically
			int act_prio = START_PRIO;

			if(act_key.equals("amenity")) {
				logger.log(Level.FINE, "key amenity  value ignoring ==="+act_value+"=== on osm-id " + this.osm_objektart + this.osm_id);
				act_prio = 20;
			}
			if(act_key.equals("building")) {
				if(act_value.equals("entrance"))
					act_prio = 1;
//TODO diverse values checken und setzen
				else if (	act_value.equals("yes") ||
							act_value.equals("office") ||
							act_value.equals("house") ||
							act_value.equals("construction") ||
							act_value.equals("apartments")) {
					act_prio = 2;
				} else {
					logger.log(Level.FINE, "other new.building value ==="+act_value+"=== on osm-id " + this.osm_objektart + this.osm_id);
					act_prio = 9;
				}
			}
			if(act_key.equals("entrance")) {
				if(		(act_value.equals("yes"))
					|| 	(act_value.equals("main"))
					|| 	(act_value.equals("home")))
					act_prio = 1;
			}
			if(act_prio < new_prio) {
				new_prio = act_prio;
				if(act_prio < START_PRIO)
					selected_tag = act_tag;
				//System.out.println("ermittelte Prio für act. object: "+new_prio+"     selected_tag ==="+selected_tag+"===");
			}
		} // end of loop over alle tags

		boolean changeState = false; 
		if(new_prio != this.osm_tag_prio) {
			changeState = true;
		}
		if(!selected_tag.equals(this.osm_tag)){
			changeState = true;
		}
		if(changeState) {
			this.osm_tag_prio = new_prio;
			this.osm_tag = selected_tag;
			String actState = this.getstate();
			if(		actState.equals("")
					||	actState.equals("dbloaded")
					||	actState.equals("unchanged")) {
				this.setstate("changed");
			}
		}
	}

	
	public String getOsmTag() {
		return this.osm_tag;
	}
	
	public int get_osm_tag_prio() {
		return this.osm_tag_prio;
	}

	public String getLonlat() {
		return this.lonlat;
		
	}
	public void setLonlat(String lonlat) {
		if((lonlat != null) && !lonlat.equals(this.lonlat)) {
			//System.out.println("in setter set_lonlat from ==="+this.lonlat+"===    to ==="+lonlat+"===    btw getstate ==="+this.getstate()+"===");
			this.lonlat = lonlat;
			String actState = this.getstate();
			if(		actState.equals("")
				||	actState.equals("dbloaded")
				||	actState.equals("unchanged")) {
				//System.out.println("in setter setstate from ==="+actState+"===    to ==="+"changed"+"===");
				this.setstate("changed");
			}
		}
	}

	public String getLonlat_source() {
		return this.lonlat_source;
		
	}

	public void setLonlat_source(String lonlat_source) {
		if((lonlat_source != null) && !lonlat_source.equals(this.lonlat_source)) {
			//System.out.println("in setter set_lonlat_source from ==="+this.lonlat_source+"===    to ==="+lonlat_source+"===   btw getstate ==="+this.getstate()+"===");
			this.lonlat_source = lonlat_source;
			String actState = this.getstate();
			if(		actState.equals("")
					||	actState.equals("dbloaded")
					||	actState.equals("unchanged")) {
				//System.out.println("in setter setstate from ==="+actState+"===    to ==="+"changed"+"===");
				this.setstate("changed");
			}
		}
		
	}
	
	public String getHousenumberComment() {
		return this.hausnummerkommentar;
		
	}

	public void setHousenumberComment(String comment) {
		this.hausnummerkommentar = comment;
	}


	/**
	 * @param create sortable version of housenumber and store in entry
	 */
	private void setHausnummerNormalisiert(String hausnummer) {
		if( ! hausnummer.equals("")) {
			String hausnummersortierbar = "";
			int numstellen = 0;
			for(int posi=0;posi<hausnummer.length();posi++) {
				int charwert = hausnummer.charAt(posi);
				//System.out.println("aktuelle Textstelle ==="+in_hausnummer.charAt(posi)+"===  char-nr.: "+charwert);
				if( (charwert >= '0') && (charwert <= '9'))
					numstellen++;
				else
					break;
			}
			for(int anzi=0;anzi<(4-numstellen);anzi++)
				hausnummersortierbar += "0";
			hausnummersortierbar += hausnummer;
			
			if(isHousenumberaddition_exactly) {
				this.hausnummer_normalisiert = hausnummersortierbar;
			} else {
				this.hausnummer_normalisiert = hausnummersortierbar.toLowerCase().replace(" ",  "");
			}

			this.hausnummer_sortierbar = this.hausnummer_normalisiert;
			//System.out.println("in .normalize of class Entry: set .hausnummer_sortierbar to ===" + this.hausnummer_sortierbar +"===");
		}
	}
	
	/**
	 * @param hausnummer the housenumber to set
	 */
	public void setHausnummer(String hausnummer) {
		this.hausnummer = hausnummer;
		setHausnummerNormalisiert(hausnummer);
	}

	/**
	 * @return the hausnummer
	 */
	public String getHausnummer() {
		return hausnummer;
	}

	/**
	 * @return the hausnummer
	 */
	public String getHousenumberNormalized() {
		return hausnummer_normalisiert;
	}

	/**
	 * @return the hausnummer
	 */
	public String getHausnummerSortierbar() {
		return hausnummer_sortierbar;
	}


	/**
	 * @return the strasse
	 */
	public String getStrasse() {
		return strasse;
	}

	/**
	 * @return the postcode
	 */
	public String getPostcode() {
		return postcode;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the internal id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param treffertyp the treffertyp to set. It means, if the Hit is list only, identical beteween list and osm or OSM only.
	 */
	public void setTreffertyp(Treffertyp treffertyp) {
		this.treffertyp = treffertyp;
	}

	/**
	 * @return the job_id
	 */
	public Treffertyp getTreffertyp() {
		return treffertyp;
	}

		
	/**
	 * @return the job_id
	 */
	public String getTreffertypText() {
		if(treffertyp == Treffertyp.IDENTICAL)
			return "i";
		else if(treffertyp == Treffertyp.LIST_ONLY)
			return "l";
		else if(treffertyp == Treffertyp.OSM_ONLY)
			return "o";
		else
			return "";
	}

	/**
	 * @param treffertyp the treffertyp to set. Only "l" or "s" for list only,, "i" for identical, "o" for osm and "" for unset allowed
	 */
	public void setTreffertyp(String treffertyp) {
		if(treffertyp.equals(""))
			this.treffertyp = Treffertyp.UNSET;
		else if(treffertyp.equals("l") || treffertyp.equals("s"))
			this.treffertyp = Treffertyp.LIST_ONLY;
		else if(treffertyp.equals("o"))
			this.treffertyp = Treffertyp.OSM_ONLY;
		else if(treffertyp.equals("i"))
			this.treffertyp = Treffertyp.IDENTICAL;
		else {
			logger.log(Level.SEVERE, "ERROR ERROR: in Workcache_Entry.setTreffertyp(String) non allowed string value ==="+treffertyp+"===");
		}
	}
	
	/**
	 * @param treffertyp the treffertyp to set. It means, if the Hit is list only, identical beteween list and osm or OSM only.
	 */
	public void setOSMObjekt(String osm_objektart, Long osm_id) {
		this.osm_objektart= osm_objektart;
		this.osm_id = osm_id;
	}
		
	/**
	 * @return the OSM Objektart
	 */
	public String getOsmObjektart() {
		return osm_objektart;
	}

	/**
	 * @return the osm id
	 */
	public long getOsmId() {
		return osm_id;
	}

	/**
	 * @param strasse the strasse to set
	 */
	public void setStrasse(String strasse) {
		this.strasse = strasse;
	}

	/**
	 * @param postcode the postcode to set
	 */
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}
	


	public void setstate( String newstate) {
		if(		newstate.equals("new") ||				// entry is really new
				newstate.equals("changed") ||			// entry has been changed
				newstate.equals("deleted") ||			// active set to delete. Should be seldom. More often, a row will be not re-set aber set to dbloaded for being invalide
				newstate.equals("dbloaded") ||			// loaded from db. If later not set to other state, then the entry is no longer valid
				newstate.equals("unchanged")			// entry hasn't been changed, but acknowledged
			) {
			this.state = newstate;
		} else {
			throw new IllegalArgumentException("Invalid Workcache-Entry state '" + newstate + "'");
		}
	}

	public String getstate() {
		return this.state;
	}
	
}

