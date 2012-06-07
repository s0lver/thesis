package tamps.cinvestav.thesis_v1.sal;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tamps.cinvestav.thesis_v1.model.RegistroAcelerometro;
import tamps.cinvestav.thesis_v1.services.Servicio;
import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

/***
 * 
 * @author Rafael
 * 
 */
public class AdministradorAcelerometro {

	public static final String Tag = "AdministradorAcelerometro";

	// [start] Sección para controlar la recalendarización de las lecturas del
	// acelerómetro.
	/***
	 * Clase que engloba los pasos realizados después de que el proceso de
	 * sensado ha sido finalizado (acelerometro). Deben de realizarse las
	 * modificaciones pertinentes
	 * 
	 * @author Rafael
	 * 
	 */
	private class TareaRecalendarizarLecturas extends TimerTask {

		@Override
		public void run() {
			// listenerAc.setSensadoActivo(false);
			// for a real device
			sm.unregisterListener(listenerAc);

			// sm.unregisterListener((SensorEventListener) listenerAc);

			LinkedList<RegistroAcelerometro> historial = listenerAc
					.getRegistros();
			servicio.setRegistrosAcelerometro(listenerAc.getRegistros());
			handlerServicio.sendEmptyMessage(LECTURAS_ACELEROMETRO_FINALIZADAS);
			listenerAc.resetListadoLecturas();
			Log.i(Tag,
					"TareaRecalendarizarLecturas Se ha 'desregistrado' al sensor acelerometro'");

			if (listenerAc.isSensadoActivo()) {
				// TODO Lógica de recalendarización aquí
				/*
				 * LÓGICA DE RECALENDARIZACION AQUI delayProximaEjecucion =
				 * Resultado de cálculos de recalendarizacion (si aplica)
				 */
				long delayProximaEjecucion = obtenerDelaySiguienteLectura(historial);

				// Y al finalizar, ...

				if (timerAuxiliar != null)
					timerAuxiliar.cancel();
				TareaAuxiliar tareaAuxiliar = new TareaAuxiliar();
				timerAuxiliar = new Timer();
				timerAuxiliar.schedule(tareaAuxiliar, delayProximaEjecucion);
				Log.i(Tag,
						"TareaRecalendarizarLecturas Se ha vuelto a recalendarizar dentro de "
								+ delayProximaEjecucion + " milisegundos");
			} else {
				Log.i(Tag,
						"TareaRecalendarizarLecturas No se ha vuelto a recalendarizar, sensadoActivo = false");
			}
		}
	};

	private long obtenerDelaySiguienteLectura(
			LinkedList<RegistroAcelerometro> historial) {
		return intervaloMuestreo;
	}

	/***
	 * Clase que funciona como herramienta para recalendarizar las tareas de
	 * sensado
	 * 
	 * @author Rafael
	 * 
	 */
	private class TareaAuxiliar extends TimerTask {

		@Override
		public void run() {
			Log.i(Tag,
					"TareaAuxiliar, metodo run: Llamando a iniciarLecturasAcelerometro");
			iniciarLecturasAcelerometro();

		}
	};

	// [end]

	// [start] Atributos, setters + getters, constructor
	/***
	 * Referencia al servicio 'padre' que crea a este AdministradorAcelerometro
	 */
	private Servicio servicio = null;

	/***
	 * Utilizada para registrarse como listener del sensor. Es enviada como
	 * argumento desde el servicio.
	 */
	private Context contexto = null;

	/***
	 * Referencia al handler del servicio para enviarle mensajes relacionados
	 * con la toma de valores del sensor acelerómetro.
	 */
	private Handler handlerServicio = null;

	/***
	 * Timer para llevar a cabo la tarea de sensado del acelerometro
	 */
	private Timer timerAcelerometro = null;

	/***
	 * Timer auxiliar para recalendarizar la tarea
	 */
	private Timer timerAuxiliar = null;

	/***
	 * Atributo que almacena la cantidad en milisegundos que el sensor estará
	 * monitoreando los cambios.
	 */
	private long tiempoMuestreo = 0;

	/***
	 * Atributo que se refiere a la frecuencia de muestreo con la cual se
	 * levantarán los datos del sensor.
	 */
	private int frecMuestreo = 0;

	/***
	 * Atributo que indica el periodo de tiempo que existirá entre cada
	 * intervención de lectura de acelerómetro.
	 */
	private long intervaloMuestreo = 0;

	/***
	 * Enlace con el administrador de sensores de la plataforma.
	 */
	private SensorManager sm = null;

	/***
	 * Un objeto ListenerAcelerometro con el cual se detectarán los cambios del
	 * sensor
	 */
	private ListenerAcelerometro listenerAc = null;

	/***
	 * Listado temporal y auxiliar de los sensores que hayan sido detectados
	 */
	private List<Sensor> sensoresAcelerometro = null;

	public long getTiempoMuestreo() {
		return tiempoMuestreo;
	}

	public void setTiempoMuestreo(long tiempoMuestreo) {
		this.tiempoMuestreo = tiempoMuestreo;
	}

	public int getFrecMuestreo() {
		return frecMuestreo;
	}

	public void setFrecMuestreo(int frecMuestreo) {
		if (frecMuestreo != SensorManager.SENSOR_DELAY_FASTEST
				&& frecMuestreo != SensorManager.SENSOR_DELAY_GAME
				&& frecMuestreo != SensorManager.SENSOR_DELAY_UI
				&& frecMuestreo != SensorManager.SENSOR_DELAY_NORMAL)
			throw new IllegalArgumentException(
					"La frecuencia de muestreo no se encuentra soportada (FASTEST | GAME | UI | NORMAL)");
		this.frecMuestreo = frecMuestreo;
	}

	public long getIntervaloMuestreo() {
		return intervaloMuestreo;
	}

	public void setIntervaloMuestreo(long intervaloMuestreo) {
		this.intervaloMuestreo = intervaloMuestreo;
	}

	/***
	 * Constante que indica que las lecturas de acelerómetro han sido
	 * finalizadas
	 */
	public static final int LECTURAS_ACELEROMETRO_FINALIZADAS = 1;

	/***
	 * Constructor del AdministradorAcelerometro
	 * 
	 * @param servicio
	 *            Referencia al servicio 'padre' que crea a este
	 *            AdministradorAcelerometro
	 * @param handlerServicio
	 *            El handler proveniente del servicio que recibirá la
	 *            notificación de que la lectura ha finalizado.
	 * @param tiempoMuestreo
	 *            La cantidad de milisegundos que será monitoreada en cada
	 *            intervención
	 * @param frecMuestreo
	 *            La frecuencia con la cual se detectarán los cambios en el
	 *            sensor
	 * @param intervaloMuestreo
	 *            El periodo de tiempo entre cada mini-ciclo de lecturas del
	 *            sensor
	 */
	public AdministradorAcelerometro(Servicio servicio) {
		super();
		this.servicio = servicio;
		this.contexto = servicio.getApplicationContext();
		// this.contexto = this.contexto;
		this.handlerServicio = servicio.getHandlerAcelerometro();

		sm = (SensorManager) contexto.getSystemService(Service.SENSOR_SERVICE);

		listenerAc = new ListenerAcelerometro();
		timerAcelerometro = new Timer();

		sensoresAcelerometro = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);

	}

	/***
	 * Determina si existe al menos un sensor acelerómetro embebido en el
	 * dispositivo
	 * 
	 * @return si existe un acelerómetro o no (true, false)
	 */
	public boolean existeAcelerometro() {
		if (sensoresAcelerometro.size() > 0)
			return true;
		return false;
	}

	// [end]

	// [start] Mecanismos para iniciar-detener las lecturas del acelerómetro
	/***
	 * Inicia las lecturas del acelerómetro durante el tiempo y la frecuencia de
	 * muestreo especificados.
	 * 
	 * Nótese que se calendariza una TimerTask (tareaLeerAcelerometro) para el
	 * tiempo en el que hayan finalizado los microsegundos de lectura
	 * estipulados en el atributo tiempoMuestreo
	 * 
	 * @return Un valor booleano que indica si se pudo registrar al sensor
	 *         acelerómetro para el proceso de lecturas
	 */
	public boolean iniciarLecturasAcelerometro() {
		if (!existeAcelerometro()) {
			throw new RuntimeException(
					"No se encontraron acelerómetros en el dispositivo");
		}
		timerAcelerometro.cancel();
		timerAcelerometro = new Timer();
		TareaRecalendarizarLecturas tareaRecalendarizarLecturas = new TareaRecalendarizarLecturas();

		boolean sensorAcRegistrado = sm.registerListener(listenerAc,
				sensoresAcelerometro.get(0), frecMuestreo);

		if (sensorAcRegistrado) {
			listenerAc.setSensadoActivo(true);

			listenerAc.resetListadoLecturas();
			timerAcelerometro.schedule(tareaRecalendarizarLecturas,
					tiempoMuestreo);
		}
		return sensorAcRegistrado;
	}

	public void detenerLecturasAcelerometro() {
		timerAcelerometro.cancel();
		listenerAc.setSensadoActivo(false);

		sm.unregisterListener(listenerAc);

	}
	// [end]
}
