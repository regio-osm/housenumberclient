import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.myjavatools.web.ClientHttpRequest;

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
import org.openstreetmap.osmosis.pgsnapshot.common.NodeLocationStoreType;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.impl.WayGeometryBuilder;
import org.postgis.GeometryCollection;
import org.postgis.LineString;
import org.postgis.Point;
import org.postgis.Polygon;
import org.postgis.LinearRing;
import org.postgis.MultiPolygon;
import org.postgis.ComposedGeom;
import org.postgis.PGgeometry;
import org.postgis.Geometry;



/**
 * @author Dietmar Seifert
 *
 * evaluate the housenumbers in a municipality or a subregion of it, check againsts an available housenumerlist
 * 	
*/




public class OsmDataReader {
	private static final int HAUSNUMMERSORTIERBARLENGTH = 4;

	String dbconnection = "";
	String dbusername = "";
	String dbpassword = "";

	
	static Integer nodes_count = 0;
	static Integer ways_count = 0;
	static Integer relations_count = 0;
	static IdTracker availableNodes = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
	static IdTracker availableWays = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
	static IdTracker availableRelations = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
	static SimpleObjectStore<EntityContainer> allNodes = new SimpleObjectStore<EntityContainer>(new SingleClassObjectSerializationFactory(EntityContainer.class), "afnd", true);
	static SimpleObjectStore<EntityContainer> allWays = new SimpleObjectStore<EntityContainer>(new SingleClassObjectSerializationFactory(EntityContainer.class), "afwy", true);
	static SimpleObjectStore<EntityContainer> allRelations = new SimpleObjectStore<EntityContainer>(new SingleClassObjectSerializationFactory(EntityContainer.class), "afrl", true);

	
	static TreeMap<Long, Node> gibmirnodes = new TreeMap<Long, Node>();
	static TreeMap<Long, Way> gibmirways = new TreeMap<Long, Way>();
	static TreeMap<Long, Relation> gibmirrelations = new TreeMap<Long, Relation>();

	static String land = "";
	static Integer land_id = -1;
	static Integer stadt_id = -1;

	
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
				}
			} catch( NumberFormatException e) {
				System.out.println("Error during parsing text to integer, text was ==="+hausnummertext+"===");
				e.printStackTrace();
			}

		}
		String[] return_string_array = new String[hausnummern_array.size()];
		return_string_array = hausnummern_array.toArray(return_string_array);
		return return_string_array;
	}


	public HousenumberCollection ReadDataFromOverpass(final Evaluation evaluation, Long relationsid) {
		URL                url; 
		URLConnection      urlConn; 
		BufferedReader     dis;

		final HousenumberCollection housenumbers = new HousenumberCollection();
		
		String overpass_url = "http://overpass-api.de";
		String overpass_queryurl = "/api/interpreter?data=";
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

		try {
			String overpass_query_encoded = URLEncoder.encode(overpass_query, "UTF-8");
			overpass_query_encoded = overpass_query_encoded.replace("%28","(");
			overpass_query_encoded = overpass_query_encoded.replace("%29",")");
			overpass_query_encoded = overpass_query_encoded.replace("+","%20");
			url_string = overpass_url + overpass_queryurl + overpass_query_encoded;
			System.out.println("url_string ===" + url_string + "===");
			
			
			StringBuffer osmresultcontent = new StringBuffer();
			url = new URL(url_string);
			
			urlConn = url.openConnection(); 
			urlConn.setDoInput(true); 
			urlConn.setUseCaches(false);
			urlConn.setRequestProperty("User-Agent", "regio-osm.de Housenumber Evaluation, contact: strassenliste@diesei.de");
			urlConn.setRequestProperty("Accept-Encoding", "gzip, compress");
			
			String inputline = "";
			InputStream overpassResponse = urlConn.getInputStream(); 

			Integer headeri = 1;
			System.out.println("Header-Fields Ausgabe ...");
			String responseContentEncoding = "";
			while(urlConn.getHeaderFieldKey(headeri) != null) {
				System.out.println("  Header # " + headeri 
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
			while ((inputline = dis.readLine()) != null)
			{ 
				osmresultcontent.append(inputline + "\n");
			}
			dis.close();

				// ok, osm result is in osmresultcontent.toString() available
			System.out.println("Dateilänge url Datenempfang: " + osmresultcontent.toString().length());
			//System.out.println("Dateioutput ===" + osmresultcontent.toString() + "===");

			Sink sinkImplementation = new Sink() {

				@Override
				public void release() {
					// TODO Auto-generated method stub
					System.out.println("hallo Sink.release   aktiv !!!");
					
				}
				
				@Override
				public void complete() {
					System.out.println("hallo Sink.complete  aktiv:    nodes #"+nodes_count+"   ways #"+ways_count+"   relations #"+relations_count);

						// loop over all osm node objects
	    	    	for (Map.Entry<Long, Node> nodemap: gibmirnodes.entrySet()) {
	    				Long objectid = nodemap.getKey();
		        		Collection<Tag> tags = nodemap.getValue().getTags();
		        		String address_street = "";
		        		String address_housenumber = "";
		        		HashMap<String,String> keyvalues = new HashMap<String,String>();
		        		for (Tag tag: tags) {
		        			System.out.println("way #" + objectid + ": Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
		        			keyvalues.put(tag.getKey(), tag.getValue());
			        		if(		tag.getKey().equals("addr:street")
			        			||	tag.getKey().equals("addr:place"))
			        			address_street = tag.getValue();
			        		if(tag.getKey().equals("addr:housenumber"))
			        			address_housenumber = tag.getValue();
						}
						if(		(! address_street.equals(""))
							&& 	(! address_housenumber.equals(""))) {
							Housenumber osmhousenumber = new Housenumber(evaluation.getHousenumberlist().ishousenumberadditionCaseSentity());
							osmhousenumber.setStrasse(address_street);
							osmhousenumber.set_osm_tag(keyvalues);
							osmhousenumber.setOSMObjekt("node", objectid);
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
								}
							}
						}
	    			}

	    	    		// loop over all osm way objects
	    	    	for (Map.Entry<Long, Way> waymap: gibmirways.entrySet()) {
	    				Long objectid = waymap.getKey();
		        		Collection<Tag> tags = waymap.getValue().getTags();
		        		String address_street = "";
		        		String address_housenumber = "";
		        		String centroid_lon = "";
		        		String centroid_lat = "";
		        		HashMap<String,String> keyvalues = new HashMap<String,String>();
		        		for (Tag tag: tags) {
		        			System.out.println("way #" + objectid + ": Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
		        			keyvalues.put(tag.getKey(), tag.getValue());
			        		if(		tag.getKey().equals("addr:street")
			        			||	tag.getKey().equals("addr:place"))
			        			address_street = tag.getValue();
			        		if(tag.getKey().equals("addr:housenumber"))
			        			address_housenumber = tag.getValue();
			        		if(tag.getKey().equals("centroid_lon"))
			        			centroid_lon = tag.getValue();
			        		if(tag.getKey().equals("centroid_lat"))
			        			centroid_lat = tag.getValue();
		        		}
						if(		(! address_street.equals(""))
							&& 	(! address_housenumber.equals(""))) {
							Housenumber osmhousenumber = new Housenumber(evaluation.getHousenumberlist().ishousenumberadditionCaseSentity());
							osmhousenumber.setStrasse(address_street);
							osmhousenumber.set_osm_tag(keyvalues);
							osmhousenumber.setOSMObjekt("way", objectid);
							osmhousenumber.setHausnummer(address_housenumber);
							String objectlonlat = centroid_lon + " " + centroid_lat;
							osmhousenumber.setLonlat(objectlonlat);
//set centroid or something similar from way object   osmhousenumber.setLonlat(rs_objekte.getString("lonlat"));
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
								}
							}
						}
	    			}
		
	    	    		// loop over all osm relation objects with addr:housenumber Tag
	    	    	for (Map.Entry<Long, Relation> relationmap: gibmirrelations.entrySet()) {
	    				Long objectid = relationmap.getKey();
		        		Collection<Tag> tags = relationmap.getValue().getTags();
		        		String address_street = "";
		        		String address_housenumber = "";
		        		HashMap<String,String> keyvalues = new HashMap<String,String>();
		        		for (Tag tag: tags) {
		        			System.out.println("relation #" + objectid + ": Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
		        			keyvalues.put(tag.getKey(), tag.getValue());
			        		if(		tag.getKey().equals("addr:street")
			        			||	tag.getKey().equals("addr:place"))
			        			address_street = tag.getValue();
			        		if(tag.getKey().equals("addr:housenumber"))
			        			address_housenumber = tag.getValue();
						}
						if(		(! address_street.equals(""))
							&& 	(! address_housenumber.equals(""))) {
							Housenumber osmhousenumber = new Housenumber(evaluation.getHousenumberlist().ishousenumberadditionCaseSentity());
							osmhousenumber.setStrasse(address_street);
							osmhousenumber.set_osm_tag(keyvalues);
							osmhousenumber.setOSMObjekt("relation", objectid);
							osmhousenumber.setHausnummer(address_housenumber);
	//set centroid or something similar from way object   osmhousenumber.setLonlat(rs_objekte.getString("lonlat"));
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
								}
							}
						}
	    			}
				}
				
				@Override
				public void initialize(Map<String, Object> metaData) {
					// TODO Auto-generated method stub
					System.out.println("hallo Sink.initialize aktiv !!!");
				}
				
				@Override
				public void process(EntityContainer entityContainer) {
					System.out.println("hallo Sink.process  aktiv:    nodes #"+nodes_count+"   ways #"+ways_count+"   relations #"+relations_count);

			        Entity entity = entityContainer.getEntity();
			        if (entity instanceof Node) {
			            //do something with the node
			        	nodes_count++;

		    			//allNodes.add(entityContainer);
		    			availableNodes.set(entity.getId());

						NodeContainer nodec = (NodeContainer) entityContainer;
						Node node = nodec.getEntity();
						System.out.println("Node lon: "+node.getLongitude() + "  lat: "+node.getLatitude()+"===");

						gibmirnodes.put(entity.getId(), node);
			        } else if (entity instanceof Way) {
			        	ways_count++;
			        	
		    			//allWays.add(entityContainer);
		    			availableWays.set(entity.getId());

						WayContainer wayc = (WayContainer) entityContainer;
						Way way = wayc.getEntity();
						//System.out.println("Weg "+way.getWayNodes()+"===");
						List<WayNode> actwaynodes = way.getWayNodes();
						System.out.println("Weg enthält Anzahl knoten: "+actwaynodes.size());
						Integer lfdnr = 0;
						Double lon_sum = 0.0D;
						Double lat_sum = 0.0D;
						List<Point> points = new LinkedList<Point>();
						for (WayNode waynode: actwaynodes) {
							Node actnode = gibmirnodes.get(waynode.getNodeId());
							Point actpoint = new Point(actnode.getLongitude(), actnode.getLatitude());
							lon_sum += actnode.getLongitude();
							lat_sum += actnode.getLatitude();
							points.add(actpoint);
							//System.out.println(" Node # " + lfdnr + "    id: " + actnode.getId() + "    lon: " + actnode.getLongitude() + "   lat: "+actnode.getLatitude());
							lfdnr++;
						}
		        		Collection<Tag> waytags = way.getTags();
						for (Tag tag: waytags) {
		        			System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
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
			        	System.out.println("Relation   " + entity.toString());
			        	List<RelationMember> relmembers =  ((Relation) entity).getMembers();

						RelationContainer relationc = (RelationContainer) entityContainer;
						Relation relation = relationc.getEntity();
			        	
		        		Collection<Tag> relationtags = entity.getTags();
		        		String relationType = "";
		        		String relationName = "";
						for (Tag tag: relationtags) {
		        			System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
		        			if(	tag.getKey().equals("type"))
		        				relationType = tag.getValue();
		        			if(	tag.getKey().equals("name"))
		        				relationName = tag.getValue();
		        			if(tag.getKey().equals("addr:housenumber"))
				    			gibmirrelations.put(entity.getId(), relation);
						}

						if(! relationType.equals("associatedStreet")) {
							System.out.println("Relation is not of type");
							return;
			        	}
						if(relationName.equals("")) {
							System.out.println("Relation has no name Tag, will be ignored");
							return;
			        	}

						System.out.println("  Anzahl Member: "+relmembers.size());


			        	for(int memberi = 0; memberi < relmembers.size(); memberi++) {
if(memberi > 0)
	System.out.println("mehr als 1 Member, aktiv: "+memberi);
			        		RelationMember actmember = relmembers.get(memberi);
			        		EntityType memberType = actmember.getMemberType();
			        		long memberId = actmember.getMemberId();

			        		System.out.println("relation member ["+memberi+"]  Typ: "+memberType+"   ==="+actmember.toString()+"===   Role ==="+actmember.getMemberRole()+"===");

			        		if(actmember.getMemberRole().equals("street"))		// ignore relation member with role street
			        			continue;

			        		if (EntityType.Node.equals(memberType)) {
			    				if (availableNodes.get(memberId)) {
			    					System.out.println("in Relation Member vom Type   NODE enthalten  ==="+gibmirnodes.get(memberId).toString()+"===");
					        		System.out.println("  Hier die Tags des Node:  "+gibmirnodes.get(memberId).getTags().toString()+"===");
					        		Collection<Tag> nodetags = gibmirnodes.get(memberId).getTags();
									for (Tag tag: nodetags) {
					        			System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
									}
									nodetags.add(new Tag("addr:street", relationName));

					        		Collection<Tag> changednodetags = gibmirnodes.get(memberId).getTags();
									for (Tag tag: changednodetags) {
					        			System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
									}
			    				}
			    			} else if (EntityType.Way.equals(memberType)) {
			    				if (availableWays.get(memberId)) {
			    					System.out.println("in Relation Member vom Type   WAY 0enthalten  ==="+gibmirways.get(memberId).toString()+"===");
			    					Collection<Tag> waytags = gibmirways.get(memberId).getTags();
									for (Tag tag: waytags) {
					        			System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
									}
									waytags.add(new Tag("addr:street", relationName));

					        		Collection<Tag> changedwaytags = gibmirways.get(memberId).getTags();
									for (Tag tag: changedwaytags) {
					        			System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
									}

									Way actway = gibmirways.get(memberId);
									List<WayNode> actwaynodes = actway.getWayNodes();
									System.out.println("Weg enthält Anzahl knoten: "+actwaynodes.size());
									Integer lfdnr = 0;
									List<Point> points = new LinkedList<Point>();
									for (WayNode waynode: actwaynodes) {
										Node actnode = gibmirnodes.get(waynode.getNodeId());
										Point actpoint = new Point(actnode.getLongitude(), actnode.getLatitude());
										points.add(actpoint);
										//System.out.println(" Node # " + lfdnr + "    id: " + actnode.getId() + "    lon: " + actnode.getLongitude() + "   lat: "+actnode.getLatitude());
										lfdnr++;
									}

									/*
									WayGeometryBuilder waygeombuilder = new WayGeometryBuilder(NodeLocationStoreType.TempFile);
									//LineString linestring = waygeombuilder.createWayLinestring(actway);
									LineString linestring = waygeombuilder.createLinestring(points);
									System.out.println("erstellter Linestring ==="+linestring.toString()+"===");
									 */

						    		
			    				}
			    			}
			        	
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
			osmfilereader = new XmlReader(tempfile, true, CompressionMethod.None);

			osmfilereader.setSink(sinkImplementation);

			Thread readerThread = new Thread(osmfilereader);
			readerThread.start();

			while (readerThread.isAlive()) {
		        readerThread.join();
			}
		} catch (OsmosisRuntimeException osmosiserror) {
			System.out.println("es folgt ein osmosis runtime fehler ...");
			osmosiserror.printStackTrace();
	    } catch (InterruptedException e) {
	        /* do nothing */
		} catch (MalformedURLException mue) {
			System.out.println("Error due to getting mapquest api-request (malformedurlexception). url was ==="+url_string+"===");					
			System.out.println(mue.toString());
			String local_messagetext = "MalformedURLException: URL-Request was ==="+url_string+"=== and got Trace ==="+mue.toString()+"===";
			return housenumbers;
		} catch (IOException ioe) {
			System.out.println("Error due to getting mapquest api-request (ioexception). url was ==="+url_string+"===");					
			System.out.println(ioe.toString());
			String local_messagetext = "IOException: URL-Request was ==="+url_string+"=== and got Trace ==="+ioe.toString()+"===";
			return housenumbers;
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
	System.out.println("FEHLER FEHLER: ReadListFromDB has not been coded yet, CANCEL");
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

				Housenumber newofficialhousenumber = new Housenumber(evaluation.getHousenumberlist().ishousenumberadditionCaseSentity());

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
