package tamps.cinvestav.thesis_v1.sal;

import java.util.Date;

import tamps.cinvestav.thesis_v1.model.RegistroGPS;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class ListenerUbicacion implements LocationListener {
	private RegistroGPS registroActual = null;

	private boolean sensadoActivo = false;

	public RegistroGPS getRegistroActual() {
		return registroActual;
	}

	public void setRegistroActual(RegistroGPS registroActual) {
		this.registroActual = registroActual;
	}

	public boolean isSensadoActivo() {
		return sensadoActivo;
	}

	public void setSensadoActivo(boolean sensadoActivo) {
		this.sensadoActivo = sensadoActivo;
	}

	private Handler handlerAdministradorUbicacion = null;
	private String tag = "ListenerUbicacion";

	public ListenerUbicacion(Handler handlerAdministradorUbicacion) {
		this.handlerAdministradorUbicacion = handlerAdministradorUbicacion;
		registroActual = new RegistroGPS();
		this.setSensadoActivo(false);
	}

	public void onLocationChanged(Location location) {
		// Se ha detectado un cambio en la ubicación...
		if (sensadoActivo) {
			float precision = location.getAccuracy();
			double latitud = location.getLatitude();
			double longitud = location.getLongitude();
			Log.i(tag, "Listener ubicacion " + latitud + ", " + longitud
					+ ", hasAccuracy " + location.hasAccuracy());
			synchronized (registroActual) {
				registroActual = new RegistroGPS(new Date(), latitud, longitud,
						precision);
			}
			handlerAdministradorUbicacion
					.sendEmptyMessage(AdministradorUbicacion.LECTURA_DISPONIBLE);
		}
	}

	public void onProviderDisabled(String provider) {
		// GPS deshabilitado
		// Toast.makeText(actividadPadre.getApplicationContext(),
		// "El GPS ha sido deshabilitado", Toast.LENGTH_LONG).show();
	}

	public void onProviderEnabled(String provider) {
		// El usuario ha habilitado el GPS
		// Toast.makeText(actividadPadre.getApplicationContext(),
		// "El GPS ha sido habilitado", Toast.LENGTH_LONG).show();

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// El proveedor de ubicación ha cambiado su estado:
		// Le ha sido imposible determinar la ubicación ó
		// Ha pasado a un modo de disponibilidad después de estar inactivo
		// durante un periodo de tiempo

	}

}