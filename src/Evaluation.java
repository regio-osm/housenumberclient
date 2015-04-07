/**
 * @author Dietmar Seifert
 *
 * evaluate the housenumbers in a municipality or a subregion of it, check againsts an available housenumerlist
 * 	
*/

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
	public static final Logger logger = Logger.getLogger(Evaluation.class.getName());


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

		if ((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("German help available with -hDE");
			System.out.println("");
			System.out.println("For local housnumberlist evaluations MUST parameters:");
			System.out.println("-file importfilename");
			System.out.println("-relationid: OSM-Relationid for Municipality, for example 123456");
			System.out.println("");
			System.out.println("For local housenumberlist evaluations OPTIONAL parameters:");
			System.out.println("-columnseparator: Character for separation of file line columns (Default, if unset: TAB-Character)");
			System.out.println("-housenumbercasesensity: should alphabetical suffixes at housenumbers be case sensitive ('yes') or not (default 'no')");
			System.out.println("");
			System.out.println("For local housenumberlist evaluations Information about the local housenumberlist:");
			System.out.println("The import file must have a first comment line, starting with the hash character #");
			System.out.println("In this comment line, the column titles must be set in any order: Stadt	Straße   Hausnummer    Hausnummerzusatz   Hausnummerzusatz2   Subid");
			System.out.println("Furthermore column titles will be ignored");
			System.out.println("");
			System.out.println("Help for client-server mode and its program call parameters:");
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
		if ((args.length >= 1) && (args[0].equals("-hDE"))) {
			System.out.println("Für lokale Auswertungen PFLICHT-Parameter:");
			System.out.println("-file importdateiname");
			System.out.println("-relationid: OSM-Relationsid für die Gemeinde, z.B. 123456");
			System.out.println("");
			System.out.println("Für lokale Auswertungen WAHL-Parameter:");
			System.out.println("-columnseparator: Zeichen zur Trennung der Spalten in einer Dateizeile (Standard, wenn ungesetzt: TAB-Zeichen)");
			System.out.println("-housenumbercasesensity: Angabe, ob alphabetische Hausnummerzusätze groß-/kleinschreibrelevant ausgewertet werden sollen (Standard: nein), Werte ja, nein");
			System.out.println("");
			System.out.println("Für lokale Auswertungen Angaben zur Hausnummerdatei:");
			System.out.println("Importdatei muss in der ersten Zeile eine Kommentarzeile haben, die mit # beginnt");
			System.out.println("Mögliche Spaltentitel sind in beliebiger Reihenfolge:   Stadt	Straße   Hausnummer    Hausnummerzusatz   Hausnummerzusatz2   Subid");
			System.out.println("Weitere Spaltentitel können vorhanden sein und werden ignoriert");
			System.out.println("");
			System.out.println("Für Client/Server Auswertungen PFLICHT-Parameter:");
			System.out.println("-country: 'Bundesrepublik Deutschland' wenn fehlend");
			System.out.println("-municipality: Stadtname");
			System.out.println("-officialkeysid 8stelligeramtlicherGemeindeschlüssel z.B. 09761000");
			System.out.println("-adminhierarchy xy -- administrative hierarchy, for example 'Bundesrepublik Deutschland,Bayern'");
			System.out.println("-jobname xy -- jobname of the municipality. Name of the muncipality or of a subadmin area, if available in osm and in offizial housenumber list");
			System.out.println("-allmissingjobs -- flag, that all missing jobs in a country should be run (default: only as specified in other parameters");
			System.out.println("-maxjobs 4711 -- maximum number of jobs, that should be worked on");
			System.out.println("-maxminutes 30 -- maximum number of minutes, the program should run. A running evaluation will be finished.");
			return;
		}


		java.util.Date programStart = new java.util.Date();

		String parameterCountry = "Bundesrepublik Deutschland";
		String parameterAdminHierarchy = "";
		String parameterMunicipiality = "";
		String parameterJobname = "";
		String parameterOfficialkeysId = "";
		boolean parameterAllMissingJobs = false;
		int parameterMaxJobs = -1;
		int parameterMaxMinutes = -1;
		boolean parameterHousenumbersCaseSensity = false;
			// client call parameters
		String parameterImportdateiname = "";
		String parameterFieldSeparator = "\t";
		Long parameterOSMRelationid = 0L;
		
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
				if (args[argsi].equals("-file")) {
					parameterImportdateiname = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-housenumbercasesensity")) {
					String yesno = args[argsi + 1].toLowerCase().substring(0,1);
					if (yesno.equals("y") || yesno.equals("j")) {
						parameterHousenumbersCaseSensity = true;
					} else {
						parameterHousenumbersCaseSensity = false;
					}
					argsOkCount += 2;
				}

				if (args[argsi].equals("-relationid")) {
					parameterOSMRelationid = Long.parseLong(args[argsi + 1]);
					argsOkCount += 2;
				}
				if (args[argsi].equals("-columnseparator")) {
					parameterFieldSeparator = args[argsi + 1];
					argsOkCount += 2;
				}
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

		String relativePathToApplicationConfiguration = "";
		if(! parameterImportdateiname.equals("")) {
			relativePathToApplicationConfiguration = "./";
		}
		Applicationconfiguration configuration = new Applicationconfiguration(relativePathToApplicationConfiguration);

		try {
			Handler handler = new ConsoleHandler();
			handler.setFormatter(new Formatter() {
		         public String format(LogRecord rec) {
		            StringBuffer buf = new StringBuffer(1000);
		            //buf.append(new java.util.Date());
		            //buf.append(" ");
		            buf.append(rec.getLevel());
		            buf.append(": ");
		            buf.append(formatMessage(rec));
		            buf.append("\n");
		            return buf.toString();
		         }
			});
			handler.setLevel(configuration.logging_console_level);
			logger.addHandler(handler);
			FileHandler fhandler = new FileHandler(configuration.logging_filename);
			fhandler.setFormatter(new Formatter() {
		         public String format(LogRecord rec) {
		            StringBuffer buf = new StringBuffer(1000);
		            buf.append(new java.util.Date());
		            buf.append(" ");
		            buf.append(rec.getLevel());
		            buf.append(" ");
		            buf.append(formatMessage(rec));
		            buf.append("\n");
		            return buf.toString();
		         }
			});
			fhandler.setLevel(configuration.logging_file_level);
			logger.addHandler(fhandler);
			logger.setLevel(configuration.logging_console_level);
		} catch (IOException e) {
			System.out.println("Fehler beim Logging-Handler erstellen, ...");
			System.out.println(e.toString());
		}

		for (int lfdnr = 0; lfdnr < args.length; lfdnr++) {
			logger.log(Level.FINE, "args[" + lfdnr + "] ===" + args[lfdnr] + "===");
		}

		parameterAdminHierarchy = parameterAdminHierarchy.replace("*", "%");
		parameterMunicipiality = parameterMunicipiality.replace("*", "%");
		parameterJobname = parameterJobname.replace("*", "%");
		parameterOfficialkeysId = parameterOfficialkeysId.replace("*", "%");

		HousenumberlistReader hnrreader = new HousenumberlistReader();
		OsmDataReader osmreader = new OsmDataReader();
		Evaluation evaluation = new Evaluation();

		if(! parameterCountry.equals("") && (! parameterMunicipiality.equals("")))
			evaluation.setMunicipality(parameterCountry, parameterMunicipiality);
		evaluation.setHousenumberAdditionCaseSensity(parameterHousenumbersCaseSensity);

		HousenumberCollection list_housenumbers = new HousenumberCollection();
		HousenumberCollection osm_housenumbers = new HousenumberCollection();
		HousenumberCollection evaluated_housenumbers = new HousenumberCollection();
		
			// local file, work offline (only connect to osm data via overpass
		if(! parameterImportdateiname.equals("")) {
			// client-server mode, connect to regio-osm.de Server and API and get jobs
			list_housenumbers = hnrreader.ReadListFromFile(evaluation, parameterImportdateiname, parameterFieldSeparator, parameterHousenumbersCaseSensity);
			osm_housenumbers = osmreader.ReadDataFromOverpass(evaluation, parameterOSMRelationid);
			evaluated_housenumbers = list_housenumbers.merge(osm_housenumbers);
			evaluated_housenumbers.printhtml("evaluation.html");
			logger.log(Level.INFO, "Number of housenumberlist entries after load of official housenumber list: "
				+ evaluation.housenumberlist.length() + "   " + evaluation.housenumberlist.count_unchanged());
		} else {
	
			HousenumberServerAPI hnrserver = new HousenumberServerAPI();
	
			hnrreader.setDBConnection(	configuration.db_application_url, 
										configuration.db_application_username, 
										configuration.db_application_password);

			List<Job> jobs;
	
			if(parameterAllMissingJobs)
				jobs = hnrserver.getMissingCountryJobs(parameterCountry);
			else
				jobs = hnrserver.findJobs(parameterCountry,parameterMunicipiality, parameterJobname, parameterOfficialkeysId);
			
	
			//jobs = hnrserver.findJobs("Schweiz","Zürich", "Zürich", "*");
			
			logger.log(Level.INFO, "Number of Jobs received from Server: " + jobs.size());
			
			java.util.Date jobstart = new java.util.Date();
			java.util.Date jobend = new java.util.Date();
	
			for(int jobindex = 0; jobindex < jobs.size(); jobindex++) {
	
		
				if((parameterMaxJobs != -1) && (parameterMaxJobs < (jobindex + 1))) {
					logger.log(Level.INFO, "maximum number of jobs, as specified, arrived. Stop further processing");
					break;
				}
				if(parameterMaxMinutes != -1) {
					Long tempprogramstarttime = programStart.getTime();
					Long maxTime = programStart.getTime() + parameterMaxMinutes * 60 * 1000;
					java.util.Date now = new java.util.Date();
					if(now.getTime() > maxTime) {
						logger.log(Level.INFO, "maximum time, as specified, arrived. Stop further processing");
						break;
					} else {
						logger.log(Level.FINEST, "Info: specified limit of minutes not arrived, useable minutes to work: " + Math.round((maxTime - now.getTime())/60/1000));
					}
				}
	
				Job actjob = jobs.get(jobindex);
	
				Long relationid = actjob.osmrelationid;
				String jobname = actjob.jobname;
	
				evaluation.setJobData(actjob);
				
				
				jobstart = new java.util.Date();
				logger.log(Level.INFO, "start working on job " + actjob.toString() + "; started at " + jobstart.toString());
	
				evaluation.setHousenumberAdditionCaseSensity(false);

				list_housenumbers.clear();
				osm_housenumbers.clear();
				evaluated_housenumbers.clear();
				
				
				java.util.Date dbloadstart = new java.util.Date();
				list_housenumbers.clear();
				//list_housenumbers = hnrreader.ReadListFromDB(evaluation);
				list_housenumbers = hnrserver.ReadListFromServer(evaluation);
				logger.log(Level.INFO, "Number of official housenumbers: " + list_housenumbers.length());
				if(list_housenumbers.length() == 0) {
					logger.log(Level.WARNING, "Warning: job will be ignored, because no official housenumbers found for job " + actjob.toString() + "; started at " + jobstart.toString());
					continue;
				}
				java.util.Date dbloadend = new java.util.Date();
				java.util.Date overpassloadstart = new java.util.Date();
				osm_housenumbers.clear();
				osm_housenumbers = osmreader.ReadDataFromOverpass(evaluation, relationid);
				logger.log(Level.INFO, "Number of OSM housenumbers: " + osm_housenumbers.length());
				evaluation.evaluationtime = overpassloadstart.getTime();
				java.util.Date overpassloadend = new java.util.Date();
				java.util.Date mergedatastart = new java.util.Date();
				evaluated_housenumbers.clear();
				evaluated_housenumbers = list_housenumbers.merge(osm_housenumbers);
				java.util.Date mergedataend = new java.util.Date();
				evaluated_housenumbers.printhtml("test.html");
	
				int lfdnr = 0;
				java.util.Date uploadstart = null;
				java.util.Date uploadend = null;
				//while(lfdnr < 1000) {
					lfdnr++;
					logger.log(Level.FINE, "upload Nr. " + lfdnr);
					uploadstart = new java.util.Date();
					hnrserver.writeEvaluationToServer(evaluation, evaluated_housenumbers);
					uploadend = new java.util.Date();
				//}
				logger.log(Level.INFO, "time for db load time in sek: " + (dbloadend.getTime() - dbloadstart.getTime())/1000);
				logger.log(Level.INFO, "time for overpass load time in sek: " + (overpassloadend.getTime() - overpassloadstart.getTime())/1000);
				logger.log(Level.INFO, "time for internal merge time in sek: " + (mergedataend.getTime() - mergedatastart.getTime())/1000);
				logger.log(Level.INFO, "time for result upload time in sek: " + (uploadend.getTime() - uploadstart.getTime())/1000);
	
				jobend = new java.util.Date();
				logger.log(Level.INFO, "finished working on job " + actjob.toString() + "; ended at " + jobend.toString() + " duration in sec: " 
					+ (jobend.getTime() - jobstart.getTime())/1000);
			}
		}	// end of else case (client/serer mode)

		java.util.Date programEnd = new java.util.Date();
		logger.log(Level.INFO, "Program finished at " + programEnd.toString() + ", Duration in sec: "
			+ (programEnd.getTime() - programStart.getTime())/1000);
	}
}
