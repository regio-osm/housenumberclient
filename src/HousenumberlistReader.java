import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;



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
			Statement queryofficialhousenumbersStmt = conHousenumbers.createStatement();
			ResultSet rsqueryofficialhousenumbers = queryofficialhousenumbersStmt.executeQuery(sqlqueryofficialhousenumbers);
	
			while (rsqueryofficialhousenumbers.next()) {

				Housenumber newofficialhousenumber = new Housenumber(evaluation.getHousenumberlist().ishousenumberadditionCaseSentity());

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
}
