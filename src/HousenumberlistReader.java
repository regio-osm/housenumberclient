import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;



/**
 * @author Dietmar Seifert
 *
 * evaluate the housenumbers in a municipality or a subregion of it, check againsts an available housenumerlist
 * 	
*/




public class HousenumberlistReader {
	private static final int HAUSNUMMERSORTIERBARLENGTH = 4;
	
	String dbconnection = "";
	String dbusername = "";
	String dbpassword = "";

	public void setDBConnection(String dbconnection, String dbusername, String dbpassword) {
		this.dbconnection = dbconnection;
		this.dbusername = dbusername;
		this.dbpassword = dbpassword;
	}


	public void ReadListFromDB(Evaluation evaluation) {
		if(		(dbconnection.equals("")) 
			||	(dbusername.equals(""))
			||	(dbpassword.equals(""))
			||	(evaluation.getCountry().equals(""))
			||	(evaluation.getMunicipality().equals(""))
			||	(evaluation.getJobname().equals(""))
			)
		{
			return;
		}

		String sqlqueryofficialhousenumbers = "";

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}

		try {
			Connection conhousenumberdb = DriverManager.getConnection(dbconnection, dbusername, dbpassword);
		
			sqlqueryofficialhousenumbers = "SELECT strasse,";
			sqlqueryofficialhousenumbers  += " sh.hausnummer AS hausnummer,";
			sqlqueryofficialhousenumbers  += " sh.hausnummer_sortierbar AS hausnummer_sortierbar,";
			sqlqueryofficialhousenumbers  += " str.id AS strasse_id, strasse";
			sqlqueryofficialhousenumbers  += " FROM stadt_hausnummern AS sh,";
			sqlqueryofficialhousenumbers  += " strasse AS str,";
			sqlqueryofficialhousenumbers  += " stadt AS s,";
			sqlqueryofficialhousenumbers  += " land as l";
			sqlqueryofficialhousenumbers  += " WHERE";
	//TODO enable subarea selection within municipality, if official housenumberlist contains subadmin name
	/*		if ((rs_jobs.getString("subids_separat").equals("y")) && (job_id != -1)) {
				logger.log(Level.FINEST, "aktueller Job ist innerhalb einer größeren Municipality mit sub_id besetzt, ");
				logger.log(Level.FINEST, "(cont) also von Stadt-Hausnummernliste nur ggfs. Teile holen ...");
				sqlqueryofficialhousenumbers  += " (sub_id = '"  + job_id  + "' OR sub_id = '-1') AND";
			}
	*/
			sqlqueryofficialhousenumbers  += " land = '" + evaluation.getCountry() + "'";
			sqlqueryofficialhousenumbers  += " AND stadt = '" + evaluation.getMunicipality() + "'";
			sqlqueryofficialhousenumbers  += " AND sh.land_id = l.id";
			sqlqueryofficialhousenumbers  += " AND sh.stadt_id = s.id";
			sqlqueryofficialhousenumbers  += " AND sh.strasse_id = str.id";
			sqlqueryofficialhousenumbers += " ORDER BY correctorder(strasse), hausnummer_sortierbar;";
			Statement stmtqueryofficialhousenumbers = conhousenumberdb.createStatement();
			ResultSet rsqueryofficialhousenumbers = stmtqueryofficialhousenumbers.executeQuery(sqlqueryofficialhousenumbers);
	
			String tempAkthausnummer = "";
			while (rsqueryofficialhousenumbers.next()) {
				tempAkthausnummer = rsqueryofficialhousenumbers.getString("hausnummer_sortierbar");
				tempAkthausnummer = tempAkthausnummer.substring(1, HAUSNUMMERSORTIERBARLENGTH);

				Workcache_Entry newofficialhousenumber = new Workcache_Entry(evaluation.getHousenumberlist().ishousenumberadditionCaseSentity());

				newofficialhousenumber.setStrasse(rsqueryofficialhousenumbers.getString("strasse"));
				newofficialhousenumber.setHausnummer(rsqueryofficialhousenumbers.getString("hausnummer"));
				newofficialhousenumber.setTreffertyp(Workcache_Entry.Treffertyp.LIST_ONLY);
	
				evaluation.housenumberlist.update(newofficialhousenumber);
			} // Ende sql-Schleife über alle Stadt-only-Hausnummern der aktuellen Strasse - while(rsqueryofficialhousenumbers.next()) {
	
			for(int loadindex=0; loadindex < evaluation.housenumberlist.length(); loadindex++) {
				Workcache_Entry aktcacheentry = evaluation.housenumberlist.entry(loadindex);
			}
	
		
		} catch (SQLException e) {
			System.out.println("ERROR: during select table auswertung_hausnummern, sqlquery was ===" 
				+ sqlqueryofficialhousenumbers + "===");
			e.printStackTrace();
		}
	}
}
