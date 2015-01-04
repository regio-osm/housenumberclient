/**
 * @author Dietmar Seifert
 *
 * evaluate the housenumbers in a municipality or a subregion of it, check againsts an available housenumerlist
 * 	
*/

import de.regioosm.housenumbers.Applicationconfiguration;



public class Evaluation {
	private String country = "";
	private String municipality = "";
	private String jobname = "";
	public HousenumberCache housenumberlist = new HousenumberCache();


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
		return this.municipality;
	}


	public HousenumberCache getHousenumberlist() {
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
		
//Integer relationsid = 1221158;	// Geetbets, 38 OSM Hausnummern bei 2518 Listenhausnummern
		
		Evaluation evaluation = new Evaluation();
		//Integer relationsid = 196184;	// Oudenburg, jede Menge associatedStreet Relationen
		//evaluation.setMunicipality("België", "Zaventem");
		//Integer relationsid = 2597486;
		//evaluation.setMunicipality("Poland", "Grudziądz");

		Integer relationsid = 2078291;
		evaluation.setMunicipality("Niederlande", "Maastricht");

		evaluation.setHousenumberAdditionCaseSensity(false);
		System.out.println("Number of housenumberlist entries at start: " + evaluation.housenumberlist.length());
		HousenumberCache list_housenumbers = hnrreader.ReadListFromDB(evaluation);
		HousenumberCache osm_housenumbers = osmreader.ReadDataFromOverpass(evaluation, relationsid);
		HousenumberCache evaluated_housenumbers = list_housenumbers.merge(osm_housenumbers);
		evaluated_housenumbers.printhtml("test.html");
		System.out.println("Number of housenumberlist entries after load of official housenumber list: "
			+ evaluation.housenumberlist.length() + "   " + evaluation.housenumberlist.count_unchanged());
	}
}
