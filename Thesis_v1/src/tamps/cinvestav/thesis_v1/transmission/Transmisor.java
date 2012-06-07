package tamps.cinvestav.thesis_v1.transmission;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import tamps.cinvestav.thesis_v1.nal.ClientHTTPRequest;
import tamps.cinvestav.thesis_v1.services.Servicio;
import tamps.cinvestav.thesis_v1.tools.Utilerias;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

/***
 * 
 * @author Rafael
 * 
 */
public class Transmisor {

	private static final String Tag = "Transmisor";
	/***
	 * Referencia al servicio 'padre' que crea a este Transmisor
	 */
	private Servicio servicio = null;

	/***
	 * Referencia al handler del servicio para enviarle mensajes relacionados
	 * con el estado de la red.
	 */
	private Handler handlerServicio = null;

	/***
	 * Timer para llevar a cabo la tarea de verificación de estado de la red
	 */
	private Timer timerVer = null;

	/***
	 * TimerTask para verificar el estado de la red
	 */
	private TareaVerificarEstatusRed tareaVer = null;

	/***
	 * Indica si se borran los archivos después de sincronizarlos con la nube
	 */
	private boolean mantenerArchivos = true;

	public boolean isMantenerarchivos() {
		return mantenerArchivos;
	}

	/***
	 * Define el intervalo de tiempo entre cada 'monitorización' de la red
	 */
	private long intervaloMonitorizacionRed = 0;

	private ClientHTTPRequest clienteHttp = null;

	/***
	 * Define la cantidad de archivos que deberán ser enviados por el transmisor
	 * en cuanto se detecte la disponibilidad de una red WiFi por el smartphone. <br />
	 * <b>Utilerias.INT_TODOS_LOS_ARCHIVOS</b> envía toda la lista.
	 */
	private int tamanyoVentanaArchivos = 5;

	/***
	 * Referencia al id del usuario activo. Si es negativo, es por que no se ha
	 * realizado un login, lo cual desencadenará excepciones en cuanto se
	 * intente llevar a cabo la transmisión de los archivos.
	 */
	private int idUsuarioActivo = -2;

	public Transmisor(Servicio servicio, long intervaloMonitorizacionRed,
			int tamanyoVentanaArchivos, boolean mantenerArchivos) {
		super();
		this.servicio = servicio;
		this.handlerServicio = servicio.getHandlerTransmisor();

		if (intervaloMonitorizacionRed <= 0) {
			Log.e(Tag,
					"Constructor: intervaloMonitorizacionRed debe ser mayor a 0");
			throw new IllegalArgumentException(
					"El valor del parámetro intervaloMonitorizacionRed debe ser mayor a 0. El valor es: "
							+ intervaloMonitorizacionRed);
		}
		this.intervaloMonitorizacionRed = intervaloMonitorizacionRed;

		if (tamanyoVentanaArchivos <= 0) {
			Log.e(Tag, "Constructor: tamanyoVentanaArchivos debe ser mayor a 0");
			throw new IllegalArgumentException(
					"El valor del parámetro tamanyoVentanaArchivos debe ser mayor a 0. El valor es: "
							+ tamanyoVentanaArchivos);
		}
		this.tamanyoVentanaArchivos = tamanyoVentanaArchivos;
		this.mantenerArchivos = mantenerArchivos;
		timerVer = new Timer();
		tareaVer = new TareaVerificarEstatusRed();
		if (servicio.getUsuarioActivo() == null) {
			Log.e(Tag, "Constructor: usuario activo es nulo. ¿Hubo un login?");
			throw new IllegalArgumentException(
					"Constructor: usuario activo es nulo. ¿Hubo un login?");
		}

		idUsuarioActivo = servicio.getUsuarioActivo().getId();
	}

	// [start] Métodos para el envío de archivos
	// TODO Verificar que el envío de archivos no 'ralentice' el funcionamiento
	// de la aplicación que utiliza al middleware

	/***
	 * Método que envía ÚNICAMENTE el primer archivo de la cola hacia el
	 * servidor remoto.
	 * 
	 * @throws IOException
	 */
	public void enviarArchivo(Queue<String> archivos) throws IOException {
		Log.i(Tag, "enviarArchivos");
		if (idUsuarioActivo < 0) {
			Log.e(Tag, "El id del usuario activo no es valido: "
					+ idUsuarioActivo + " ¿Hubo un login?");
			throw new IllegalArgumentException(
					"El id del usuario activo no es valido: " + idUsuarioActivo
							+ " ¿Hubo un login?");
		}
		synchronized (archivos) {
			this.clienteHttp = new ClientHTTPRequest(
					Utilerias.STRING_URL_SERVICIO + "upload.php");
			String nombreArchivo = archivos.poll();
			// Si aun existen archivos...
			if (nombreArchivo != null) {
				File archivo = new File(
						Environment.getExternalStorageDirectory() + "/"
								+ nombreArchivo);
				clienteHttp.setParameter("idUsuario", idUsuarioActivo);
				clienteHttp.setParameter("archivo", archivo);
				Log.i(Tag, "enviarArchivo Intentando enviar el archivo "
						+ nombreArchivo);
				InputStream streamRespuesta = clienteHttp.post();

				// Hacer lo que sea adecuado con respuesta
				String respuesta = Utilerias
						.obtenerStringRespuesta(streamRespuesta);
				Log.i(Tag, "Archivo " + nombreArchivo
						+ " enviado, respuesta del servidor: " + respuesta);

				if (!isMantenerarchivos()) {
					archivo.delete();
				}
			}
		}
	}

	/***
	 * Método que envìa el Top [tamanyoVentanaArchivos] de archivos presentes en
	 * la lista.
	 * 
	 * @throws IOException
	 *             Si el clienteHttp no puede 'adjuntarlo' como parámetro.
	 */
	public void enviarArchivos(Queue<String> archivos) throws IOException {
		Log.i(Tag, "enviarArchivos");
		if (idUsuarioActivo < 0) {
			Log.e(Tag, "El id del usuario activo no es valido: "
					+ idUsuarioActivo + " ¿Hubo un login?");
			throw new IllegalArgumentException(
					"El id del usuario activo no es valido: " + idUsuarioActivo
							+ " ¿Hubo un login?");
		}
		if (archivos.size() == 0) {
			Log.w(Tag, "enviarArchivos Nada por enviar...");
			return;
		}
		LinkedList<File> listaTmpArchivos = new LinkedList<File>();
		synchronized (archivos) {
			this.clienteHttp = new ClientHTTPRequest(
					Utilerias.STRING_URL_SERVICIO + "upload.php");
			clienteHttp.setParameter("idUsuario", idUsuarioActivo);

			int i = 0;
			Log.i(Tag, "enviarArchivos enviando " + archivos.size()
					+ " archivos");
			while (archivos.size() != 0 && i < tamanyoVentanaArchivos) {
				String nombreArchivo = archivos.poll();
				File archivo = new File(
						Environment.getExternalStorageDirectory() + "/"
								+ nombreArchivo);

				// clienteHttp.setParameter(nombreArchivo, archivo);
				clienteHttp.setParameter(nombreArchivo, archivo);
				Log.i(Tag, "Intentando enviar el archivo " + nombreArchivo);

				listaTmpArchivos.add(archivo);
				// Log.i(Tag, "Archivo " + nombreArchivo
				// + " enviado, respuesta del servidor: " + respuesta);
				i++;
			}
		}
		InputStream streamRespuesta = clienteHttp.post();
		// Hacer lo que sea adecuado con respuesta
		String respuesta = Utilerias.obtenerStringRespuesta(streamRespuesta);
		Log.i(Tag, "enviarArchivos El servidor ha respondido: " + respuesta);
		if (isMantenerarchivos()) {
			for (File archivo : listaTmpArchivos) {
				archivo.delete();
			}
			listaTmpArchivos = null;
		}

	}

	/***
	 * Método que envía todos los archivos presentes en la cola. Este método
	 * puede generar retrasos.
	 * 
	 * @throws IOException
	 *             Si el clienteHttp no puede 'adjuntarlos' como parámetro.
	 */
	public void enviarTodosArchivos(Queue<String> archivos) throws IOException {
		Log.i(Tag, "enviarTodosArchivos");
		if (idUsuarioActivo < 0) {
			Log.e(Tag, "El id del usuario activo no es valido: "
					+ idUsuarioActivo + " ¿Hubo un login?");
			throw new IllegalArgumentException(
					"El id del usuario activo no es valido: " + idUsuarioActivo
							+ " ¿Hubo un login?");
		}
		LinkedList<File> listaTmpArchivos = new LinkedList<File>();
		synchronized (archivos) {
			this.clienteHttp = new ClientHTTPRequest(
					Utilerias.STRING_URL_SERVICIO + "upload.php");
			clienteHttp.setParameter("idUsuario", idUsuarioActivo);

			while (!archivos.isEmpty()) {
				String nombreArchivo = archivos.poll();
				File archivo = new File(
						Environment.getExternalStorageDirectory() + "/"
								+ nombreArchivo);

				// clienteHttp.setParameter(nombreArchivo, archivo);
				clienteHttp.setParameter(nombreArchivo, archivo);
				Log.i(Tag, "Intentando enviar el archivo " + nombreArchivo);

				// Log.i(Tag, "Archivo " + nombreArchivo
				// + " enviado, respuesta del servidor: " + respuesta);
				listaTmpArchivos.add(archivo);
			}
		}
		InputStream streamRespuesta = clienteHttp.post();
		// Hacer lo que sea adecuado con respuesta
		String respuesta = Utilerias.obtenerStringRespuesta(streamRespuesta);
		Log.i(Tag, "enviarArchivos El servidor ha respondido: " + respuesta);
		if (isMantenerarchivos()) {
			for (File archivo : listaTmpArchivos) {
				archivo.delete();
			}
			listaTmpArchivos = null;
		}
	}

	// [end]

	// [start] Interfaces para monitorizar la red. Métodos llamados desde el
	// servicio.
	/***
	 * Inicia la monitorización de la red, desencadenando los timers necesarios
	 * para llevarlo a cabo. Antes de utilizar este método, el transmisor debe
	 * ser configurado. @See Transmisor() Constructor
	 */
	public void iniciarMonitorizacionRed() {
		if (timerVer == null) {
			timerVer = new Timer();
			tareaVer = new TareaVerificarEstatusRed();
		}
		timerVer.scheduleAtFixedRate(tareaVer, 0, intervaloMonitorizacionRed);
	}

	/***
	 * Detiene la monitorización/chequeo del estado de la red
	 */
	public void detenerMonitorizacionRed() {
		timerVer.cancel();
		timerVer = null;
	}

	/***
	 * Clase que funciona como una tarea que verifica el estatus de la red,
	 * detectando el momento en cuanto haya una red wifi disponible
	 * notificándolo al servicio 'padre'
	 * 
	 * @author Rafael
	 * 
	 */
	private class TareaVerificarEstatusRed extends TimerTask {

		@Override
		public void run() {

			boolean hayRedWifi = Utilerias.verificarEstatusRed(servicio);

			if (hayRedWifi) {
				handlerServicio
						.sendEmptyMessage(Utilerias.INT_RED_WIFI_DISPONIBLE);
			} else {
				handlerServicio
						.sendEmptyMessage(Utilerias.INT_RED_WIFI_NO_DISPONIBLE);
			}
		}
	};
	// [end]

}
