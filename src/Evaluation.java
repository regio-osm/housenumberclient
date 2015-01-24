/**
 * @author Dietmar Seifert
 *
 * evaluate the housenumbers in a municipality or a subregion of it, check againsts an available housenumerlist
 * 	
*/

import java.util.Date;
import java.util.List;

import de.regioosm.housenumbers.Applicationconfiguration;


public class Evaluation {
	private final static long MINUTES_IN_MILLISECONDS = 60 * 1000;
	private String country = "";
	private String municipality = "";
	private String jobname = "";
	public Long evaluationtime = 0L;
	public Long osmtime = 0L;
	public HousenumberCollection housenumberlist = new HousenumberCollection();


	public void initialize() {
		country = "";
		municipality = "";
		jobname = "";
		housenumberlist.clear();
	}

	public String getCountry() {
		return this.country;
	}
	
	public String getMunicipality() {
		return this.municipality;
	}
	
	
	public String getJobname() {
		return this.jobname;
	}


	public HousenumberCollection getHousenumberlist() {
		return this.housenumberlist;
	}

	public void setHousenumberAdditionCaseSensity(boolean casesensity) {
		housenumberlist.setHousenumberadditionCaseSentity(casesensity);
	}

	/**
	 * set unique name of municipality, for which the evaluation should be run
	 * @param country
	 * @param municipality
	 */
	public void setMunicipality(String country, String municipality) {
		this.country = country;
		this.municipality = municipality;
		if(this.jobname.equals(""))
			this.jobname = municipality;
	}

	/**
	 * set unique name of municipality, for which the evaluation should be run
	 * @param country
	 * @param municipality
	 */
	public void setMunicipalityAndJobname(String country, String municipality, String jobname) {
		this.country = country;
		this.municipality = municipality;
		this.jobname = jobname;
	}

	/**
	 * set unique name of municipality, for which the evaluation should be run
	 * @param country
	 * @param municipality
	 */
	public void setMunicipalityJobname(String jobname) {
		this.jobname = jobname;
	}

	/**
	 * load the official housenumber list. At this time, unsure, if from one specific source or multiple inputs (pbf, overpass-query, osm2pgsql ...)
	 */
	public void loadOfficialHousenumberlist() {
		
	}


	/**
	 * execute the evaluation
	 */
	public void run() {
		
	}


	/**
	 * store the result of an evaluation to server
	 */
	public void storeEvaluation() {
		
	}


	public static void main(String args[]) {
		Applicationconfiguration configuration = new Applicationconfiguration();

		HousenumberlistReader hnrreader = new HousenumberlistReader();
		hnrreader.setDBConnection(	configuration.db_application_url, 
									configuration.db_application_username, 
									configuration.db_application_password);
		OsmDataReader osmreader = new OsmDataReader();
		Evaluation evaluation = new Evaluation();
		HousenumberServerAPI hnrserver = new HousenumberServerAPI();

		List<Job> jobs = hnrserver.findJobs("Poland","*", "14*");


		//Integer relationsid = 2597485;
		//evaluation.setMunicipality("Poland", "Gdańsk");		

/*		Long relationid = 2981500L;
		String country = "Poland";
		String municipality = "Górowo Iławeckie";
		String jobname = municipality;
*/
boolean skipping = true;
		for(int jobindex = 0; jobindex < jobs.size(); jobindex++) {

			Job actjob = jobs.get(jobindex);

			Long relationid = actjob.osmrelationid;
			String country = actjob.country;
			String municipality = actjob.municipality;
			String jobname = actjob.jobname;
		
			if(jobname.equals("gmina Wolanów"))
				skipping = false;
//			if(relationid == 2708018)
//				skipping = false;
			if(skipping)
				continue;

			evaluation.setMunicipalityAndJobname(country, municipality, jobname);

			evaluation.setHousenumberAdditionCaseSensity(false);
			System.out.println("Number of housenumberlist entries at start: " + evaluation.housenumberlist.length());

			java.util.Date dbloadstart = new java.util.Date();
			HousenumberCollection list_housenumbers = hnrreader.ReadListFromDB(evaluation);
			java.util.Date dbloadend = new java.util.Date();
			java.util.Date overpassloadstart = new java.util.Date();
			HousenumberCollection osm_housenumbers = osmreader.ReadDataFromOverpass(evaluation, relationid);
			evaluation.osmtime = overpassloadstart.getTime() - 5 * MINUTES_IN_MILLISECONDS;
			evaluation.evaluationtime = overpassloadstart.getTime();
			java.util.Date overpassloadend = new java.util.Date();
			java.util.Date mergedatastart = new java.util.Date();
			HousenumberCollection evaluated_housenumbers = list_housenumbers.merge(osm_housenumbers);
			java.util.Date mergedataend = new java.util.Date();
			evaluated_housenumbers.printhtml("test.html");
			System.out.println("Number of housenumberlist entries after load of official housenumber list: "
				+ evaluation.housenumberlist.length() + "   " + evaluation.housenumberlist.count_unchanged());

			int lfdnr = 0;
			//while(1==1) {
				lfdnr++;
				System.out.println("upload Nr. " + lfdnr);
				java.util.Date uploadstart = new java.util.Date();
				hnrserver.writeEvaluationToServer(evaluation, evaluated_housenumbers);
				java.util.Date uploadend = new java.util.Date();
			//}
			System.out.println("time for db load time in sek: " + (dbloadend.getTime() - dbloadstart.getTime())/1000);
			System.out.println("time for overpass load time in sek: " + (overpassloadend.getTime() - overpassloadstart.getTime())/1000);
			System.out.println("time for internal merge time in sek: " + (mergedataend.getTime() - mergedatastart.getTime())/1000);
			System.out.println("time for result upload time in sek: " + (uploadend.getTime() - uploadstart.getTime())/1000);
		}
	}
}
