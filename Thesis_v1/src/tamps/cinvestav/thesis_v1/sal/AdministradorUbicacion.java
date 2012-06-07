package tamps.cinvestav.thesis_v1.sal;

import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import tamps.cinvestav.thesis_v1.model.RegistroGPS;
import tamps.cinvestav.thesis_v1.services.Servicio;
import tamps.cinvestav.thesis_v1.tools.Utilerias;
import android.content.Context;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/***
 * 
 * @author Rafael
 * 
 */
public class AdministradorUbicacion {
	private final static String Tag = "AdministradorUbicacion";

	// [start] Handler, mecanismos para controlar el comportamiento del
	// administrador despu�s de recibir la notificaci�n de una lectura
	// disponible o no disponible.
	/***
	 * Indica que no se pudo obtener una ubicaci�n GPS
	 */
	public final static int LECTURA_NO_DISPONIBLE = 1;

	/***
	 * Indica que si se pudo obtener una ubicaci�n GPS
	 */
	public final static int LECTURA_DISPONIBLE = 0;

	/***
	 * Referencia al handler del servicio para enviarle mensajes relacionados
	 * con la toma de valores del GPS.
	 */
	private Handler handlerServicio = null;

	/***
	 * Handler para conectarse con el listener del GPS y recibir sus mensajes.
	 */
	private Handler handlerListener = new Handler() {
		public void handleMessage(Message msj) {

			switch (msj.what) {
			case LECTURA_DISPONIBLE:
				// La lectura fue obtenida y se encuentra en la lista.
				// Debe de cancelarse el timer y la tarea de fin de tiempo de
				// espera...

				// Log.i(Tag,
				// "Handler listener: purgando y cancelando timerTmpoEspera");
				timerFinTiempoEspera.purge();
				timerFinTiempoEspera.cancel();

				RegistroGPS r = miListenerGPS.getRegistroActual();

				servicio.getRegistrosGPS().add(r);
				Log.i(Tag,
						"Handler Listener: se obtuvo lectura " + r.toString());

				historial.add(r);
				handlerServicio.sendEmptyMessage(LECTURA_DISPONIBLE);
				detenerLecturasGPS();
				// TODO l�gica de recalendarizacion aqui
				delayEntreLecturas = obtenerDelaySiguienteLectura(
						LECTURA_DISPONIBLE, historial);
				break;
			case LECTURA_NO_DISPONIBLE:
				r = new RegistroGPS(new Date(), 0.0, 0.0, -2.0f);
				Log.w(Tag,
						"Handler Listener: NO se obtuvo lectura "
								+ r.toString());
				servicio.getRegistrosGPS().add(r);
				historial.add(r);
				handlerServicio.sendEmptyMessage(LECTURA_NO_DISPONIBLE);
				detenerLecturasGPS();

				// TODO l�gica de recalendarizacion aqui
				delayEntreLecturas = obtenerDelaySiguienteLectura(
						LECTURA_NO_DISPONIBLE, historial);
				break;
			}
			recalendarizacion(delayEntreLecturas);
		}
	};

	// [start] Mecanismos para controlar el probable 'TIMEOUT' de las lecturas
	/***
	 * Timer auxiliar empleado para detectar un timeOut en el proceso de lectura
	 * del GPS
	 */
	Timer timerFinTiempoEspera = null;

	/***
	 * TimerTask auxiliar para detectar que no hubo lectura GPS despu�s de un
	 * timeOut
	 */
	private TareaTiempoEsperaFinalizado tareaTiempoEsperaFinalizado = null;

	/***
	 * Clase TimerTask auxiliar que modela la situaci�n de no haber detectado
	 * una lectura GPS despu�s de un timeOut
	 */
	private class TareaTiempoEsperaFinalizado extends TimerTask {

		@Override
		public void run() {
			// No se ha obtenido respuesta, se debe enviar un mensaje al
			// servicio indic�ndolo
			Log.w(Tag,
					"Tiempo de espera finalizado, no se report� ubicaci�n GPS");
			handlerListener.sendEmptyMessage(LECTURA_NO_DISPONIBLE);
		}
	}

	// [end]

	// [end]

	// [start] M�todos para recalendarizar pr�ximas lecturas de ubicaci�n
	private final static int MAX_HISTORIAL = 10;

	/***
	 * Indica la distancia (en metros) que el usuario debe superar entre dos
	 * lecturas de GPS para que el middleware considere que haya realizado un
	 * desplazamiento
	 */
	private double umbralDistancia = 10;

	public double getUmbralDistancia() {
		return umbralDistancia;
	}

	public void setUmbralDistancia(double umbralDistancia) {
		this.umbralDistancia = umbralDistancia;
	}

	/***
	 * Mantiene el valor de la proxima tarea de recalendarizacion
	 */
	private long delayEntreLecturas;

	public long getDelayEntreLecturas() {
		return delayEntreLecturas;
	}

	public void setDelayEntreLecturas(long delayEntreLecturas) {
		this.delayEntreLecturas = delayEntreLecturas;
	}

	/***
	 * Breve lista de las �ltimas lecturas GPS recabadas (Disponibles o No)
	 */
	private LinkedList<RegistroGPS> historial;

	/***
	 * Recalendariza la tarea de obtenci�n de lecturas de GPS para un tiempo
	 * especificado
	 * 
	 * @param delay
	 *            El periodo de espera para iniciar la tarea.
	 */
	private void recalendarizacion(long delay) {
		if (timerRecalendarizacion != null) {
			timerRecalendarizacion.cancel();
			timerRecalendarizacion.purge();
		}
		TareaRecalendarizacion tareaRecalendarizacion = new TareaRecalendarizacion();
		timerRecalendarizacion = new Timer();
		timerRecalendarizacion.schedule(tareaRecalendarizacion, delay);
		Log.i(Tag,
				"TareaRecalendarizarLecturas Se ha vuelto a recalendarizar dentro de "
						+ delay + " milisegundos");
	}

	/***
	 * Timer auxiliar utilizado solamente para recalendarizar la pr�xima lectura
	 * de ubicaci�n.
	 */
	private Timer timerRecalendarizacion = null;

	/***
	 * Timer Task utilizada como 'hack' para recalendarizar la pr�xima lectura
	 * de ubicaci�n.
	 * 
	 * @author Rafael
	 * 
	 */
	private class TareaRecalendarizacion extends TimerTask {
		@Override
		public void run() {
			// Log.i(Tag,
			// "TareaAuxiliar, metodo run: Llamando a iniciarLecturasGPS");
			Looper.prepare();
			iniciarLecturasGPS();
			Looper.loop();
		}
	};

	/***
	 * Obtiene el tiempo de espera para realizar la siguiente operaci�n de
	 * lectura de ubicaci�n en base a una causa y al historial de ubicaciones
	 * anteriores.
	 * 
	 * @param causa
	 *            El motivo por el que se recalendariza (LECTURA_DISPONIBLE,
	 *            LECTURA_NO_DISPONIBLE).
	 * @param historial
	 *            Un breve historial de las lecturas GPS que han sido obtenidas.
	 * @return El delay para la siguiente lectura.
	 */
	private long obtenerDelaySiguienteLectura(int causa,
			LinkedList<RegistroGPS> historial) {
		long r = getDelayEntreLecturas();
		switch (causa) {
		case LECTURA_DISPONIBLE:
			// Si el historial tiene una sola lectura, es la �nica que se tomar�
			// como referencia. Por lo tanto se asume que el usuario est� en
			// movimiento. En conclusi�n, el delay no es modificado.
			if (historial.size() > 1) {
				RegistroGPS ultimo = historial.get(historial.size() - 1);
				RegistroGPS penultimo = historial.get(historial.size() - 2);

				// Si la pen�ltima ubicaci�n fue obtenida, entonces las dos
				// pueden ser consideradas para recalendarizar.
				if (Utilerias.esUbicacionValida(penultimo)) {
					double distancia = Utilerias.distancia(penultimo, ultimo);
					// Si la distancia entre ambos puntos supera al umbral, el
					// usuario est� en movimiento.
					if (distancia > umbralDistancia) {
						r = (long) (getDelayEntreLecturas() * 0.5);
						Log.i(Tag, "obtenerDelay: Hay movimiento del usuario");
						// El umbral no debe ser menor a un minuto.
						if (r < 60000)
							r = 60000;
					}
					// En caso contrario, el usuario no se ha desplazado, el
					// delay debe incrementarse.
					else {
						r = 2 * getDelayEntreLecturas();
						Log.i(Tag, "obtenerDelay: Sin movimiento del usuario");
						// El umbral no debe ser mayor a una hora (1000 * 60 *
						// 60)
						if (r > 3600000) {
							r = 3600000;
							Log.i(Tag, "obtenerDelay: edging");
						}
					}
				}
			}
			break;
		case LECTURA_NO_DISPONIBLE:
			r = 2 * getDelayEntreLecturas();
			// El umbral no debe ser mayor a una hora (1000 * 60 *
			// 60)
			Log.i(Tag, "obtenerDelay: Lectura no fue v�lida, doblando tiempo");
			if (r > 3600000)
				r = 3600000;
			break;
		default:
			throw new RuntimeException(
					"obtenerDelay: La causa especificada para recalendarizar no es v�lida");
		}
		if (historial.size() > MAX_HISTORIAL) {
			int dif = historial.size() - MAX_HISTORIAL;
			for (int i = 0; i < dif; i++) {
				historial.removeFirst();
			}
		}
		return r;
	}

	// [end]

	// [start] Interfaces para iniciar - detener las lecturas de ubicaci�n. Son
	// llamados por el servicio.
	/***
	 * Tiempo a esperar para considerar que (en ausencia de respuesta por parte
	 * del listener de ubicacion) no se pudo obtener la lectura de GPS
	 */
	private long margenTiempoLecturas = 60000;

	public long getMargenTiempoLecturas() {
		return margenTiempoLecturas;
	}

	public void setMargenTiempoLecturas(long margenTiempoLecturas) {
		this.margenTiempoLecturas = margenTiempoLecturas;
	}

	/***
	 * Referencia al servicio 'padre' que cre� a este administrador
	 */
	private Servicio servicio = null;

	/***
	 * Cantidad de tiempo a esperar (inicialmente) entre cada intervenci�n de
	 * lectura del GPS
	 */
	// private long intervaloMuestreoInicial = 0;

	/***
	 * Manager provisto por la API de Android
	 */
	private LocationManager miLocManager = null;

	/***
	 * Un listener para los cambios detectados por el GPS.
	 */
	private ListenerUbicacion miListenerGPS = null;

	/***
	 * Constructor del AdministradorUbicacion
	 * 
	 * @param servicio
	 *            Una referencia al servicio
	 * @param intervaloMuestreoInicial
	 *            Intervalo en milisegundos entre cada lectura del GPS
	 */
	public AdministradorUbicacion(Servicio servicio) {
		super();
		this.servicio = servicio;
		this.handlerServicio = servicio.getHandlerUbicacion();
		this.historial = new LinkedList<RegistroGPS>();
	}

	/***
	 * Inicia las lecturas de GPS en el dispositivo. Si transcurrida cierta
	 * cantidad de tiempo no se obtiene respuesta, se asumir� que fue imposible
	 * obtener la ubicaci�n del dispositivo.
	 */
	public void iniciarLecturasGPS() {
		Log.i(Tag, "iniciarLecturasGPS()");
		if (timerFinTiempoEspera != null) {
			timerFinTiempoEspera.cancel();
			tareaTiempoEsperaFinalizado.cancel();
			timerFinTiempoEspera.purge();
		}

		// wakeLock.acquire();
		timerFinTiempoEspera = new Timer();
		tareaTiempoEsperaFinalizado = new TareaTiempoEsperaFinalizado();

		// Se calendariza la tarea de tiempo de espera finalizado con la
		// intenci�n de darse cuenta de que no hubo lectura
		// Log.i(Tag, "Calendarizando tarea tiempo espera finalizado");
		timerFinTiempoEspera.schedule(tareaTiempoEsperaFinalizado,
				getMargenTiempoLecturas());

		// Se inicia la solicitud de cambios en la ubicaci�n del dispositivo
		Log.i(Tag, "Solicitando actualizaciones de ubicaci�n con un margen de "
				+ getMargenTiempoLecturas());
		// TODO verificar que rollo si es necesaria estas l�nea:
		miLocManager = (LocationManager) servicio
				.getSystemService(Context.LOCATION_SERVICE);

		miListenerGPS = new ListenerUbicacion(handlerListener);

		miListenerGPS.setSensadoActivo(true);
		miLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
				miListenerGPS);

		// Debe notarse que en caso de que se obtenga la ubicaci�n, debe de
		// cancelarse la tarea de fin de tiempo de espera, evitando as� que se
		// indique que no hubo lecturas cuando en realidad esto no es cierto
	}

	/***
	 * Detiene las lecturas de GPS en el dispositivo.
	 */
	public void detenerLecturasGPS() {
		Log.i(Tag, "Deteniendo lecturas de GPS");
		miListenerGPS.setSensadoActivo(false);
		miLocManager.removeUpdates(miListenerGPS);
		// wakeLock.release();
	}

	// [end]

}
