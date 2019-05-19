

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;




















import de.regioosm.housenumbers.Applicationconfiguration;
import de.zalando.typemapper.postgres.HStore;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;
import org.postgis.Point;

import java.text.ParseException;



/**
 * @author Dietmar Seifert
 *
 * evaluate the housenumbers in a municipality or a subregion of it, check againsts an available housenumerlist
 * 	
*/




public class OsmDataReader {
	private static final Integer MAXOVERPASSTRIES = 3;
	private static final Logger logger = Evaluation.logger;
	static Connection con_mapnik = null;
	static Connection con_housenumbers = null;

	Applicationconfiguration configuration = new Applicationconfiguration("");

	String dbconnection = "";
	String dbusername = "";
	String dbpassword = "";

	
	Integer nodes_count = 0;
	Integer ways_count = 0;
	Integer relations_count = 0;
	IdTracker availableNodes = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
	IdTracker availableWays = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
	IdTracker availableRelations = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
	
	TreeMap<Long, Node> gibmirnodes = new TreeMap<Long, Node>();
	TreeMap<Long, Way> gibmirways = new TreeMap<Long, Way>();
	TreeMap<Long, Relation> gibmirrelations = new TreeMap<Long, Relation>();

	Integer overpassResponseStates[] = new Integer[MAXOVERPASSTRIES + 2];
	
	Long pointsQueryDuration = 0L;
	Long pointsAnalyzeDuration = 0L;
	Long linesQueryDuration = 0L;
	Long linesAnalyzeDuration = 0L;
	Long polygonsQueryDuration = 0L;
	Long polygonsAnalyzeDuration = 0L;
	
	String land = "";
	Integer land_id = -1;
	Integer stadt_id = -1;

	public OsmDataReader() {
		for(Integer numindex = 0; numindex < MAXOVERPASSTRIES + 2; numindex++) {
			overpassResponseStates[numindex] = 0;
		}
	}

	private void setResponseState(Integer numberfailedtries) {
		if(numberfailedtries > (MAXOVERPASSTRIES + 1))
			numberfailedtries = MAXOVERPASSTRIES + 1;
		overpassResponseStates[numberfailedtries]++;
	}

	public String getResponseStatesPrintable() {
		String outputtext = "";
		Integer sum = 0;

		for(Integer numindex = 0; numindex < MAXOVERPASSTRIES + 2; numindex++) {
			sum += overpassResponseStates[numindex];
		}

		for(Integer numindex = 0; numindex < MAXOVERPASSTRIES + 2; numindex++) {
			if(numindex > 0)
				outputtext += "\t";
			if(sum == 0)
				outputtext += 0;
			else
				outputtext += Math.round(100 * overpassResponseStates[numindex] / sum);
		}
		return outputtext;
	}

	public void printTimeDurations() {
		logger.log(Level.INFO, "Time for lines DB query in msec: " + linesQueryDuration);
		logger.log(Level.INFO, "Time for lines Analyzing in msec: " + linesAnalyzeDuration);
		logger.log(Level.INFO, "Time for polygons DB query in msec: " + polygonsQueryDuration);
		logger.log(Level.INFO, "Time for polygons Analyzing in msec: " + polygonsAnalyzeDuration);
		logger.log(Level.INFO, "Time for points DB query in msec: " + pointsQueryDuration);
		logger.log(Level.INFO, "Time for points Analyzing in msec: " + pointsAnalyzeDuration);
	}
	
	public void openDBConnection(String dbconnection, String dbusername, String dbpassword) {
		this.dbconnection = dbconnection;
		this.dbusername = dbusername;
		this.dbpassword = dbpassword;

	
		try {
			if(con_mapnik == null) {
				String url_mapnik = dbconnection;
				con_mapnik = DriverManager.getConnection(url_mapnik, dbusername, dbpassword);
			}
			logger.log(Level.INFO, "Connection to database " + dbconnection + " established.");
		}
		catch (SQLException e) {
			logger.log(Level.SEVERE, "ERROR: failed to connect to database " + dbconnection);
			logger.log(Level.SEVERE, e.toString());
			System.out.println("ERROR: failed to connect to database " + dbconnection);
			System.out.println(e.toString());
			return;
		}
	}

	public void closeDBConnection() {
		try {
			if(con_mapnik != null) {
				logger.log(Level.INFO, "Connection to database" + con_mapnik.getMetaData().getURL() + "closed.");
				con_mapnik.close();
				con_mapnik = null;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			if(con_housenumbers != null) {
				logger.log(Level.INFO, "Connection to database" + con_housenumbers.getMetaData().getURL() + "closed.");
				con_housenumbers.close();
				con_housenumbers = null;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.dbconnection = "";
		this.dbusername = "";
		this.dbpassword = "";
	}
	
	private static String[] Hausnummernbereich_aufloesen(String hausnummertext) {
		List<String>	hausnummern_array = new ArrayList<String>();

		if(hausnummertext.indexOf("-") == -1) {
			hausnummern_array.add(hausnummertext);
		} else {
			try {
				Integer bindestrich_pos = hausnummertext.indexOf("-");
//TODO parseInt fails for 28d for example
				Integer hausnummer_start_int = Integer.parseInt(hausnummertext.substring(0,bindestrich_pos).trim());
				Integer hausnummer_ende_int = Integer.parseInt(hausnummertext.substring(bindestrich_pos+1).trim());
				if(hausnummer_ende_int > hausnummer_start_int) {
					for(Integer hausnummerindex=hausnummer_start_int; hausnummerindex <= hausnummer_ende_int; hausnummerindex+=2)
						hausnummern_array.add(hausnummerindex.toString());
				} else {
					hausnummern_array.add(hausnummertext);
				}					
			} catch( NumberFormatException e) {
				//logger.log(Level.WARNING, "Error during parsing text to integer, text was ==="+hausnummertext+"===");
				//e.printStackTrace();
				hausnummern_array.add(hausnummertext);
			}

		}
		String[] return_string_array = new String[hausnummern_array.size()];
		return_string_array = hausnummern_array.toArray(return_string_array);
		return return_string_array;
	}


	public HousenumberCollection ReadData(final Evaluation evaluation, final HousenumberCollection housenumbers, Long relationsid) {
		String osmdatasource = evaluation.getOsmdatasource();
		if(osmdatasource.equals("overpass")) {
			return ReadDataFromOverpass(evaluation, housenumbers, relationsid);
		} else if(osmdatasource.equals("db")) {
			if(con_mapnik == null)
				openDBConnection(this.dbconnection, this.dbusername, this.dbpassword);
			ReadDataFromDB(evaluation, housenumbers, relationsid);
			return housenumbers;
		} else {
			return new HousenumberCollection();
		}
	}


	private HousenumberCollection ReadDataFromOverpass(final Evaluation evaluation, final HousenumberCollection housenumbers, Long relationsid) {
		URL                url; 
		URLConnection      urlConn; 
		BufferedReader     dis;


		nodes_count = 0;
		ways_count = 0;
		relations_count = 0;
		gibmirnodes.clear();
		gibmirways.clear();
		gibmirrelations.clear();
		
		
		String overpass_url = "http://overpass-api.de/api/";
		//String overpass_url = "http://overpass.osm.rambler.ru/cgi/";
		//String overpass_url = "http://dev.overpass-api.de/api_mmd/";
		String overpass_queryurl = "interpreter?data=";
		String overpass_query = "[timeout:3600][maxsize:1073741824]\n"
			+ "[out:xml];\n"
			+ "area(" + (3600000000L + relationsid) + ")->.boundaryarea;\n"
			+ "(\n"
			+ "node(area.boundaryarea)[\"addr:housenumber\"];\n"
			+ "way(area.boundaryarea)[\"addr:housenumber\"];>;\n"
			+ "rel(area.boundaryarea)[\"addr:housenumber\"];>>;\n"
			+ "rel(area.boundaryarea)[\"type\"=\"associatedStreet\"];>>;\n"
			+ ");\n"
			+ "out meta;";
		logger.log(Level.FINE, "OSM Overpass Query ===" + overpass_query + "===");

		String url_string = "";
		File osmFile = null;

		try {
			String overpass_query_encoded = URLEncoder.encode(overpass_query, "UTF-8");
			overpass_query_encoded = overpass_query_encoded.replace("%28","(");
			overpass_query_encoded = overpass_query_encoded.replace("%29",")");
			overpass_query_encoded = overpass_query_encoded.replace("+","%20");
			url_string = overpass_url + overpass_queryurl + overpass_query_encoded;
			logger.log(Level.INFO, "Request for Overpass-API to get housenumbers ...");
			logger.log(Level.FINE, "Overpass Request URL to get housenumbers ===" + url_string + "===");

			StringBuffer osmresultcontent = new StringBuffer();

			InputStream overpassResponse = null; 
			String responseContentEncoding = "";
			Integer numberfailedtries = 0;
			boolean finishedoverpassquery = false;
			do {
				try {
					url = new URL(url_string);

					if(numberfailedtries > 0) {
						logger.log(Level.WARNING, "sleeping now for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again, now: " 
							+ new Date().toString());
						TimeUnit.SECONDS.sleep(2 * numberfailedtries);
						logger.log(Level.WARNING, "ok, slept for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again, now: "
							+ new Date().toString());
					}
					
					urlConn = url.openConnection(); 
					urlConn.setDoInput(true); 
					urlConn.setUseCaches(false);
					urlConn.setRequestProperty("User-Agent", "regio-osm.de Housenumber Evaluation Client, contact: strassenliste@diesei.de");
					urlConn.setRequestProperty("Accept-Encoding", "gzip, compress");
					
					overpassResponse = urlConn.getInputStream(); 
		
					Integer headeri = 1;
					logger.log(Level.FINE, "Overpass URL Response Header-Fields ...");
					while(urlConn.getHeaderFieldKey(headeri) != null) {
						logger.log(Level.FINE, "  Header # " + headeri 
							+ ":  [" + urlConn.getHeaderFieldKey(headeri)
							+ "] ===" + urlConn.getHeaderField(headeri) + "===");
						if(urlConn.getHeaderFieldKey(headeri).equals("Content-Encoding"))
							responseContentEncoding = urlConn.getHeaderField(headeri);
						headeri++;
					}
					finishedoverpassquery = true;
				} catch (MalformedURLException mue) {
					logger.log(Level.WARNING, "Overpass API request produced a malformed Exception (Request #" 
						+ (numberfailedtries + 1) + ", Request URL was ===" + url_string + "===, Details follows ...");					
					logger.log(Level.WARNING, mue.toString());
					numberfailedtries++;
					if(numberfailedtries > MAXOVERPASSTRIES) {
						logger.log(Level.SEVERE, "Overpass API didn't delivered data, gave up after 3 failed requests, Request URL was ===" + url_string + "===");
						setResponseState(numberfailedtries);
						return null;
					}
					//logger.log(Level.WARNING, "sleeping now for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again");
					//TimeUnit.SECONDS.sleep(2 * numberfailedtries);
				} catch( ConnectException conerror) {
					numberfailedtries++;
					if(numberfailedtries > MAXOVERPASSTRIES) {
						logger.log(Level.SEVERE, "Overpass API didn't delivered data, gave up after 3 failed requests, Request URL was ===" + url_string + "===");
						setResponseState(numberfailedtries);
						return null;
					}

					url_string = overpass_url + "status";
					System.out.println("url to get overpass status for this server requests ===" + url_string + "===");
					url = new URL(url_string);
					urlConn = url.openConnection(); 
					urlConn.setDoInput(true); 
					urlConn.setUseCaches(false);
					urlConn.setRequestProperty("User-Agent", "regio-osm.de Housenumber Evaluation Client, contact: strassenliste@diesei.de");
					urlConn.setRequestProperty("Accept-Encoding", "gzip, compress");
					
					overpassResponse = urlConn.getInputStream(); 
		
					Integer headeri = 1;
					logger.log(Level.FINE, "Overpass URL Response Header-Fields ...");
					while(urlConn.getHeaderFieldKey(headeri) != null) {
						logger.log(Level.FINE, "  Header # " + headeri 
							+ ":  [" + urlConn.getHeaderFieldKey(headeri)
							+ "] ===" + urlConn.getHeaderField(headeri) + "===");
						if(urlConn.getHeaderFieldKey(headeri).equals("Content-Encoding"))
							responseContentEncoding = urlConn.getHeaderField(headeri);
						headeri++;
					}
					if(responseContentEncoding.equals("gzip")) {
						dis = new BufferedReader(new InputStreamReader(new GZIPInputStream(overpassResponse),"UTF-8"));
					} else {
						dis = new BufferedReader(new InputStreamReader(overpassResponse,"UTF-8"));
					}
					String inputline = "";
					while ((inputline = dis.readLine()) != null)
					{ 
						System.out.println("Content ===" + inputline + "===\n");
					}
					dis.close();
				
				} catch (IOException ioe) {
					logger.log(Level.WARNING, "Overpass API request produced an Input/Output Exception  (Request #" 
						+ (numberfailedtries + 1) + ", Request URL was ===" + url_string + "===, Details follows ...");					
					logger.log(Level.WARNING, ioe.toString());
					numberfailedtries++;
					if(numberfailedtries > MAXOVERPASSTRIES) {
						logger.log(Level.SEVERE, "Overpass API didn't delivered data, gave up after 3 failed requests, Request URL was ===" + url_string + "===");
						setResponseState(numberfailedtries);
						return null;
					}
					//logger.log(Level.WARNING, "sleeping now for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again");
					//TimeUnit.SECONDS.sleep(2 * numberfailedtries);

				
					url_string = overpass_url + "status";
					System.out.println("url to get overpass status for this server requests ===" + url_string + "===");
					url = new URL(url_string);
					urlConn = url.openConnection(); 
					urlConn.setDoInput(true); 
					urlConn.setUseCaches(false);
					urlConn.setRequestProperty("User-Agent", "regio-osm.de Housenumber Evaluation Client, contact: strassenliste@diesei.de");
					urlConn.setRequestProperty("Accept-Encoding", "gzip, compress");
					
					overpassResponse = urlConn.getInputStream(); 
		
					Integer headeri = 1;
					logger.log(Level.FINE, "Overpass URL Response Header-Fields ...");
					while(urlConn.getHeaderFieldKey(headeri) != null) {
						logger.log(Level.FINE, "  Header # " + headeri 
							+ ":  [" + urlConn.getHeaderFieldKey(headeri)
							+ "] ===" + urlConn.getHeaderField(headeri) + "===");
						if(urlConn.getHeaderFieldKey(headeri).equals("Content-Encoding"))
							responseContentEncoding = urlConn.getHeaderField(headeri);
						headeri++;
					}
					if(responseContentEncoding.equals("gzip")) {
						dis = new BufferedReader(new InputStreamReader(new GZIPInputStream(overpassResponse),"UTF-8"));
					} else {
						dis = new BufferedReader(new InputStreamReader(overpassResponse,"UTF-8"));
					}
					String inputline = "";
					while ((inputline = dis.readLine()) != null)
					{ 
						System.out.println("Content ===" + inputline + "===\n");
					}
					dis.close();
				
				
				}
			} while(! finishedoverpassquery);
	
			setResponseState(numberfailedtries);
			String inputline = "";
			if(responseContentEncoding.equals("gzip")) {
				dis = new BufferedReader(new InputStreamReader(new GZIPInputStream(overpassResponse),"UTF-8"));
			} else {
				dis = new BufferedReader(new InputStreamReader(overpassResponse,"UTF-8"));
			}
			while ((inputline = dis.readLine()) != null)
			{ 
				osmresultcontent.append(inputline + "\n");
			}
			dis.close();
			
				// first, save upload data as local file, just for checking or for history
			DateFormat time_formatter = new SimpleDateFormat("yyyyMMdd-HHmmss'Z'");
			String downloadtime = time_formatter.format(new Date());
			
			String filename = configuration.application_datadir + File.separator + "overpassdownload" 
				+ File.separator + downloadtime + ".osm";

			try {
				osmFile = new File(filename);
				PrintWriter osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(filename),StandardCharsets.UTF_8)));
				osmOutput.println(osmresultcontent.toString());
				osmOutput.close();
				logger.log(Level.INFO, "Saved Overpass OSM Data Content to file " + filename);
			} catch (IOException ioe) {
				logger.log(Level.SEVERE, "Error, when tried to save Overpass OSM Data in file " + filename);
				logger.log(Level.SEVERE, ioe.toString());
			}
				// ok, osm result is in osmresultcontent.toString() available
			logger.log(Level.FINE, "Dateilänge nach optionalem Entpacken in Bytes: " + osmresultcontent.toString().length());

			int firstnodepos = osmresultcontent.toString().indexOf("<node");
			if(firstnodepos != -1) {
				String osmheader = osmresultcontent.toString().substring(0,firstnodepos);
				int osm_base_pos = osmheader.indexOf("osm_base=");
				if(osm_base_pos != 1) {
					int osm_base_valuestartpos = osmheader.indexOf("\"",osm_base_pos);
					int osm_base_valueendpos = osmheader.indexOf("\"",osm_base_valuestartpos + 1);
					if((osm_base_valuestartpos != -1) && (osm_base_valueendpos != -1)) { 
						String osm_base_value = osmheader.substring(osm_base_valuestartpos + 1,osm_base_valueendpos);
						DateFormat utc_formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
						try {
							evaluation.osmtime = utc_formatter.parse(osm_base_value).getTime();
						} catch (ParseException parseerror) {
							logger.log(Level.SEVERE, "Couldn't parse OSM DB timestamp from Overpass OSM Data, timestamp was ===" + osm_base_value + "===");					
							logger.log(Level.SEVERE, parseerror.toString());
						}
					}
				}
			}

			
			
			Sink sinkImplementation = new Sink() {

				@Override
				public void release() {
					// TODO Auto-generated method stub
					logger.log(Level.FINEST, "hallo Sink.release   aktiv !!!");
				}
				
				@Override
				public void complete() {
					logger.log(Level.FINEST, "hallo Sink.complete  aktiv:    nodes #"+nodes_count+"   ways #"+ways_count+"   relations #"+relations_count);

        			Integer onlyPartOfStreetnameIndexNo = evaluation.getOnlyPartOfStreetnameIndexNo();
        			String onlyPartOfStreetnameSeparator = evaluation.getOnlyPartOfStreetnameSeparator();
					
						// loop over all osm node objects
	    	    	for (Map.Entry<Long, Node> nodemap: gibmirnodes.entrySet()) {
	    				Long objectid = nodemap.getKey();
	    				Collection<Tag> tags = nodemap.getValue().getTags();
		        		String address_street = "";
		        		String address_streetlocalized = "";
		        		String address_postcode = "";
		        		String address_housenumber = "";
		        		HashMap<String,String> keyvalues = new HashMap<String,String>();
		        		for (Tag tag: tags) {
		        			//System.out.println("way #" + objectid + ": Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
		        			keyvalues.put(tag.getKey(), tag.getValue());
			        		if(		tag.getKey().equals("addr:street")
			        			||	tag.getKey().equals("addr:place")) {
			        			if(onlyPartOfStreetnameIndexNo != -1) {
			        				String streetnameParts[] = tag.getValue().split(onlyPartOfStreetnameSeparator);
			        				if(streetnameParts.length >= (onlyPartOfStreetnameIndexNo+1))
			        					address_street = streetnameParts[onlyPartOfStreetnameIndexNo];
			        			} else {
			        				address_street = tag.getValue();
			        			}
			        		}
			        		if(!evaluation.getUselanguagecode().equals("")) {
				        		if(		tag.getKey().equals("addr:street:" + evaluation.getUselanguagecode())
				        			||	tag.getKey().equals("addr:place:" + evaluation.getUselanguagecode()))
				        			address_streetlocalized = tag.getValue();
			        		}
			        		if(tag.getKey().equals("addr:postcode"))
			        			address_postcode = tag.getValue().replace(" ", "");
			        		if(tag.getKey().equals("addr:housenumber"))
			        			address_housenumber = tag.getValue();
						}
						logger.log(Level.FINEST,  "raw node with housenumber ===" + address_housenumber 
								+ "=== in street ===" + address_street + "===, node id #" + objectid + "===");
						if(!address_housenumber.equals("")) {
							if(!address_street.equals("")) {
								Housenumber osmhousenumber = new Housenumber(housenumbers);
								osmhousenumber.setOSMObjekt("node", objectid);
								if(!address_streetlocalized.equals(""))
									osmhousenumber.setStrasse(address_streetlocalized);
								else
									osmhousenumber.setStrasse(address_street);
								osmhousenumber.setPostcode(address_postcode);
								osmhousenumber.set_osm_tag(keyvalues);
								String objectlonlat = nodemap.getValue().getLongitude() + " " + nodemap.getValue().getLatitude();
								osmhousenumber.setLonlat(objectlonlat);
								osmhousenumber.setLonlat_source("OSM");
								osmhousenumber.setTreffertyp(Housenumber.Treffertyp.OSM_ONLY);
								
								if(address_housenumber.indexOf(",") != -1)
									address_housenumber = address_housenumber.replace(",",";");
								String[] addressHousenumberParts = address_housenumber.split(";");
								for(int tempi = 0; tempi < addressHousenumberParts.length; tempi++) {
									String actSingleHousenumber = addressHousenumberParts[tempi].trim();
									String[] temp_akthausnummer_single_array = Hausnummernbereich_aufloesen(actSingleHousenumber);
									for(String tempsinglei: temp_akthausnummer_single_array) {
										osmhousenumber.setHausnummer(tempsinglei);
										housenumbers.add_newentry(osmhousenumber);
										logger.log(Level.FINEST,  "add node with housenumber ===" + osmhousenumber.getHausnummer() 
											+ "=== in street ===" + osmhousenumber.getStrasse() + "===, node id #" + osmhousenumber.getOsmId() + "===");
									}
								}
							} else {
								logger.log(Level.WARNING, "OSM Node has a housenumber, but no street or place Information and will be ignored. OSM-Node id is " + objectid);
							}
						}
	    			}

	    	    		// loop over all osm way objects
	    	    	for (Map.Entry<Long, Way> waymap: gibmirways.entrySet()) {
	    				Long objectid = waymap.getKey();
		        		Collection<Tag> tags = waymap.getValue().getTags();
		        		String address_street = "";
		        		String address_streetlocalized = "";
		        		String address_postcode = "";
		        		String address_housenumber = "";
		        		String centroid_lon = "";
		        		String centroid_lat = "";
		        		HashMap<String,String> keyvalues = new HashMap<String,String>();
		        		for (Tag tag: tags) {
		        			//System.out.println("way #" + objectid + ": Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
		        			keyvalues.put(tag.getKey(), tag.getValue());
			        		if(		tag.getKey().equals("addr:street")
			        			||	tag.getKey().equals("addr:place")) {
			        			if(onlyPartOfStreetnameIndexNo != -1) {
			        				String streetnameParts[] = tag.getValue().split(onlyPartOfStreetnameSeparator);
			        				if(streetnameParts.length >= (onlyPartOfStreetnameIndexNo+1))
			        					address_street = streetnameParts[onlyPartOfStreetnameIndexNo];
			        			} else {
			        				address_street = tag.getValue();
			        			}
		        			}
			        		if(!evaluation.getUselanguagecode().equals("")) {
				        		if(		tag.getKey().equals("addr:street:" + evaluation.getUselanguagecode())
				        			||	tag.getKey().equals("addr:place:" + evaluation.getUselanguagecode()))
				        			address_streetlocalized = tag.getValue();
			        		}
			        		if(tag.getKey().equals("addr:postcode"))
			        			address_postcode = tag.getValue().replace(" ", "");
			        		if(tag.getKey().equals("addr:housenumber"))
			        			address_housenumber = tag.getValue();
			        		if(tag.getKey().equals("centroid_lon"))
			        			centroid_lon = tag.getValue();
			        		if(tag.getKey().equals("centroid_lat"))
			        			centroid_lat = tag.getValue();
		        		}
						if(!address_housenumber.equals("")) {
							if(		!address_street.equals("") 
								||	!address_streetlocalized.equals("") ) {
								Housenumber osmhousenumber = new Housenumber(housenumbers);
								osmhousenumber.setOSMObjekt("way", objectid);
								if(!address_streetlocalized.equals(""))
									osmhousenumber.setStrasse(address_streetlocalized);
								else
									osmhousenumber.setStrasse(address_street);
								osmhousenumber.setPostcode(address_postcode);
								osmhousenumber.set_osm_tag(keyvalues);
								osmhousenumber.setHausnummer(address_housenumber);
								String objectlonlat = centroid_lon + " " + centroid_lat;
								osmhousenumber.setLonlat(objectlonlat);
								osmhousenumber.setLonlat_source("OSM");
								osmhousenumber.setTreffertyp(Housenumber.Treffertyp.OSM_ONLY);
	
								if(address_housenumber.indexOf(",") != -1)
									address_housenumber = address_housenumber.replace(",",";");
								String[] addressHousenumberParts = address_housenumber.split(";");
								for(int tempi = 0; tempi < addressHousenumberParts.length; tempi++) {
									String actSingleHousenumber = addressHousenumberParts[tempi].trim();
									String[] temp_akthausnummer_single_array = Hausnummernbereich_aufloesen(actSingleHousenumber);
									for(String tempsinglei: temp_akthausnummer_single_array) {
										osmhousenumber.setHausnummer(tempsinglei);
										housenumbers.add_newentry(osmhousenumber);
										logger.log(Level.FINEST,  "add way with housenumber ===" + osmhousenumber.getHausnummer() 
												+ "=== in street ===" + osmhousenumber.getStrasse() + "===, way id #" + osmhousenumber.getOsmId() + "===");
									}
								}
							} else {
								logger.log(Level.WARNING, "OSM Way has a housenumber, but no street or place Information and will be ignored. OSM-Way id is " + objectid);
							}
						}
	    			}
		
	    	    		// loop over all osm relation objects with addr:housenumber Tag
	    	    	for (Map.Entry<Long, Relation> relationmap: gibmirrelations.entrySet()) {
	    				Long objectid = relationmap.getKey();
		        		Collection<Tag> tags = relationmap.getValue().getTags();
		        		String address_street = "";
		        		String address_streetlocalized = "";
		        		String address_postcode = "";
		        		String address_housenumber = "";
		        		String centroid_lon = "";
		        		String centroid_lat = "";
		        		HashMap<String,String> keyvalues = new HashMap<String,String>();
		        		for (Tag tag: tags) {
		        			//System.out.println("relation #" + objectid + ": Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
		        			keyvalues.put(tag.getKey(), tag.getValue());
			        		if(		tag.getKey().equals("addr:street")
			        			||	tag.getKey().equals("addr:place")) {
			        			if(onlyPartOfStreetnameIndexNo != -1) {
			        				String streetnameParts[] = tag.getValue().split(onlyPartOfStreetnameSeparator);
			        				if(streetnameParts.length >= (onlyPartOfStreetnameIndexNo+1))
			        					address_street = streetnameParts[onlyPartOfStreetnameIndexNo];
			        			} else {
			        				address_street = tag.getValue();
			        			}
			        		}
			        		if(!evaluation.getUselanguagecode().equals("")) {
				        		if(		tag.getKey().equals("addr:street:" + evaluation.getUselanguagecode())
				        			||	tag.getKey().equals("addr:place:" + evaluation.getUselanguagecode()))
				        			address_streetlocalized = tag.getValue();
			        		}
			        		if(tag.getKey().equals("addr:postcode"))
			        			address_postcode = tag.getValue().replace(" ", "");
			        		if(tag.getKey().equals("addr:housenumber"))
			        			address_housenumber = tag.getValue();
			        		if(tag.getKey().equals("centroid_lon"))
			        			centroid_lon = tag.getValue();
			        		if(tag.getKey().equals("centroid_lat"))
			        			centroid_lat = tag.getValue();
						}
						if(!address_housenumber.equals("")) {
							if(!address_street.equals("")) {
								Housenumber osmhousenumber = new Housenumber(housenumbers);
								osmhousenumber.setOSMObjekt("relation", objectid);
								if(!address_streetlocalized.equals(""))
									osmhousenumber.setStrasse(address_streetlocalized);
								else
									osmhousenumber.setStrasse(address_street);
								osmhousenumber.setPostcode(address_postcode);
								osmhousenumber.set_osm_tag(keyvalues);
								osmhousenumber.setHausnummer(address_housenumber);
								if(!centroid_lon.equals("")) {
									String objectlonlat = centroid_lon + " " + centroid_lat;
									osmhousenumber.setLonlat(objectlonlat);
									osmhousenumber.setLonlat_source("OSM");
								}
								osmhousenumber.setTreffertyp(Housenumber.Treffertyp.OSM_ONLY);
	
								if(address_housenumber.indexOf(",") != -1)
									address_housenumber = address_housenumber.replace(",",";");
								String[] addressHousenumberParts = address_housenumber.split(";");
								for(int tempi = 0; tempi < addressHousenumberParts.length; tempi++) {
									String actSingleHousenumber = addressHousenumberParts[tempi].trim();
									String[] temp_akthausnummer_single_array = Hausnummernbereich_aufloesen(actSingleHousenumber);
									for(String tempsinglei: temp_akthausnummer_single_array) {
										osmhousenumber.setHausnummer(tempsinglei);
										housenumbers.add_newentry(osmhousenumber);
										logger.log(Level.FINEST,  "add relation with housenumber ===" + osmhousenumber.getHausnummer() 
												+ "=== in street ===" + osmhousenumber.getStrasse() + "===, relation id #" + osmhousenumber.getOsmId() + "===");
									}
								}
							} else {
								logger.log(Level.WARNING, "OSM Relation has a housenumber, but no street or place Information and will be ignored. OSM-Relation id is " + objectid);
							}
						}
	    			}
				}
				
				@Override
				public void initialize(Map<String, Object> metaData) {
	    	    	for (Map.Entry<String, Object> daten: metaData.entrySet()) {
	    				String key = daten.getKey();
	    				Object tags = daten.getValue();
	    	    	}
				}
				
				@Override
				public void process(EntityContainer entityContainer) {

			        Entity entity = entityContainer.getEntity();
			        if (entity instanceof Node) {
			            //do something with the node
			        	nodes_count++;

		    			availableNodes.set(entity.getId());

						NodeContainer nodec = (NodeContainer) entityContainer;
						Node node = nodec.getEntity();
						//System.out.println("Node lon: "+node.getLongitude() + "  lat: "+node.getLatitude()+"===");

						gibmirnodes.put(entity.getId(), node);
			        } else if (entity instanceof Way) {
			        	ways_count++;
			        	
		    			availableWays.set(entity.getId());

						WayContainer wayc = (WayContainer) entityContainer;
						Way way = wayc.getEntity();
						//System.out.println("Weg "+way.getWayNodes()+"===");
						List<WayNode> actwaynodes = way.getWayNodes();
						//System.out.println("Weg enthält Anzahl knoten: "+actwaynodes.size());
						Integer lfdnr = 0;
						Double lon_sum = 0.0D;
						Double lat_sum = 0.0D;
						for (WayNode waynode: actwaynodes) {
							Node actnode = gibmirnodes.get(waynode.getNodeId());
							Point actpoint = new Point(actnode.getLongitude(), actnode.getLatitude());
							lon_sum += actnode.getLongitude();
							lat_sum += actnode.getLatitude();
							//System.out.println(" Node # " + lfdnr + "    id: " + actnode.getId() + "    lon: " + actnode.getLongitude() + "   lat: "+actnode.getLatitude());
							lfdnr++;
						}
		        		Collection<Tag> waytags = way.getTags();
						for (Tag tag: waytags) {
		        			//System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
						}
						Double centroid_lon = lon_sum / lfdnr;
						Double centroid_lat = lat_sum / lfdnr;
						waytags.add(new Tag("centroid_lon", centroid_lon.toString()));
						waytags.add(new Tag("centroid_lat", centroid_lat.toString()));
			        
		    			gibmirways.put(entity.getId(), way);
			        
			        } else if (entity instanceof Relation) {
			    		Integer polygonanzahl = 0;
			            //do something with the relation
			        	relations_count++;
			        	//System.out.println("Relation   " + entity.toString());
			        	List<RelationMember> relmembers =  ((Relation) entity).getMembers();

						RelationContainer relationc = (RelationContainer) entityContainer;
						Relation relation = relationc.getEntity();
			        	
		        		Collection<Tag> relationtags = entity.getTags();
		        		String relationType = "";
		        		String relationName = "";
		        		boolean relationContainsAddrhousenumber = false;
						for (Tag tag: relationtags) {
		        			//System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
		        			if(	tag.getKey().equals("type"))
		        				relationType = tag.getValue();
		        			if(	tag.getKey().equals("name"))
		        				relationName = tag.getValue();
		        			if(tag.getKey().equals("addr:housenumber"))
		        				relationContainsAddrhousenumber = true;
						}

						if(		! relationType.equals("associatedStreet")
							&& 	! relationType.equals("multipolygon")) {
							logger.log(Level.WARNING, "Relation is not of type associatedStreet, instead ===" + relationType + "===, OSM-Id ===" + entity.getId() + "===");
							return;
			        	}
						if(		relationType.equals("associatedStreet")
							&&	(relationName.equals(""))) {
							logger.log(Level.WARNING, "Relation has no name Tag, will be ignored, OSM-Id ===" + entity.getId() + "===");
							return;
			        	}

						Integer lfdnr = 0;
						Double lon_sum = 0.0D;
						Double lat_sum = 0.0D;
			        	for(int memberi = 0; memberi < relmembers.size(); memberi++) {
			        		RelationMember actmember = relmembers.get(memberi);
			        		EntityType memberType = actmember.getMemberType();
			        		long memberId = actmember.getMemberId();

			        		//System.out.println("relation member ["+memberi+"]  Typ: "+memberType+"   ==="+actmember.toString()+"===   Role ==="+actmember.getMemberRole()+"===");

			        		if(actmember.getMemberRole().equals("street")) {		// ignore relation member with role street
								logger.log(Level.WARNING, "Relation is of type=street, will be ignored, OSM-Id ===" + entity.getId() + "===");
			        			continue;
			        		}

			        		if (EntityType.Node.equals(memberType)) {
			    				if (availableNodes.get(memberId)) {
			    					logger.log(Level.INFO, "in Relation Member vom Type   NODE enthalten  ==="+gibmirnodes.get(memberId).toString()+"===");
			    					logger.log(Level.INFO, "  Hier die Tags des Node:  "+gibmirnodes.get(memberId).getTags().toString()+"===");
					        		Collection<Tag> nodetags = gibmirnodes.get(memberId).getTags();
									for (Tag tag: nodetags) {
					        			//System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
									}
									nodetags.add(new Tag("addr:street", relationName));

					        		Collection<Tag> changednodetags = gibmirnodes.get(memberId).getTags();
									for (Tag tag: changednodetags) {
					        			//System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
									}
			    				}
			    			} else if (EntityType.Way.equals(memberType)) {
			    				if (availableWays.get(memberId)) {
			    					//System.out.println("in Relation Member vom Type   WAY 0enthalten  ==="+gibmirways.get(memberId).toString()+"===");
			    					Collection<Tag> waytags = gibmirways.get(memberId).getTags();
									for (Tag tag: waytags) {
					        			//System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
									}
									waytags.add(new Tag("addr:street", relationName));

					        		Collection<Tag> changedwaytags = gibmirways.get(memberId).getTags();
									for (Tag tag: changedwaytags) {
					        			//System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
									}

									Way actway = gibmirways.get(memberId);
									List<WayNode> actwaynodes = actway.getWayNodes();
									//System.out.println("Weg enthält Anzahl knoten: "+actwaynodes.size());
									//List<Point> points = new LinkedList<Point>();
									for (WayNode waynode: actwaynodes) {
										Node actnode = gibmirnodes.get(waynode.getNodeId());
										if(!actmember.getMemberRole().equals("inner")) {
											lon_sum += actnode.getLongitude();
											lat_sum += actnode.getLatitude();
											lfdnr++;
										}
										//Point actpoint = new Point(actnode.getLongitude(), actnode.getLatitude());
										//points.add(actpoint);
										//System.out.println(" Node # " + lfdnr + "    id: " + actnode.getId() + "    lon: " + actnode.getLongitude() + "   lat: "+actnode.getLatitude());
									}
									/*
									WayGeometryBuilder waygeombuilder = new WayGeometryBuilder(NodeLocationStoreType.TempFile);
									//LineString linestring = waygeombuilder.createWayLinestring(actway);
									LineString linestring = waygeombuilder.createLinestring(points);
									System.out.println("erstellter Linestring ==="+linestring.toString()+"===");
									 */
			    				}
			    			}
			        	} // lloop over alle relation members
			        		// if relation contains a housenumber (so its not an assocatedStreet relation, but a multipolygon with address)
			        	if(relationContainsAddrhousenumber) {
							Double centroid_lon = lon_sum / lfdnr;
							Double centroid_lat = lat_sum / lfdnr;
							relationtags.add(new Tag("centroid_lon", centroid_lon.toString()));
							relationtags.add(new Tag("centroid_lat", centroid_lat.toString()));
			    			gibmirrelations.put(entity.getId(), relation);
			        	}
			        }
				}
			};
			
			RunnableSource osmfilereader;


			File tempfile = null;
			try {
			    // Create temp file.
			    tempfile = File.createTempFile("overpassresult", ".osm");
			    // Delete temp file when program exits.
			    tempfile.deleteOnExit();
			    // Write to temp file
			    BufferedWriter out = new BufferedWriter(new FileWriter(tempfile));
			    out.write(osmresultcontent.toString());
			    out.close();
			} catch (IOException e) {
			}	
			osmfilereader = new XmlReader(osmFile, true, CompressionMethod.None);
//			osmfilereader = new XmlReader(tempfile, true, CompressionMethod.None);

			osmfilereader.setSink(sinkImplementation);

			Thread readerThread = new Thread(osmfilereader);
			readerThread.start();

			while (readerThread.isAlive()) {
		        readerThread.join();
			}
		} catch (OsmosisRuntimeException osmosiserror) {
			logger.log(Level.SEVERE, "Osmosis runtime Error ...");
			logger.log(Level.SEVERE, osmosiserror.toString());
	    } catch (InterruptedException e) {
	    	logger.log(Level.WARNING, "Execution of type InterruptedException occured, details follows ...");
			logger.log(Level.WARNING, e.toString());
	        /* do nothing */
	    } catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
	    	logger.log(Level.SEVERE, "Execution of type InterruptedException occured, details follows ...");
			logger.log(Level.SEVERE, e.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
	    	logger.log(Level.SEVERE, "Execution of type InterruptedException occured, details follows ...");
			logger.log(Level.SEVERE, e.toString());
		}
		return housenumbers;
	}


	private HousenumberCollection ReadDataFromDB(final Evaluation evaluation, final HousenumberCollection housenumbers, Long relationsid) {

	
		if(con_mapnik == null) {
			return housenumbers;
		}

		if(con_housenumbers == null) {
			String url_housenumbers = configuration.db_application_url;
			try {
				if(con_housenumbers == null) {
					con_housenumbers = DriverManager.getConnection(url_housenumbers, configuration.db_application_username, configuration.db_application_password);
				}
				logger.log(Level.INFO, "Connection to database " + url_housenumbers + " established.");
			}
			catch (SQLException e) {
				logger.log(Level.SEVERE, "ERROR: failed to connect to database " + url_housenumbers);
				logger.log(Level.SEVERE, e.toString());
				System.out.println("ERROR: failed to connect to database " + url_housenumbers);
				System.out.println(e.toString());
				return housenumbers;
			}
		}

		Integer onlyPartOfStreetnameIndexNo = evaluation.getOnlyPartOfStreetnameIndexNo();
		String onlyPartOfStreetnameSeparator = evaluation.getOnlyPartOfStreetnameSeparator();
		

		logger.log(Level.FINE, "   ----------------------------------------------------------------------------------------");
		logger.log(Level.FINE, "   ----------------------------------------------------------------------------------------");
		logger.log(Level.FINE, "      Read OSM Data - Job  =" + evaluation.getJobname()
			+ "=, admin_level: " + evaluation.getJobAdminlevel() + "   Municipality " + evaluation.getJobMunicipality() 
			+ "  Country " + evaluation.getJobCountry());

		try {
			String jobPolygonQuerySql = "SELECT polygon FROM jobs AS j, gebiete AS g, stadt AS s, land AS l"
				+ " WHERE j.gebiete_id = g.id AND j.stadt_id = s.id AND j.land_id = l.id"
				+ " AND jobname = ? AND admin_level = ? AND stadt = ? AND land = ?;";
			PreparedStatement jobPolygonQueryStmt = con_housenumbers.prepareStatement(jobPolygonQuerySql);
			Integer statementIndex = 1;
			String preparedParameters = ""; 
			jobPolygonQueryStmt.setString(statementIndex++, evaluation.getJobname());
			preparedParameters += " [" + (statementIndex - 1) + "] ===" + evaluation.getJobname() + "===";
			jobPolygonQueryStmt.setInt(statementIndex++, evaluation.getJobAdminlevel());
			preparedParameters += " [" + (statementIndex - 1) + "] ===" + evaluation.getJobAdminlevel() + "===";
			jobPolygonQueryStmt.setString(statementIndex++, evaluation.getJobMunicipality());
			preparedParameters += " [" + (statementIndex - 1) + "] ===" + evaluation.getJobMunicipality() + "===";
			jobPolygonQueryStmt.setString(statementIndex++, evaluation.getJobCountry());
			preparedParameters += " [" + (statementIndex - 1) + "] ===" + evaluation.getJobCountry() + "===";
			logger.log(Level.FINE, "Prepared statement parameters " + preparedParameters);
	
			String polygonBinaryString = "";
			ResultSet jobPolygonQueryRS = jobPolygonQueryStmt.executeQuery();
			if(jobPolygonQueryRS.next()) {
				polygonBinaryString = jobPolygonQueryRS.getString("polygon");
			}
			jobPolygonQueryRS.close();
			jobPolygonQueryStmt.close();
	
			if(polygonBinaryString.equals("")) {
				return housenumbers;
			}


				// ------------------------------------------------------------------------------
				// 1. get all addr:housenumber node objects from the PLANET_POINT table
	
			String pointsObjectsSql = "SELECT osm_id AS id, tags->'addr:street' AS addrstreet, tags->'addr:place' AS addrplace,";
			if(!evaluation.getUselanguagecode().equals(""))
				pointsObjectsSql += "tags -> ? AS streetlocalized, tags -> ? AS placelocalized,";
			else
				pointsObjectsSql += "tags -> null AS streetlocalized, tags -> null AS placelocalized,";
			pointsObjectsSql += " tags->'addr:postcode', tags->'addr:housenumber',"
				+ " tags, ST_X(ST_Transform(way, 4326)) AS lon, ST_Y(ST_Transform(way, 4326)) AS lat"
				+ " FROM planet_point WHERE"
				+ " ST_Covers(?::geometry, way) AND"	// ST_Covers = complete inside
				+ " exist(tags, 'addr:housenumber');";
	
			PreparedStatement pointsObjectsStmt = con_mapnik.prepareStatement(pointsObjectsSql);
			statementIndex = 1;
			preparedParameters = ""; 
			pointsObjectsSql += "tags -> ? AS streetlocalized, tags -> ? AS placelocalized,";
			if(!evaluation.getUselanguagecode().equals("")) {
				pointsObjectsStmt.setString(statementIndex++, "addr:street:" + evaluation.getUselanguagecode());
				preparedParameters += " [" + (statementIndex - 1) + "] ===" + "addr:street:" + evaluation.getUselanguagecode() + "===";
				pointsObjectsStmt.setString(statementIndex++, "addr:place:" + evaluation.getUselanguagecode());
				preparedParameters += " [" + (statementIndex - 1) + "] ===" + "addr:place:" + evaluation.getUselanguagecode() + "===";
			}
			pointsObjectsStmt.setString(statementIndex++, polygonBinaryString);
			preparedParameters += " [" + (statementIndex - 1) + "] ===((completepolygon))===";
			logger.log(Level.FINE, "Prepared statement parameters " + preparedParameters);

			java.util.Date local_query_start = new Date();
	
			ResultSet pointsObjectsRS = pointsObjectsStmt.executeQuery();
	
			java.util.Date local_query_end = new Date();
			logger.log(Level.FINEST, "TIME single-step quer osm-ways in ms. "+(local_query_end.getTime()-local_query_start.getTime()));
			pointsQueryDuration += local_query_end.getTime() - local_query_start.getTime();
			local_query_start = local_query_end;

			Integer countFoundObjects = 0;
			while( pointsObjectsRS.next() ) {
				countFoundObjects++;
				HStore hstore = new HStore(pointsObjectsRS.getString("tags"));
				try {
					HashMap<String,String> tags = (HashMap<String,String>) hstore.asMap();
					
					Long objectid = pointsObjectsRS.getLong("id");
	        		String address_street = "";
	        		String address_streetlocalized = "";
	        		String address_postcode = "";
	        		String address_housenumber = "";

	        		if(pointsObjectsRS.getString("addrstreet") != null && !pointsObjectsRS.getString("addrstreet").equals("")) {
	        			if(onlyPartOfStreetnameIndexNo != -1) {
	        				String streetnameParts[] = pointsObjectsRS.getString("addrstreet").split(onlyPartOfStreetnameSeparator);
	        				if(streetnameParts.length >= (onlyPartOfStreetnameIndexNo+1))
	        					address_street = streetnameParts[onlyPartOfStreetnameIndexNo];
	        			} else {
	        				address_street = pointsObjectsRS.getString("addrstreet");
	        			}
	        		}
	        		if(		(!evaluation.getUselanguagecode().equals(""))
	        			&&	(pointsObjectsRS.getString("streetlocalized") != null && !pointsObjectsRS.getString("streetlocalized").equals(""))) {
	       				address_street = pointsObjectsRS.getString("streetlocalized");
	        		}

	        		if(pointsObjectsRS.getString("addrplace") != null && !pointsObjectsRS.getString("addrplace").equals("")) {
	        			if(onlyPartOfStreetnameIndexNo != -1) {
	        				String streetnameParts[] = pointsObjectsRS.getString("addrplace").split(onlyPartOfStreetnameSeparator);
	        				if(streetnameParts.length >= (onlyPartOfStreetnameIndexNo+1))
	        					address_street = streetnameParts[onlyPartOfStreetnameIndexNo];
	        			} else {
	        				address_street = pointsObjectsRS.getString("addrplace");
	        			}
	        		}
	        		if(		(!evaluation.getUselanguagecode().equals(""))
	        			&&	(pointsObjectsRS.getString("placelocalized") != null && !pointsObjectsRS.getString("placelocalized").equals(""))) {
	       				address_street = pointsObjectsRS.getString("placelocalized");
	        		}
	    		
	        		if(tags.get("addr:postcode") != null) {
	        			address_postcode = tags.get("addr:postcode").replace(" ", "");
	        		}
	        		if(tags.get("addr:housenumber") != null) {
	        			address_housenumber = tags.get("addr:housenumber");
	        		}
					logger.log(Level.FINEST,  "raw node with housenumber ===" + address_housenumber 
							+ "=== in street ===" + address_street + "===, node id #" + objectid + "===");

					if(!address_street.equals("")) {
						Housenumber osmhousenumber = new Housenumber(housenumbers);
						osmhousenumber.setOSMObjekt("node", objectid);
						if(!address_streetlocalized.equals(""))
							osmhousenumber.setStrasse(address_streetlocalized);
						else
							osmhousenumber.setStrasse(address_street);
						osmhousenumber.setPostcode(address_postcode);
						osmhousenumber.set_osm_tag(tags);
						String objectlonlat = pointsObjectsRS.getDouble("lon") + " " + pointsObjectsRS.getDouble("lat");
						osmhousenumber.setLonlat(objectlonlat);
						osmhousenumber.setLonlat_source("OSM");
						osmhousenumber.setTreffertyp(Housenumber.Treffertyp.OSM_ONLY);
						
						if(address_housenumber.indexOf(",") != -1)
							address_housenumber = address_housenumber.replace(",",";");
						String[] addressHousenumberParts = address_housenumber.split(";");
						for(int tempi = 0; tempi < addressHousenumberParts.length; tempi++) {
							String actSingleHousenumber = addressHousenumberParts[tempi].trim();
							String[] temp_akthausnummer_single_array = Hausnummernbereich_aufloesen(actSingleHousenumber);
							for(String tempsinglei: temp_akthausnummer_single_array) {
								osmhousenumber.setHausnummer(tempsinglei);
								housenumbers.add_newentry(osmhousenumber);
								logger.log(Level.FINEST,  "add node with housenumber ===" + osmhousenumber.getHausnummer() 
									+ "=== in street ===" + osmhousenumber.getStrasse() + "===, node id #" + osmhousenumber.getOsmId() + "===");
							}
						}
					} else {
						logger.log(Level.WARNING, "OSM Node has a housenumber, but no street or place Information and will be ignored. OSM-Node id is " + objectid);
					}
				} catch (IllegalStateException hstoreerror) {
					System.out.println("error, when hstore tried to interpret. hstore in DB ===" + hstore + ", table auswertung_hausnummern, db id: " + pointsObjectsRS.getString("id"));
				}
			}
			pointsObjectsRS.close();
			pointsObjectsStmt.close();
			local_query_end = new Date();
			logger.log(Level.FINEST, "Number of osm point address objects: " + countFoundObjects);
			logger.log(Level.FINEST, "TIME single-step analyzing osm-ways in ms. "+(local_query_end.getTime()-local_query_start.getTime()));
			pointsAnalyzeDuration += local_query_end.getTime() - local_query_start.getTime();


				// ------------------------------------------------------------------------------
				// 2. get all addr:housenumber node objects from the PLANET_LINE table
	
			String linesObjectsSql = "SELECT osm_id AS id, tags->'addr:street' AS addrstreet, tags->'addr:place' AS addrplace,";
			if(!evaluation.getUselanguagecode().equals(""))
				linesObjectsSql += "tags -> ? AS streetlocalized, tags -> ? AS placelocalized,";
			else
				linesObjectsSql += "tags -> null AS streetlocalized, tags -> null AS placelocalized,";
			linesObjectsSql += " tags->'addr:postcode', tags->'addr:housenumber',"
				+ " tags, ST_X(ST_Transform(ST_Centroid(way), 4326)) AS lon, ST_Y(ST_Transform(ST_Centroid(way), 4326)) AS lat"
				+ " FROM planet_line WHERE"
				+ " (ST_Covers(?::geometry, way) OR"	// ST_Covers = complete inside
				+ " ST_Crosses(?::geometry, way)) AND"	// ST_Crosses = partly inside
				+ " exist(tags, 'addr:housenumber');";
	
			PreparedStatement linesObjectsStmt = con_mapnik.prepareStatement(linesObjectsSql);
			statementIndex = 1;
			preparedParameters = ""; 
			linesObjectsSql += "tags -> ? AS streetlocalized, tags -> ? AS placelocalized,";
			if(!evaluation.getUselanguagecode().equals("")) {
				linesObjectsStmt.setString(statementIndex++, "addr:street:" + evaluation.getUselanguagecode());
				preparedParameters += " [" + (statementIndex - 1) + "] ===" + "addr:street:" + evaluation.getUselanguagecode() + "===";
				linesObjectsStmt.setString(statementIndex++, "addr:place:" + evaluation.getUselanguagecode());
				preparedParameters += " [" + (statementIndex - 1) + "] ===" + "addr:place:" + evaluation.getUselanguagecode() + "===";
			}
			linesObjectsStmt.setString(statementIndex++, polygonBinaryString);
			preparedParameters += " [" + (statementIndex - 1) + "] ===((completepolygon))===";
			linesObjectsStmt.setString(statementIndex++, polygonBinaryString);
			preparedParameters += " [" + (statementIndex - 1) + "] ===((completepolygon))===";
			logger.log(Level.FINE, "Prepared statement parameters " + preparedParameters);
	
			local_query_start = new Date();
	
			ResultSet linesObjectsRS = linesObjectsStmt.executeQuery();
	
			local_query_end = new Date();
			logger.log(Level.FINEST, "TIME single-step quer osm-ways in ms. "+(local_query_end.getTime()-local_query_start.getTime()));
			linesQueryDuration += local_query_end.getTime() - local_query_start.getTime();
			local_query_start = local_query_end;
	
			countFoundObjects = 0;
			while( linesObjectsRS.next() ) {
				countFoundObjects++;
				HStore hstore = new HStore(linesObjectsRS.getString("tags"));
				try {
					HashMap<String,String> tags = (HashMap<String,String>) hstore.asMap();
					
					Long objectid = linesObjectsRS.getLong("id");
	        		String address_street = "";
	        		String address_streetlocalized = "";
	        		String address_postcode = "";
	        		String address_housenumber = "";
	
	        		if(linesObjectsRS.getString("addrstreet") != null && !linesObjectsRS.getString("addrstreet").equals("")) {
	        			if(onlyPartOfStreetnameIndexNo != -1) {
	        				String streetnameParts[] = linesObjectsRS.getString("addrstreet").split(onlyPartOfStreetnameSeparator);
	        				if(streetnameParts.length >= (onlyPartOfStreetnameIndexNo+1))
	        					address_street = streetnameParts[onlyPartOfStreetnameIndexNo];
	        			} else {
	        				address_street = linesObjectsRS.getString("addrstreet");
	        			}
	        		}
	        		if(		(!evaluation.getUselanguagecode().equals(""))
	        			&&	(linesObjectsRS.getString("streetlocalized") != null && !linesObjectsRS.getString("streetlocalized").equals(""))) {
	       				address_street = linesObjectsRS.getString("streetlocalized");
	        		}
	
	        		if(linesObjectsRS.getString("addrplace") != null && !linesObjectsRS.getString("addrplace").equals("")) {
	        			if(onlyPartOfStreetnameIndexNo != -1) {
	        				String streetnameParts[] = linesObjectsRS.getString("addrplace").split(onlyPartOfStreetnameSeparator);
	        				if(streetnameParts.length >= (onlyPartOfStreetnameIndexNo+1))
	        					address_street = streetnameParts[onlyPartOfStreetnameIndexNo];
	        			} else {
	        				address_street = linesObjectsRS.getString("addrplace");
	        			}
	        		}
	        		if(		(!evaluation.getUselanguagecode().equals(""))
	        			&&	(linesObjectsRS.getString("placelocalized") != null && !linesObjectsRS.getString("placelocalized").equals(""))) {
	       				address_street = linesObjectsRS.getString("placelocalized");
	        		}
	    		
	        		if(tags.get("addr:postcode") != null) {
	        			address_postcode = tags.get("addr:postcode").replace(" ", "");
	        		}
	        		if(tags.get("addr:housenumber") != null) {
	        			address_housenumber = tags.get("addr:housenumber");
	        		}
					logger.log(Level.FINEST,  "raw node with housenumber ===" + address_housenumber 
							+ "=== in street ===" + address_street + "===, node id #" + objectid + "===");
	
					if(!address_street.equals("")) {
						Housenumber osmhousenumber = new Housenumber(housenumbers);
						osmhousenumber.setOSMObjekt("node", objectid);
						if(!address_streetlocalized.equals(""))
							osmhousenumber.setStrasse(address_streetlocalized);
						else
							osmhousenumber.setStrasse(address_street);
						osmhousenumber.setPostcode(address_postcode);
						osmhousenumber.set_osm_tag(tags);
						String objectlonlat = linesObjectsRS.getDouble("lon") + " " + linesObjectsRS.getDouble("lat");
						osmhousenumber.setLonlat(objectlonlat);
						osmhousenumber.setLonlat_source("OSM");
						osmhousenumber.setTreffertyp(Housenumber.Treffertyp.OSM_ONLY);
						
						if(address_housenumber.indexOf(",") != -1)
							address_housenumber = address_housenumber.replace(",",";");
						String[] addressHousenumberParts = address_housenumber.split(";");
						for(int tempi = 0; tempi < addressHousenumberParts.length; tempi++) {
							String actSingleHousenumber = addressHousenumberParts[tempi].trim();
							String[] temp_akthausnummer_single_array = Hausnummernbereich_aufloesen(actSingleHousenumber);
							for(String tempsinglei: temp_akthausnummer_single_array) {
								osmhousenumber.setHausnummer(tempsinglei);
								housenumbers.add_newentry(osmhousenumber);
								logger.log(Level.FINEST,  "add node with housenumber ===" + osmhousenumber.getHausnummer() 
									+ "=== in street ===" + osmhousenumber.getStrasse() + "===, node id #" + osmhousenumber.getOsmId() + "===");
							}
						}
					} else {
						logger.log(Level.WARNING, "OSM Node has a housenumber, but no street or place Information and will be ignored. OSM-Node id is " + objectid);
					}
				} catch (IllegalStateException hstoreerror) {
					System.out.println("error, when hstore tried to interpret. hstore in DB ===" + hstore + ", table auswertung_hausnummern, db id: " + linesObjectsRS.getString("id"));
				}
			}
			linesObjectsRS.close();
			linesObjectsStmt.close();
			local_query_end = new Date();
			logger.log(Level.FINEST, "Number of osm line address objects: " + countFoundObjects);
			logger.log(Level.FINEST, "TIME single-step analyzing osm-ways in ms. "+(local_query_end.getTime()-local_query_start.getTime()));
			linesAnalyzeDuration += local_query_end.getTime() - local_query_start.getTime();
		

				// ------------------------------------------------------------------------------
				// 3. get all addr:housenumber node objects from the PLANET_POLYGON table
		
			String polygonsObjectsSql = "SELECT osm_id AS id, tags->'addr:street' AS addrstreet, tags->'addr:place' AS addrplace,";
			if(!evaluation.getUselanguagecode().equals(""))
				polygonsObjectsSql += "tags -> ? AS streetlocalized, tags -> ? AS placelocalized,";
			else
				polygonsObjectsSql += "tags -> null AS streetlocalized, tags -> null AS placelocalized,";
			polygonsObjectsSql += " tags->'addr:postcode', tags->'addr:housenumber',"
				+ " tags, ST_X(ST_Transform(ST_Centroid(way), 4326)) AS lon, ST_Y(ST_Transform(ST_Centroid(way), 4326)) AS lat"
				+ " FROM planet_polygon WHERE"
				+ " (ST_Covers(?::geometry, way) OR"	// ST_Covers = complete inside
				+ " ST_Crosses(?::geometry, way)) AND"	// ST_Crosses = partly inside
				+ " exist(tags, 'addr:housenumber');";
		
			PreparedStatement polygonsObjectsStmt = con_mapnik.prepareStatement(polygonsObjectsSql);
			statementIndex = 1;
			preparedParameters = ""; 
			polygonsObjectsSql += "tags -> ? AS streetlocalized, tags -> ? AS placelocalized,";
			if(!evaluation.getUselanguagecode().equals("")) {
				polygonsObjectsStmt.setString(statementIndex++, "addr:street:" + evaluation.getUselanguagecode());
				preparedParameters += " [" + (statementIndex - 1) + "] ===" + "addr:street:" + evaluation.getUselanguagecode() + "===";
				polygonsObjectsStmt.setString(statementIndex++, "addr:place:" + evaluation.getUselanguagecode());
				preparedParameters += " [" + (statementIndex - 1) + "] ===" + "addr:place:" + evaluation.getUselanguagecode() + "===";
			}
			polygonsObjectsStmt.setString(statementIndex++, polygonBinaryString);
			preparedParameters += " [" + (statementIndex - 1) + "] ===((completepolygon))===";
			polygonsObjectsStmt.setString(statementIndex++, polygonBinaryString);
			preparedParameters += " [" + (statementIndex - 1) + "] ===((completepolygon))===";
			logger.log(Level.FINE, "Prepared statement parameters " + preparedParameters);
		
			local_query_start = new Date();
		
			ResultSet polygonsObjectsRS = polygonsObjectsStmt.executeQuery();
		
			local_query_end = new Date();
			logger.log(Level.FINEST, "TIME single-step quer osm-ways in ms. "+(local_query_end.getTime()-local_query_start.getTime()));
			polygonsQueryDuration += local_query_end.getTime() - local_query_start.getTime();
			local_query_start = local_query_end;
		
			countFoundObjects = 0;
			while( polygonsObjectsRS.next() ) {
				countFoundObjects++;
				HStore hstore = new HStore(polygonsObjectsRS.getString("tags"));
				try {
					HashMap<String,String> tags = (HashMap<String,String>) hstore.asMap();
					
					Long objectid = polygonsObjectsRS.getLong("id");
		    		String address_street = "";
		    		String address_streetlocalized = "";
		    		String address_postcode = "";
		    		String address_housenumber = "";
		
		    		if(polygonsObjectsRS.getString("addrstreet") != null && !polygonsObjectsRS.getString("addrstreet").equals("")) {
		    			if(onlyPartOfStreetnameIndexNo != -1) {
		    				String streetnameParts[] = polygonsObjectsRS.getString("addrstreet").split(onlyPartOfStreetnameSeparator);
		    				if(streetnameParts.length >= (onlyPartOfStreetnameIndexNo+1))
		    					address_street = streetnameParts[onlyPartOfStreetnameIndexNo];
		    			} else {
		    				address_street = polygonsObjectsRS.getString("addrstreet");
		    			}
		    		}
		    		if(		(!evaluation.getUselanguagecode().equals(""))
		    			&&	(polygonsObjectsRS.getString("streetlocalized") != null && !polygonsObjectsRS.getString("streetlocalized").equals(""))) {
		   				address_street = polygonsObjectsRS.getString("streetlocalized");
		    		}
		
		    		if(polygonsObjectsRS.getString("addrplace") != null && !polygonsObjectsRS.getString("addrplace").equals("")) {
		    			if(onlyPartOfStreetnameIndexNo != -1) {
		    				String streetnameParts[] = polygonsObjectsRS.getString("addrplace").split(onlyPartOfStreetnameSeparator);
		    				if(streetnameParts.length >= (onlyPartOfStreetnameIndexNo+1))
		    					address_street = streetnameParts[onlyPartOfStreetnameIndexNo];
		    			} else {
		    				address_street = polygonsObjectsRS.getString("addrplace");
		    			}
		    		}
		    		if(		(!evaluation.getUselanguagecode().equals(""))
		    			&&	(polygonsObjectsRS.getString("placelocalized") != null && !polygonsObjectsRS.getString("placelocalized").equals(""))) {
		   				address_street = polygonsObjectsRS.getString("placelocalized");
		    		}
				
		    		if(tags.get("addr:postcode") != null) {
		    			address_postcode = tags.get("addr:postcode").replace(" ", "");
		    		}
		    		if(tags.get("addr:housenumber") != null) {
		    			address_housenumber = tags.get("addr:housenumber");
		    		}
					logger.log(Level.FINEST,  "raw node with housenumber ===" + address_housenumber 
							+ "=== in street ===" + address_street + "===, node id #" + objectid + "===");
		
					if(!address_street.equals("")) {
						Housenumber osmhousenumber = new Housenumber(housenumbers);
						osmhousenumber.setOSMObjekt("node", objectid);
						if(!address_streetlocalized.equals(""))
							osmhousenumber.setStrasse(address_streetlocalized);
						else
							osmhousenumber.setStrasse(address_street);
						osmhousenumber.setPostcode(address_postcode);
						osmhousenumber.set_osm_tag(tags);
						String objectlonlat = polygonsObjectsRS.getDouble("lon") + " " + polygonsObjectsRS.getDouble("lat");
						osmhousenumber.setLonlat(objectlonlat);
						osmhousenumber.setLonlat_source("OSM");
						osmhousenumber.setTreffertyp(Housenumber.Treffertyp.OSM_ONLY);
						
						if(address_housenumber.indexOf(",") != -1)
							address_housenumber = address_housenumber.replace(",",";");
						String[] addressHousenumberParts = address_housenumber.split(";");
						for(int tempi = 0; tempi < addressHousenumberParts.length; tempi++) {
							String actSingleHousenumber = addressHousenumberParts[tempi].trim();
							String[] temp_akthausnummer_single_array = Hausnummernbereich_aufloesen(actSingleHousenumber);
							for(String tempsinglei: temp_akthausnummer_single_array) {
								osmhousenumber.setHausnummer(tempsinglei);
								housenumbers.add_newentry(osmhousenumber);
								logger.log(Level.FINEST,  "add node with housenumber ===" + osmhousenumber.getHausnummer() 
									+ "=== in street ===" + osmhousenumber.getStrasse() + "===, node id #" + osmhousenumber.getOsmId() + "===");
							}
						}
					} else {
						logger.log(Level.WARNING, "OSM Node has a housenumber, but no street or place Information and will be ignored. OSM-Node id is " + objectid);
					}
				} catch (IllegalStateException hstoreerror) {
					System.out.println("error, when hstore tried to interpret. hstore in DB ===" + hstore + ", table auswertung_hausnummern, db id: " + polygonsObjectsRS.getString("id"));
				}
			}
			polygonsObjectsRS.close();
			polygonsObjectsStmt.close();
			local_query_end = new Date();
			logger.log(Level.FINEST, "Number of osm polygon address objects: " + countFoundObjects);
			logger.log(Level.FINEST, "TIME single-step analyzing osm-ways in ms. "+(local_query_end.getTime()-local_query_start.getTime()));
			polygonsAnalyzeDuration += local_query_end.getTime() - local_query_start.getTime();

		}
		catch( SQLException sqle) {
			logger.log(Level.SEVERE, "SQL-Exception occured, Details \n" + sqle.toString());
//TODO stored errors, like org.postgresql.util.PSQLException: ERROR: GEOS covers() threw an error!
			System.out.println("SQL-Exception occured, Details \n" + sqle.toString());
		}
		return housenumbers;
	}

}
