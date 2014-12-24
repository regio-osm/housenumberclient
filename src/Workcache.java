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
import java.util.HashMap;




public class Workcache {
	Workcache_Entry cache[] = new Workcache_Entry[500000];
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
		for(int cacheentries = 0; cacheentries < this.length(); cacheentries++) {
			cache[cacheentries] = null;
		}
		cache_count = 0;
	}
	
	public int count_unchanged() {
		int count = 0;
		for(int entryindex = 0; entryindex < cache_count; entryindex++) {
			if(cache[entryindex].getstate().equals("unchanged"))
				count++;
		}
		return count;
	}

	public int countTreffertyp(Workcache_Entry.Treffertyp treffertyp) {
		int count = 0;
		for(int entryindex = 0; entryindex < cache_count; entryindex++) {
			if(cache[entryindex].getTreffertyp() == treffertyp)
				count++;
		}
		return count;
	}


	// set an entry, which is from database table, so not a real new entry
	public Workcache_Entry add_dbentry(Workcache_Entry entry) {
		Workcache_Entry newentry = new Workcache_Entry(this.ishousenumberadditionCaseSentity());
		newentry.set(entry);
		newentry.setstate("dbloaded");
		newentry.toStringlong();
		cache[cache_count++] = newentry;
		
		return newentry;
	}

		// add a real new entry
	public void add_newentry( Workcache_Entry in_newentry) {
		Workcache_Entry newentry = new Workcache_Entry(this.ishousenumberadditionCaseSentity());
		newentry.set(in_newentry);
		newentry.setstate("new");
		newentry.toStringlong();
		cache[cache_count++] = newentry;
	}

	
	private int find_entry_in_cache(Workcache_Entry findentry) {

		for(int entryindex = 0; entryindex < cache_count; entryindex++) {
			if(cache[entryindex].getStrasse() == findentry.getStrasse()) {
				if(		this.ishousenumberadditionCaseSentity()
					&& cache[entryindex].getHausnummer().equals(findentry.getHausnummer())) {
					return entryindex;
				} else if(!this.ishousenumberadditionCaseSentity()) {
					if(cache[entryindex].getHausnummer().toLowerCase().equals(findentry.getHausnummer().toLowerCase())) {
						if(!cache[entryindex].getHausnummer().equals(findentry.getHausnummer())) {
							System.out.println("Hausnummer nur identisch, weil grosskleinirrelevant findentry.getHausnummer()===" + findentry.getHausnummer() + "===  cache[entryindex].getHausnummer() ===" + cache[entryindex].getHausnummer() + "===");
						}
						return entryindex;
					}
				}
			}
		}
		return -1;
	}
	
	/**
	 * search the findentry housenumber object, if it is known in Workcache and optionaly return the found object.
	 * The only one optionaly attribute in findentry can be treffertyp (set "" to ignore)
	 * @param findentry: housenumber object, which will be searched in structure, if available
	 * @return:			 the found housenumber object (only one) or null
	 */
	public  Workcache_Entry get_entry_in_cache(Workcache_Entry findentry) {	// public version of find_entry, but slightly different works
		for(int entryindex = 0; entryindex < this.length(); entryindex++) {
			if(		(this.cache[entryindex].getStrasse() == findentry.getStrasse()) 
				&&	(this.cache[entryindex].getHausnummerNormalisiert().equals(findentry.getHausnummerNormalisiert()))
				&&	((findentry.getTreffertyp() == Workcache_Entry.Treffertyp.UNSET) || (this.cache[entryindex].getTreffertyp() == findentry.getTreffertyp()))	// treffertyp optional if set
				) {
				return this.cache[entryindex];
			}
		}
		return null;
	}

	
	/**
	 * get the cacheentry object with the given index
	 * @param index: index of the cacheentry
	 * @return:			 the found housenumber object (only one) or null
	 */
	public Workcache_Entry entry(int index) {	// public version of find_entry, but slightly different works
		if(index > cache_count) {
			return null;
		} else { 
			return cache[index];
		}
	}
	

	
		// update in this cache
	public void update(Workcache_Entry updateentry) {
		boolean debug_output = true;
		if(debug_output) System.out.println("start of .update of class Workcache ...");

		if(debug_output) System.out.println("in .update of class Workcache: find entry ...");
		int entry_index = find_entry_in_cache(updateentry);
		if(debug_output) System.out.println("found? "+(entry_index > -1));
		if(entry_index > -1) {
			try {
				if(debug_output) System.out.println("in .update of class Workcache: before call .update of class Entry ...");
				cache[entry_index].update(updateentry);
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

	

}

