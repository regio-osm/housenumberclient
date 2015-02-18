package de.regioosm.housenumbers;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;
import java.util.logging.Level;


public class Applicationconfiguration {

	public String housenumberseverAPIprotocol = "http";
	public String housenumberseverAPIhost = "regio-osm.de";
	public String housenumberseverAPIport = "80";
	public String housenumberseverAPIrooturl = "/housenumberserverAPI";
	
		// common parameters for all evaluation programs, still not checked for use in housenumberclient
	public String servername = "";
	public String application_homedir = "";
	public String application_datadir = "";
	public String db_structure = "osm2pgsql";
	public String db_application_url = "";
	public String db_application_username = "";
	public String db_application_password = "";
	public String db_application_listofstreets_url = "";
	public String db_application_listofstreets_username = "";
	public String db_application_listofstreets_password = "";
	public String db_osm2pgsql_url = "";
	public String db_osm2pgsql_username = "";
	public String db_osm2pgsql_password = "";
	public String osmosis_laststatefile = "";
	public String logging_filename = "";
	public Level logging_console_level = Level.FINEST;
	public Level logging_file_level = Level.FINEST;
	
	public Applicationconfiguration (String path) {
			// get some configuration infos
		String configuration_filename =  path + ".." + File.separator + ".." + File.separator + "housenumberclient.properties";

	try {
			Reader reader = new FileReader( configuration_filename );
			Properties prop = new Properties();
			prop.load( reader );
			prop.list( System.out );
		

			if( prop.getProperty("housenumberseverAPIprotocol") != null)
				this.housenumberseverAPIprotocol = prop.getProperty("housenumberseverAPIprotocol");
			if( prop.getProperty("housenumberseverAPIhost") != null)
				this.housenumberseverAPIhost = prop.getProperty("housenumberseverAPIhost");
			if( prop.getProperty("housenumberseverAPIport") != null)
				this.housenumberseverAPIport = prop.getProperty("housenumberseverAPIport");
			if( prop.getProperty("housenumberseverAPIrooturl") != null)
				this.housenumberseverAPIrooturl = prop.getProperty("housenumberseverAPIrooturl");

			if( prop.getProperty("servername") != null)
				this.servername = prop.getProperty("servername");
			if( prop.getProperty("db_structure") != null)
				this.db_structure = prop.getProperty("db_structure");
			if( prop.getProperty("application_homedir") != null)
				this.application_homedir = prop.getProperty("application_homedir");
			if( prop.getProperty("application_datadir") != null)
				this.application_datadir = prop.getProperty("application_datadir");
			if( prop.getProperty("db_application_url") != null)
				this.db_application_url = prop.getProperty("db_application_url");
			if( prop.getProperty("db_application_username") != null)
				this.db_application_username = prop.getProperty("db_application_username");
			if( prop.getProperty("db_application_password") != null)
				this.db_application_password = prop.getProperty("db_application_password");
			if( prop.getProperty("db_application_listofstreets_url") != null)
				this.db_application_listofstreets_url = prop.getProperty("db_application_listofstreets_url");
			if( prop.getProperty("db_application_listofstreets_username") != null)
				this.db_application_listofstreets_username = prop.getProperty("db_application_listofstreets_username");
			if( prop.getProperty("db_application_listofstreets_password") != null)
				this.db_application_listofstreets_password = prop.getProperty("db_application_listofstreets_password");
			if( prop.getProperty("db_osm2pgsql_url") != null)
				this.db_osm2pgsql_url = prop.getProperty("db_osm2pgsql_url");
			if( prop.getProperty("db_osm2pgsql_username") != null)
				this.db_osm2pgsql_username = prop.getProperty("db_osm2pgsql_username");
			if( prop.getProperty("db_osm2pgsql_password") != null)
				this.db_osm2pgsql_password = prop.getProperty("db_osm2pgsql_password");


			if( prop.getProperty("osmosis_laststatefile") != null)
				this.osmosis_laststatefile = prop.getProperty("osmosis_laststatefile");
			if( prop.getProperty("logging_filename") != null)
				this.logging_filename = prop.getProperty("logging_filename");
			if( prop.getProperty("logging_console_level") != null)
				this.logging_console_level = Level.parse(prop.getProperty("logging_console_level"));
			if( prop.getProperty("logging_file_level") != null)
				this.logging_file_level = Level.parse(prop.getProperty("logging_file_level"));

			
			System.out.println(" .housenumberseverAPIprotocol             ==="+this.housenumberseverAPIprotocol+"===");
			System.out.println(" .housenumberseverAPIhost                 ==="+this.housenumberseverAPIhost+"===");
			System.out.println(" .housenumberseverAPIport                 ==="+this.housenumberseverAPIport+"===");
			System.out.println(" .housenumberseverAPIrooturl              ==="+this.housenumberseverAPIrooturl+"===");
			
			
			System.out.println(" .servername                              ==="+this.servername+"===");
			System.out.println(" .application_homedir                     ==="+this.application_homedir+"===");
			System.out.println(" .application_datadir                     ==="+this.application_datadir+"===");
			System.out.println(" .db_application_url                      ==="+this.db_application_url+"===");
			System.out.println(" .db_application_username                 ==="+this.db_application_username+"===");
			System.out.println(" .db_application_password                 ==="+this.db_application_password+"===");
			System.out.println(" .db_application_listofstreets_url        ==="+this.db_application_listofstreets_url+"===");
			System.out.println(" .db_application_listofstreets_username   ==="+this.db_application_listofstreets_username+"===");
			System.out.println(" .db_application_listofstreets_password   ==="+this.db_application_listofstreets_password+"===");
			System.out.println(" .db_osm2pgsql_url                        ==="+this.db_osm2pgsql_url+"===");
			System.out.println(" .db_osm2pgsql_username                   ==="+this.db_osm2pgsql_username+"===");
			System.out.println(" .db_osm2pgsql_password                   ==="+this.db_osm2pgsql_password+"===");
			System.out.println(" .osmosis_laststatefile                   ==="+this.osmosis_laststatefile+"===");
			System.out.println(" .logging_filename                        ==="+this.logging_filename +"===");
			System.out.println(" .logging_console_level                   ==="+this.logging_console_level.toString() +"===");
			System.out.println(" .logging_file_level                      ==="+this.logging_file_level.toString() +"===");

		} catch (Exception e) {
			System.out.println("ERROR: failed to read file ==="+configuration_filename+"===");
			e.printStackTrace();
			return;
		}
	}
}
