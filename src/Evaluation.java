/**
 * @author Dietmar Seifert
 *
 * evaluate the housenumbers in a municipality or a subregion of it, check againsts an available housenumerlist
 * 	
*/

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.openstreetmap.osmosis.core.bound.v0_6.BoundComputer;

import de.regioosm.housenumbers.Applicationconfiguration;



public class Evaluation {
	private final static long MINUTES_IN_MILLISECONDS = 60 * 1000;
	private String country = "";
	private String countrycode = "";
	private String municipality = "";
	private String officialkeysId = "";
	private Integer adminLevel = 0;
	private String jobname = "";
	private String subid = "";
	private String serverobjectid = "";
	private String uselanguagecode = "";
	private String osmdatasource = "";
	public Long evaluationtime = 0L;
	public Long osmtime = 0L;
	public HousenumberCollection housenumberlist = new HousenumberCollection();
	public static final Logger logger = Logger.getLogger(Evaluation.class.getName());
	public static Integer onlyPartOfStreetnameIndexNo = -1;
	public static String onlyPartOfStreetnameSeparator = "";

	public void initialize() {
		country = "";
		countrycode = "";
		municipality = "";
		officialkeysId = "";
		uselanguagecode = "";
		adminLevel = 0;
		jobname = "";
		housenumberlist.clear();
	}

	public String getOsmdatasource() {
		return this.osmdatasource;
	}

	public String getCountry() {
		return this.country;
	}
	
	public String getCountrycode() {
		return this.countrycode;
	}

	public String getMunicipality() {
		return this.municipality;
	}
	
	public String getOfficialkeysId() {
		return this.officialkeysId;
	}
	
	public String getUselanguagecode() {
		return this.uselanguagecode;
	}

	public Integer getAdminLevel() {
		return this.adminLevel;
	}

	public String getJobname() {
		return this.jobname;
	}

	public String getJobCountry() {
		return this.country;
	}

	public String getJobCountrycode() {
		return this.countrycode;
	}

	public String getJobMunicipality() {
		return this.municipality;
	}
	
	public String getJobOfficialkeysId() {
		return this.officialkeysId;
	}
	
	public Integer getJobAdminlevel() {
		return this.adminLevel;
	}
	
	public String getJobSubid() {
		return this.subid;
	}
	
	public String getJobServerobjectid() {
		return this.serverobjectid;
	}

	public String getSubid() {
		return this.subid;
	}
	
	public String getServerobjectid() {
		return this.serverobjectid;
	}

	public Integer getOnlyPartOfStreetnameIndexNo() {
		return this.onlyPartOfStreetnameIndexNo;
	}
	
	public String getOnlyPartOfStreetnameSeparator() {
		return this.onlyPartOfStreetnameSeparator;
	}

	public HousenumberCollection getHousenumberlist() {
		return this.housenumberlist;
	}


	public void setOsmdatasource(String datasource) {
		this.osmdatasource = datasource;
	}


	public void setHousenumberAdditionCaseSensity(boolean casesensity) {
		housenumberlist.setHousenumberadditionCaseSentity(casesensity);
	}

	/**
	 * set unique name of municipality, for which the evaluation should be run
	 * @param country
	 * @param municipality
	 */
	public void setMunicipality(String country, String countrycode, String municipality) {
		this.country = country;
		this.countrycode = countrycode;
		this.municipality = municipality;
		if(this.jobname.equals(""))
			this.jobname = municipality;
	}

	/**
	 * set unique name of municipality, for which the evaluation should be run
	 * @param country
	 * @param municipality
	 */
	public void setMunicipalityAndJobname(String country, String countrycode, String municipality, String jobname) {
		this.country = country;
		this.countrycode = countrycode;
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


	public void setOnlyPartOfStreetnameIndexNo(Integer indexNo) {
		this.onlyPartOfStreetnameIndexNo = indexNo;
	}
	
	public void setOnlyPartOfStreetnameSeparator(String separator) {
		this.onlyPartOfStreetnameSeparator = separator;
	}

	/**
	 * set language ISO-Code for use in evaluation. Examples are name and addr:street
	 * @param uselanguagecode
	 */
	public void setUselanguagecode(String uselanguagecode) {
		if(!uselanguagecode.equals(""))
			this.uselanguagecode = uselanguagecode.toLowerCase();
	}


	/**
	 * copy complete job data into evaluation structure
	 * @param job
	 */
	public void setJobData(Job job) {
		this.country = job.country;
		this.countrycode = job.countrycode;
		this.municipality = job.municipality;
		this.officialkeysId = job.officialkeysId;
		this.adminLevel = job.adminLevel;
		this.jobname = job.jobname;
		this.subid = job.subid;
		this.serverobjectid = job.serverobjectid;
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
			System.out.println("-osmdatasource overpass | db  -- source of osm data, where to get from (default: 'overpass')");
			System.out.println("-country countryname -- ('Bundesrepublik Deutschland' or other countries in english version, for exampel 'Netherland'");
			System.out.println("-officialkeysid id -- official id of the muncipality. In Germany amtlicher Gemeindeschlüssel. Wildcard at the end * possible");
			System.out.println("-adminhierarchy xy -- administrative hierarchy, for example 'Bundesrepublik Deutschland,Bayern'");
			System.out.println("-municipality muniname -- name of the municipality");
			System.out.println("-jobname xy -- jobname of the municipality. Name of the muncipality or of a subadmin area, if available in osm and in offizial housenumber list");
			System.out.println("-languagecode xy (2digits ISO-Code, like DE)");
			System.out.println("-allmissingjobs -- flag, that all missing jobs in a country should be run (default: only as specified in other parameters");
			System.out.println("-maxjobs 4711 -- maximum number of jobs, that should be worked on");
			System.out.println("-maxminutes 30 -- maximum number of minutes, the program should run. A running evaluation will be finished.");
			System.out.println("-queuejobs yes/no -- yes: get next jobs, which are regularly to process; no (default): get jobs, according to other parameters like -country, -municipality etc.");
			System.out.println("-queuefilter instant/regular -- if not set, both; instant: only instant requested jobs; regular: only jobs, which are scheduled on regularly time basis - only active, if parameter -queuejobs set to yes");
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
			System.out.println("-osmdatasource overpass | db  -- Quelle, wo OSM Daten abgefragt werden sollen (Standard: 'overpass')");
			System.out.println("-country: 'Bundesrepublik Deutschland' wenn fehlend");
			System.out.println("-municipality: Stadtname");
			System.out.println("-officialkeysid 8stelligeramtlicherGemeindeschlüssel z.B. 09761000");
			System.out.println("-adminhierarchy xy -- administrative hierarchy, for example 'Bundesrepublik Deutschland,Bayern'");
			System.out.println("-jobname xy -- jobname of the municipality. Name of the muncipality or of a subadmin area, if available in osm and in offizial housenumber list");
			System.out.println("-languagecode xy (2digits ISO-Code, like DE)");
			System.out.println("-allmissingjobs -- flag, that all missing jobs in a country should be run (default: only as specified in other parameters");
			System.out.println("-maxjobs 4711 -- maximum number of jobs, that should be worked on");
			System.out.println("-maxminutes 30 -- maximum number of minutes, the program should run. A running evaluation will be finished.");
			System.out.println("-queuejobs yes/no -- yes: get next jobs, which are regularly to process; no (default): get jobs, according to other parameters like -country, -municipality etc. Please set -maxjobs, too");
			System.out.println("-queuefilter instant/regular -- if not set, both; instant: only instant requested jobs; regular: only jobs, which are scheduled on regularly time basis - only active, if parameter -queuejobs set to yes");
			return;
		}


		java.util.Date programStart = new java.util.Date();

		DateFormat dateformat = DateFormat.getDateTimeInstance();
		
		String parameterCountry = "";
		String parameterAdminHierarchy = "";
		String parameterMunicipiality = "";
		String parameterJobname = "";
		String parameterOfficialkeysId = "";
		String parameterLanguagecode = "";
		String parameterOsmDatasource = "";
		boolean parameterAllMissingJobs = false;
		boolean parameterGetQueueJobs = false;
		String parameterQueueFilter = "";
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
								
				if (args[argsi].equals("-osmdatasource")) {
					if(!args[argsi + 1].equals("")) {
						if(		(args[argsi + 1].toLowerCase().equals("overpass"))
							||	(args[argsi + 1].toLowerCase().equals("db"))) {
							parameterOsmDatasource = args[argsi + 1];
							argsOkCount  += 2;
						} else {
							System.out.println("Program input parameter -osmdatasource has invalid value ===" + args[argsi + 1] + "===, but only 'overpass' and 'db' are allowed");
						}
					}
				} else if (args[argsi].equals("-country")) {
					parameterCountry = args[argsi + 1];
					argsOkCount  += 2;
				} else if (args[argsi].equals("-adminhierarchy")) {
					parameterAdminHierarchy = args[argsi + 1];
					argsOkCount  += 2;
				} else if (args[argsi].equals("-municipality")) {
					parameterMunicipiality = args[argsi + 1];
					argsOkCount  += 2;
				} else  if (args[argsi].equals("-jobname")) {
					parameterJobname = args[argsi + 1];
					argsOkCount  += 2;
				} else if (args[argsi].equals("-officialkeysid")) {
					parameterOfficialkeysId = args[argsi + 1];
					argsOkCount  += 2;
				} else if (args[argsi].equals("-maxjobs")) {
					parameterMaxJobs = Integer.parseInt(args[argsi + 1]);
					argsOkCount  += 2;
				} else if (args[argsi].equals("-maxminutes")) {
					parameterMaxMinutes = Integer.parseInt(args[argsi + 1]);
					argsOkCount  += 2;
				} else if(args[argsi].equals("-languagecode")) {
					parameterLanguagecode = args[argsi+1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-allmissingjobs")) {
					parameterAllMissingJobs = true;
					argsOkCount  += 1;
				} else if (args[argsi].equals("-file")) {
					parameterImportdateiname = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-housenumbercasesensity")) {
					String yesno = args[argsi + 1].toLowerCase().substring(0,1);
					if (yesno.equals("y") || yesno.equals("j")) {
						parameterHousenumbersCaseSensity = true;
					} else {
						parameterHousenumbersCaseSensity = false;
					}
					argsOkCount += 2;
				} else if (args[argsi].equals("-queuejobs")) {
					String yesno = args[argsi + 1].toLowerCase().substring(0,1);
					if (yesno.equals("y") || yesno.equals("j")) {
						parameterGetQueueJobs = true;
					} else {
						parameterGetQueueJobs = false;
					}
					argsOkCount += 2;
				} else if (args[argsi].equals("-queuefilter")) {
					String parametervalue = args[argsi + 1].toLowerCase().substring(0,1);
					if (parametervalue.equals("i") || parametervalue.equals("i")) {
						parameterQueueFilter = "instant";
					} else {
						parameterQueueFilter = "regular";
					}
					argsOkCount += 2;
				} else if (args[argsi].equals("-relationid")) {
					parameterOSMRelationid = Long.parseLong(args[argsi + 1]);
					argsOkCount += 2;
				} else if (args[argsi].equals("-columnseparator")) {
					parameterFieldSeparator = args[argsi + 1];
					argsOkCount += 2;
				}
			}
			if (argsOkCount < args.length) {
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return;
			}
		}
		
		if((1==0) &&		parameterCountry.equals("") 
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
		File importworkPathandFilenameHandle = null;

		PrintWriter workprogressOutput = null;
		PrintWriter osmoverpassOutput = null;
		String importworkPathandFilename = "evaluation.active";
		PrintWriter munininfoOutput = null;
		String munininfoPathandFilename = "evaluationprogress.txt";
		String muninosmoverpassPathandFilename = "muninosmoverpass.txt";
		
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

		
				// set working filename to be sure, that only one instance is running in one file directory:
				//   both important for overpass requests and at least for -queuejobs mode
			importworkPathandFilenameHandle = new File(importworkPathandFilename);
			if(importworkPathandFilenameHandle.exists() && !importworkPathandFilenameHandle.isDirectory()) {
				Long filedate_milliseconds = importworkPathandFilenameHandle.lastModified();
				Long nowdate_milliseconds = new Date().getTime();
				System.out.println("filedate_msec ===" + filedate_milliseconds + "===");
				System.out.println("nowdate msec  ===" + nowdate_milliseconds + "===");
				System.out.println("diff now minus filedate msec ===" + (nowdate_milliseconds - filedate_milliseconds));
				System.out.println("diff now minus filedate sec ===" + (nowdate_milliseconds - filedate_milliseconds)/1000);
				Long maxtimeAssumeSystemWorks_milliseconds = (long) (30 * 60 * 1000);		// 30 minutes
				if((nowdate_milliseconds - filedate_milliseconds) > maxtimeAssumeSystemWorks_milliseconds) {
					System.out.println("Evaluation active File found, but to old (in sec: " + ((nowdate_milliseconds - filedate_milliseconds)/1000) + "), it will be deleted");
					importworkPathandFilenameHandle.delete();
					importworkPathandFilenameHandle = new File(importworkPathandFilename);
				} else {
					// delete munin report file at program end
					File muninosmoverpassPathandFilenameHandle = new File(muninosmoverpassPathandFilename);
					if(muninosmoverpassPathandFilenameHandle.exists() && !muninosmoverpassPathandFilenameHandle.isDirectory()) {
						filedate_milliseconds = muninosmoverpassPathandFilenameHandle.lastModified();
						nowdate_milliseconds = new Date().getTime();
						System.out.println("munin filedate_msec ===" + filedate_milliseconds + "===");
						System.out.println("nowdate msec  ===" + nowdate_milliseconds + "===");
						System.out.println("diff now minus munin filedate msec ===" + (nowdate_milliseconds - filedate_milliseconds));
						System.out.println("diff now minus munin filedate sec ===" + (nowdate_milliseconds - filedate_milliseconds)/1000);
						maxtimeAssumeSystemWorks_milliseconds = (long) (5 * 60 * 1000);		// 5 minutes
						if((nowdate_milliseconds - filedate_milliseconds) > maxtimeAssumeSystemWorks_milliseconds) {
							System.out.println("Evaluation active File found, but to old (in sec: " + ((nowdate_milliseconds - filedate_milliseconds)/1000) + "), it will be deleted");
							if(muninosmoverpassPathandFilenameHandle.delete())
								System.out.println("Info: Munin osm overpass file was killed at program end correctly");
							else
								System.out.println("ERROR: Munin osm overpass file couldn't be killed at program end correctly, filename was " + muninosmoverpassPathandFilename);
						}
					}
					System.out.println("Evaluation already active, stopp processing of this program");
					return;
				}
			}
			workprogressOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(importworkPathandFilename, true),StandardCharsets.UTF_8)));
			workprogressOutput.println("Start of Evaluation: " + dateformat.format(new Date()));
			workprogressOutput.close();
//TODO working file workprogressOutput should be updated with jobs, which have been finished
		
		} catch (IOException e) {
			System.out.println("Fehler beim Logging-Handler erstellen, ...");
			System.out.println(e.toString());
		}

		for (int lfdnr = 0; lfdnr < args.length; lfdnr++) {
			logger.log(Level.FINE, "args[" + lfdnr + "] ===" + args[lfdnr] + "===");
		}

		try {
			parameterAdminHierarchy = parameterAdminHierarchy.replace("*", "%");
			parameterMunicipiality = parameterMunicipiality.replace("*", "%");
			parameterJobname = parameterJobname.replace("*", "%");
			parameterOfficialkeysId = parameterOfficialkeysId.replace("*", "%");
	
			HousenumberlistReader hnrreader = new HousenumberlistReader();
			OsmDataReader osmreader = new OsmDataReader();
			Evaluation evaluation = new Evaluation();
	
			evaluation.setMunicipality(parameterCountry, "", parameterMunicipiality);
			evaluation.setHousenumberAdditionCaseSensity(parameterHousenumbersCaseSensity);
			evaluation.setUselanguagecode(parameterLanguagecode);
	
			//evaluation.setOnlyPartOfStreetnameSeparator(" - ");	// special settings for addr:street Values, when there are two languages in one value
			//evaluation.setOnlyPartOfStreetnameIndexNo(1);			// special settings for addr:street Values, when there are two languages in one value
	
	
			HousenumberCollection list_housenumbers = new HousenumberCollection();
			HousenumberCollection osm_housenumbers = new HousenumberCollection();
			HousenumberCollection evaluated_housenumbers = new HousenumberCollection();
	
				// local file, work offline (only connect to osm data via overpass
			if(! parameterImportdateiname.equals("")) {
				// client-server mode, connect to regio-osm.de Server and API and get jobs
				if(parameterCountry.equals("Netherland")) {
					list_housenumbers.setFieldsForUniqueAddress(HousenumberCollection.FieldsForUniqueAddress.STREET_POSTCODE_HOUSENUMBER);
					list_housenumbers.addFieldsForUniqueAddress("NetherlandAlternative", HousenumberCollection.FieldsForUniqueAddress.STREET_HOUSENUMBER);
				} else {
					list_housenumbers.setFieldsForUniqueAddress(HousenumberCollection.FieldsForUniqueAddress.STREET_HOUSENUMBER);
					list_housenumbers.setAlternateFieldsForUniqueAddress(null);
				}
				list_housenumbers = hnrreader.ReadListFromFile(evaluation, parameterImportdateiname, parameterFieldSeparator, parameterHousenumbersCaseSensity);
				osm_housenumbers.setFieldsForUniqueAddress(list_housenumbers.getFieldsForUniqueAddress());
				HousenumberCollection tempreceived_osm_housenumbers = osmreader.ReadData(evaluation, osm_housenumbers, parameterOSMRelationid);

				osmoverpassOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(muninosmoverpassPathandFilename),StandardCharsets.UTF_8)));
				osmoverpassOutput.println(osmreader.getResponseStatesPrintable());
				osmoverpassOutput.close();

				if(tempreceived_osm_housenumbers != null) {
					osm_housenumbers = tempreceived_osm_housenumbers;
					evaluated_housenumbers = list_housenumbers.merge(osm_housenumbers, list_housenumbers.getAlternateFieldsForUniqueAddress());
					evaluated_housenumbers.printhtml("evaluation.html");
					logger.log(Level.INFO, "Number of housenumberlist entries after load of official housenumber list: "
						+ evaluation.housenumberlist.length() + "   " + evaluation.housenumberlist.count_unchanged());
				}
			} else {
				HousenumberServerAPI hnrserver = new HousenumberServerAPI();
				List<Job> jobs;
		
				if(parameterAllMissingJobs) {
					jobs = hnrserver.getMissingCountryJobs(parameterCountry);
				} else if(parameterGetQueueJobs) {
					if(parameterMaxJobs == -1)
						parameterMaxJobs = 5;
					jobs = hnrserver.getQueueJobs(evaluation, parameterQueueFilter, parameterMaxJobs);
				} else {
					jobs = hnrserver.findJobs(parameterCountry,parameterMunicipiality, parameterJobname, parameterOfficialkeysId, parameterAdminHierarchy);
				}
				logger.log(Level.INFO, "Number of Jobs received from Server: " + jobs.size());

					// initialize munin file with number of jobs and nothing done yet
				munininfoOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(munininfoPathandFilename),StandardCharsets.UTF_8)));
				munininfoOutput.println("0" + " " +  jobs.size() + " " + jobs.size());
				munininfoOutput.close();

				java.util.Date jobstart = new java.util.Date();
				java.util.Date jobend = new java.util.Date();

				osmreader.openDBConnection(configuration.db_osm2pgsql_url, configuration.db_osm2pgsql_username, configuration.db_osm2pgsql_password);
				
				for(int jobindex = 0; jobindex < jobs.size(); jobindex++) {
					if((parameterMaxJobs != -1) && (parameterMaxJobs < (jobindex + 1))) {
						logger.log(Level.INFO, "maximum number of jobs, as specified, arrived. Stop further processing");
						break;
					}
					if(parameterMaxMinutes != -1) {
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
					evaluation.setJobData(actjob);
					
					if(parameterOsmDatasource.equals("")) {
						if(evaluation.getCountry().equals("Ísland")) {
							evaluation.setOsmdatasource("db");
							logger.log(Level.INFO, "for active Job, the local osm db will be used to get osm data");
						} else
							evaluation.setOsmdatasource("overpass");
					} else {
						evaluation.setOsmdatasource(parameterOsmDatasource);
					}
					
					//TODO change this fix code to other kind of coding
						// FIX Code for Italia, Region Südtirol: for jobs in this area, use german language code
					if(	evaluation.getCountry().equals("Italia")) {
						if(evaluation.officialkeysId.substring(0,3).equals("021"))
							evaluation.setUselanguagecode("DE");
						else
							evaluation.setUselanguagecode(evaluation.getJobCountrycode());
					} else
						evaluation.setUselanguagecode(evaluation.getJobCountrycode());

						

					jobstart = new java.util.Date();
					logger.log(Level.INFO, "start working on job " + actjob.toString() + "; started at " + jobstart.toString());
		
					evaluation.setHousenumberAdditionCaseSensity(false);
	
					list_housenumbers.clear();
					osm_housenumbers.clear();
					evaluated_housenumbers.clear();
					
					
					java.util.Date dbloadstart = new java.util.Date();
					list_housenumbers.clear();
					//list_housenumbers = hnrreader.ReadListFromDB(evaluation);
					if(parameterCountry.equals("Netherland")) {
						list_housenumbers.setFieldsForUniqueAddress(HousenumberCollection.FieldsForUniqueAddress.STREET_POSTCODE_HOUSENUMBER);
						list_housenumbers.setAlternateFieldsForUniqueAddress(HousenumberCollection.FieldsForUniqueAddress.POSTCODE_HOUSENUMBER);
					} else {
						list_housenumbers.setFieldsForUniqueAddress(HousenumberCollection.FieldsForUniqueAddress.STREET_HOUSENUMBER);
						list_housenumbers.setAlternateFieldsForUniqueAddress(null);
					}
if(parameterMunicipiality.equals("Köln")) {
	list_housenumbers.setFieldsForUniqueAddress(HousenumberCollection.FieldsForUniqueAddress.STREET_POSTCODE_HOUSENUMBER);
	list_housenumbers.addFieldsForUniqueAddress("NetherlandAlternative", HousenumberCollection.FieldsForUniqueAddress.STREET_HOUSENUMBER);
}
					list_housenumbers = hnrserver.ReadListFromServer(evaluation, list_housenumbers);
					logger.log(Level.INFO, "Number of official housenumbers: " + list_housenumbers.length());
					if(list_housenumbers.length() == 0) {
						logger.log(Level.WARNING, "Warning: job will be ignored, because no official housenumbers found for job " + actjob.toString() + "; started at " + jobstart.toString());
						continue;
					}
					java.util.Date dbloadend = new java.util.Date();
					java.util.Date overpassloadstart = new java.util.Date();
					osm_housenumbers.clear();
					osm_housenumbers.setFieldsForUniqueAddress(list_housenumbers.getFieldsForUniqueAddress());
					osm_housenumbers.setAlternateFieldsForUniqueAddress(list_housenumbers.getAlternateFieldsForUniqueAddress());
	
					HousenumberCollection tempreceived_osm_housenumbers = osmreader.ReadData(evaluation, osm_housenumbers, actjob.osmrelationid);

					osmoverpassOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(muninosmoverpassPathandFilename),StandardCharsets.UTF_8)));
					osmoverpassOutput.println(osmreader.getResponseStatesPrintable());
					osmoverpassOutput.close();

					if(tempreceived_osm_housenumbers == null) {
						logger.log(Level.WARNING, "Warning: job will be ignored, because request to overpass for osm housenumbers failed for job " + actjob.toString() + "; started at " + jobstart.toString());
						continue;
					}
					osm_housenumbers = tempreceived_osm_housenumbers;
					logger.log(Level.INFO, "Number of OSM housenumbers: " + osm_housenumbers.length());
					evaluation.evaluationtime = overpassloadstart.getTime();
					java.util.Date overpassloadend = new java.util.Date();
					java.util.Date mergedatastart = new java.util.Date();
					evaluated_housenumbers.clear();
					evaluated_housenumbers = list_housenumbers.merge(osm_housenumbers, list_housenumbers.getAlternateFieldsForUniqueAddress());
					java.util.Date mergedataend = new java.util.Date();
					evaluated_housenumbers.printhtml("test.html");
		
					java.util.Date uploadstart = null;
					java.util.Date uploadend = null;
					uploadstart = new java.util.Date();
					hnrserver.writeEvaluationToServer(evaluation, evaluated_housenumbers);
					uploadend = new java.util.Date();
						
					String importworkoutputline = "";
					importworkoutputline = actjob.municipality + "/" + actjob.jobname + "\t" + dateformat.format(jobstart) + "\t";
					importworkoutputline += dateformat.format(uploadend) + "\t" + "successful";
					workprogressOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(importworkPathandFilename, true),StandardCharsets.UTF_8)));
					workprogressOutput.println(importworkoutputline);
					workprogressOutput.close();


					munininfoOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(munininfoPathandFilename),StandardCharsets.UTF_8)));
					munininfoOutput.println((jobindex + 1) + " " +  (jobs.size() - jobindex - 1) + " " + jobs.size());
					munininfoOutput.close();

					logger.log(Level.INFO, "time for db load time in sek: " + (dbloadend.getTime() - dbloadstart.getTime())/1000);
					logger.log(Level.INFO, "time for overpass load time in sek: " + (overpassloadend.getTime() - overpassloadstart.getTime())/1000);
					logger.log(Level.INFO, "time for internal merge time in sek: " + (mergedataend.getTime() - mergedatastart.getTime())/1000);
					logger.log(Level.INFO, "time for result upload time in sek: " + (uploadend.getTime() - uploadstart.getTime())/1000);

					jobend = new java.util.Date();
					logger.log(Level.INFO, "finished working on job " + actjob.toString() + "; ended at " + jobend.toString() + " duration in sec: " 
						+ (jobend.getTime() - jobstart.getTime())/1000);
				}
			}	// end of else case (client/serer mode)
			if(importworkPathandFilenameHandle.exists() && !importworkPathandFilenameHandle.isDirectory()) {
				String destinationworkPathandFilename = "evaluation.finished";
				File destinationworkPathandFilenameHandle = new File(destinationworkPathandFilename);
				if(importworkPathandFilenameHandle.renameTo(destinationworkPathandFilenameHandle))
					System.out.println("Batchimport progress file renamed to finish-state");
				else {
					System.out.println("ERROR: Batchimport progress file couldn't renamed to finish-state !!!");
					if(importworkPathandFilenameHandle.delete())
						System.out.println("Info: Batchimport progress file was killed as fallback, because it couldn't renamed to finish-state");
					else
						System.out.println("ERROR: Batchimport progress file couldn't be deleted, housenumberclient will be locked  !!!");
				}
			}
			osmreader.printTimeDurations();
			osmreader.closeDBConnection();

				// delete munin report file at program end
			File muninosmoverpassPathandFilenameHandle = new File(muninosmoverpassPathandFilename);
			if(muninosmoverpassPathandFilenameHandle.exists() && !muninosmoverpassPathandFilenameHandle.isDirectory()) {
				if(muninosmoverpassPathandFilenameHandle.delete())
					System.out.println("Info: Munin osm overpass file was killed at program end correctly");
				else
					System.out.println("ERROR: Munin osm overpass file couldn't be killed at program end correctly, filename was " + muninosmoverpassPathandFilename);
			}

			java.util.Date programEnd = new java.util.Date();
			logger.log(Level.INFO, "Program finished at " + programEnd.toString() + ", Duration in sec: "
				+ (programEnd.getTime() - programStart.getTime())/1000);
		}
		catch (Exception e) {
			System.out.println("ERROR: Generic Exception happened in main client, Details ...");
			e.printStackTrace();

			if(importworkPathandFilenameHandle.exists() && !importworkPathandFilenameHandle.isDirectory()) {
				String destinationworkPathandFilename = "evaluation.finished";
				File destinationworkPathandFilenameHandle = new File(destinationworkPathandFilename);
				if(importworkPathandFilenameHandle.renameTo(destinationworkPathandFilenameHandle))
					System.out.println("Batchimport progress file renamed to finish-state");
				else {
					System.out.println("ERROR: Batchimport progress file couldn't renamed to finish-state !!!");
					if(importworkPathandFilenameHandle.delete())
						System.out.println("Info: Batchimport progress file was killed as fallback, because it couldn't renamed to finish-state");
					else
						System.out.println("ERROR: Batchimport progress file couldn't be deleted, housenumberclient will be locked  !!!");
				}
			}
			return;
		}
	}
}
