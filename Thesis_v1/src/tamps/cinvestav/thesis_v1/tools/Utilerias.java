package tamps.cinvestav.thesis_v1.tools;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticCurve;
import org.gavaghan.geodesy.GlobalCoordinates;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import tamps.cinvestav.thesis_v1.model.Registro;
import tamps.cinvestav.thesis_v1.model.RegistroAcelerometro;
import tamps.cinvestav.thesis_v1.model.RegistroGPS;
import tamps.cinvestav.thesis_v1.model.Usuario;
import tamps.cinvestav.thesis_v1.services.Servicio;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

/***
 * Implementa algunos mŽtodos œtiles para la aplicaci—n como parsers,
 * constructor de archivos XML, etc.
 * 
 * @author Rafael
 * 
 */
public class Utilerias {

	private final static String TIPO_ACELEROMETRO = "acelerometro";
	private final static String TIPO_GPS = "GPS";

	public final static String STRING_BUSQUEDA_SIN_RESULTADOS = "vacio";
	public final static String STRING_USUARIO_EXISTE = "existe";
	// public final static String STRING_URL_SERVICIO =
	// "http://148.247.204.33/computoMovil/proyecto/";

	// iMac
	// public final static String STRING_URL_SERVICIO =
	// "http://148.247.204.186/";

	// RoadRunner @home
	public final static String STRING_URL_SERVICIO = "http://192.168.0.101/";

	// // RoadRunner @home->Kachinet
	// public final static String STRING_URL_SERVICIO = "http://192.168.1.106/";

	// Direct link between android & RoadRunner
	// public final static String STRING_URL_SERVICIO =
	// "http://192.168.137.1/tesis/";

	// RoadRunner @sergio's home
	// public final static String STRING_URL_SERVICIO = "http://192.168.1.66/";

	public final static int INT_USUARIO_EXISTE = -1;
	public final static int INT_BUSQUEDA_USUARIO_NO_EXITOSA = -1;
	public final static int INT_ERROR_AL_SUBIR_IMAGEN = -2;

	public final static int INT_RED_WIFI_NO_DISPONIBLE = 0;
	public final static int INT_RED_WIFI_DISPONIBLE = 1;

	// public final static int INT_TODOS_LOS_ARCHIVOS = -1;

	private final static SimpleDateFormat formato = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	/***
	 * Obtiene el String que el servidor remoto ha entregado como respuesta
	 * 
	 * @param is
	 *            El InputStream que existe entre la aplicaci—n y el servidor
	 * @return La cadena devuelta por el servidor. Null en caso de haber una
	 *         excepci—n
	 */
	public static String obtenerStringRespuesta(InputStream is) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			String linea = br.readLine();
			while (linea != null) {
				sb.append(linea);
				linea = br.readLine();
			}
			br.close();
			Log.i("Cinvestav Utilerias obtenerStringRespuesta",
					"Cadena ha sido obtenida desde el servidor");
		} catch (Exception e) {
			Log.e("Cinvestav Utilerias obtenerStringRespuesta", e.getMessage());
			return null;
		}
		return sb.toString();
	}

	/***
	 * Crea un usuario a partir de un string en formato JSON devuelto por el
	 * servidor
	 * 
	 * @param cadena
	 *            La cadena JSON en base a la cual se crear‡ un usuario
	 * @return El usuario creado
	 * @throws JSONException
	 *             Si existe un error al hacer el parsing JSON (si no existen
	 *             los atributos especificados)
	 * @throws ParseException
	 *             Si se produce un error al transformar la fecha devuelta por
	 *             el servidor al formato "Java"
	 */
	public static Usuario crearUsuarioDesdeStringJson(String cadena)
			throws JSONException, ParseException {
		Log.i("Cinvestav Utilerias cUsuarioDSJSON", "cadena recibida: "
				+ cadena);
		// Se debe de crear un objeto Usuario en base al JSON de respuesta
		// Se deben eliminar los corchetes si es que aparecen al inicio y al
		// final...
		if (cadena.startsWith("[")) {
			cadena = cadena.substring(1, cadena.length() - 1);
		}

		JSONObject objJSON = new JSONObject(cadena);
		int id = objJSON.getInt("id");
		String nombre = objJSON.getString("nombre");
		String username = objJSON.getString("username");
		String password = objJSON.getString("password");
		String strFecha = objJSON.getString("fecha_ingreso");
		Date fechaIngreso = formato.parse(strFecha);
		return new Usuario(id, nombre, username, password, fechaIngreso);
	}

	/***
	 * Escribe la cadena especificada como argumento en el archivo especificado
	 * 
	 * @param cadena
	 *            La cadena de texto a escribir
	 * @param nombreArchivoSalida
	 *            el nombre de archivo salida (puede ser un path que incluya al
	 *            nombre de archivo)
	 */
	public static void escribirCadenaEnArchivo(String cadena,
			String nombreArchivoSalida) {
		try {
			String ruta = new StringBuilder(Environment
					.getExternalStorageDirectory().getAbsolutePath()).append(
					"/").toString();

			Log.i("Cinvestav escribirArchivo", "Tratando de escribir en "
					+ ruta);

			FileWriter archivoSalida = new FileWriter(new StringBuilder(ruta)
					.append(nombreArchivoSalida).toString());
			archivoSalida.write(cadena);
			archivoSalida.close();
			Log.i("Utilerias escribirArchivo", "Archivo " + nombreArchivoSalida
					+ " ha sido creado");
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("Utilerias escribirArchivo",
					"No se ha podido crear el archivo " + nombreArchivoSalida);
		}
	}

	/***
	 * Crea una cadena XML lista para ser utilizada. El origen es una lista de
	 * tipo Registro.
	 * 
	 * @param lista
	 *            La lista de Registro a ser transformada. Permite que haya una
	 *            mezcla de registros de acelerometro, GPS, etc.
	 * @return Una cadena en formato XML representando la lista de registros.
	 *         Null en caso de encontrarse un error
	 */
	public static String crearXml(List<Registro> lista) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Log.i("Cinvestav Utilerias CrearXml", "Intentando crear Xml de "
				+ lista.size() + " elementos");
		try {
			serializer.setOutput(writer);

			serializer.setFeature(
					"http://xmlpull.org/v1/doc/features.html#indent-output",
					true);
			serializer.startDocument("UTF-8", true);

			serializer.startTag("", "raiz");
			for (Registro registro : lista) {
				serializer.startTag("", "registro");

				// serializer.attribute("", "timestamp",
				// String.valueOf(registro.getTimestamp().getTime()));
				serializer.attribute("", "timestamp",
						sdf.format(registro.getTimestamp()));
				if (registro instanceof RegistroAcelerometro) {
					serializer.attribute("", "tipo", TIPO_ACELEROMETRO);
					serializer.startTag("", "ejeX");
					serializer.text(String
							.valueOf(((RegistroAcelerometro) registro)
									.getEjeX()));
					serializer.endTag("", "ejeX");

					serializer.startTag("", "ejeY");
					serializer.text(String
							.valueOf(((RegistroAcelerometro) registro)
									.getEjeY()));
					serializer.endTag("", "ejeY");

					serializer.startTag("", "ejeZ");
					serializer.text(String
							.valueOf(((RegistroAcelerometro) registro)
									.getEjeZ()));
					serializer.endTag("", "ejeZ");
				} else if (registro instanceof RegistroGPS) {
					serializer.attribute("", "tipo", TIPO_GPS);
					serializer.startTag("", "latitud");
					serializer.text(String.valueOf(((RegistroGPS) registro)
							.getLatitud()));
					serializer.endTag("", "latitud");

					serializer.startTag("", "longitud");
					serializer.text(String.valueOf(((RegistroGPS) registro)
							.getLongitud()));
					serializer.endTag("", "longitud");

					serializer.startTag("", "precision");
					serializer.text(String.valueOf(((RegistroGPS) registro)
							.getPrecision()));
					serializer.endTag("", "precision");
				}

				serializer.endTag("", "registro");
			}
			serializer.endTag("", "raiz");
			serializer.endDocument();
			Log.i("Cinvestav Utilerias crearXML",
					"La cadena XML ha sido creada");
			return writer.toString();

		} catch (IOException e) {
			Log.e("Cinvestav Utilerias crearXml",
					"No se pudo crear la cadena XML " + e.getMessage());
			return null;
		}
	}

	/***
	 * Convierte un String JSON en una lista de objetos que parten de la clase
	 * Registro, es decir, retorna una lista de objetos que pueden ser
	 * RegistroGPS y RegistroAcelerometro
	 * 
	 * @param cadena
	 *            La cadena con contenido JSON
	 * @return Una LinkedList con objetos de clases que hereden a Registro (En
	 *         este momento RegistroGPS y RegistroAcelerometro)
	 */
	private static LinkedList<Registro> crearJSON(String cadena) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		LinkedList<Registro> respuesta = new LinkedList<Registro>();
		try {
			JSONArray array = new JSONArray(cadena);
			int cantRegistros = array.length();
			Registro registroActual = null;
			for (int i = 0; i < cantRegistros; i++) {
				JSONObject objetoRegistro = array.getJSONObject(i);

				Date timestamp = sdf.parse(objetoRegistro
						.getString("timestamp"));
				String tipo = objetoRegistro.getString("tipo");
				if (RegistroAcelerometro.TIPO.equals(tipo)) {
					float ejeX = (float) objetoRegistro.getDouble("ejeX");
					float ejeY = (float) objetoRegistro.getDouble("ejeY");
					float ejeZ = (float) objetoRegistro.getDouble("ejeZ");

					registroActual = new RegistroAcelerometro(timestamp, ejeX,
							ejeY, ejeZ);
				} else if (RegistroGPS.TIPO.equals(tipo)) {
					double latitud = objetoRegistro.getDouble("latitud");
					double longitud = objetoRegistro.getDouble("longitud");
					float precision = (float) objetoRegistro
							.getDouble("precision");
					registroActual = new RegistroGPS(timestamp, latitud,
							longitud, precision);
				}
				// Sea cual sea el tipo, se agrega a la lista de resultados.
				respuesta.add(registroActual);
			}
		} catch (Exception whatever) {
			System.out.println(whatever.getMessage());
			whatever.printStackTrace();
		}

		return respuesta;
	}

	/***
	 * Determina si existe una conexi—n Wi-Fi en un instante en el dispositivo
	 * m—vil
	 * 
	 * @param servicio
	 * @return Si existe conexi—n WiFi o no
	 */
	public static boolean verificarEstatusRed(Servicio servicio) {
		boolean hayWifiConectada = false;
		// boolean hayRedMovilConectada = false;

		ConnectivityManager cm = (ConnectivityManager) servicio
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if ("WIFI".equals(ni.getTypeName()))
				if (ni.getTypeName().equalsIgnoreCase("WIFI"))
					if (ni.isConnected())
						hayWifiConectada = true;
		}

		return hayWifiConectada;

		// FIXME solo cuando se estŽ probando el middleware en el emulador, por
		// la cuesti—n del GPS
		// return true;
		// || hayRedMovilConectada;
	}

	public static double distancia(RegistroGPS p, RegistroGPS q) {
		// instantiate the calculator
		GeodeticCalculator geoCalc = new GeodeticCalculator();

		// select a reference elllipsoid
		Ellipsoid reference = Ellipsoid.WGS84;

		// set origin coordinates
		GlobalCoordinates origin;
		origin = new GlobalCoordinates(p.getLatitud(), p.getLongitud());

		// set destination coordinates
		GlobalCoordinates destination;
		destination = new GlobalCoordinates(q.getLatitud(), q.getLongitud());

		// calculate the geodetic curve
		GeodeticCurve geoCurve = geoCalc.calculateGeodeticCurve(reference,
				origin, destination);
		return geoCurve.getEllipsoidalDistance();
	}

	/***
	 * Indica si una ubicaci—n es v‡lida, es decir si fue realmente obtenida por
	 * el Listener de ubicaci—n (LECURA_DISPONIBLE)
	 * 
	 * @param ubicacionGPS
	 *            La ubicaci—n GPS a evaluar
	 * @return Si la ubicaci—n especificada fue obtenida como LECTURA_DISPONIBLE
	 *         o no
	 */
	public static boolean esUbicacionValida(RegistroGPS ubicacionGPS) {
		if (ubicacionGPS.getPrecision() < 0)
			return false;
		return true;
	}
}
