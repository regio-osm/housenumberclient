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
	private String officialkeysId = "";
	private Integer adminLevel = 0;
	private String jobname = "";
	private String subid = "";
	public Long evaluationtime = 0L;
	public Long osmtime = 0L;
	public HousenumberCollection housenumberlist = new HousenumberCollection();


	public void initialize() {
		country = "";
		municipality = "";
		officialkeysId = "";
		adminLevel = 0;
		jobname = "";
		housenumberlist.clear();
	}

	public String getCountry() {
		return this.country;
	}
	
	public String getMunicipality() {
		return this.municipality;
	}
	
	public String getOfficialkeysId() {
		return this.officialkeysId;
	}
	
	public Integer getAdminLevel() {
		return this.adminLevel;
	}

	public String getJobname() {
		return this.jobname;
	}

	public String getSubid() {
		return this.subid;
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
	public void setOfficialkeysId(String officialkeysId) {
		this.officialkeysId = officialkeysId;
	}
	
	/**
	 * copy complete job data into evaluation structure
	 * @param job
	 */
	public void setJobData(Job job) {
		this.country = job.country;
		this.municipality = job.municipality;
		this.officialkeysId = job.officialkeysId;
		this.adminLevel = job.adminLevel;
		this.jobname = job.jobname;
		this.subid = job.subid;
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
		Applicationconfiguration configuration = new Applicationconfiguration("./");

		java.util.Date programStart = new java.util.Date();
		
		for (int lfdnr = 0; lfdnr < args.length; lfdnr++) {
			System.out.println("args[" + lfdnr + "] ===" + args[lfdnr] + "===");
		}
		if ((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-country countryname -- ('Bundesrepublik Deutschland' or other countries in english version, for exampel 'Netherland'");
			System.out.println("-adminhierarchy xy -- administrative hierarchy, for example 'Bundesrepublik Deutschland,Bayern'");
			System.out.println("-municipality muniname -- name of the municipality");
			System.out.println("-jobname xy -- jobname of the municipality. Name of the muncipality or of a subadmin area, if available in osm and in offizial housenumber list");
			System.out.println("-officialkeysid id -- official id of the muncipality. In Germany amtlicher Gemeindeschlüssel. Wildcard at the end * possible");
			System.out.println("-allmissingjobs -- flag, that all missing jobs in a country should be run (default: only as specified in other parameters");
			System.out.println("-maxjobs 4711 -- maximum number of jobs, that should be worked on");
			System.out.println("-maxminutes 30 -- maximum number of minutes, the program should run. A running evaluation will be finished.");
			return;
		}
		String parameterCountry = "";
		String parameterAdminHierarchy = "";
		String parameterMunicipiality = "";
		String parameterJobname = "";
		String parameterOfficialkeysId = "";
		boolean parameterAllMissingJobs = false;
		int parameterMaxJobs = -1;
		int parameterMaxMinutes = -1;
		
		if (args.length >= 1) {
			int argsOkCount = 0;
			for (int argsi = 0; argsi < args.length; argsi += 2) {
				System.out.print(" args pair analysing #: " + argsi + "  ===" + args[argsi] + "===");
				if (args.length > argsi + 1) {
					System.out.println("  args # + 1: " + (argsi + 1) + "   ===" + args[argsi + 1] + "===");
				}
				if (args[argsi].equals("-country")) {
					parameterCountry = args[argsi + 1];
					argsOkCount  += 2;
				}
				if (args[argsi].equals("-adminhierarchy")) {
					parameterAdminHierarchy = args[argsi + 1];
					argsOkCount  += 2;
				}
				if (args[argsi].equals("-municipality")) {
					parameterMunicipiality = args[argsi + 1];
					argsOkCount  += 2;
				}
				if (args[argsi].equals("-jobname")) {
					parameterJobname = args[argsi + 1];
					argsOkCount  += 2;
				}
				if (args[argsi].equals("-officialkeysid")) {
					parameterOfficialkeysId = args[argsi + 1];
					argsOkCount  += 2;
				}

				if (args[argsi].equals("-maxjobs")) {
					parameterMaxJobs = Integer.parseInt(args[argsi + 1]);
					argsOkCount  += 2;
				}
				if (args[argsi].equals("-maxminutes")) {
					parameterMaxMinutes = Integer.parseInt(args[argsi + 1]);
					argsOkCount  += 2;
				}
				if (args[argsi].equals("-allmissingjobs")) {
					parameterAllMissingJobs = true;
					argsOkCount  += 1;
				}

				System.out.println("-maxjobs 4711 -- maximum number of jobs, that should be worked on");
System.out.println("-maxminutes 30 -- maximum number of minutes, the program should run. A running evaluation will be finished.");
			}
			if (argsOkCount < args.length) {
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return;
			}
		}
		
		if(		parameterCountry.equals("") 
			|| (parameterCountry.indexOf("*") != -1)
			|| (parameterCountry.indexOf("*") != -1))
		{
			System.out.println("Parameter -country must be set at least and can't contain a wildcard");
			return;
		}
		
		
		parameterAdminHierarchy = parameterAdminHierarchy.replace("*", "%");
		parameterMunicipiality = parameterMunicipiality.replace("*", "%");
		parameterJobname = parameterJobname.replace("*", "%");
		parameterOfficialkeysId = parameterOfficialkeysId.replace("*", "%");

		
		
		HousenumberlistReader hnrreader = new HousenumberlistReader();
		hnrreader.setDBConnection(	configuration.db_application_url, 
									configuration.db_application_username, 
									configuration.db_application_password);
		OsmDataReader osmreader = new OsmDataReader();
		Evaluation evaluation = new Evaluation();
		HousenumberServerAPI hnrserver = new HousenumberServerAPI();

		HousenumberCollection list_housenumbers = new HousenumberCollection();
		HousenumberCollection osm_housenumbers = new HousenumberCollection();
		HousenumberCollection evaluated_housenumbers = new HousenumberCollection();
		
		List<Job> jobs;

		if(parameterAllMissingJobs)
			jobs = hnrserver.getMissingCountryJobs(parameterCountry);
		else
			jobs = hnrserver.findJobs(parameterCountry,parameterMunicipiality, parameterJobname, parameterOfficialkeysId);
		

		//jobs = hnrserver.findJobs("Schweiz","Zürich", "Zürich", "*");
		
		System.out.println("Number of Jobs received from Server: " + jobs.size());
		
		java.util.Date jobstart = new java.util.Date();
		java.util.Date jobend = new java.util.Date();

		for(int jobindex = 0; jobindex < jobs.size(); jobindex++) {

	
			if((parameterMaxJobs != -1) && (parameterMaxJobs < (jobindex + 1))) {
				System.out.println("maximum number of jobs, as specified, arrived. Stop further processing");
				break;
			}
			if(parameterMaxMinutes != -1) {
				Long tempprogramstarttime = programStart.getTime();
				Long maxTime = programStart.getTime() + parameterMaxMinutes * 60 * 1000;
				java.util.Date now = new java.util.Date();
				if(now.getTime() > maxTime) {
					System.out.println("maximum time, as specified, arrived. Stop further processing");
					break;
				} else {
					System.out.println("Info: specified limit of minutes not arrived, useable minutes to work: " + Math.round((maxTime - now.getTime())/60/1000));
				}
			}

			Job actjob = jobs.get(jobindex);

			Long relationid = actjob.osmrelationid;
			String jobname = actjob.jobname;

			evaluation.setJobData(actjob);
			
			
			jobstart = new java.util.Date();
			System.out.println("start working on job " + actjob.toString() + "; started at " + jobstart.toString());

			evaluation.setHousenumberAdditionCaseSensity(false);

			java.util.Date dbloadstart = new java.util.Date();
			list_housenumbers.clear();
			//list_housenumbers = hnrreader.ReadListFromDB(evaluation);
			list_housenumbers = hnrserver.ReadListFromServer(evaluation);
			System.out.println("Number of official housenumbers: " + list_housenumbers.length());
			if(list_housenumbers.length() == 0) {
				System.out.println("Warning: job will be ignored, because no official housenumbers found for job " + actjob.toString() + "; started at " + jobstart.toString());
				continue;
			}
			java.util.Date dbloadend = new java.util.Date();
			java.util.Date overpassloadstart = new java.util.Date();
			osm_housenumbers.clear();
			osm_housenumbers = osmreader.ReadDataFromOverpass(evaluation, relationid);
			System.out.println("Number of OSM housenumbers: " + osm_housenumbers.length());
			//evaluation.osmtime will be set inside readdatafromoverpass
			evaluation.evaluationtime = overpassloadstart.getTime();
			java.util.Date overpassloadend = new java.util.Date();
			java.util.Date mergedatastart = new java.util.Date();
			evaluated_housenumbers.clear();
			evaluated_housenumbers = list_housenumbers.merge(osm_housenumbers);
			java.util.Date mergedataend = new java.util.Date();
			evaluated_housenumbers.printhtml("test.html");
			System.out.println("Number of housenumberlist entries after load of official housenumber list: "
				+ evaluation.housenumberlist.length() + "   " + evaluation.housenumberlist.count_unchanged());

			int lfdnr = 0;
			java.util.Date uploadstart = null;
			java.util.Date uploadend = null;
			//while(lfdnr < 1000) {
				lfdnr++;
				System.out.println("upload Nr. " + lfdnr);
				uploadstart = new java.util.Date();
				hnrserver.writeEvaluationToServer(evaluation, evaluated_housenumbers);
				uploadend = new java.util.Date();
			//}
			System.out.println("time for db load time in sek: " + (dbloadend.getTime() - dbloadstart.getTime())/1000);
			System.out.println("time for overpass load time in sek: " + (overpassloadend.getTime() - overpassloadstart.getTime())/1000);
			System.out.println("time for internal merge time in sek: " + (mergedataend.getTime() - mergedatastart.getTime())/1000);
			System.out.println("time for result upload time in sek: " + (uploadend.getTime() - uploadstart.getTime())/1000);

			jobend = new java.util.Date();
			System.out.println("finished working on job " + actjob.toString() + "; ended at " + jobend.toString() + " duration in sec: " 
				+ (jobend.getTime() - jobstart.getTime())/1000);
		}
		java.util.Date programEnd = new java.util.Date();
		System.out.println("Program finished at " + programEnd.toString() + " duration in sec: "
			+ (programEnd.getTime() - programStart.getTime())/1000);
	}
}
