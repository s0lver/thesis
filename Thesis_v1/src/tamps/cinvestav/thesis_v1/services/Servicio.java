/*
 *C—mo utilizar este servicio?
 *Servicio instanciaServicio;
	boolean servicioEnlazado = false;
	
	if (servicioEnlazado) {
				// Call a method from the LocalService.
				// However, if this call were something that might hang, then
				// this
				// request should
				// occur in a separate thread to avoid slowing down the activity
				// performance.
				boolean sePudo = instanciaServicio
						.iniciarLecturasAcelerometro();
				if (sePudo)
					Toast.makeText(getApplicationContext(),
							"Si se pudieron iniciar las lecturas",
							Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(getApplicationContext(),
							"No se pudieron iniciar las lecturas",
							Toast.LENGTH_SHORT).show();

			}
			
	C—mo registrarlo?
	En una actividad hay que sobreescribir ciertos mŽtodos
	
	@Override
	protected void onStart() {
		super.onStart();
		// Bind to LocalService
		Intent intent = new Intent(this, Servicio.class);

		// La siguiente l’nea lo crea y lo mantiene encendido sin importar si la
		// aplicaci—n se "desconecta"
		ComponentName myService = startService(new Intent(this, Servicio.class));
		bindService(intent, conexionAServicio, Context.BIND_AUTO_CREATE);
	}

 ***
 * Define callbacks para el enlace del servicio, pasado a bindService()
 *
	private ServiceConnection conexionAServicio = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// Se ha realizado el enlace al servicio "Servicio", ahora se debe
			// hacer cast al IBinder y obtener la instancia de "Servicio"
			LocalBinder enlace = (LocalBinder) service;
			instanciaServicio = enlace.getService();
			servicioEnlazado = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			servicioEnlazado = false;
		}
	};

	protected void onDestroy() {
		super.onDestroy();
		if (servicioEnlazado) {
			unbindService(conexionAServicio);
			servicioEnlazado = false;
		}
	}
 * 
 */

package tamps.cinvestav.thesis_v1.services;

// TODO Verificar: Que cuando no se requieran lecturas de GPS, el darle click al bot—n no quede en un 'buffer' de manera que la siguiente lectura no sea actualizada (sabes a lo que me refiero) (y Si lo regresas a LInked List y solo tomas la ultima ~.)
// TODO despues de checar lo anterior: Ver que se GENEREN y ENVIEN los archivos.!
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONException;

import tamps.cinvestav.thesis_v1.R;
import tamps.cinvestav.thesis_v1.GUI.ActividadLogin;
import tamps.cinvestav.thesis_v1.dal.DalUsuarios;
import tamps.cinvestav.thesis_v1.model.Registro;
import tamps.cinvestav.thesis_v1.model.RegistroAcelerometro;
import tamps.cinvestav.thesis_v1.model.RegistroGPS;
import tamps.cinvestav.thesis_v1.model.Usuario;
import tamps.cinvestav.thesis_v1.sal.AdministradorAcelerometro;
import tamps.cinvestav.thesis_v1.sal.AdministradorUbicacion;
import tamps.cinvestav.thesis_v1.tools.Utilerias;
import tamps.cinvestav.thesis_v1.transmission.Transmisor;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/***
 * 
 * @author Rafael
 * 
 */
public class Servicio extends Service {

	// [start] Atributos Globales / auxiliares...
	private final static String Tag = "Servicio";

	/***
	 * Mantiene el registro de los datos obtenidos de TODOS los sensores
	 */
	private LinkedList<Registro> registrosListadoGlobal = new LinkedList<Registro>();

	/***
	 * Auxiliar para el nombrado de los archivos creados
	 */
	private SimpleDateFormat formatoFecha = new SimpleDateFormat(
			"yyyy-MM-dd-HH-mm-ss");
	// [end]

	// [start] MŽtodos / interfaz cl‡sica del servicio

	/***
	 * ID del servicio
	 */
	private final static int myID = 1234;

	/***
	 * Binder entregado a los clientes
	 */
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		public Servicio getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return Servicio.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		registrosGPS = new LinkedList<RegistroGPS>();
		adminUbicacion = new AdministradorUbicacion(this);
		adminAcelerometro = new AdministradorAcelerometro(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// The intent to launch when the user clicks the expanded notification
		Intent intent2 = new Intent(this, ActividadLogin.class); // ActividadLogin
																	// or
																	// any other
		intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent2,
				0);

		// This constructor is deprecated. Use Notification.Builder instead
		Notification notice = new Notification(R.drawable.ic_launcher,
				"Servicio-middleware iniciado", System.currentTimeMillis());

		// This method is deprecated. Use Notification.Builder instead.
		notice.setLatestEventInfo(this, "Servicio-middleware",
				"El servicio del middleware ha sido iniciado correctamente",
				pendIntent);

		notice.flags |= Notification.FLAG_NO_CLEAR;
		startForeground(myID, notice);

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(this.getClass().getName(), "UNBIND");
		return true;
	}

	// [end] MŽtodos / interfaz cl‡sica del servicio

	// [start] Secci—n de administraci—n del GPS
	public static long INTERVALO_GPS = 60000;
	public LinkedList<RegistroGPS> registrosGPS = null;

	public LinkedList<RegistroGPS> getRegistrosGPS() {
		return registrosGPS;
	}

	public void setRegistrosGPS(LinkedList<RegistroGPS> registrosGPS) {
		this.registrosGPS = registrosGPS;
	}

	private AdministradorUbicacion adminUbicacion = null;

	private Handler handlerUbicacion = new Handler() {
		public void handleMessage(Message msj) {
			switch (msj.what) {
			case AdministradorUbicacion.LECTURA_NO_DISPONIBLE: {
				Log.w(Tag,
						"No se pudo obtener la ubicaci—n GPS en este instante "
								+ registrosGPS.getLast());
			}
				break;
			case AdministradorUbicacion.LECTURA_DISPONIBLE: {
				// Log.i(Tag, "Se obtuvo una ubicaci—n: " +
				// registrosGPS.element());

				Log.i(Tag, "Se obtuvo una ubicaci—n: " + registrosGPS.getLast());
				// Fusionar a lista temporal con la lista global
				// copiarAListaGlobal(LISTA_GPS);
			}
				break;
			}
			copiarAListaGlobal(LISTA_GPS);
		}
	};

	public Handler getHandlerUbicacion() {
		return handlerUbicacion;
	}

	public void configurarGPS(long delayEntreLecturas,
			long margenTiempoLecturas, double umbralDistancia) {
		adminUbicacion.setDelayEntreLecturas(delayEntreLecturas);
		adminUbicacion.setMargenTiempoLecturas(margenTiempoLecturas);
		adminUbicacion.setUmbralDistancia(umbralDistancia);
		// administradorUbicacion = new AdministradorUbicacion(this,
		// tiempoEntreLecturas);
	}

	public void iniciarLecturaGPS() {
		if (adminUbicacion == null)
			throw new IllegalStateException(
					"AdministradorUbicacion es null, se llam— a configurarGPS?");
		adminUbicacion.iniciarLecturasGPS();
	}

	public void detenerLecturasGPS() {
		adminUbicacion.detenerLecturasGPS();
	}

	// [end]

	// [start] Secci—n de administraci—n del sensor Aceler—metro
	/***
	 * Almacena la lista de valores recogidos del acelerometro
	 */
	public LinkedList<RegistroAcelerometro> registrosAcelerometro = null;

	public LinkedList<RegistroAcelerometro> getRegistrosAcelerometro() {
		return registrosAcelerometro;
	}

	public void setRegistrosAcelerometro(
			LinkedList<RegistroAcelerometro> registrosAcelerometro) {
		this.registrosAcelerometro = registrosAcelerometro;
	}

	/***
	 * Enlace con el aceler—metro
	 */
	private AdministradorAcelerometro adminAcelerometro = null;

	/***
	 * Handler que recibe los mensajes procedentes del aceler—metro (p.ej.
	 * LECTURAS_ACELEROMETRO_FINALIZADAS)
	 */
	private Handler handlerAcelerometro = new Handler() {
		public void handleMessage(Message msj) {
			switch (msj.what) {
			case AdministradorAcelerometro.LECTURAS_ACELEROMETRO_FINALIZADAS: {
				Log.i("Cinvestav",
						"Tama–o lecturas " + registrosAcelerometro.size());

				// Fusionar a lista temporal con la lista global
				copiarAListaGlobal(LISTA_ACELEROMETRO);

			}
				break;
			}
		}
	};

	public Handler getHandlerAcelerometro() {
		return handlerAcelerometro;
	}

	/***
	 * Configura el administrador del acelerometro para llevar a cabo las tareas
	 * de lectura de una forma adecuada.
	 * 
	 * @param tiempoMuestreo
	 *            El tiempo (milisegundos) en que se leer‡n datos del sensor.
	 * @param frecuenciaMuestreo
	 *            La frecuencia con la que se levantar‡n datos del sensor en
	 *            cada intervenci—n. @see SensorManager.SENSOR_DELAY_FASTEST,
	 *            SensorManager.SENSOR_DELAY_GAME,
	 *            SensorManager.SENSOR_DELAY_UI,
	 *            SensorManager.SENSOR_DELAY_NORMAL)
	 * @param intervaloMuestreo
	 *            El periodo de tiempo (milisegundos) entre cada intervenci—n
	 */
	public void configurarAcelerometro(long tiempoMuestreo,
			int frecuenciaMuestreo, long intervaloMuestreo) {
		adminAcelerometro.setTiempoMuestreo(tiempoMuestreo);
		adminAcelerometro.setFrecMuestreo(frecuenciaMuestreo);
		adminAcelerometro.setIntervaloMuestreo(intervaloMuestreo);
		// adminAcelerometro = new AdministradorAcelerometro(this,
		// tiempoMuestreo,
		// frecuenciaMuestreo, intervaloMuestreo);
	}

	/***
	 * Provoca que el servicio inicie las lecturas de acelerometro
	 * 
	 * @return true / false dependiendo de si se han podido iniciar las lecturas
	 */
	public boolean iniciarLecturasAcelerometro() {
		return adminAcelerometro.iniciarLecturasAcelerometro();
	}

	public void detenerLecturasAcelerometro() {
		adminAcelerometro.detenerLecturasAcelerometro();
	}

	// [end] Administraci—n del aceler—metro

	// [start] Administraci—n de usuarios

	/***
	 * Auxiliar en la administracion de usuarios
	 */
	private static DalUsuarios capaUsuarios = new DalUsuarios();

	/***
	 * El usuario 'activo' en el servicio / middleware. Esta referencia es
	 * utilizada para generar y almacenar los datos de los sensores en 'la nube'
	 */
	private Usuario usuarioActivo = null;

	public Usuario getUsuarioActivo() {
		return usuarioActivo;
	}

	public void setUsuarioActivo(Usuario usuarioActivo) {
		this.usuarioActivo = usuarioActivo;
	}

	/***
	 * Realiza un login al sistema dados un nombre de usuario y una contrase–a
	 * 
	 * @param username
	 *            El nombre del usuairo
	 * @param password
	 *            El password del usuario
	 * @return El usuario registrado en el sistema. Null en caso de error de
	 *         acceso (username/password inv‡lidos)
	 * @throws IOException
	 * @throws JSONException
	 * @throws ParseException
	 */
	public Usuario hacerLogin(String username, String password)
			throws IOException, JSONException, ParseException {
		usuarioActivo = capaUsuarios.obtenerUsuarioPorUsernameYContrasenia(
				username, password);
		return usuarioActivo;
	}

	/***
	 * Registra (agrega) un usuario al sistema.
	 * 
	 * @param nombreUsuario
	 *            Nombre del usuario a agregar
	 * @param username
	 *            Nombre de usuario (identificador, login) para llevar a cabo el
	 *            registro.
	 * @param password
	 *            Contrase–a del usuario
	 * @return El objeto usuario que acaba de ser agregado. En el campo id se
	 *         incluye el id del registro a–adido. Retorna
	 *         Utilerias.INT_USUARIO_EXISTE en caso de que el usuario (username)
	 *         se haya registrado previamente en el sistema.
	 * @throws IOException
	 */
	public Usuario registrarUsuario(String nombreUsuario, String username,
			String password) throws IOException {
		Usuario usuario = new Usuario(nombreUsuario, username, password,
				new Date());
		int idGenerado = capaUsuarios.agregarUsuario(usuario);
		usuario.setId(idGenerado);
		return usuario;
	}

	// [end]Administraci—n de usuarios

	// [start] Secci—n de administraci—n de la transmisi—n de los datos

	private final static int LISTA_ACELEROMETRO = 1;
	private final static int LISTA_GPS = 2;
	private final static int LISTA_TODAS = 0;

	/***
	 * Define la cantidad m‡xima de registros que pueden existir en la lista
	 * global antes de que sean mapeados a un archivo en el dispositivo de
	 * almacenamiento 'masivo' del smartphone.Þ
	 */
	private final static int UMBRAL_REGISTROS = 12;

	/***
	 * Transmisor que se encargar‡ de varias tareas en el servicio: <br />
	 * - Verificar peri—dicamente la disponibilidad de una red WiFi <br />
	 * - Enviar archivos hacia el servidor destino <br />
	 * - Etc ...
	 */
	private Transmisor transmisor = null;

	private Queue<String> listadoArchivos = new LinkedList<String>();
	/***
	 * Handler que recibe los mensajes procedentes del transmisor.
	 */
	private Handler handlerTransmisor = new Handler() {
		public void handleMessage(Message msj) {
			switch (msj.what) {
			case Utilerias.INT_RED_WIFI_DISPONIBLE:
				// Se ha recibido el mensaje desde el transmisor de que hay una
				// red wifi disponible y con conexi—n. Se procede a enviar los
				// archivos presentes en la lista en una ventana de tama–o N
				// (Top N archivos presentes en la lista).
				try {
					Log.i(Tag,
							"HandlerTransmisor Msj recibido desde transmisor, red WiFi disponible");
					// transmisor.enviarArchivo();
					transmisor.enviarArchivos(listadoArchivos);
				} catch (IOException e) {
					Log.e(Tag, "Ocurrio un error al enviar los archivos");
					e.printStackTrace();
				}
				break;
			case Utilerias.INT_RED_WIFI_NO_DISPONIBLE:
			default:
				break;
			}
		};
	};

	public Handler getHandlerTransmisor() {
		return handlerTransmisor;
	}

	/***
	 * Inicia el proceso de monitoreo - sincronizaci—n de datos con la nube
	 */
	public void iniciarSincronizacionConLaNube() {
		Log.i(Tag, "Se ha dado inicio a la sincronizaci—n con la nube");
		transmisor.iniciarMonitorizacionRed();
	}

	/***
	 * Detiene el proceso de monitoreo - sincronizaci—n de datos con la nube
	 */
	public void detenerSincronizacionConLaNube() {
		transmisor.detenerMonitorizacionRed();
		Log.w(Tag, "Se ha detenido la sincronizaci—n con la nube");
	}

	/***
	 * MŽtodo que 'funde' la lista especificada como argumento con la lista
	 * global de registros generados. Autom‡ticamente verifica que no se exceda
	 * de un umbral de registros. En caso de serlo, se genera un archivo XML con
	 * los datos actuales y se 'reinicia' la lista
	 * 
	 * @param cual
	 *            La lista que se va a fusionar con la lista global.<br />
	 *            <b>LISTA_ACELEROMETRO</b> = 1,<br/>
	 *            <b>LISTA_GPS</b> = 2,<br/>
	 *            <b>LISTA_TODAS</b> = 0;
	 */
	private void copiarAListaGlobal(int cual) {
		synchronized (registrosListadoGlobal) {
			if (cual == LISTA_ACELEROMETRO) {
				synchronized (registrosAcelerometro) {
					for (RegistroAcelerometro ra : registrosAcelerometro) {
						registrosListadoGlobal.add(ra);
					}
					registrosAcelerometro.clear();
				}

			} else if (cual == LISTA_GPS) {
				synchronized (registrosGPS) {
					for (RegistroGPS rg : registrosGPS) {
						registrosListadoGlobal.add(rg);
					}
					registrosGPS.clear();
				}
			} else if (cual == LISTA_TODAS) {
				synchronized (registrosAcelerometro) {
					for (RegistroAcelerometro ra : registrosAcelerometro) {
						registrosListadoGlobal.add(ra);
					}
					registrosAcelerometro.clear();
				}
				synchronized (registrosGPS) {
					for (RegistroGPS rg : registrosGPS) {
						registrosListadoGlobal.add(rg);
					}
					registrosGPS.clear();
				}
			}
		}
		// DespuŽs de haber agregado los registros a la lista global, hay
		// que verificar que sea o no necesario crear algœn archivo para
		// vaciar la lista.
		generarArchivos();
	}

	/***
	 * Comprueba que la cantidad de registros de la lista global no sobrepasen
	 * cierto umbral. En caso de ser una cantidad superior, vac’a el contenido a
	 * un archivo XML con un nombre similar a:
	 * "middleware-2012-12-31-14-59-59.xml"
	 */
	private void generarArchivos() {
		synchronized (registrosListadoGlobal) {
			if (registrosListadoGlobal.size() >= UMBRAL_REGISTROS) {
				String nombreArchivo = "middleware-"
						+ formatoFecha.format(new Date()) + ".xml";
				String textoXML = Utilerias.crearXml(registrosListadoGlobal);
				Utilerias.escribirCadenaEnArchivo(textoXML, nombreArchivo);
				registrosListadoGlobal.clear();
				listadoArchivos.add(nombreArchivo);
				Log.i(Tag, "Generando archivo " + nombreArchivo);
			}
		}
	}

	/***
	 * Conigura el transmisor de los datos. ƒste es el elemento que se encarga
	 * de llevar a cabo el env’o de los datos a la nube
	 * 
	 * @param intervaloMonitorizacionRed
	 *            Indica cada cu‡nto se estar‡ verificando la existencia de la
	 *            red WiFi para llevar a cabo el env’o de los archivos XML.
	 * @param tamanyoVentanaArchivos
	 *            Indica la cantidad de archivos que ser‡n enviados a la nube
	 *            durante cada intervenci—n. Utilerias.INT_TODOS_LOS_ARCHIVOS
	 *            env’a todos los archivos presentes en la lista.
	 * @param mantenerArchivos
	 *            Indica si los archivos creados son mantenidos o no después de
	 *            ser enviados a la nube
	 */
	public void configurarTransmisor(long intervaloMonitorizacionRed,
			int tamanyoVentanaArchivos, boolean mantenerArchivos) {
		transmisor = new Transmisor(this, intervaloMonitorizacionRed,
				tamanyoVentanaArchivos, mantenerArchivos);
	}
	// [end] Administraci—n del Transmisor
}
