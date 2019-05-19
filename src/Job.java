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
	public final long jobidUnset = 0L;
	
	private String country = "";
	private String countrycode = "";
	private String municipality = "";
	private String officialkeysId = "";
	private int adminLevel = 0;
	private String jobname = "";
	private long jobid = jobidUnset;
	private String subid = "";
	private long osmrelationid = 0L;
	private String serverqueuejobid = "";		// optionally unique queuejob-id from server to give information about source of job (up to now set only when job is from jobqueue in 06/2015)

	public Job() {
		this.country = "";
		this.countrycode = "";
		this.municipality = "";
		this.officialkeysId = "";
		this.adminLevel = 0;
		this.jobname = "";
		this.jobid = jobidUnset;
		this.subid = "";
		this.osmrelationid = 0L;
		this.serverqueuejobid = "";
	}

	/**
	 * 	elder Production constructor for API-Response from /findjobs, up to 2018-04
	 * @param country
	 * @param countrycode
	 * @param municipality
	 * @param officialkeysId
	 * @param adminLevel
	 * @param jobname
	 * @param subid
	 * @param relationid
	 */
	public Job(String country, String countrycode, String municipality, 
		String officialkeysId, Integer adminLevel, 
		String jobname, String subid, Long relationid) {
		this.country = country;
		this.countrycode = countrycode;
		this.municipality = municipality;
		this.officialkeysId = officialkeysId;
		this.adminLevel = adminLevel;
		this.jobname = jobname;
		this.subid = subid;
		this.osmrelationid = relationid;
		this.serverqueuejobid = "";
	}

	/**
	 * 	Production constructor for API-Response from /findjobs, since 2018-04
	 * @param country
	 * @param countrycode
	 * @param municipality
	 * @param officialkeysId
	 * @param adminLevel
	 * @param jobname
	 * @param jobid
	 * @param subid
	 * @param relationid
	 */
	public Job(String country, String countrycode, String municipality, 
		String officialkeysId, Integer adminLevel, 
		String jobname, long jobid, String subid, Long relationid) {
		this.country = country;
		this.countrycode = countrycode;
		this.municipality = municipality;
		this.officialkeysId = officialkeysId;
		this.adminLevel = adminLevel;
		this.jobname = jobname;
		this.jobid = jobid;
		this.subid = subid;
		this.osmrelationid = relationid;
		this.serverqueuejobid = "";
	}

	/**
	 *  
	 * 	elder Production constructor for API-Response from /getqueuejobs, up to 2018-04
	 *  without jobid
	 * @param country
	 * @param countrycode
	 * @param municipality
	 * @param officialkeysId
	 * @param adminLevel
	 * @param jobname
	 * @param subid
	 * @param relationid
	 * @param serverjobqueueid
	 */
	public Job(String country, String countrycode, String municipality, 
		String officialkeysId, Integer adminLevel, 
		String jobname, String subid, Long relationid, String serverjobqueueid) {
		this.country = country;
		this.countrycode = countrycode;
		this.municipality = municipality;
		this.officialkeysId = officialkeysId;
		this.adminLevel = adminLevel;
		this.jobname = jobname;
		this.subid = subid;
		this.osmrelationid = relationid;
		this.serverqueuejobid = serverjobqueueid;
	}
	
	/**
	 * 	Production constructor for API-Response from /getqueuejobs, since 2018-04
	 *  with jobid
	 * @param country
	 * @param countrycode
	 * @param municipality
	 * @param officialkeysId
	 * @param adminLevel
	 * @param jobname
	 * @param jobid
	 * @param subid
	 * @param relationid
	 * @param serverjobqueueid
	 */
	public Job(String country, String countrycode, String municipality, 
		String officialkeysId, Integer adminLevel, 
		String jobname, long jobid, String subid, Long relationid, String serverjobqueueid) {
		this.country = country;
		this.countrycode = countrycode;
		this.municipality = municipality;
		this.officialkeysId = officialkeysId;
		this.adminLevel = adminLevel;
		this.jobname = jobname;
		this.jobid = jobid;
		this.subid = subid;
		this.osmrelationid = relationid;
		this.serverqueuejobid = serverjobqueueid;
	}


	public int getAdminlevel() {
		return this.adminLevel;
	}
	
	public String getCountry() {
		return this.country;
	}
	
	public String getCountrycode() {
		return this.countrycode;
	}
	
	public long getJobID() {
		return this.jobid;
	}

	public String getJobname() {
		return this.jobname;
	}
	
	public String getMunicipality() {
		return this.municipality;
	}
	
	public String getOfficialkeysID() {
		return this.officialkeysId;
	}
	
	public long getOSMRelationID() {
		return this.osmrelationid;
	}
	
	public String getServerQueueJobId() {
		return this.serverqueuejobid;
	}

	public String getSubareaID() {
		return this.subid;
	}
	

	public String toString() {
		String output = "country=" + country + ", countrycode=" + countrycode + ", municipality=" + municipality + ", officialkeysId=" + officialkeysId + ", "
			+ "adminLevel=" + adminLevel + ", jobname=" + jobname + ", jobid= " + jobid + ", subid=" + subid + ", osmrelationid=" + osmrelationid;
		return output; 
	}
}

