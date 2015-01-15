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
		return this.municipality;
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


		//Integer relationsid = 1221158;	// Geetbets, 38 OSM Hausnummern bei 2518 Listenhausnummern
		//Integer relationsid = 196184;	// Zaventem
		//evaluation.setMunicipality("België", "Zaventem");
		//Integer relationsid = 2597486;
		//evaluation.setMunicipality("Poland", "Grudziądz");

		
		//Integer relationsid = 1263541;
		//evaluation.setMunicipality("België", "Geel");

		//Integer relationsid = 2078291;
		//evaluation.setMunicipality("Niederlande", "Maastricht");
		//Integer relationsid = 161966;
		//evaluation.setMunicipality("Bundesrepublik Deutschland", "Kühbach");

		//Integer relationsid = 62464;
		//evaluation.setMunicipality("Bundesrepublik Deutschland", "Würzburg");

		//Integer relationsid = 62578;
		//evaluation.setMunicipality("Bundesrepublik Deutschland", "Köln");

		//Integer relationsid = 2593494;
		//evaluation.setMunicipality("Poland", "Gorzów Wielkopolski");

		Integer relationsid = 2597485;
		evaluation.setMunicipality("Poland", "Gdańsk");
		
		
		evaluation.setHousenumberAdditionCaseSensity(false);
		System.out.println("Number of housenumberlist entries at start: " + evaluation.housenumberlist.length());
		HousenumberCollection list_housenumbers = hnrreader.ReadListFromDB(evaluation);
		HousenumberCollection osm_housenumbers = osmreader.ReadDataFromOverpass(evaluation, relationsid);
		HousenumberCollection evaluated_housenumbers = list_housenumbers.merge(osm_housenumbers);
		evaluated_housenumbers.printhtml("test.html");
		System.out.println("Number of housenumberlist entries after load of official housenumber list: "
			+ evaluation.housenumberlist.length() + "   " + evaluation.housenumberlist.count_unchanged());

		HousenumberServerAPI hnrserver = new HousenumberServerAPI();
		int lfdnr = 0;
		//while(1==1) {
			lfdnr++;
			System.out.println("upload Nr. " + lfdnr);
			hnrserver.writeEvaluationToServer(evaluation, evaluated_housenumbers);
		//}
	}
}
