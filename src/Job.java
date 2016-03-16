import java.util.ArrayList;
import java.util.List;

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



public class Job {
	public String country = "";
	public String countrycode = "";
	public String municipality = "";
	public String officialkeysId = "";
	public Integer adminLevel = 0;
	public String jobname = "";
	public String subid = "";
	public Long osmrelationid = 0L;
	public String serverobjectid = "";		// optionally unique id from server to give information about source of job (up to now set only when job is from jobqueue in 06/2015)

	public Job() {
		this.country = "";
		this.countrycode = "";
		this.municipality = "";
		this.officialkeysId = "";
		this.adminLevel = 0;
		this.jobname = "";
		this.subid = "";
		this.osmrelationid = 0L;
		this.serverobjectid = "";
	}

	public Job(String country, String countrycode, String municipality, String officialkeysId, Integer adminLevel, String jobname, String subid, Long relationid) {
		this.country = country;
		this.countrycode = countrycode;
		this.municipality = municipality;
		this.officialkeysId = officialkeysId;
		this.adminLevel = adminLevel;
		this.jobname = jobname;
		this.subid = subid;
		this.osmrelationid = relationid;
		this.serverobjectid = serverobjectid;
	}

	public Job(String country, String countrycode, String municipality, String officialkeysId, Integer adminLevel, String jobname, String subid, Long relationid, String serverobjectid) {
		this.country = country;
		this.countrycode = countrycode;
		this.municipality = municipality;
		this.officialkeysId = officialkeysId;
		this.adminLevel = adminLevel;
		this.jobname = jobname;
		this.subid = subid;
		this.osmrelationid = relationid;
		this.serverobjectid = serverobjectid;
	}

	

	List<Job> jobs = new ArrayList<Job>();

	public String toString() {
		String output = "country=" + country + ", countrycode=" + countrycode + ", municipality=" + municipality + ", officialkeysId=" + officialkeysId + ", "
			+ "adminLevel=" + adminLevel + ", jobname=" + jobname + ", subid=" + subid + ", osmrelationid=" + osmrelationid;
		return output; 
	}
}

