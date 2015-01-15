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


		Changes to Database: in procedure public void store_to_db(), records will be insert, updated or delete in table auswertung_hausnummern
*/

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;





public class HousenumberCollection {
	TreeMap<String,Housenumber> cache = new TreeMap<String,Housenumber>(); 
	public boolean debug = false;
	public int cache_count = 0;
	private boolean housenumberadditionCaseSentity = true;
	String land = "";
	String stadt = "";
	String jobname = "";
	
	
	private void workcache_debug(String outputtext) {
		PrintWriter debug_output = null;
		String workcache_debugfile = "workcache_debug.txt";

		if(!debug) {
			return;
		} else {
			try {
				debug_output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(workcache_debugfile, true),"UTF-8")));
				debug_output.println(outputtext);
				System.out.println("***" + outputtext);
				debug_output.close();
			} catch (IOException ioerror) {
				System.out.println("ERROR: couldn't open file to write, filename was ==="+workcache_debugfile+"===");
				ioerror.printStackTrace();
			}
		}
	}

	/**
	 * @return the housenumberadditionCaseSentity
	 */
	public boolean ishousenumberadditionCaseSentity() {
		return housenumberadditionCaseSentity;
	}

	/**
	 * @param housenumberadditionCaseSentity the housenumberadditionCaseSentity to set
	 */
	public void setHousenumberadditionCaseSentity(boolean housenumberadditionCaseSentity) {
		this.housenumberadditionCaseSentity = housenumberadditionCaseSentity;
	}

	public int length() {
		return cache_count;
	}

	public void clear() {
		cache.clear();
		cache_count = 0;
	}
	
	public int count_unchanged() {
		int count = 0;
    	for (Map.Entry<String,Housenumber> entry : cache.entrySet()) {
			String key = entry.getKey();
			Housenumber housenumber = entry.getValue();
			if(housenumber.getstate().equals("unchanged"))
				count++;
		}
		return count;
	}

	public int countTreffertyp(Housenumber.Treffertyp treffertyp) {
		int count = 0;
    	for (Map.Entry<String,Housenumber> entry : cache.entrySet()) {
			String key = entry.getKey();
			Housenumber housenumber = entry.getValue();
			if(housenumber.getTreffertyp() == treffertyp)
				count++;
		}
		return count;
	}


	// set an entry, which is from database table, so not a real new entry
	public Housenumber add_dbentry(Housenumber entry) {
		Housenumber newentry = new Housenumber(this.ishousenumberadditionCaseSentity());
		newentry.set(entry);
		newentry.setstate("dbloaded");
		newentry.toStringlong();
		cache.put(entry.getListKey(), entry);
		cache_count++;
		return newentry;
	}

		// add a real new entry
	public void add_newentry( Housenumber in_newentry) {
		Housenumber newentry = new Housenumber(this.ishousenumberadditionCaseSentity());
		newentry.set(in_newentry);
		newentry.setstate("new");
		newentry.toStringlong();
		cache.put(in_newentry.getListKey(), in_newentry);
		cache_count++;
	}

	
	private Housenumber find_entry_in_cache(Housenumber findentry) {

		if(cache.get(findentry.getListKey()) != null)
			return cache.get(findentry.getListKey());
		else
			return null;
	}
	
	/**
	 * search the findentry housenumber object, if it is known in Workcache and optionaly return the found object.
	 * The only one optionaly attribute in findentry can be treffertyp (set "" to ignore)
	 * @param findentry: housenumber object, which will be searched in structure, if available
	 * @return:			 the found housenumber object (only one) or null
	 */
	public  Housenumber ___WILL_NOT_BE_USED____get_entry_in_cache(Housenumber findentry) {	// public version of find_entry, but slightly different works
		if(cache.get(findentry.getListKey()) != null)
			return cache.get(findentry.getListKey());
		else
			return null;

/*		for(int entryindex = 0; entryindex < this.length(); entryindex++) {
			if(		(this.cache[entryindex].getStrasse() == findentry.getStrasse()) 
				&&	(this.cache[entryindex].getHausnummerNormalisiert().equals(findentry.getHausnummerNormalisiert()))
				&&	((findentry.getTreffertyp() == Workcache_Entry.Treffertyp.UNSET) || (this.cache[entryindex].getTreffertyp() == findentry.getTreffertyp()))	// treffertyp optional if set
				) {
				return this.cache[entryindex];
			}
		}
		return null;
*/
	}

	
	/**
	 * get the cacheentry object with the given index
	 * @param index: index of the cacheentry
	 * @return:			 the found housenumber object (only one) or null
	 */
	public Housenumber entry(String listkey) {	// public version of find_entry, but slightly different works
		if(cache.get(listkey) != null)
			return cache.get(listkey);
		else
			return null;
	}
	

	public HousenumberCollection merge(HousenumberCollection osmhousenumbers) {
		HousenumberCollection mergedhousenumbers = new HousenumberCollection();

		System.out.println("at start of merge ...");
		System.out.println("   count list housenumbers: " + cache.size());
		System.out.println("   count osm housenumbers: " + osmhousenumbers.cache.size());
		int count = 0;
		for (Map.Entry<String,Housenumber> entry : cache.entrySet()) {
			String thiskey = entry.getKey();
			Housenumber listhousenumber = entry.getValue();
			Housenumber newhousenumber = listhousenumber;
			if(osmhousenumbers.cache.containsKey(thiskey)) {
				Housenumber foundosmhousenumber = osmhousenumbers.cache.get(thiskey);
//				private long 	id = -1L;
				newhousenumber.setTreffertyp(Housenumber.Treffertyp.IDENTICAL);
				newhousenumber.set_osm_tag_rawvalues(foundosmhousenumber);
				newhousenumber.setOSMObjekt(foundosmhousenumber.getOsmObjektart(), foundosmhousenumber.getOsmId());
				newhousenumber.setLonlat(foundosmhousenumber.getLonlat());
				newhousenumber.setLonlat_source(foundosmhousenumber.getLonlat_source());
				mergedhousenumbers.add_newentry(newhousenumber);
			} else {
				mergedhousenumbers.add_newentry(listhousenumber);
			}
		}
			// ok, now all single list housenumbers and identical housenumbers are in mergedhousenumbers
			
			// now check, which housenumber are osm only housenumbers and add them
    	for (Map.Entry<String,Housenumber> entry : osmhousenumbers.cache.entrySet()) {
			String thiskey = entry.getKey();
			Housenumber osmhousenumber = entry.getValue();
			if(! mergedhousenumbers.cache.containsKey(thiskey)) {
				mergedhousenumbers.add_newentry(osmhousenumber);
			}
		}
		System.out.println("at end of merge ...");
		System.out.println("   count merged housenumbers: " + mergedhousenumbers.cache.size());
    	return mergedhousenumbers;
	}
	
	
		// update in this cache
	public void update(Housenumber updateentry) {
		boolean debug_output = true;
		if(debug_output) System.out.println("start of .update of class Workcache ...");

		if(debug_output) System.out.println("in .update of class Workcache: find entry ...");
		Housenumber found_entry = find_entry_in_cache(updateentry);
		if(debug_output) {  
			if(found_entry == null)
				System.out.println("found? no");
			else
				System.out.println("found? " + found_entry.toString());
		}
		if(found_entry != null) {
			try {
				if(debug_output) System.out.println("in .update of class Workcache: before call .update of class Entry ...");
				found_entry.update(updateentry);
				if(debug_output) System.out.println("in .update of class Workcache: after call .update of class Entry ...");
			} catch (HausnummernException e) {
				System.out.println("Böser Fehler in Workcache, Fkt. update: offenbar brachte find_entry_in_cache einen entry zurück, der nicht passte, bitte prüfen");
				e.printStackTrace();
				return;
			}
		} else {
			if(debug_output) System.out.println("in .update of class Workcache: before call add_newentry ...");
			add_newentry(updateentry);
			if(debug_output) System.out.println("in .update of class Workcache: after call add_newentry ...");
		}
		if(debug_output) System.out.println("end of .update of class Workcache ...");
	}

	
	/** 
	 * 
	 * @param fieldseparator one or more characters to separate the fields from each other. Defaults to Tabulator character
	 * @param includeHeaderline if set to true, a header will be place in first line
	 *
	 */
	public String toString(String fieldseparator, boolean includeHeaderline) {
		if(fieldseparator.equals(""))
			fieldseparator = "\t";

		StringBuffer outputbuffer = new StringBuffer();

		String actrecord = "";

		if(includeHeaderline) {
			actrecord = "#Strasse" + fieldseparator
					+	"Hausnummer" + fieldseparator
					+	"Treffertyp" + fieldseparator
					+	"OSMId" + fieldseparator
					+	"OSMTyp" + fieldseparator
					+	"OSMTag" + fieldseparator
					+	"OSMTagPrio" + fieldseparator
					+	"LonLat" + fieldseparator;
			outputbuffer.append(actrecord + "\n");
		}
		
		for (Map.Entry<String,Housenumber> entry : cache.entrySet()) {
			String thiskey = entry.getKey();
			Housenumber housenumber = entry.getValue();

			actrecord = housenumber.getStrasse() + fieldseparator
				+	housenumber.getHausnummer() + fieldseparator
				+	housenumber.getTreffertypText() + fieldseparator
				+	housenumber.getOsmId() + fieldseparator
				+	housenumber.getOsmObjektart() + fieldseparator
				+	housenumber.getOsmTag() + fieldseparator
				+	housenumber.get_osm_tag_prio() + fieldseparator
				+	housenumber.getLonlat() + fieldseparator;
			outputbuffer.append(actrecord + "\n");
		}
		
		return outputbuffer.toString();
	}
	


	public void printhtml(String filename) {

		System.out.println("at start of merge ...");
		System.out.println("   count housenumbers: " + cache.size());

		StringBuffer outputbuffer = new StringBuffer();
		
		

		String laststreetname = "";
		String actstreetname = "";
		String actoutputline = "";

		Integer count_total_identical = 0;
		Integer count_total_listonly = 0;
		Integer count_total_osmonly = 0;
		Integer count_identical = 0;
		Integer count_listonly = 0;
		Integer count_osmonly = 0;
		String list_identical = "";
		String list_listonly = "";
		String list_osmonly = "";

		outputbuffer.append("<html>\n");
		outputbuffer.append("	<head>\n");
		outputbuffer.append("		<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n");
		outputbuffer.append("		<link rel='stylesheet' href='http://regio-osm.de/hausnummerauswertung/css/normalize.css'>\n");
		outputbuffer.append("		<link href='http://fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet' type='text/css'>\n");
		outputbuffer.append("		<link rel='stylesheet' href='http://regio-osm.de/hausnummerauswertung/css/main.css'>\n");
		outputbuffer.append("		<link rel='stylesheet' type='text/css' href='http://regio-osm.de/hausnummerauswertung/css/hausnummern.css'>\n");
		outputbuffer.append("		<script type='text/javascript'>\n");
		outputbuffer.append("		function setcookie() {\n");
		outputbuffer.append("			var lang = document.getElementById('language').value;\n");
		outputbuffer.append("			//TODO\n");
		outputbuffer.append("			document.cookie = 'language='+lang+'; expires=Fri, 3 Aug 2019 20:47:11 UTC; path=/hausnummerauswertung';\n");
		outputbuffer.append("			window.location.reload();\n");
		outputbuffer.append("		}\n");
		outputbuffer.append("		function readCookie(name) {\n");
		outputbuffer.append("		    var nameEQ = name + '=';\n");
		outputbuffer.append("		    var ca = document.cookie.split(';');\n");
		outputbuffer.append("		    for(var i=0;i < ca.length;i++) {\n");
		outputbuffer.append("		        var c = ca[i];\n");
		outputbuffer.append("		        while (c.charAt(0)==' ') c = c.substring(1,c.length);\n");
		outputbuffer.append("		        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);\n");
		outputbuffer.append("		    }\n");
		outputbuffer.append("		    return null;\n");
		outputbuffer.append("		}\n");
		outputbuffer.append("		function showactivelanguage() {\n");
		outputbuffer.append("			var lang = readCookie('language');\n");
		outputbuffer.append("			var languageobject = document.getElementById('language');\n");
		outputbuffer.append("			languageobject.value = lang;\n");
		outputbuffer.append("		}\n");
		outputbuffer.append("		</script>\n");
		outputbuffer.append("		\n");
		outputbuffer.append("	<link rel='stylesheet' type='text/css' href='http://regio-osm.de/hausnummerauswertung/css/tablesorter.css'>\n");
		outputbuffer.append("	<script type='text/javascript' src='http://regio-osm.de/hausnummerauswertung/js/jquery-1.10.2.min.js'></script>\n");
		outputbuffer.append("	<script type='text/javascript' src='http://regio-osm.de/hausnummerauswertung/js/jquery.tablesorter.min.js'></script>\n");
		outputbuffer.append("	<script type='text/javascript' src='http://regio-osm.de/hausnummerauswertung/js/popup.js'></script>\n");
		outputbuffer.append("	<script type='text/javascript'>\n");
		outputbuffer.append("	function getTextExtractor() {\n");
		outputbuffer.append("		  return (function() {\n");
		outputbuffer.append("				var patternLetters = /[öäüÖÄÜáàâéèêúùûóòôÁÀÂÉÈÊÚÙÛÓÒÔßÐðÍíÝýÞþÆæĆĘŁŃÓŚŹŻćęłńóśźż]/g;\n");
		outputbuffer.append("				var patternDateDmy = /^(?:\\D+)?(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2,4})$/;\n");
		outputbuffer.append("				var lookupLetters = {\n");
		outputbuffer.append("					'ä': 'a', 'ö': 'o', 'ü': 'u',\n");
		outputbuffer.append("					'Ä': 'A', 'Ö': 'O', 'Ü': 'U',\n");
		outputbuffer.append("					'á': 'a', 'à': 'a', 'â': 'a',\n");
		outputbuffer.append("					'é': 'e', 'è': 'e', 'ê': 'e',\n");
		outputbuffer.append("					'ú': 'u', 'ù': 'u', 'û': 'u',\n");
		outputbuffer.append("					'ó': 'o', 'ò': 'o', 'ô': 'o',\n");
		outputbuffer.append("					'Á': 'A', 'À': 'A', 'Â': 'A',\n");
		outputbuffer.append("					'É': 'E', 'È': 'E', 'Ê': 'E',\n");
		outputbuffer.append("					'Ú': 'U', 'Ù': 'U', 'Û': 'U',\n");
		outputbuffer.append("					'Ó': 'O', 'Ò': 'O', 'Ô': 'O',\n");
		outputbuffer.append("					'ß': 's',\n");
		outputbuffer.append("					'Ð': 'DZZ', 'ð': 'dzz', 			// iceland chars\n");
		outputbuffer.append("					'Í': 'IZZ', 'í': 'izz', 			// iceland chars\n");
		outputbuffer.append("					'Ý': 'YZZ', 'ý': 'yzz', 			// iceland chars\n");
		outputbuffer.append("					'Þ': 'ZZZ1', 'þ': 'zzz1',			// iceland chars\n");
		outputbuffer.append("					'Æ': 'ZZZ2', 'æ': 'zzz2',			// iceland chars\n");
		outputbuffer.append("					'Ą': 'AZZ', 'ą': 'azz',				// poland chars\n");
		outputbuffer.append("					'Ć': 'CZZ', 'ć': 'czz',				// poland chars\n");
		outputbuffer.append("					'Ę': 'EZZ',	'ę': 'ezz',				// poland chars\n");
		outputbuffer.append("					'Ł': 'LZZ',	'ł': 'lzz',				// poland chars\n");
		outputbuffer.append("					'Ń': 'NZZ',	'ń': 'nzz',				// poland chars\n");
		outputbuffer.append("					'Ó': 'OZZ',	'ó': 'ozz',				// poland chars\n");
		outputbuffer.append("					'Ś': 'SZZ',	'ś': 'szz',				// poland chars\n");
		outputbuffer.append("					'Ź': 'ZZZ1', 'ź': 'zzz1',			// poland chars\n");
		outputbuffer.append("					'Ż': 'ZZZ2', 'ż': 'zzz2'			// poland chars\n");
		outputbuffer.append("				};\n");
		outputbuffer.append("				var letterTranslator = function(match) { \n");
		outputbuffer.append("					return lookupLetters[match] || match;\n");
		outputbuffer.append("				}\n");
		outputbuffer.append("	\n");
		outputbuffer.append("		    return function(node) {\n");
		outputbuffer.append("		      var text = $.trim($(node).text());\n");
		outputbuffer.append("		      var date = text.match(patternDateDmy);\n");
		outputbuffer.append("		      if (date)\n");
		outputbuffer.append("		        return [date[3], date[2], date[1]].join('-');\n");
		outputbuffer.append("		      else\n");
		outputbuffer.append("		        return text.replace(patternLetters, letterTranslator);\n");
		outputbuffer.append("		    }\n");
		outputbuffer.append("		  })();\n");
		outputbuffer.append("		}\n");
		outputbuffer.append("		var josmPopup = new Popup();\n");
		outputbuffer.append("		function openJOSM(c, a, b) {													//source: http://tools.geofabrik.de/osmi/\n");
		outputbuffer.append("			var link = createJOSMLink(c, a, b);\n");
		outputbuffer.append("			return link;\n");
		outputbuffer.append("		}\n");
		outputbuffer.append("\n");
		outputbuffer.append("		function createJOSMLink(area,osmobjecttype,osmobjectid) {		//source: http://tools.geofabrik.de/osmi/\n");
		outputbuffer.append("			var h = area.split(' ');\n");
		outputbuffer.append("			var c = parseFloat(h[2]) - parseFloat(h[0]);\n");
		outputbuffer.append("			var a = parseFloat(h[3]) - parseFloat(h[1]);\n");
		outputbuffer.append("			var f = 'http://localhost:8111/load_and_zoom?left=' + h[0] + '&bottom=' + h[1] + '&right=' + h[2] + '&top=' + h[3];\n");
		outputbuffer.append("			if(osmobjecttype && osmobjectid) {\n");
		outputbuffer.append("				f += '&select=' + osmobjecttype + osmobjectid;\n");
		outputbuffer.append("			}\n");
		outputbuffer.append("			return f;\n");
		outputbuffer.append("		}\n");
		outputbuffer.append("		function EnhancedPopup(divobjectid, relatedobjectid, directiondivtorelatedobject, propertiesforpopup, lon, lat) {\n");
		outputbuffer.append("			var osmobjecttyp;\n");
		outputbuffer.append("			var osmobjectid;\n");
		outputbuffer.append("			if(relatedobjectid) {\n");
		outputbuffer.append("				var relatedobjectid_parts = relatedobjectid.split('_');\n");
		outputbuffer.append("				osmobjecttyp = relatedobjectid_parts[0];\n");
		outputbuffer.append("				osmobjectid = relatedobjectid_parts[1];\n");
		outputbuffer.append("			}\n");
		outputbuffer.append("			var area = (parseFloat(lon) - 0.001) + ' ' + (parseFloat(lat) - 0.001) + ' ' + (parseFloat(lon) + 0.001) + ' ' + (parseFloat(lat) + 0.001);\n");
		outputbuffer.append("			var relatedobject = document.getElementById(relatedobjectid);\n");
		outputbuffer.append("			if(lon == -1)\n");
		outputbuffer.append("				josmPopup.content = '-sorry, no geocoordinate available-';\n");
		outputbuffer.append("			else\n");
		outputbuffer.append("				josmPopup.content = 'Hno: '+relatedobject.innerHTML + ': edit in <a target=\"josmiframe\" href=\"'+openJOSM(area,osmobjecttyp, osmobjectid)+'\">Josm</a>';\n");
		outputbuffer.append("			josmPopup.reference = relatedobjectid;\n");
		outputbuffer.append("			josmPopup.style = {'border':'3px solid black','backgroundColor':'white'};\n");
		outputbuffer.append("			josmPopup.width = 300;\n");
		outputbuffer.append("			josmPopup.height = 150;\n");
		outputbuffer.append("			josmPopup.position = 'above center';\n");
		outputbuffer.append("			josmPopup.show(divobjectid, relatedobjectid, directiondivtorelatedobject, propertiesforpopup);\n");
		outputbuffer.append("		}\n");
		outputbuffer.append("	</script>\n");
		outputbuffer.append("\n");
		outputbuffer.append("	</head>\n");
		outputbuffer.append("<body>\n");
		outputbuffer.append("<div id='popup_notexist' style='background-color:white;border:2px solid black;display:none;width:400px;height:200px;'>popup text</div>\n");
		outputbuffer.append("<table id='auswertetabelle' class='tablesorter' border='all'>\n");
		outputbuffer.append("<thead>\n"); 
		outputbuffer.append("<tr>\n");
		outputbuffer.append("<th>Straße</th>\n");
		outputbuffer.append("<th>% erfüllt</th>\n");
		outputbuffer.append("<th>Anz. Liste</th>\n");
		outputbuffer.append("<th>Anz. identisch</th>\n");
		outputbuffer.append("<th>Anz. in OSM fehlend</th>\n");
		outputbuffer.append("<th>Anz. nur in OSM</th>\n");
		outputbuffer.append("<th>Liste Hausnummern identisch</th>\n");
		outputbuffer.append("<th>Liste Hausnummern in OSM fehlend</th>\n");
		outputbuffer.append("<th>Liste Hausnummern nur in OSM</th>\n");
		outputbuffer.append("</tr>\n");
		outputbuffer.append("</thead>\n");
		outputbuffer.append("<tbody>\n");
		outputbuffer.append("<script type='text/javascript'>\n");
		outputbuffer.append("var myTextExtraction = function(node)\n");
		outputbuffer.append("{\n");
		outputbuffer.append("var inhalt = node.childNodes[0].childNodes[0].innerHTML;\n");
		outputbuffer.append("return inhalt;\n");
		outputbuffer.append("}\n");
		outputbuffer.append("$(document).ready(function()\n");
		outputbuffer.append("{\n");
		outputbuffer.append("$('#auswertetabelle').tablesorter(  {textExtraction: getTextExtractor()});\n"); 
		outputbuffer.append("});\n");
		outputbuffer.append("</script>\n");
	
		
		
		for (Map.Entry<String,Housenumber> entry : cache.entrySet()) {
			String thiskey = entry.getKey();
			Housenumber housenumber = entry.getValue();

			laststreetname = actstreetname;
			actstreetname = housenumber.getStrasse();
			if(!laststreetname.equals("")
				&& ! laststreetname.equals(actstreetname)) {
				// output all informations about last streetname
				actoutputline += "<tr>";
				actoutputline += "<td>" + laststreetname + "</td>";
				if((count_identical + count_listonly) != 0)
					actoutputline += "<td>" + 100 * count_identical / (count_identical + count_listonly) + "</td>";
				else
					actoutputline += "<td>-</td>";
				actoutputline += "<td>" + (count_identical + count_listonly) + "</td>";
				actoutputline += "<td>" + count_identical + "</td>";
				actoutputline += "<td>" + count_listonly + "</td>";
				actoutputline += "<td>" + count_osmonly + "</td>";
				actoutputline += "<td>" + list_identical + "</td>";
				actoutputline += "<td>" + list_listonly + "</td>";
				actoutputline += "<td>" + list_osmonly + "</td>";
				actoutputline += "</tr>\n";

				count_total_identical += count_identical;
				count_total_listonly += count_listonly;
				count_total_osmonly += count_osmonly;
				
				outputbuffer.append(actoutputline);
				actoutputline = "";

					// reset all variable for next street
				count_listonly = 0;
				count_osmonly = 0;
				count_identical = 0;
				list_identical = "";
				list_listonly = "";
				list_osmonly = "";
			}

			if(housenumber.getTreffertyp() == Housenumber.Treffertyp.IDENTICAL) {
				count_identical++;
				if(! list_identical.equals(""))
					list_identical += " ";
				if(housenumber.getOsmId() != -1L) {
					String lonlat = housenumber.getLonlat();
					if(! lonlat.equals("")) {
						String housenumber_lon = lonlat.split(" ")[0];
						String housenumber_lat = lonlat.split(" ")[1];
						list_identical += "<a target='osmwindows' href='http://www.openstreetmap.org/?" + housenumber.getOsmObjektart() + "=" + housenumber.getOsmId() + "'"
							+ " id='" + housenumber.getOsmObjektart() + "_" + housenumber.getOsmId() + "'"
							+ " onmouseover=\"EnhancedPopup(\'popup_notexist\',\'" + housenumber.getOsmObjektart() + "_" + housenumber.getOsmId()
							+ "\',\'above center\',{\'constrainToScreen\':true}, " + housenumber_lon + "," + housenumber_lat + ");return false;\">";
					} else {
						list_identical += "<a target='osmwindows' href='http://www.openstreetmap.org/?" + housenumber.getOsmObjektart() + "=" + housenumber.getOsmId() + "'>";
					}
				}
				list_identical += housenumber.getHausnummer();
				if(housenumber.getOsmId() != -1L) {
					list_identical += "</a>";
				}
			} else if(housenumber.getTreffertyp() == Housenumber.Treffertyp.LIST_ONLY) {
				count_listonly++;
				if(! list_listonly.equals(""))
					list_listonly += " ";
				list_listonly += housenumber.getHausnummer();
			} else if(housenumber.getTreffertyp() == Housenumber.Treffertyp.OSM_ONLY) {
				count_osmonly++;
				if(! list_osmonly.equals(""))
					list_osmonly += " ";
				if(housenumber.getOsmId() != -1L) {
					String lonlat = housenumber.getLonlat();
					if(! lonlat.equals("")) {
						String housenumber_lon = lonlat.split(" ")[0];
						String housenumber_lat = lonlat.split(" ")[1];
						list_osmonly += "<a target='osmwindows' href='http://www.openstreetmap.org/?" + housenumber.getOsmObjektart() + "=" + housenumber.getOsmId() + "'"
							+ " id='" + housenumber.getOsmObjektart() + "_" + housenumber.getOsmId() + "'"
							+ " onmouseover=\"EnhancedPopup(\'popup_notexist\',\'" + housenumber.getOsmObjektart() + "_" + housenumber.getOsmId()
							+ "\',\'above center\',{\'constrainToScreen\':true}, " + housenumber_lon + "," + housenumber_lat + ");return false;\">";
					} else {
						list_osmonly += "<a target='osmwindows' href='http://www.openstreetmap.org/?" + housenumber.getOsmObjektart() + "=" + housenumber.getOsmId() + "'>";
					}
				}
				list_osmonly += housenumber.getHausnummer();
				if(housenumber.getOsmId() != -1L) {
					list_osmonly += "</a>";
				}
			}
		}
		if(!actstreetname.equals("")) {
				// output all informations about last streetname
			actoutputline = "";
			actoutputline += "<tr>";
			actoutputline += "<td>" + actstreetname + "</td>";
			if((count_identical + count_listonly) != 0)
				actoutputline += "<td>" + 100 * count_identical / (count_identical + count_listonly) + "</td>";
			else
				actoutputline += "<td>-</td>";
			actoutputline += "<td>" + (count_identical + count_listonly) + "</td>";
			actoutputline += "<td>" + count_identical + "</td>";
			actoutputline += "<td>" + count_listonly + "</td>";
			actoutputline += "<td>" + count_osmonly + "</td>";
			actoutputline += "<td>" + list_identical + "</td>";
			actoutputline += "<td>" + list_listonly + "</td>";
			actoutputline += "<td>" + list_osmonly + "</td>";
			actoutputline += "</tr>\n";
			actoutputline += "</tbody>\n";

			count_total_identical += count_identical;
			count_total_listonly += count_listonly;
			count_total_osmonly += count_osmonly;

			outputbuffer.append(actoutputline);
		}

		actoutputline = "";
		actoutputline += "<tfoot>";
		actoutputline += "<tr>";
		actoutputline += "<td>Summe</td>";
		if((count_total_identical + count_total_listonly) != 0)
			actoutputline += "<td>" + 100 * count_total_identical / (count_total_identical + count_total_listonly) + "</td>";
		else
			actoutputline += "<td>-</td>";
		actoutputline += "<td>" + (count_total_identical + count_total_listonly) + "</td>";
		actoutputline += "<td>" + count_total_identical + "</td>";
		actoutputline += "<td>" + count_total_listonly + "</td>";
		actoutputline += "<td>" + count_total_osmonly + "</td>";
		actoutputline += "<td>&nbsp;</td>";
		actoutputline += "<td>&nbsp;</td>";
		actoutputline += "<td>&nbsp;</td>";
		actoutputline += "</tr>\n";
		outputbuffer.append(actoutputline);
		outputbuffer.append("</tfoot>\n");
		outputbuffer.append("</table>\n");

		outputbuffer.append("<iframe id='josmiframe' name='josmiframe' style='display:none;'></iframe>\n");
		outputbuffer.append("</body>\n");
		outputbuffer.append("</html>\n");
		
		try {
		    PrintWriter osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename),StandardCharsets.UTF_8)));
			osmOutput.println(outputbuffer.toString());
			osmOutput.close();
		} 
		catch (IOException ioerror) {
			System.out.println("ERROR: IO-Exception during parsing of xml-content");
			ioerror.printStackTrace();
			return;
		}
	}
	

}

