package tamps.cinvestav.thesis_v1.sal;

import java.util.Date;
import java.util.LinkedList;

import tamps.cinvestav.thesis_v1.model.RegistroAcelerometro;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/***
 * 
 * @author Rafael
 * 
 */
public class ListenerAcelerometro implements SensorEventListener {
	private LinkedList<RegistroAcelerometro> registros = null;
	private boolean sensadoActivo = false;

	/***
	 * Crea un nuevo objeto con una lista de registros vac’a (no nula)
	 */
	public ListenerAcelerometro() {
		this.registros = new LinkedList<RegistroAcelerometro>();
		this.sensadoActivo = false;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Called when the accuracy of a sensor has changed.
	}

	public void onSensorChanged(SensorEvent event) {
		// Called when sensor values have changed.
		if (sensadoActivo) {
			synchronized (registros) {
				registros.add(new RegistroAcelerometro(new Date(),
						event.values[0], event.values[1], event.values[2]));
			}
		}
	}

	public boolean isSensadoActivo() {
		return sensadoActivo;
	}

	public void setSensadoActivo(boolean sensadoActivo) {
		this.sensadoActivo = sensadoActivo;
	}

	public void resetListadoLecturas() {
		this.registros = new LinkedList<RegistroAcelerometro>();
	}

	public LinkedList<RegistroAcelerometro> getRegistros() {
		return registros;
	}
}
