

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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;










import de.regioosm.housenumbers.Applicationconfiguration;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
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
	private static final int HAUSNUMMERSORTIERBARLENGTH = 4;
	private static final Logger logger = Evaluation.logger;

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
	SimpleObjectStore<EntityContainer> allNodes = new SimpleObjectStore<EntityContainer>(new SingleClassObjectSerializationFactory(EntityContainer.class), "afnd", true);
	SimpleObjectStore<EntityContainer> allWays = new SimpleObjectStore<EntityContainer>(new SingleClassObjectSerializationFactory(EntityContainer.class), "afwy", true);
	SimpleObjectStore<EntityContainer> allRelations = new SimpleObjectStore<EntityContainer>(new SingleClassObjectSerializationFactory(EntityContainer.class), "afrl", true);

	
	TreeMap<Long, Node> gibmirnodes = new TreeMap<Long, Node>();
	TreeMap<Long, Way> gibmirways = new TreeMap<Long, Way>();
	TreeMap<Long, Relation> gibmirrelations = new TreeMap<Long, Relation>();

	String land = "";
	Integer land_id = -1;
	Integer stadt_id = -1;

	
	public void setDBConnection(String dbconnection, String dbusername, String dbpassword) {
		this.dbconnection = dbconnection;
		this.dbusername = dbusername;
		this.dbpassword = dbpassword;
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


	public HousenumberCollection ReadDataFromOverpass(final Evaluation evaluation, final HousenumberCollection housenumbers, Long relationsid) {
		URL                url; 
		URLConnection      urlConn; 
		BufferedReader     dis;


		nodes_count = 0;
		ways_count = 0;
		relations_count = 0;
		gibmirnodes.clear();
		gibmirways.clear();
		gibmirrelations.clear();
		
		
		//String overpass_url = "http://overpass-api.de/api/";
		//String overpass_url = "http://overpass.osm.rambler.ru/cgi/";
		String overpass_url = "http://dev.overpass-api.de/api_mmd/";
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
							+ new java.util.Date().toString());
						TimeUnit.SECONDS.sleep(2 * numberfailedtries);
						logger.log(Level.WARNING, "ok, slept for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again, now: "
							+ new java.util.Date().toString());
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
					if(numberfailedtries > 3) {
						logger.log(Level.SEVERE, "Overpass API didn't delivered data, gave up after 3 failed requests, Request URL was ===" + url_string + "===");
						return null;
					}
					//logger.log(Level.WARNING, "sleeping now for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again");
					//TimeUnit.SECONDS.sleep(2 * numberfailedtries);
				} catch (IOException ioe) {
					logger.log(Level.WARNING, "Overpass API request produced an Input/Output Exception  (Request #" 
						+ (numberfailedtries + 1) + ", Request URL was ===" + url_string + "===, Details follows ...");					
					logger.log(Level.WARNING, ioe.toString());
					numberfailedtries++;
					if(numberfailedtries > 3) {
						logger.log(Level.SEVERE, "Overpass API didn't delivered data, gave up after 3 failed requests, Request URL was ===" + url_string + "===");
						return null;
					}
					//logger.log(Level.WARNING, "sleeping now for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again");
					//TimeUnit.SECONDS.sleep(2 * numberfailedtries);
				}
			} while(! finishedoverpassquery);
	
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

		    			//allNodes.add(entityContainer);
		    			availableNodes.set(entity.getId());

						NodeContainer nodec = (NodeContainer) entityContainer;
						Node node = nodec.getEntity();
						//System.out.println("Node lon: "+node.getLongitude() + "  lat: "+node.getLatitude()+"===");

						gibmirnodes.put(entity.getId(), node);
			        } else if (entity instanceof Way) {
			        	ways_count++;
			        	
		    			//allWays.add(entityContainer);
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
if(1 == 1) {
	logger.log(Level.SEVERE, "method ReadListFromDB has not been coded yet, CANCEL");
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

				Housenumber newofficialhousenumber = new Housenumber(housenumbers);

				newofficialhousenumber.setStrasse(rsqueryofficialhousenumbers.getString("strasse"));
				newofficialhousenumber.setHausnummer(rsqueryofficialhousenumbers.getString("hausnummer"));
				newofficialhousenumber.setTreffertyp(Housenumber.Treffertyp.LIST_ONLY);
	
				housenumbers.add_newentry(newofficialhousenumber);
			} // Ende sql-Schleife über alle Stadt-only-Hausnummern der aktuellen Strasse - while(rsqueryofficialhousenumbers.next()) {
	
//			for(int loadindex=0; loadindex < evaluation.housenumberlist.length(); loadindex++) {
//				Workcache_Entry aktcacheentry = evaluation.housenumberlist.entry(loadindex);
//			}
	
		
		} catch (SQLException e) {
			System.out.println("ERROR: during select table auswertung_hausnummern, sqlquery was ===" 
				+ sqlqueryofficialhousenumbers + "===");
			e.printStackTrace();
		}
		
		return housenumbers;
	}
}
