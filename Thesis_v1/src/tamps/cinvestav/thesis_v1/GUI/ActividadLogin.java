package tamps.cinvestav.thesis_v1.GUI;

import tamps.cinvestav.thesis_v1.R;
import tamps.cinvestav.thesis_v1.model.Usuario;
import tamps.cinvestav.thesis_v1.services.Servicio;
import tamps.cinvestav.thesis_v1.services.Servicio.LocalBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ActividadLogin extends Activity {
	private static final long TIEMPO_ENTRE_LECTURAS_GPS = 4000;
	private static final long TIEMPO_MARGEN_LECTURAS_GPS = 60000;
	private static final double UMBRAL_DISTANCIA = 5.0;
	private final String tag = "ActividadLogin";

	static Servicio instanciaServicio = null;
	static boolean servicioEnlazado = false;

	public static Usuario USUARIO_ACTIVO = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		if (servicioEnlazado)
			((Button) findViewById(R.id.btnServicio_AL)).setEnabled(false);

	}

	public void click(View v) {
		// Click al Login
		if ((Button) (v) == (Button) findViewById(R.id.btnLogin_AL)) {
			try {
				// 1. Obtener input
				String username = ((EditText) findViewById(R.id.txtUsername_AL))
						.getText().toString();
				String password = ((EditText) findViewById(R.id.txtPassword_AL))
						.getText().toString();

				// 2. Validar input
				if (username.length() == 0 || password.length() == 0) {
					Toast.makeText(getApplicationContext(),
							"Error, Introduzca las credenciales de acceso",
							Toast.LENGTH_LONG).show();
				}

				// 3. Intentar hacer login...
				USUARIO_ACTIVO = instanciaServicio.hacerLogin(username,
						password);

				// 4. Si usuario = null, entonces hubo error al realizar el
				// login
				if (USUARIO_ACTIVO != null) {
					// Login exitoso
					Log.i("Cinvestav Actividad Login",
							USUARIO_ACTIVO.getNombre()
									+ " ha iniciado sesi—n en el sistema, pasando a ActividadCentral");
					Toast.makeText(
							getApplicationContext(),
							USUARIO_ACTIVO.getNombre()
									+ " ha iniciado sesi—n en el sistema",
							Toast.LENGTH_LONG).show();
					// TODO la sig linea comentada para mantenerme en la act
					// inicial
					// startActivity(new Intent(this, ActividadCentral.class));
				} else {
					// Login fallido
					Toast.makeText(
							getApplicationContext(),
							"Error al realizar login, datos de acceso incorrectos",
							Toast.LENGTH_LONG).show();
				}

			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), e.getMessage(),
						Toast.LENGTH_LONG).show();
				Log.e("Cinvestav Actividad Login", e.getMessage());
			}
		}

		// Click a registrar nuevo usuario
		if ((Button) (v) == (Button) findViewById(R.id.btnRegistrar_AL)) {
			Log.i("Cinvestav Actividad Login",
					"seleccionado Registrar nuevo usuario");

			startActivity(new Intent(this, ActividadRegistrarUsuario.class));
		}

		// Click a boton de llamado a servicio
		if ((Button) (v) == (Button) findViewById(R.id.btnServicio_AL)) {
			if (servicioEnlazado) {
				// Call a method from the LocalService.
				// However, if this call were something that might hang, then
				// this
				// request should
				// occur in a separate thread to avoid slowing down the activity
				// performance.
				//

				instanciaServicio.configurarAcelerometro(2000,
						SensorManager.SENSOR_DELAY_NORMAL, 4000);
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

				instanciaServicio.configurarGPS(TIEMPO_ENTRE_LECTURAS_GPS,
						TIEMPO_MARGEN_LECTURAS_GPS, UMBRAL_DISTANCIA);
				instanciaServicio.iniciarLecturaGPS();

			}
		}
		if ((Button) (v) == (Button) findViewById(R.id.btnIniciarTransmisor)) {
			if (servicioEnlazado) {
				instanciaServicio.configurarTransmisor(20000, 4, true);
				instanciaServicio.iniciarSincronizacionConLaNube();
			}
		}
		if ((Button) (v) == (Button) findViewById(R.id.btnDetenerTransmisor)) {
			if (servicioEnlazado) {
				instanciaServicio.detenerSincronizacionConLaNube();
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Bind to LocalService
		Intent intent = new Intent(this, Servicio.class);

		// La siguiente l’nea lo crea y lo mantiene encendido sin importar si la
		// aplicaci—n se "desconecta"
		// ComponentName myService =
		// if (instanciaServicio == null) {
		startService(new Intent(this, Servicio.class));
		bindService(intent, conexionAServicio, Context.BIND_AUTO_CREATE);
		// }
		Log.i(tag, "onResume");
	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindService(conexionAServicio);
		Log.i(tag, "onPause");
	}

	/***
	 * Define callbacks para el enlace del servicio, pasado a bindService()
	 */
	private ServiceConnection conexionAServicio = new ServiceConnection() {

		// @Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// Se ha realizado el enlace al servicio "Servicio", ahora se debe
			// hacer cast al IBinder y obtener la instancia de "Servicio"
			LocalBinder enlace = (LocalBinder) service;
			instanciaServicio = enlace.getService();
			servicioEnlazado = true;
			Log.i(tag, "Enlace al servicio realizado");
		}

		// @Override
		public void onServiceDisconnected(ComponentName arg0) {
			servicioEnlazado = false;
			Log.i(tag, "Enlace al servicio finalizado");
		}
	};

	protected void onDestroy() {
		super.onDestroy();
		if (servicioEnlazado) {
			// unbindService(conexionAServicio);
			// servicioEnlazado = false;
		}
	}
}