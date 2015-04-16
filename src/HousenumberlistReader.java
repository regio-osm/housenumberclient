import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * @author Dietmar Seifert
 *
 * evaluate the housenumbers in a municipality or a subregion of it, check againsts an available housenumerlist
 * 	
*/




public class HousenumberlistReader {
	private static final int HAUSNUMMERSORTIERBARLENGTH = 4;
	private static final Logger logger = Evaluation.logger;
	
	String dbconnection = "";
	String dbusername = "";
	String dbpassword = "";

	public void setDBConnection(String dbconnection, String dbusername, String dbpassword) {
		this.dbconnection = dbconnection;
		this.dbusername = dbusername;
		this.dbpassword = dbpassword;
	}


	public HousenumberCollection ReadListFromDB(Evaluation evaluation) {
		final HousenumberCollection housenumbers = new HousenumberCollection();

		if(		(dbconnection.equals("")) 
			||	(dbusername.equals(""))
			||	(dbpassword.equals(""))
			||	(evaluation.getCountry().equals(""))
			||	(evaluation.getMunicipality().equals(""))
			||	(evaluation.getJobname().equals(""))
			)
		{
			return housenumbers;
		}

		String sqlqueryofficialhousenumbers = "";

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Failed to load postgres DB-Driver, Details follows ...");
			logger.log(Level.SEVERE, e.toString());
			e.printStackTrace();
			return housenumbers;
		}

		try {
			Connection conHousenumbers = DriverManager.getConnection(dbconnection, dbusername, dbpassword);

				// check, if job needs to get all housenumbers or just a part of the municipality,
				// if official housenuber list contains subarea context for housenumbers
			String sqlbefehlJobs = "SELECT subareasidentifyable as subids_separat,";
			sqlbefehlJobs += " admin_level, ST_AsText(ST_Transform(polygon,4326)) AS polygongeometryastext,";
			sqlbefehlJobs += " housenumberaddition_exactly";
			sqlbefehlJobs += " FROM jobs, gebiete, stadt, land WHERE";
			sqlbefehlJobs += " stadt.id = jobs.stadt_id AND stadt.land_id = jobs.land_id AND land.id = jobs.land_id";
			sqlbefehlJobs += " AND jobs.gebiete_id = gebiete.id";
			if (!evaluation.getCountry().equals("")) {
				sqlbefehlJobs  += " AND land.land like '"  + evaluation.getCountry() + "'";
			}
//TODO preparedStatement
			if (!evaluation.getMunicipality().equals("")) {
				sqlbefehlJobs  += " AND stadt.stadt like '"  + evaluation.getMunicipality().replace("'", "''") + "'";
			}
			if (!evaluation.getJobname().equals("")) {
				sqlbefehlJobs  += " AND gebiete.name like '"  + evaluation.getJobname().replace("'", "''") + "'";
			}
			sqlbefehlJobs  += " ORDER BY land.land, stadt.stadt, jobname;";

			boolean subids_separat = false;
			Statement queryjobSmt = conHousenumbers.createStatement();
			ResultSet queryJobRS = queryjobSmt.executeQuery(sqlbefehlJobs);
			while (queryJobRS.next()) {
				housenumbers.adminLevel = queryJobRS.getInt("admin_level");
				if(queryJobRS.getString("subids_separat").equals("y"))
					subids_separat = true;
				housenumbers.polygonAsString = queryJobRS.getString("polygongeometryastext");
			}

			sqlqueryofficialhousenumbers = "SELECT strasse,";
			sqlqueryofficialhousenumbers  += " sh.hausnummer AS hausnummer,";
			sqlqueryofficialhousenumbers  += " sh.hausnummer_sortierbar AS hausnummer_sortierbar,";
			sqlqueryofficialhousenumbers  += " str.id AS strasse_id, strasse";
			sqlqueryofficialhousenumbers  += " FROM stadt_hausnummern AS sh,";
			sqlqueryofficialhousenumbers  += " strasse AS str,";
			sqlqueryofficialhousenumbers  += " stadt AS s,";
			sqlqueryofficialhousenumbers  += " land as l";
			sqlqueryofficialhousenumbers  += " WHERE";
			if (!evaluation.getMunicipality().equals(evaluation.getJobname()) && subids_separat) {
				sqlqueryofficialhousenumbers  += " (sub_id = '"  + evaluation.getSubid().replace("'","''") + "' OR sub_id = '-1') AND";
			}
			sqlqueryofficialhousenumbers  += " land = '" + evaluation.getCountry() + "'";
			sqlqueryofficialhousenumbers  += " AND stadt = '" + evaluation.getMunicipality().replace("'","''") + "'";
			sqlqueryofficialhousenumbers  += " AND sh.land_id = l.id";
			sqlqueryofficialhousenumbers  += " AND sh.stadt_id = s.id";
			sqlqueryofficialhousenumbers  += " AND sh.strasse_id = str.id";
			sqlqueryofficialhousenumbers += " ORDER BY correctorder(strasse), hausnummer_sortierbar;";
			logger.log(Level.FINE, "SQL-Query to get all list housenumber entries ===" + sqlqueryofficialhousenumbers + "===");
			Statement queryofficialhousenumbersStmt = conHousenumbers.createStatement();
			ResultSet rsqueryofficialhousenumbers = queryofficialhousenumbersStmt.executeQuery(sqlqueryofficialhousenumbers);
	
			while (rsqueryofficialhousenumbers.next()) {

				Housenumber newofficialhousenumber = new Housenumber(housenumbers);

				newofficialhousenumber.setStrasse(rsqueryofficialhousenumbers.getString("strasse"));
				newofficialhousenumber.setHausnummer(rsqueryofficialhousenumbers.getString("hausnummer"));
				newofficialhousenumber.setTreffertyp(Housenumber.Treffertyp.LIST_ONLY);
	
				housenumbers.add_newentry(newofficialhousenumber);
			} // Ende sql-Schleife über alle Stadt-only-Hausnummern der aktuellen Strasse - while(rsqueryofficialhousenumbers.next()) {
	
			queryjobSmt.close();
			queryofficialhousenumbersStmt.close();
			conHousenumbers.close();
		
		} catch (SQLException e) {
			System.out.println("ERROR: during select table auswertung_hausnummern, sqlquery was ===" 
				+ sqlqueryofficialhousenumbers + "===");
			e.printStackTrace();
		}

		return housenumbers;
	}

	/**
	 * Read a local housenumber list file
	 * Up to now (2015-04), only text files without geocoordinates will be processed)
	 * The local file must have one line for every housenumber.
	 * The file must start with a first comment line, starting with "#" and following column header field names (case insensitive)
	 * "Stadt" or "Gemeinde": CAUTION WILL NOT BE SUPOORTED UP TO NOW; Original: name of municipality, if file contains more than ohne municipality
	 * "Stadtid" or "Gemeindeid" or "Gemeinde-Id" or "Gemeinde_Id": CAUTION WILL NOT BE SUPOORTED UP TO NOW; Original: official municipality id, in Germany Gemeindeschlüssel. This Id must be available in administrative polygon in OSM for the municipaltity
	 * "Straße" or "Strasse": name of the street, the housenumber belongs to
	 * "PLZ" or "Postcode" or "Postleitzahl": CAUTION WILL NOT BE SUPOORTED UP TO NOW; Original: postcode for the housenumber belongs to
	 * "Hausnummer": the housenumber. Either complete with suffix or the numeric part
	 * "Hausnumerzusatz": suffix for the housenumber, if available: housenumber is numeric only in column housenumber
	 * "Hausnummerzusatz2": second suffix for the housenumber, if available:  in seldom cases, a housenumber like 11 1/2 a is separated to housenumber 11, hausnummerzusatz 1/2 and hausnummerzusatz2 a
	 * "Subid" or "Sud-Id" or "Sub_Id": CAUTION WILL NOT BE SUPOORTED UP TO NOW; Original: name or id of subarea in municipality, in Germany Stadtteil, Stadtbezirk. Only one subarea will be supported for a municipality
	 * "lon" or "Längengrad" CAUTION: WILL NOT BE SUPPORTED UP TO NOW
	 * "lat" or "Breitengrad": CAUTION: WILL NOT BE SUPPORTED UP TO NOW
	 * A active housenumber file line must have at least values in columns street and housenumber
	 * All other columns are optionally
	 * 
	 * @param evaluation			information about actual country, municipality and so on. Up to now not necessary be set for load local file 
	 * @param localfilename			filename of local file, including relative oder absolute path
	 * @param filecolumnseparator	file column separator. If empty string, TAB-character will be used
	 * @param housenumberexactly	true, if alphabetically housneumber suffixes should be used case sensity or case-insensity (false)
	 * @return						return the housenumbers in HousenumberCache map structure
	 */
	public HousenumberCollection ReadListFromFile(Evaluation evaluation, String localfilename, String filecolumnseparator, boolean housenumberexactly) {
		final HousenumberCollection housenumbers = new HousenumberCollection();

		String fieldSeparator = "\t";
		if(! filecolumnseparator.equals(""))
			fieldSeparator = filecolumnseparator;

		try {
			BufferedReader filereader = new BufferedReader(new FileReader(localfilename));

			String dateizeile;
			long zeilenr = 0;

			boolean geocoordindatesavailable = false;
			int spaltenindexStadt = -1;
			int spaltenindexAgs = -1;
			int spaltenindexStrasse = 0;
			int spaltenindexPostcode = -1;
			int spaltenindexHausnummer = -1;
			int spaltenindexHausnummerzusatz = -1;
			int spaltenindexHausnummerzusatz2 = -1;
			int spaltenindexBemerkung = -1;
			int spaltenindexSubid = -1;
			int spaltenindexLaengengrad = -1;
			int spaltenindexBreitengrad = -1;
			String stadt_zuletzt = "";
			String stadt = "";
			String ags = "";
			String ags_zuletzt = "";
			String strasse = "";
			String postcode = "";
			String hausnummer = "";
			String bemerkung = "";
			double laengengrad = 0f;
			double breitengrad = 0f;

			String subid = "-1";
			while ((dateizeile = filereader.readLine()) != null) {
				zeilenr++;
				
				if (dateizeile.equals("")) {
					continue;
				}
				if (dateizeile.indexOf("#") == 0) {
					if (zeilenr == 1L) {
						logger.log(Level.FINE, "First comment line, should have column header fields, line is ===" + dateizeile + "===");						
						dateizeile = dateizeile.substring(1);
						String[] kopfspalten = dateizeile.split(fieldSeparator);
						for (int spaltei = 0; spaltei < kopfspalten.length; spaltei++) {
							if(		kopfspalten[spaltei].toLowerCase().equals("stadt")
								||	kopfspalten[spaltei].toLowerCase().equals("gemeinde")) {
								spaltenindexStadt = spaltei;
							}
							if(		kopfspalten[spaltei].toLowerCase().equals("stadtid")
								||	kopfspalten[spaltei].toLowerCase().equals("gemeindeid")
								||	kopfspalten[spaltei].toLowerCase().equals("gemeinde_id")
								||	kopfspalten[spaltei].toLowerCase().equals("gemeinde-id")) {
								spaltenindexAgs = spaltei;
							}
							if (	kopfspalten[spaltei].toLowerCase().equals("straße")
								||	kopfspalten[spaltei].toLowerCase().equals("strasse")) {
								spaltenindexStrasse = spaltei;
							}
							if (	kopfspalten[spaltei].toLowerCase().equals("postcode")
								||	kopfspalten[spaltei].toLowerCase().equals("plz")
								||	kopfspalten[spaltei].toLowerCase().equals("postleitzahl")) {
								spaltenindexPostcode = spaltei;
							}
							if (kopfspalten[spaltei].toLowerCase().equals("hausnummer")) {
								spaltenindexHausnummer = spaltei;
							}
							if (kopfspalten[spaltei].toLowerCase().equals("hausnummerzusatz")) {
								spaltenindexHausnummerzusatz = spaltei;
							}
							if (kopfspalten[spaltei].toLowerCase().equals("hausnummerzusatz2")) {
								spaltenindexHausnummerzusatz2 = spaltei;
							}
							if (kopfspalten[spaltei].toLowerCase().equals("bemerkung")) {
								spaltenindexBemerkung = spaltei;
							}
							if (kopfspalten[spaltei].toLowerCase().equals("lon") ||
								kopfspalten[spaltei].toLowerCase().equals("längengrad") ||
								kopfspalten[spaltei].toLowerCase().equals("längengrad")) {
								spaltenindexLaengengrad = spaltei;
							}
							if (kopfspalten[spaltei].toLowerCase().equals("lat") ||
								kopfspalten[spaltei].toLowerCase().equals("breitengrad")) {
								spaltenindexBreitengrad = spaltei;
							}
							if(		kopfspalten[spaltei].toLowerCase().equals("subid")
								|| 	kopfspalten[spaltei].toLowerCase().equals("sub_id")
								|| 	kopfspalten[spaltei].toLowerCase().equals("sub-id")) {
								spaltenindexSubid = spaltei;
							}
						}

						logger.log(Level.INFO, "Kopfzeile analysiert ...");
						logger.log(Level.INFO, "      Spalte-Stadt: " + spaltenindexStadt);
						logger.log(Level.INFO, "      Spalte-Stadtid: " + spaltenindexAgs);
						logger.log(Level.INFO, "      Spalte-Straße: " + spaltenindexStrasse);
						if(spaltenindexPostcode != -1)
							logger.log(Level.INFO, "  Spalte-Postcode: " + spaltenindexPostcode);
						logger.log(Level.INFO, "  Spalte-Hausnummer: " + spaltenindexHausnummer);
						if(spaltenindexHausnummerzusatz != -1)
							logger.log(Level.INFO, "  Spalte-Hausnummerzusatz: " + spaltenindexHausnummerzusatz);
						if(spaltenindexHausnummerzusatz2 != -1)
							logger.log(Level.INFO, "  Spalte-Hausnummerzusatz2: " + spaltenindexHausnummerzusatz2);
						logger.log(Level.INFO, "   Spalte-Subid: " + spaltenindexSubid);
						if(spaltenindexLaengengrad != -1)
							logger.log(Level.INFO, "   Spalte-Längengrad: " + spaltenindexLaengengrad);
						if(spaltenindexBreitengrad != -1)
							logger.log(Level.INFO, "   Spalte-Breitengrad: " + spaltenindexBreitengrad);
					} else {
						logger.log(Level.FINE, "Dateizeile #" + zeilenr + "  war sonstige Kommentarzeile ===" + dateizeile + "===");
					}
					continue;
				}

				logger.log(Level.FINEST, "Dateizeile # " + zeilenr + " ===" + dateizeile + "===");

				String[] spalten = dateizeile.split("\t");

				stadt_zuletzt = stadt;
				stadt = "";
				if((spalten.length > spaltenindexStadt) && (spaltenindexStadt != -1)) {
					stadt = spalten[spaltenindexStadt].trim();
					if( (! stadt.equals(stadt_zuletzt)) && (! stadt_zuletzt.equals(""))) {
						logger.log(Level.WARNING, "more than one municipality found in file. Only first municipality was used for imported, rest of file will be ignored");
						return housenumbers;
					}
				}

				ags_zuletzt = ags;
				ags = "";
				if((spalten.length > spaltenindexAgs) && (spaltenindexAgs != -1)) {
					ags = spalten[spaltenindexAgs].trim();
					if( (! ags.equals(ags_zuletzt)) && (! ags_zuletzt.equals(""))) {
						logger.log(Level.WARNING, "WARNING: more than one municipality found in file. Only first municipality was used for imported, rest of file will be ignored");
						return housenumbers;
					}
				}

				Housenumber newhousenumber = new Housenumber(housenumbers);
				newhousenumber.setTreffertyp(Housenumber.Treffertyp.LIST_ONLY);
				
				strasse = "";
				if((spalten.length > spaltenindexStrasse) && (spaltenindexStrasse != -1)) {
					strasse = spalten[spaltenindexStrasse].trim();
					newhousenumber.setStrasse(strasse);
				}
				postcode = "";
				/*
				if((spalten.length > spaltenindexPostcode) && (spaltenindexPostcode != -1)) {
					postcode = spalten[spaltenindexPostcode].trim();
				}
				*/
				hausnummer = "";
				if((spalten.length > spaltenindexHausnummer) && (spaltenindexHausnummer != -1)) {
					hausnummer = spalten[spaltenindexHausnummer].trim();
				}
				if((spalten.length > spaltenindexHausnummerzusatz) && (spaltenindexHausnummerzusatz != -1)) {
					if(! spalten[spaltenindexHausnummerzusatz].trim().equals(""))
						hausnummer += spalten[spaltenindexHausnummerzusatz].trim();
				}
				if((spalten.length > spaltenindexHausnummerzusatz2) && (spaltenindexHausnummerzusatz2 != -1)) {
					if(! spalten[spaltenindexHausnummerzusatz2].trim().equals(""))
						hausnummer += "-" + spalten[spaltenindexHausnummerzusatz2].trim();
				}
				if(! hausnummer.equals(""))
					newhousenumber.setHausnummer(hausnummer);
				
				bemerkung = "";
				/*
				if((spalten.length > spaltenindexBemerkung) && (spaltenindexBemerkung != -1)) {
					bemerkung = spalten[spaltenindexBemerkung].trim();
				}
				*/

				subid = "-1";
				/*
				if ((spalten.length > spaltenindexSubid) && (spaltenindexSubid != -1)) {
					subid = spalten[spaltenindexSubid];
				}
				*/

				laengengrad = 0d;
				/*
				if ((spalten.length > spaltenindexLaengengrad) && (spaltenindexLaengengrad != -1)) {
					String localstring = spalten[spaltenindexLaengengrad].trim();
					try {
						localstring = localstring.replace(",",".");
						laengengrad = Double.parseDouble(localstring);
						geocoordindatesavailable = true;
					} catch (NumberFormatException nofloat) {
						System.out.println("Warning: cannot convert input Längengrad value as float ===" + spalten[spaltenindexLaengengrad].trim() + "===  preconvert to number ==="+localstring +"===");
						System.out.println(" (cont) error stack follows " + nofloat.toString());
					}
				}
				*/
				breitengrad = 0d;
				/*
				if ((spalten.length > spaltenindexBreitengrad) && (spaltenindexBreitengrad != -1)) {
					String localstring = spalten[spaltenindexBreitengrad].trim();
					try {
						localstring = localstring.replace(",",".");
						breitengrad = Double.parseDouble(localstring);
						geocoordindatesavailable = true;
					} catch (NumberFormatException nofloat) {
						System.out.println("Warning: cannot convert input Breitengrad value as float ===" + spalten[spaltenindexBreitengrad].trim() + "===  preconvert to number ==="+localstring +"===");
						System.out.println(" (cont) error stack follows " + nofloat.toString());
					}
				}
				*/
				
					// Zeile mit richtiger Spaltenanzahl, aber leere Spalte(n) überspringen
				if (strasse.equals("") || hausnummer.equals("")) {
					continue;
				}
				//System.out.print("# " + zeilenr + "  Straße ===" + strasse);
				//System.out.print(";   subid ===" + subid + "===");
				//System.out.println(";   Hausnummer ===" + hausnummer + "===");

				housenumbers.add_newentry(newhousenumber);
			}
			filereader.close();

	    } catch (FileNotFoundException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
		return housenumbers;
	}
}
