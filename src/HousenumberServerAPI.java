import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;



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
 *
 * Connection to housenumber server part via Server API
 * <p>
 * You can get housenumber lists from the server or upload evaluation result to the server
 * 
 * @author Dietmar Seifert
 * 	
*/




public class HousenumberServerAPI {
	//private String serverUrl = "http://regio-osm.de";
	//private String serverUrl = "http://localhost";
	private String serverUrl = "http://localhost:8080"; //     /housenumberserverJavaServlet/Upload
//TODO configuration

	public boolean writeEvaluationToServer(Evaluation evaluation, HousenumberCollection result) {

		String result_content = result.toString("\t", true);

		String filename = "";
		try {
				// first, save upload data as local file, just for checking or for history
			DateFormat time_formatter = new SimpleDateFormat("yyyyMMdd-HHmmssZ");
			String uploadtime = time_formatter.format(new Date());
	
			filename += "uploaddata" + "/" + uploadtime + ".result";
			System.out.println("uploaddatei ===" + filename + "===");

			File outputfile = new File(filename);
			outputfile.createNewFile();
			outputfile.setReadable(true);
			outputfile.setWritable(true);
			outputfile.setExecutable(false);

			PrintWriter osmOutput = null;
			osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename),StandardCharsets.UTF_8)));
			
			osmOutput.println(result_content);
			osmOutput.close();
			
			
			String url_string = serverUrl + "/housenumberserverAPI/Upload";
//TODO configuration
			System.out.println(" will upload result to server with url ===" + url_string + "===");


				// now upload as multipart/form-data request - should work also for larger content
				//http://stackoverflow.com/questions/2469451/upload-files-with-java
				// ===================================================================================================================
			java.util.Date sendToServerStarttime = new java.util.Date();
			
			String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.

			URLConnection connection = new URL(url_string).openConnection();
			connection.setDoOutput(true); // This sets request method to POST.
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			PrintWriter writer = null;
			try {
			    writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
			    StringBuffer temptoutput = new StringBuffer();
			    	// please note, that all \r\n are necessary. \n is not enough 
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"country\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(evaluation.getCountry() + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"municipality\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(evaluation.getMunicipality() + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"result\"; filename=\"result.txt\"" + "\r\n");
			    temptoutput.append("Content-Type: text/plain; charset=" + StandardCharsets.UTF_8.toString() + "\r\n");
			    temptoutput.append("\r\n");
	            temptoutput.append(result_content + "\r\n");
			    temptoutput.append("--" + boundary + "--" + "\r\n");
			    System.out.println("Upload-Request Start ===\n" + temptoutput.toString().substring(0, 8000) + "\n===");
			    System.out.println("Upload-Request Ende ===\n" + temptoutput.toString().substring(temptoutput.toString().length() - 1000, temptoutput.toString().length()) + "\n===");
			    writer.println(temptoutput.toString());
			} finally {
			    if (writer != null) writer.close();
			}

			// Connection is lazily executed whenever you request any status.
			int responseCode = ((HttpURLConnection) connection).getResponseCode();
			System.out.println(responseCode); // Should be 200
				// ===================================================================================================================


			Integer headeri = 1;
			System.out.println("Header-Fields Ausgabe ...");
			while(((HttpURLConnection) connection).getHeaderFieldKey(headeri) != null) {
				System.out.println("  Header # "+headeri+":  ["+((HttpURLConnection) connection).getHeaderFieldKey(headeri)+"] ==="+((HttpURLConnection) connection).getHeaderField(headeri)+"===");
				headeri++;
			}

			java.util.Date sendToServerEndtime = new java.util.Date();
			System.out.println("Duration for wating to Server response after upload result content in ms: " + (sendToServerStarttime.getTime() - sendToServerStarttime.getTime()));
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuffer xml_content = new StringBuffer();
			String fileline = "";
			while((fileline = reader.readLine()) != null) {
				xml_content.append(fileline + "\n");
				System.out.println(fileline);
			}
			writer.close();
			reader.close();
			//only httpurl not url...     connection.disconnect();
		
		}
		catch (MalformedURLException mue) {
			System.out.println("ERROR: MalformedURLException, Details ...");
			mue.printStackTrace();
			return false;
		} 
		catch (IOException ioe) {
			System.out.println("ERROR: IOException, Details ...");
			ioe.printStackTrace();
			return false;
		}
		return true;
	}


	public HousenumberCollection ReadListFromServer(Evaluation evaluation) {
		final HousenumberCollection housenumbers = new HousenumberCollection();


		if(		(evaluation.getCountry().equals(""))
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
	
		return housenumbers;
	}
}
